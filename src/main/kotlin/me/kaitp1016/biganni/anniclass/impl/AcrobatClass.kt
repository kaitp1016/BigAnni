package me.kaitp1016.biganni.anniclass.impl

import com.destroystokyo.paper.event.server.ServerTickStartEvent
import me.kaitp1016.biganni.anniclass.AnniClass
import me.kaitp1016.biganni.plugin
import net.minecraft.world.item.Items
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.Sound
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeModifier
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.inventory.ItemStack
import org.bukkit.util.Vector
import kotlin.math.max

object AcrobatClass: AnniClass(), Listener {
    override val name = "Acrobat"
    override val shortName = "ACR"
    override val icon = Items.FEATHER
    override val description = arrayOf(
        "常に落下ダメージを食らわなくなる。",
        "ダブルジャンプをすることができる。"
    )

    override fun getDefaultItems(player: Player): MutableList<ItemStack> {
        return super.getDefaultItems(player).also {
            it.add(ItemStack(Material.BOW).uniqueClassItem().soulbound())
            it.add(ItemStack(Material.ARROW).uniqueClassItem().soulbound().also { it.amount = 8 })
        }
    }

    val ACROBAT_FALL_DAMAGE_RESITANCE = NamespacedKey(plugin,"acrobat_fall_damage_resistance")

    data class AcrobatCooldown(val player: Player, var tick: Int)
    val cooldowns = mutableListOf<AcrobatCooldown>()

    override fun onSelect(player: Player) {
        player.allowFlight = true
        player.getAttribute(Attribute.FALL_DAMAGE_MULTIPLIER)?.addModifier(AttributeModifier(ACROBAT_FALL_DAMAGE_RESITANCE,-1000.0, AttributeModifier.Operation.ADD_NUMBER))
        super.onSelect(player)
    }

    override fun onUnselect(player: Player) {
        player.allowFlight = false
        player.getAttribute(Attribute.FALL_DAMAGE_MULTIPLIER)?.removeModifier(ACROBAT_FALL_DAMAGE_RESITANCE)
        cooldowns.removeIf { it.player == player }

        super.onUnselect(player)
    }

    override fun onUserTick(player: Player) {
        if (!player.isFlying) return

        if (!cooldowns.any { it.player == player }) {
            player.velocity = player.location.direction.clone().apply {
                add(Vector(0.0,0.2,0.0))
                normalize()
                y = max(y,0.6)
                multiply(1.75)
            }

            player.playSound(player , Sound.ENTITY_ZOMBIE_INFECT,1f,2f)
            cooldowns.add(AcrobatCooldown(player,200))
        }

        player.allowFlight = false
    }

    @EventHandler
    fun onTick(event: ServerTickStartEvent) {
        if (cooldowns.isEmpty()) return

        cooldowns.removeAll{
            it.tick--
            if (it.tick < 1) {
                it.player.allowFlight = true
                it.player.playSound(it.player, Sound.ENTITY_EXPERIENCE_ORB_PICKUP,1f,2f)
                return@removeAll true
            }
            return@removeAll false
        }
    }
}