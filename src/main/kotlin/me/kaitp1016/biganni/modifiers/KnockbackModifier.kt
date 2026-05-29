package me.kaitp1016.biganni.modifiers

import com.destroystokyo.paper.event.entity.EntityKnockbackByEntityEvent
import com.destroystokyo.paper.event.server.ServerTickStartEvent
import io.papermc.paper.event.entity.EntityKnockbackEvent
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener

object KnockbackModifier: Listener {
    val currentKnockbacks = mutableListOf<Entity>()

    @EventHandler
    fun onKnockback(event: EntityKnockbackByEntityEvent) {
        if (event.cause != EntityKnockbackEvent.Cause.ENTITY_ATTACK || event.hitBy !is Player) return

        val entity = event.entity
        val strength = event.knockbackStrength
        val y = event.knockback.y

        if (currentKnockbacks.contains(entity)) {
            event.knockback = event.knockback.apply {
                this.y = 0.0
            }.normalize().apply {
                multiply(strength * 0.35)
                this.y = y * 0.65
            }
        }
        else {
            event.knockback = event.knockback.apply {
                this.y = 0.0
            }.normalize().apply {
                multiply(strength * 1.25)
                this.y = y * 0.85
            }

            currentKnockbacks.add(entity)
        }
    }

    @EventHandler
    fun onTick(event: ServerTickStartEvent) {
        if (!currentKnockbacks.isEmpty()) {
            currentKnockbacks.clear()
        }
    }
}