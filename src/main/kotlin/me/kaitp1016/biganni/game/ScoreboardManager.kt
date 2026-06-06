package me.kaitp1016.biganni.game

import me.kaitp1016.biganni.mc
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.contents.objects.AtlasSprite
import net.minecraft.network.chat.contents.objects.PlayerSprite
import net.minecraft.network.chat.numbers.BlankFormat
import net.minecraft.network.protocol.game.ClientboundResetScorePacket
import net.minecraft.network.protocol.game.ClientboundSetDisplayObjectivePacket
import net.minecraft.network.protocol.game.ClientboundSetObjectivePacket
import net.minecraft.network.protocol.game.ClientboundSetScorePacket
import net.minecraft.resources.Identifier
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.item.component.ResolvableProfile
import net.minecraft.world.scores.DisplaySlot
import net.minecraft.world.scores.Objective
import net.minecraft.world.scores.criteria.ObjectiveCriteria
import java.util.*
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

object ScoreboardManager {
    private const val INTERNAL_OBJECTIVE_NAME = "annihilation"
    private val OBJECTIVE = Objective(mc.scoreboard, INTERNAL_OBJECTIVE_NAME, ObjectiveCriteria.DUMMY, Component.literal("§c§lANNI§e§lHI§9§lLATI§a§lON"), ObjectiveCriteria.RenderType.INTEGER, false, BlankFormat.INSTANCE)

    private val lines = mutableMapOf<Int, Component>(
        0 to Component.literal("§6apple.playit.plus"),
        1 to Component.empty(),
        2 to Component.empty(),
        3 to Component.empty(),
        4 to Component.empty(),
        5 to Component.empty(),
    )

    private var isSetted = false

    fun onJoin(player: ServerPlayer) {
        player.connection.send(ClientboundSetObjectivePacket(OBJECTIVE, ClientboundSetObjectivePacket.METHOD_ADD))
        player.connection.send(ClientboundSetDisplayObjectivePacket(DisplaySlot.SIDEBAR, OBJECTIVE))

        lines.forEach { (index, text) ->
            player.connection.send(ClientboundSetScorePacket("$index", INTERNAL_OBJECTIVE_NAME, index, Optional.of(text), Optional.of(BlankFormat.INSTANCE)))
        }
    }

    fun onTick() {
        if (!isSetted) {
            ScoreboardAnimator.onTick()
        }
    }

    fun reset() {
        mc.playerList.players.forEach { player ->
            lines.forEach { (index, component) ->
                player.connection.send(ClientboundResetScorePacket("$index", INTERNAL_OBJECTIVE_NAME))
            }
        }

        lines.clear()
    }

    fun setLine(index: Int, component: Component) {
        setLineInternal(index, component)
        isSetted = true
    }

    private fun setLineInternal(index: Int, component: Component) {
        mc.playerList.players.forEach {
            it.connection.send(ClientboundSetScorePacket("$index", INTERNAL_OBJECTIVE_NAME, index, Optional.of(component), Optional.of(BlankFormat.INSTANCE)))
        }

        lines[index] = component
    }

    object ScoreboardAnimator {
        private val players = arrayOf(
            UUID.fromString("ab467df5-5346-4ed5-a25e-d3435403cfb1"), // applepants88
            UUID.fromString("5d46a400-3858-4314-8275-1b78c41aae7d"), // monekun
            UUID.fromString("0cb5afe8-ed62-44b9-8e31-099cd018eb57"), // kaitp1016
            UUID.fromString("7524701e-4a20-4d36-8996-85c7796976f8"), // tikuwa_yay
            UUID.fromString("78f320a2-d80a-4572-8420-a538e4984acd"), // naokinn2022
            UUID.fromString("3e33630d-3367-48f5-9921-2d4b016ffa57"), // kawaii_nekomimi
        )

