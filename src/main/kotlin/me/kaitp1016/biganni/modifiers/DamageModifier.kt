package me.kaitp1016.biganni.modifiers

import me.kaitp1016.biganni.utils.MCUtils.toMCCopy
import net.minecraft.tags.ItemTags
import org.bukkit.damage.DamageType
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityDamageEvent

object DamageModifier: Listener {
    // 弓ダメージ減らす
    @EventHandler(priority = EventPriority.LOWEST)
    fun onDamage(event: EntityDamageByEntityEvent) {
        val source = event.damageSource
        val type = source.damageType
        if (type == DamageType.ARROW) {
            event.damage *= 0.5f
        }

        if (event.cause == EntityDamageEvent.DamageCause.ENTITY_SWEEP_ATTACK) {
            event.isCancelled = true
        }

        if (type == DamageType.PLAYER_ATTACK) {
            val item = (event.damager as? Player)?.inventory?.itemInMainHand
            if (item?.toMCCopy()?.`is`(ItemTags.SWORDS) == true) {
                event.damage -= if (event.isCritical) 1.5 else 1.0
            }
        }
    }
}