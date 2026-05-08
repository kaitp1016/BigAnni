package me.kaitp1016.biganni.anniclass

import com.destroystokyo.paper.event.server.ServerTickStartEvent
import me.kaitp1016.biganni.anniclass.AnniClassManager.getAnniClass
import org.bukkit.Bukkit
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
    fun onRespawn(event: PlayerRespawnEvent) {
        val player = event.player
        val uuid = player.uniqueId

        AnniClassManager.classes[uuid]?.onRespawn(player)
    }

    @EventHandler
    fun onTick(event: ServerTickStartEvent) {
        Bukkit.getOnlinePlayers().forEach {
            it.getAnniClass()?.onUserTick(it)
        }
    }
}