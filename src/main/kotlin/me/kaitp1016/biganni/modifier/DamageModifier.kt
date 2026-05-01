package me.kaitp1016.biganni.modifier

import org.bukkit.damage.DamageType
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageEvent

object DamageModifier: Listener {
    // 弓ダメージ減らす
    @EventHandler
    fun onDamage(event: EntityDamageEvent) {
        val source = event.damageSource
        if (source.damageType == DamageType.ARROW) {
            event.damage *= 0.5f
        }
    }
}