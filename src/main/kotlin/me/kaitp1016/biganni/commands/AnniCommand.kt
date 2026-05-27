package me.kaitp1016.biganni.commands

import com.mojang.brigadier.arguments.IntegerArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.suggestion.SuggestionProvider
import com.mojang.brigadier.suggestion.Suggestions
import com.mojang.brigadier.suggestion.SuggestionsBuilder
import io.papermc.paper.command.brigadier.CommandSourceStack
import io.papermc.paper.command.brigadier.Commands
import io.papermc.paper.command.brigadier.argument.ArgumentTypes
import io.papermc.paper.command.brigadier.argument.resolvers.selector.PlayerSelectorArgumentResolver
import me.kaitp1016.biganni.config.Config
import me.kaitp1016.biganni.features.DelayingBlock
import me.kaitp1016.biganni.features.TeamDoor
import me.kaitp1016.biganni.game.boss.BossManager
import me.kaitp1016.biganni.game.Game
import me.kaitp1016.biganni.game.StartCountdown
import me.kaitp1016.biganni.packetgui.impl.AnniClassSelector
import me.kaitp1016.biganni.packetgui.impl.WeaeponShopGui
import me.kaitp1016.biganni.packetgui.impl.BrewingShopGui
import me.kaitp1016.biganni.utils.MCUtils.toMC
import net.minecraft.commands.SharedSuggestionProvider
import org.bukkit.entity.Player
import java.util.concurrent.CompletableFuture

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
        })).then(Commands.literal("startimmediately").executes {
            if (Game.isStarted || StartCountdown.isStarted) {
                it.source.sender.sendMessage("ゲームが始まっているため開始できません。/anni resetをしてから開始してください。")
                return@executes 1
            }

            Game.start()
            return@executes 1
        }).then(Commands.literal("start").executes {
            if (Game.isStarted || StartCountdown.isStarted) {
                it.source.sender.sendMessage("ゲームが始まっているため開始できません。/anni resetをしてから開始してください。")
                return@executes 1
            }

            StartCountdown.start()
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
        }))).then(Commands.literal("reset").executes {
            Game.reset()
            return@executes 1
        }).then(Commands.literal("givedelayblock").executes {
            (it.source.sender as Player).give(DelayingBlock.createItem())
            return@executes 1
        }).then(Commands.literal("giveteamdoor").executes {
            (it.source.sender as Player).give(TeamDoor.createItem())
            return@executes 1
        }).then(Commands.literal("givebossbuff").executes {
            (it.source.sender as Player).give(BossManager.createBossBuffItem())
            return@executes 1
        }).then(Commands.literal("spawnboss").executes {
            BossManager.spawn()
            return@executes 1
        }).then(Commands.literal("setmap").then(Commands.argument("map", StringArgumentType.greedyString()) .executes {
            val mapName = StringArgumentType.getString(it,"map")
            val map = Config.getMap(mapName)
            if (map != null) {
                Game.map = map
                it.source.sender.sendMessage("マップを ${map.name} にしました!")
            }
            else {
                it.source.sender.sendMessage("マップが見つかりませんでした!")
            }

            return@executes 1
        }.suggests(MapSuggestion))).then(Commands.literal("openweaponshop").executes {
            WeaeponShopGui((it.source.executor as Player).toMC()).open()
            return@executes 1
        }).then(Commands.literal("openbrewingshop").executes {
            BrewingShopGui((it.source.executor as Player).toMC()).open()
            return@executes 1
        }).then(Commands.literal("resetcooldowns").executes {
            val player = (it.source.executor as Player).toMC()
            val cooldowns = player.cooldowns

            cooldowns.cooldowns.toMutableMap().forEach {
                cooldowns.removeCooldown(it.key)
            }
            return@executes 1
        })
    }

    object MapSuggestion: SuggestionProvider<CommandSourceStack> {
        override fun getSuggestions(context: CommandContext<CommandSourceStack>, builder: SuggestionsBuilder): CompletableFuture<Suggestions> {
            return SharedSuggestionProvider.suggest(Config.getMapNames(),builder)
        }
    }
}