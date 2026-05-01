package me.kaitp1016.biganni.events

import io.netty.channel.ChannelDuplexHandler
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelPromise
import me.kaitp1016.biganni.events.impl.PacketReciveEvent
import me.kaitp1016.biganni.events.impl.PacketSendEvent
import me.kaitp1016.biganni.plugin
import me.kaitp1016.biganni.utils.MCUtils.toMC
import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent

object EventManager: Listener {
    @EventHandler
    fun onJoin(event: PlayerJoinEvent) {
        val player = event.player

        player.toMC().connection.connection.channel.pipeline().addBefore("packet_handler","battleroyal_packet_listener_${player.name}", PacketListener(player))
    }

    class PacketListener: ChannelDuplexHandler {
        val player: Player

        constructor(player: Player) {
            this.player = player
        }

        override fun channelRead(ctx: ChannelHandlerContext?, packet: Any?) {
            if (packet is Packet<*>) {
                val event = PacketReciveEvent(packet, player)
                Bukkit.getPluginManager().callEvent(event)

                if (!event.isCancelled) {
                    super.channelRead(ctx, packet)
                }
            }
        }

        override fun write(ctx: ChannelHandlerContext?, packet: Any?, promise: ChannelPromise?) {
            if (packet is Packet<*>) {
                val event = PacketSendEvent(packet, player)
                Bukkit.getPluginManager().callEvent(event)

                if (!event.isCancelled) {
                    super.write(ctx, event.packet, promise)
                }
            }
        }
    }
}