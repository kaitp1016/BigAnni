package me.kaitp1016.biganni.anniclass.impl

import com.destroystokyo.paper.event.server.ServerTickStartEvent
import io.papermc.paper.datacomponent.DataComponentTypes
import io.papermc.paper.datacomponent.item.UseCooldown
import me.kaitp1016.biganni.PLUGIN_ID
import me.kaitp1016.biganni.anniclass.AnniClass
import me.kaitp1016.biganni.utils.ItemUtils.getAnniId
import me.kaitp1016.biganni.utils.ItemUtils.setAnniItem
import me.kaitp1016.biganni.utils.MCUtils.toMC
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.minecraft.core.BlockPos
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket
import net.minecraft.tags.BlockTags
import net.minecraft.world.item.Items
import net.minecraft.world.level.block.Blocks
import org.bukkit.*
import org.bukkit.block.Block
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.ItemStack

object DasherClass: AnniClass(), Listener {
    override val name = "Dasher"
    override val deathMessageName = "DSR"
    override val icon = Items.PURPLE_DYE
    override val description = arrayOf(
        "アビリティを使用することで視点の先にテレポートができる。",
        "アビリティを使用するためにはスニークする必要がある。"
    )

    const val BLINK_ITEM_ID = "dasher_blink"
    const val BLINK_COOLDOWN = 200
    val BLINK_COOLDOWN_GROUP = Key.key(PLUGIN_ID,"dasher_blink")

    override fun getDefaultItems(player: Player): MutableList<ItemStack> {
        return super.getDefaultItems(player).also {
            it.add(ItemStack(Material.PURPLE_DYE).apply {
                uniqueClassItem()
                soulbound()
                setAnniItem(BLINK_ITEM_ID)

                setData(DataComponentTypes.USE_COOLDOWN, UseCooldown.useCooldown(BLINK_COOLDOWN / 20f).cooldownGroup(BLINK_COOLDOWN_GROUP).build())

                editMeta {
                    it.itemName(Component.text("Dasher").color(NamedTextColor.GOLD))
                }
            })
        }
    }

    val previewPos = mutableListOf<Pair<Player, BlockPos>>()

    @EventHandler
    fun onInteract(event: PlayerInteractEvent) {
        val player = event.player
        if (!player.isSneaking || !isSelected(player)) return

        val item = event.item ?: return
        if (item.getAnniId() != BLINK_ITEM_ID || player.hasCooldown(item)) return

        val pos = player.world.rayTraceBlocks(player.eyeLocation,player.location.direction,30.0, FluidCollisionMode.NEVER,true)?.hitBlock ?: return
        if (!canTeleport(pos)) return

        val distance = pos.location.distance(player.location)
        val delta = player.location.clone().subtract(pos.location).subtract(0.5,1.0,0.5).multiply(-1.0 / distance / 2)

        repeat((distance * 2).toInt()) {
            val pos = player.eyeLocation.clone().add(delta.clone().multiply(it.toDouble()))

            Particle.END_ROD.builder()
                .location(pos)
                .count(0)
                .offset(0.0,0.0,0.0)
                .receivers(64,true)
                .spawn()
        }

        val cooldown = pos.location.distance(player.location).toInt() * 20 + 200

        val location = Location(pos.world,pos.x + 0.5,pos.y + 1.0,pos.z + 0.5,player.yaw,player.pitch)
        player.teleport(location)
        player.playSound(player, Sound.ENTITY_ENDERMAN_TELEPORT,1f,1f)

        item.setData(DataComponentTypes.USE_COOLDOWN, UseCooldown.useCooldown(cooldown / 20f).cooldownGroup(BLINK_COOLDOWN_GROUP).build())
        player.setCooldown(BLINK_COOLDOWN_GROUP,cooldown)
    }

    @EventHandler
    fun onTick(event: ServerTickStartEvent) {
        if (previewPos.isNotEmpty()) {
            previewPos.forEach { (player,pos) ->
                val player = player.toMC()
                player.connection.send(ClientboundBlockUpdatePacket(pos,player.level().getBlockState(pos)))
            }

            previewPos.clear()
        }

        Bukkit.getOnlinePlayers().forEach { player ->
            if (!player.isSneaking || !isSelected(player) || player.inventory.itemInMainHand.getAnniId() != BLINK_ITEM_ID) return@forEach

            val pos = player.world.rayTraceBlocks(player.eyeLocation,player.location.direction,30.0, FluidCollisionMode.NEVER,true)?.hitBlock ?: return@forEach
            val block = if (canTeleport(pos)) Blocks.DIAMOND_BLOCK else Blocks.REDSTONE_BLOCK

            val blockPos = BlockPos(pos.x,pos.y,pos.z)
            player.toMC().connection.send(ClientboundBlockUpdatePacket(blockPos,block.defaultBlockState()))

            previewPos.add(Pair(player, blockPos))
        }
    }

    fun canTeleport(block: Block): Boolean {
        val world = block.world
        val underBlock = world.getBlockAt(block.x,block.y,block.z)
        if (underBlock.type == Material.GLASS || underBlock.type == Material.BRICKS) return false

        val feetBlock = world.getBlockAt(block.x,block.y + 1,block.z)
        val chestBlock = world.getBlockAt(block.x,block.y + 2, block.z)
        return feetBlock.isPassable && !feetBlock.isLiquid && chestBlock.isPassable && !feetBlock.isLiquid
    }
}