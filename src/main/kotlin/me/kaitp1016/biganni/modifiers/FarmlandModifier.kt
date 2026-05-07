package me.kaitp1016.biganni.modifiers

import org.bukkit.Material
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent

object FarmlandModifier: Listener {
    @EventHandler
    fun onInteract(event: PlayerInteractEvent) {
        if (event.action == Action.PHYSICAL && event.clickedBlock?.type == Material.FARMLAND) {
            event.isCancelled = true
        }
    }
}