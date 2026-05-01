package me.kaitp1016.biganni.commands

import com.mojang.brigadier.builder.LiteralArgumentBuilder
import io.papermc.paper.command.brigadier.CommandSourceStack
import io.papermc.paper.command.brigadier.Commands
import io.papermc.paper.command.brigadier.argument.ArgumentTypes
import io.papermc.paper.command.brigadier.argument.resolvers.selector.PlayerSelectorArgumentResolver
import me.kaitp1016.biganni.game.Game
import me.kaitp1016.biganni.packetgui.impl.AnniClassSelector
import me.kaitp1016.biganni.utils.MCUtils.toMC
import org.bukkit.entity.Player

object AnniCommand {
    fun register(): LiteralArgumentBuilder<CommandSourceStack> {
        return Commands.literal("anni").requires { it.sender.hasPermission("biganni.command.anni") }.then(
            Commands.literal("classselector").executes {
            val player = (it.source.executor as Player).toMC()
            AnniClassSelector(player, null).open()
            return@executes 1
        }.then(Commands.argument("targets", ArgumentTypes.players()).executes {
            val targetResolver: PlayerSelectorArgumentResolver = it.getArgument("targets", PlayerSelectorArgumentResolver::class.java)
            val targets = targetResolver.resolve(it.getSource())

            targets.forEach { player ->
                val player = player.toMC()
                AnniClassSelector(player, null).open()
            }

            return@executes 1
        })).then(Commands.literal("start").executes {
            Game.start()
            return@executes 1
        }).then(Commands.literal("skipphase").executes {
            Game.phaseTime = 1
            return@executes 1
        })
    }
}