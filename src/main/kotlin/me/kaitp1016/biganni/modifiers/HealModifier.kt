package me.kaitp1016.biganni.modifiers

import com.destroystokyo.paper.event.entity.EntityAddToWorldEvent
import me.kaitp1016.biganni.utils.MCUtils.toMC
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener

object HealModifier: Listener {
    @EventHandler
    fun onAddEntity(event: EntityAddToWorldEvent) {
        val player = event.entity as? Player ?: return
        val mcPlayer = player.toMC()
        mcPlayer.foodData.saturatedRegenRate = 40
    }
}