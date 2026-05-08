package me.kaitp1016.biganni.modifiers

import com.destroystokyo.paper.event.entity.EntityKnockbackByEntityEvent
import io.papermc.paper.event.entity.EntityKnockbackEvent
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener

object KnockbackModifier: Listener {
    /*
    @EventHandler
    fun onKnockback(event: EntityKnockbackByEntityEvent) {
        if (event.cause != EntityKnockbackEvent.Cause.ENTITY_ATTACK) return

        event.knockback = event.knockback.apply {
            y = 0.0
        }.normalize().apply {
            multiply(event.knockbackStrength / 3 + 0.135)
            y = 0.2
        }
    }

     */
}