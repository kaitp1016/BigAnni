package me.kaitp1016.biganni.commands

import com.mojang.brigadier.arguments.IntegerArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
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
        return Commands.literal("anni").then(
            Commands.literal("classselector").executes {
            val player = (it.source.executor as Player).toMC()
            AnniClassSelector(player, null).open()
            return@executes 1
        }.then(Commands.argument("targets", ArgumentTypes.players()).executes {
            val targetResolver = it.getArgument("targets", PlayerSelectorArgumentResolver::class.java)
            val targets = targetResolver.resolve(it.getSource())

            targets.forEach { player ->
                val player = player.toMC()
                AnniClassSelector(player, null).open()
            }

            return@executes 1
        })).then(Commands.literal("start").executes {
            if (Game.isStarted) {
                it.source.sender.sendMessage("ゲームが始まっているため開始できません。/anni resetをしてから開始してください。")
                return@executes 1
            }

            Game.start()
            return@executes 1
        }).then(Commands.literal("skipphase").executes {
            Game.phaseTime = 1
            return@executes 1
        }).then(Commands.literal("setnexus").then(Commands.argument("team", StringArgumentType.word()).then(Commands.argument("health", IntegerArgumentType.integer()).executes {
            val teamName = StringArgumentType.getString(it,"team")
            val health = IntegerArgumentType.getInteger(it,"health")

            val team = Game.teams.find { team -> team.name == teamName }
            if (team == null) {
                it.source.sender.sendMessage("チームが見つかりませんでした!")
                return@executes 1
            }

            team.health = health
            Game.updateNexusHealth(team)

            it.source.sender.sendMessage("$teamName のネクサスの体力を $health にしました!")

            return@executes 1
        }))) .then(Commands.literal("reset").executes {
            Game.reset()
            return@executes 1
        })
    }
}