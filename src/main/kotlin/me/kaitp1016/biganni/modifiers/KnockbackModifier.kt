package me.kaitp1016.biganni.modifiers

import com.destroystokyo.paper.event.entity.EntityKnockbackByEntityEvent
import com.destroystokyo.paper.event.server.ServerTickStartEvent
import io.papermc.paper.event.entity.EntityKnockbackEvent
import me.kaitp1016.biganni.anniclass.AnniClassManager.getAnniClass
import me.kaitp1016.biganni.anniclass.AnniClasses
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener

object KnockbackModifier: Listener {
    val currentKnockbacks = mutableListOf<Entity>()

    var sprintMultiply = 0.35
    var sprintMultiplyY = 0.65
    var normalMultiply = 1.3
    var normalMultiplyY = 0.85

    @EventHandler
    fun onKnockback(event: EntityKnockbackByEntityEvent) {
        if (event.cause != EntityKnockbackEvent.Cause.ENTITY_ATTACK) return

        val hitBy = event.hitBy
        if (hitBy !is Player || hitBy.getAnniClass() == AnniClasses.CHARGER) return

        val entity = event.entity
        val strength = event.knockbackStrength
        val y = event.knockback.y

        if (currentKnockbacks.contains(entity)) {
            event.knockback = event.knockback.apply {
                this.y = 0.0
            }.normalize().apply {
                multiply(strength * sprintMultiply)
                this.y = y * sprintMultiplyY
            }
        }
        else {
            event.knockback = event.knockback.apply {
                this.y = 0.0
            }.normalize().apply {
                multiply(strength * normalMultiply)
                this.y = y * normalMultiplyY
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