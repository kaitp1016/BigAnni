package me.kaitp1016.biganni.anniclass

import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.event.player.PlayerRespawnEvent

object AnniClassHandler: Listener {
    @EventHandler
    fun onQuit(event: PlayerQuitEvent) {
        val player = event.player
        val uuid = player.uniqueId

        AnniClassManager.classes[uuid]?.onUnselect(player)
        AnniClassManager.classes.remove(uuid)
    }

    @EventHandler
    fun onJoin(event: PlayerJoinEvent) {}


    @EventHandler
    fun onRespawn(event: PlayerRespawnEvent) {
        val player = event.player
        val uuid = player.uniqueId

        AnniClassManager.classes[uuid]?.onRespawn(player)
    }
}