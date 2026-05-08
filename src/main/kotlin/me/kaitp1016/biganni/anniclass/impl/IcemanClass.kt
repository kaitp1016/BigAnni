package me.kaitp1016.biganni.anniclass.impl

import com.destroystokyo.paper.event.server.ServerTickStartEvent
import me.kaitp1016.biganni.anniclass.AnniClass
import me.kaitp1016.biganni.utils.ItemUtils.getAnniId
import me.kaitp1016.biganni.utils.ItemUtils.setAnniItem
import me.kaitp1016.biganni.utils.MCUtils.toMC
import me.kaitp1016.biganni.utils.Utils.toIntCorrect
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.minecraft.core.BlockPos
import net.minecraft.world.item.Items
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Blocks
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.ItemStack

object IcemanClass: AnniClass(), Listener {
    override val icon = Items.ICE
    override val name = "Iceman"
    override val deathMessageName = "ICM"
    override val description = arrayOf(
        "この職業の時、自身の周りの水が凍るようになる。",
        "アビリティを使用することで能力を切り替えることができる。",
    )

    const val TOGGLE_FROST_WALKER_ITEM_ID = "iceman_toggle_frost_walker"

    override fun getDefaultItems(player: Player): MutableList<ItemStack> {
        return super.getDefaultItems(player).also {
            it.add(ItemStack(Material.WOODEN_SHOVEL).uniqueClassItem().soulbound())

            it.removeIf { it.type == Material.WOODEN_SWORD }
            it.add(ItemStack(Material.STONE_SWORD).uniqueClassItem().soulbound())

            it.add(ItemStack(Material.ICE).apply {
                uniqueClassItem()
                soulbound()
                setAnniItem(TOGGLE_FROST_WALKER_ITEM_ID)

                editMeta {
                    it.itemName(Component.text("Toggle Frost Walker").color(NamedTextColor.GOLD))
                }
            })

            it.add(ItemStack(Material.LILY_PAD).uniqueClassItem().soulbound().also { item -> item.amount = 10 })
        }
    }

    data class FrostIce(val level: Level, val pos: BlockPos,var tick:Int = 20)

    val enabledPlayers = mutableListOf<Player>()
    val ices = mutableListOf<FrostIce>()

    override fun onUnselect(player: Player) {
        super.onUnselect(player)
        enabledPlayers.remove(player)
    }

    @EventHandler
    fun onInteract(event: PlayerInteractEvent) {
        val player = event.player
        if (!isSelected(player)) return

        val item = event.item ?: return
        if (item.getAnniId() != TOGGLE_FROST_WALKER_ITEM_ID) return

        if (enabledPlayers.contains(player)) {
            enabledPlayers.remove(player)
            player.sendMessage(Component.text("氷渡りを無効にしました").color(NamedTextColor.GREEN))
        }
        else {
            enabledPlayers.add(player)
            player.sendMessage(Component.text("氷渡りを有効にしました").color(NamedTextColor.GREEN))
        }

        player.playSound(player, Sound.UI_BUTTON_CLICK,1f,1f)

        event.isCancelled = true
    }

    @EventHandler
    fun onTick(event: ServerTickStartEvent) {
        Bukkit.getOnlinePlayers().forEach { player ->
            if (!enabledPlayers.contains(player) || !isSelected(player)) return@forEach

            val level = player.world.toMC()
            val pos = BlockPos(player.x.toInt(), (player.y - 0.999).toIntCorrect(), player.z.toInt())

            for (dx in -2..2) {
                for (dz in -2..2) {
                    val pos = pos.offset(dx, 0, dz)
                    val block = level.getBlockState(pos)
                    if (block.block == Blocks.WATER && block.fluidState.isSource && level.getBlockState(pos.offset(0, 1, 0)).isAir) {
                        level.setBlockAndUpdate(pos, Blocks.FROSTED_ICE.defaultBlockState())
                        ices.add(FrostIce(level, pos))
                    }
                    if (block.block == Blocks.FROSTED_ICE) {
                        ices.find { it.pos == pos }?.tick = 20
                        level.setBlockAndUpdate(pos, Blocks.FROSTED_ICE.defaultBlockState())
                    }
                }
            }
        }

        if (!ices.isEmpty()) {
            ices.removeAll { ice ->
                ice.tick--

                if (ice.tick < 1) {
                    if (ice.level.getBlockState(ice.pos).block == Blocks.FROSTED_ICE) ice.level.setBlockAndUpdate(ice.pos, Blocks.WATER.defaultBlockState())
                    return@removeAll true
                }

                false
            }
        }
    }
}