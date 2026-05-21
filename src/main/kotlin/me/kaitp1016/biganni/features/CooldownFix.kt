package me.kaitp1016.biganni.features

import me.kaitp1016.biganni.events.impl.PacketReciveEvent
import me.kaitp1016.biganni.utils.MCUtils.toMC
import net.minecraft.network.protocol.game.ClientboundCooldownPacket
import net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener

object CooldownFix: Listener {
    @EventHandler
    fun onPacket(event: PacketReciveEvent) {
        if (event.packet !is ServerboundPlayerLoadedPacket) return
        val player = event.player.toMC()
        val tick = player.cooldowns.tickCount

        player.cooldowns.cooldowns.forEach { (group,cooldown) ->
            val time = cooldown.endTime - tick
            if (time > 1) {
                player.connection.send(ClientboundCooldownPacket(group, time))
            }
        }
    }
}