package me.kaitp1016.biganni.features

import com.mojang.datafixers.util.Either
import me.kaitp1016.biganni.utils.MCUtils.toMC
import net.minecraft.network.protocol.common.ClientboundServerLinksPacket
import net.minecraft.server.ServerLinks
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent

object ServerLink: Listener {
    @EventHandler
    fun onJoin(event: PlayerJoinEvent) {
        event.player.toMC().connection.send(
            ClientboundServerLinksPacket(
                listOf(
                    ServerLinks.UntrustedEntry(Either.left(ServerLinks.KnownLinkType.ANNOUNCEMENTS), "https://discord.gg/nKp5JTghh4"),
                    ServerLinks.UntrustedEntry(Either.left(ServerLinks.KnownLinkType.FEEDBACK), "https://discord.gg/nKp5JTghh4"),
                    ServerLinks.UntrustedEntry(Either.left(ServerLinks.KnownLinkType.COMMUNITY), "https://discord.gg/nKp5JTghh4"),
                    ServerLinks.UntrustedEntry(Either.left(ServerLinks.KnownLinkType.STATUS), "https://discord.gg/nKp5JTghh4"),
                    ServerLinks.UntrustedEntry(Either.left(ServerLinks.KnownLinkType.SUPPORT), "https://discord.gg/nKp5JTghh4"),
                    ServerLinks.UntrustedEntry(Either.left(ServerLinks.KnownLinkType.WEBSITE), "https://discord.gg/nKp5JTghh4"),
                    ServerLinks.UntrustedEntry(Either.left(ServerLinks.KnownLinkType.BUG_REPORT), "https://discord.gg/nKp5JTghh4"),
                )
            )
        )
    }
}