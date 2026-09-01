package me.kaitp1016.biganni.commands

import com.mojang.brigadier.arguments.DoubleArgumentType
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
import me.kaitp1016.biganni.anniclass.AnniClassManager.isClassSelected
import me.kaitp1016.biganni.anniclass.AnniClassManager.selectAnniClass
import me.kaitp1016.biganni.anniclass.AnniClasses
import me.kaitp1016.biganni.config.Config
import me.kaitp1016.biganni.features.DelayingBlock
import me.kaitp1016.biganni.features.TeamDoor
import me.kaitp1016.biganni.game.Game
import me.kaitp1016.biganni.game.StartCountdown
import me.kaitp1016.biganni.game.WorldResetter
import me.kaitp1016.biganni.game.boss.BossManager
import me.kaitp1016.biganni.mc
import me.kaitp1016.biganni.modifiers.KnockbackModifier
import me.kaitp1016.biganni.packetgui.impl.AnniClassSelector
import me.kaitp1016.biganni.packetgui.impl.BrewingShopGui
import me.kaitp1016.biganni.packetgui.impl.WeaponShopGui
import me.kaitp1016.biganni.utils.MCUtils.toMC
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.Style
import net.kyori.adventure.text.format.TextDecoration
import net.minecraft.commands.SharedSuggestionProvider
import net.minecraft.core.registries.Registries
import net.minecraft.resources.ResourceKey
import net.minecraft.server.permissions.LevelBasedPermissionSet
import org.bukkit.Bukkit
import org.bukkit.Sound
import org.bukkit.entity.Player
import java.util.concurrent.CompletableFuture
import kotlin.random.Random

object AnniCommand {
    fun register(): LiteralArgumentBuilder<CommandSourceStack> {
        return Commands.literal("anni").requires { (it as net.minecraft.commands.CommandSourceStack).permissions() == LevelBasedPermissionSet.GAMEMASTER || it.sender.isOp }.then(Commands.literal("classselector").executes {
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
        }).then(Commands.literal("startwithcountdown").executes {
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
            val teamName = StringArgumentType.getString(it, "team")
            val health = IntegerArgumentType.getInteger(it, "health")

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
        }).then(Commands.literal("setmap").then(Commands.argument("map", StringArgumentType.greedyString()).executes {
            val mapName = StringArgumentType.getString(it, "map")
            val map = Config.getMap(mapName)
            if (map != null) {
                Game.map = map
                it.source.sender.sendMessage("マップを ${map.name} にしました!")
            } else {
                it.source.sender.sendMessage("マップが見つかりませんでした!")
            }

            return@executes 1
        }.suggests(MapSuggestion))).then(Commands.literal("openweaponshop").executes {
            WeaponShopGui((it.source.executor as Player).toMC()).open()
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
        }).then(Commands.literal("kbtest").then(Commands.argument("type", StringArgumentType.word()).then(Commands.argument("num", DoubleArgumentType.doubleArg()).executes {
            val type = StringArgumentType.getString(it, "type")
            val num = DoubleArgumentType.getDouble(it, "num")

            when (type) {
                "normal" -> KnockbackModifier.normalMultiply = num
                "normalY" -> KnockbackModifier.normalMultiplyY = num
                "sprint" -> KnockbackModifier.sprintMultiply = num
                "sprintY" -> KnockbackModifier.sprintMultiplyY = num
            }
            return@executes 1
        }))).then(Commands.literal("randomteam").executes { context ->
            val unteamPlayers = mc.playerList.players.sortedBy { Random.nextInt() }.toMutableList()
            val teams = Game.map.teams.sortedBy { Random.nextInt() }

            while (unteamPlayers.isNotEmpty()) {
                teams.forEach { anniTeam ->
                    val player = unteamPlayers.removeFirstOrNull() ?: return@forEach
                    val team = mc.scoreboard.playerTeams.find { it.name.equals(anniTeam.name, true) } ?: run {
                        context.source.executor?.sendMessage("チーム ${anniTeam.name} が見つかりませんでした!")
                        return@forEach
                    }

                    mc.scoreboard.addPlayerToTeam(player.scoreboardName, team)
                }
            }

            return@executes 1
        }).then(Commands.literal("resetworld").executes {
            if (Game.isStarted || StartCountdown.isStarted) {
                it.source.sender.sendMessage("ゲームが始まっているためマップをリセットできません。/anni resetをしてから開始してください。")
                return@executes 1
            }

            val map = Game.map
            val registry = ResourceKey.create(Registries.DIMENSION, map.dimension)
            val level = mc.getLevel(registry)!!

            Bukkit.broadcast(Component.text("マップをリセット中です。数秒固まることがあります。").style(Style.style().decoration(TextDecoration.BOLD, true).color(NamedTextColor.RED).build()))

            val start = System.currentTimeMillis()

            if (!WorldResetter.reset(level.world, map.name)) {
                it.source.sender.sendMessage("§cマップのリセットに失敗しました!")
            }
            else {
                val timeTook = System.currentTimeMillis() - start
                it.source.sender.sendMessage("§aマップをリセットしました! (${timeTook}ms)")
            }

            Bukkit.getOnlinePlayers().forEach { player ->
                player.playSound(player, Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1f)
            }

            return@executes 1
        }).then(Commands.literal("setteam").then(Commands.argument("player", StringArgumentType.word()).then(Commands.argument("team", StringArgumentType.greedyString()).executes {
            val playerName = StringArgumentType.getString(it, "player")
            val team = StringArgumentType.getString(it, "team")
            val player = mc.playerList.getPlayer(playerName)
            if (player == null) {
                it.source.sender.sendMessage("§cプレイヤーが見つかりませんでした!")
                return@executes 1
            }

            val mcTeam = mc.scoreboard.playerTeams.find { it.name.equals(team, true) }
            if (mcTeam == null) {
                it.source.sender.sendMessage("§cチームが見つかりませんでした!")
                return@executes 1
            }

            mc.scoreboard.addPlayerToTeam(player.scoreboardName, mcTeam)
            player.kill(player.level())

            if (!player.bukkitEntity.isClassSelected()) {
                player.bukkitEntity.selectAnniClass(AnniClasses.CIVILIAN)
            }

            return@executes 1
        })))
    }

    object MapSuggestion : SuggestionProvider<CommandSourceStack> {
        override fun getSuggestions(context: CommandContext<CommandSourceStack>, builder: SuggestionsBuilder): CompletableFuture<Suggestions> {
            return SharedSuggestionProvider.suggest(Config.getMapNames(), builder)
        }
    }
}