package me.kaitp1016.biganni.features

import com.destroystokyo.paper.event.server.ServerTickStartEvent
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.player.PlayerQuitEvent

object CombatTag: Listener {
    const val COMBAT_TAG_TICK = 600

    val combatTagStatus = mutableMapOf<Player, Int>()

    @EventHandler(priority = EventPriority.MONITOR)
    fun onDamage(event: EntityDamageEvent) {
        if (event.isCancelled || event.damage <= 0) return

        val entity = event.entity as? Player ?: return
        val damager = event.damageSource.causingEntity as? Player ?: return

        combatTagStatus[damager] = COMBAT_TAG_TICK
        combatTagStatus[entity] = COMBAT_TAG_TICK
    }

    fun Player.isCombating(): Boolean {
        return combatTagStatus.contains(this)
    }

    @EventHandler
    fun onLogout(event: PlayerQuitEvent) {
        val player = event.player
        if (player.isCombating()) {
            player.kill()
        }
    }

    @EventHandler
    fun onTick(event: ServerTickStartEvent) {
        combatTagStatus.replaceAll { player, tick -> tick - 1 }
        combatTagStatus.entries.removeAll { it.value < 1 }
    }
}