        private val numbers = mapOf(
            '0' to '⁰',
            '1' to '¹',
            '2' to '²',
            '3' to '³',
            '4' to '⁴',
            '5' to '⁵',
            '6' to '⁶',
            '7' to '⁷',
            '8' to '⁸',
            '9' to '⁹',
            '-' to '⁻',
        )

        private var animationTick = 0
        private var defendingLine = 2
        private var attackingStep = -5
        private var defendingStep = -999
        private var attackingPlayer: UUID? = null
        private var defendingPlayer: UUID? = null
        private var health = 250

        fun onTick() {
            animationTick++
            if (animationTick % 15 != 0) return

            animationTick = 0

            if (attackingStep > 0) {
                if (defendingStep == -999) { // 守りに行っている人がいない
                    defendingStep = Random.nextInt(-20, -4)
                    defendingPlayer = randomPlayer(attackingPlayer)
                    defendingLine = if (Random.nextBoolean()) 2 else 4
                }

                defendingStep++
                attackingStep++

                if (defendingStep > 5) { // 攻めている人を倒す
                    attackingPlayer = null
                    defendingStep = Random.nextInt(3, 25)
                }
            } else { // 警戒を緩める
                defendingStep--
                attackingStep++

                if (defendingStep < -10 && defendingStep != -999) { // 気にかけてる人がいなくなった
                    defendingStep = -999
                    defendingPlayer = null
                }
            }

            if (attackingPlayer == null) {
                attackingPlayer = randomPlayer(defendingPlayer)
                attackingStep = Random.nextInt(-50, -10)
            }

            if (attackingStep > 6) {
                health--
            }

            setLineInternal(3, createNexusLine(attackingStep, attackingPlayer))
            setLineInternal(defendingLine, createDefenseLine(defendingStep, defendingPlayer))
        }

        private fun createDefenseLine(step: Int, player: UUID?): Component {
            if (player == null || step < 0) return Component.empty()

            val isAttacking = step > 2
            val backwardSpaces = min(max(0, step), 2)
            var backwardSpace = ""

            repeat(backwardSpaces) {
                backwardSpace += ' '
            }

            val sword = if (isAttacking) Component.`object`(AtlasSprite(Identifier.withDefaultNamespace("items"), Identifier.parse("item/iron_sword"))) else Component.empty()

            return Component.literal(backwardSpace).append(Component.`object`(PlayerSprite(ResolvableProfile.createUnresolved(player), true)).append(sword))
        }

        private fun createNexusLine(step: Int, player: UUID?): Component {
            var healthText = health.toString()
            numbers.forEach {
                healthText = healthText.replace(it.key, it.value)
            }

            val nexus = Component.literal(" $healthText")
            if (player == null || step < 0) return Component.literal("            ").append(Component.`object`(AtlasSprite(Identifier.withDefaultNamespace("blocks"), Identifier.parse("block/end_stone"))).append(nexus))

            val isMining = step > 6
            val backwardSpaces = min(max(0, step), 7)
            val fowardSpaces = 8 - backwardSpaces
            var backwardSpace = ""
            var fowardSpace = ""

            repeat(fowardSpaces) {
                fowardSpace += ' '
            }

            repeat(backwardSpaces) {
                backwardSpace += ' '
            }

            val pickaxe = if (isMining) Component.`object`(AtlasSprite(Identifier.withDefaultNamespace("items"), Identifier.parse("item/golden_pickaxe"))) else Component.literal("  ")
            return Component.literal(backwardSpace).append(Component.`object`(PlayerSprite(ResolvableProfile.createUnresolved(player), true)).append(pickaxe.append(Component.literal(fowardSpace).append(Component.`object`(AtlasSprite(Identifier.withDefaultNamespace("blocks"), Identifier.parse("block/end_stone"))).append(nexus)))))
        }

        private fun randomPlayer(exclude: UUID?): UUID {
            return (players + mc.playerList.players.map { it.uuid }).toSet().filter { it != exclude }.random()
        }
    }
}