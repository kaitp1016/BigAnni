package me.kaitp1016.biganni.events.impl

import net.minecraft.network.protocol.Packet
import org.bukkit.entity.Player
import org.bukkit.event.Cancellable
import org.bukkit.event.HandlerList
import org.bukkit.event.player.PlayerEvent

class PacketReciveEvent: PlayerEvent, Cancellable {
    val packet:Packet<*>
    @JvmField
    var isCancelled = false

    constructor(packet:Packet<*>,player: Player):super(player,true) {
        this.packet = packet
    }

    override fun getHandlers(): HandlerList {
        return handler
    }

    override fun isCancelled(): Boolean {
        return isCancelled
    }

    override fun setCancelled(cancel: Boolean) {
        isCancelled = cancel
    }

    companion object {
        val handler = HandlerList()

        @JvmStatic
        fun getHandlerList(): HandlerList {
            return handler
        }
    }
}