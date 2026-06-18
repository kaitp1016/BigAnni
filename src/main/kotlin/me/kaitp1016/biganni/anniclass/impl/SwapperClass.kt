package me.kaitp1016.biganni.anniclass.impl

import com.destroystokyo.paper.event.server.ServerTickStartEvent
import io.papermc.paper.datacomponent.DataComponentTypes
import io.papermc.paper.datacomponent.item.UseCooldown
import me.kaitp1016.biganni.PLUGIN_ID
import me.kaitp1016.biganni.anniclass.AnniClass
import me.kaitp1016.biganni.utils.FallDamageResistance
import me.kaitp1016.biganni.utils.ItemUtils.getAnniId
import me.kaitp1016.biganni.utils.ItemUtils.setAnniItem
import me.kaitp1016.biganni.utils.MCUtils.toMC
import me.kaitp1016.biganni.utils.Utils.isFullBlock
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.minecraft.world.item.Items
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType

object SwapperClass: AnniClass(), Listener {
    override val name = "Swapper"
    override val shortName = "SWA"
    override val icon = Items.MUSIC_DISC_CAT
    override val description = arrayOf(
        "アビリティを使用すると視点先の敵と自身の位置を入れ替える。",
    )

    const val SWAPPER_ITEM_ID = "swapper_swapper"
    const val SWAPPER_COOLDOWN = 1200
    val SWAPPER_COOLDOWN_GROUP = Key.key(PLUGIN_ID, "swapper_swapper")

    override fun getDefaultItems(player: Player): MutableList<ItemStack> {
        return super.getDefaultItems(player).also {
            it.add(ItemStack(Material.MUSIC_DISC_CAT).apply {
                uniqueClassItem()
                soulbound()
                setAnniItem(SWAPPER_ITEM_ID)

                setData(DataComponentTypes.USE_COOLDOWN, UseCooldown.useCooldown(SWAPPER_COOLDOWN / 20f).cooldownGroup(SWAPPER_COOLDOWN_GROUP).build())

                editMeta {
                    it.itemName(Component.text("Swapper").color(NamedTextColor.GOLD))
                }
            })
        }
    }

    const val SWAP_RECEIVE_COOLDOWN = 100

    val receiveCooldown = mutableMapOf<Player, Int>()

    @EventHandler
    fun onInteract(event: PlayerInteractEvent) {
        val player = event.player
        if (!isSelected(player)) return

        val item = event.item ?: return
        if (item.getAnniId() != SWAPPER_ITEM_ID || player.hasCooldown(item)) return

        val mcPlayer = player.toMC()
        val pos = mcPlayer.blockPosition()
        val world = player.world
        val level = world.toMC()
        if (!world.getBlockState(player.location).block.isEmpty || !world.getBlockState(player.location.clone().add(0.0, 1.0, 0.0)).block.isEmpty || !level.isFullBlock(pos.offset(0, -1, 0))) {
            player.sendMessage("ここでは使用できません!")
            return
        }

        if (player.isSneaking) {
            player.sendMessage("スニーク中は使用できません!")
            return
        }

        val raytrace = player.rayTraceEntities(20)
        val target = raytrace?.hitEntity as? Player ?: return
        if (player.toMC().teamColor == target.toMC().teamColor || target.isInvisible || BerserkerClass.isUsingAbility(target) || receiveCooldown.contains(target)) return

        val selfLocation = player.location
        player.teleport(target.location)
        target.teleport(selfLocation)

        world.playSound(player.location, Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1f)
        world.playSound(target.location, Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1f)

        target.addPotionEffect(PotionEffect(PotionEffectType.ABSORPTION, 100, 1))
        target.addPotionEffect(PotionEffect(PotionEffectType.SLOWNESS, 60, 1))
        player.addPotionEffect(PotionEffect(PotionEffectType.ABSORPTION, 100, 1))

        FallDamageResistance.add(target, 100)
        receiveCooldown[target] = SWAP_RECEIVE_COOLDOWN

        player.setCooldown(SWAPPER_COOLDOWN_GROUP, SWAPPER_COOLDOWN)
    }

    @EventHandler
    fun onTick(event: ServerTickStartEvent) {
        if (receiveCooldown.isEmpty()) return

        receiveCooldown.replaceAll { player, tick -> tick - 1 }
        receiveCooldown.entries.removeIf { it.value < 1 }
    }
}