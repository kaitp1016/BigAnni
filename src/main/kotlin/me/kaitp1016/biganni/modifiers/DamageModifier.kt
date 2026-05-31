package me.kaitp1016.biganni.modifiers

import me.kaitp1016.biganni.plugin
import org.bukkit.Bukkit
import org.bukkit.NamespacedKey
import org.bukkit.damage.DamageType
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityDamageEvent
import kotlin.math.max

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
            event.damage = max(event.damage - 1.0, 1.0)
        }
    }
}