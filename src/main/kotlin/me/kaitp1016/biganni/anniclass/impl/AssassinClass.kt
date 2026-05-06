package me.kaitp1016.biganni.anniclass.impl

import io.papermc.paper.datacomponent.DataComponentTypes
import io.papermc.paper.datacomponent.item.UseCooldown
import me.kaitp1016.biganni.PLUGIN_ID
import me.kaitp1016.biganni.anniclass.AnniClass
import me.kaitp1016.biganni.plugin
import me.kaitp1016.biganni.utils.FallDamageResistance
import me.kaitp1016.biganni.utils.FullyInvisible
import me.kaitp1016.biganni.utils.ItemUtils.getAnniId
import me.kaitp1016.biganni.utils.ItemUtils.setAnniItem
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.minecraft.world.item.Items
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.Sound
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeModifier
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.util.Vector
import kotlin.math.max

object AssassinClass: AnniClass(), Listener {
    override val name = "Assassin"
    override val icon = Items.FEATHER
    override val description = arrayOf(
        "アビリティを使用すると前に飛び、6秒間の透明化を獲得する。",
        "この透明化は防具も透明化される。"
    )

    const val LEAP_ITEM_ID = "assassin_leap"
    const val LEAP_COOLDOWN = 800
    val LEAP_COOLDOWN_GROUP = Key.key(PLUGIN_ID,"assassin_leap")

    override fun getDefaultItems(player: Player): MutableList<ItemStack> {
        return super.getDefaultItems(player).also {
            it.add(ItemStack(Material.FEATHER).apply {
                uniqueClassItem()
                soulbound()
                setAnniItem(LEAP_ITEM_ID)

                setData(DataComponentTypes.USE_COOLDOWN, UseCooldown.useCooldown(LEAP_COOLDOWN / 20f).cooldownGroup(LEAP_COOLDOWN_GROUP).build())

                editMeta {
                    it.itemName(Component.text("Leap").color(NamedTextColor.GOLD))
                }
            })
        }
    }

    const val LEAP_INVISIBLE_TIME = 120

    @EventHandler
    fun onInteract(event: PlayerInteractEvent) {
        if (event.action != Action.RIGHT_CLICK_BLOCK && event.action != Action.RIGHT_CLICK_AIR) return

        val player = event.player
        if (!isSelected(player)) return

        val item = event.item ?: return
        if (item.getAnniId() != LEAP_ITEM_ID || player.hasCooldown(item)) return

        FullyInvisible.add(player,LEAP_INVISIBLE_TIME)

        player.velocity = player.location.direction.clone().apply {
            add(Vector(0.0,0.2,0.0))
            normalize()
            y = max(y,0.6)
            multiply(1.5)
        }

        player.world.playSound(player.location, Sound.ENTITY_WITHER_SHOOT,2f,2f)
        FallDamageResistance.add(player,160)

        player.setCooldown(LEAP_COOLDOWN_GROUP,LEAP_COOLDOWN)
    }
}