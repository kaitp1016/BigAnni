package me.kaitp1016.biganni.packetgui

import com.destroystokyo.paper.event.server.ServerTickStartEvent
import me.kaitp1016.biganni.events.impl.PacketReciveEvent
import me.kaitp1016.biganni.events.impl.PacketSendEvent
import me.kaitp1016.biganni.utils.MCUtils.toMC
import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener

object PacketGuiManager: Listener {
    @EventHandler
    fun onPacketSend(event: PacketSendEvent) {
        val player = event.player.toMC()

        val inv = player.containerMenu
        if (inv is PacketGuiContainer) {
            inv.packetGui.onPacketSend(event)
        }
    }

    @EventHandler
    fun onPacketRecive(event: PacketReciveEvent) {
        val player = event.player.toMC()

        val inv = player.containerMenu
        if (inv is PacketGuiContainer) {
            inv.packetGui.onPacketRecive(event)
        }
    }

    @EventHandler
    fun onTick(event: ServerTickStartEvent) {
        Bukkit.getOnlinePlayers().forEach { player ->
            val inv = player.toMC().containerMenu
            if (inv is PacketGuiContainer) {
                inv.packetGui.onTick()
            }
        }
    }
}