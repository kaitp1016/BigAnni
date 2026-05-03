package me.kaitp1016.biganni.game

import me.kaitp1016.biganni.mc
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.numbers.BlankFormat
import net.minecraft.network.protocol.game.ClientboundResetScorePacket
import net.minecraft.network.protocol.game.ClientboundSetDisplayObjectivePacket
import net.minecraft.network.protocol.game.ClientboundSetObjectivePacket
import net.minecraft.network.protocol.game.ClientboundSetScorePacket
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.scores.DisplaySlot
import net.minecraft.world.scores.Objective
import net.minecraft.world.scores.criteria.ObjectiveCriteria
import java.util.*

object ScoreboardManager {
    private const val INTERNAL_OBJECTIVE_NAME = "annihilation"

    private val lines = mutableMapOf<Int,Component>()

    fun onJoin(player: ServerPlayer) {
        val objective = Objective(mc.scoreboard,INTERNAL_OBJECTIVE_NAME, ObjectiveCriteria.DUMMY, Component.literal("§c§lANNI§e§lHI§9§lLATI§a§lON"), ObjectiveCriteria.RenderType.INTEGER,false, BlankFormat.INSTANCE)
        player.connection.send(ClientboundSetObjectivePacket(objective, ClientboundSetObjectivePacket.METHOD_ADD))
        player.connection.send(ClientboundSetDisplayObjectivePacket(DisplaySlot.SIDEBAR, objective))

        lines.forEach { (index,text) ->
            player.connection.send(ClientboundSetScorePacket("$index",INTERNAL_OBJECTIVE_NAME,index,Optional.of(text), Optional.of(BlankFormat.INSTANCE)))
        }
    }

    fun reset() {
        mc.playerList.players.forEach {player ->
            lines.forEach { (index, component) ->
                player.connection.send(ClientboundResetScorePacket("$index",INTERNAL_OBJECTIVE_NAME))
            }
        }

        lines.clear()
    }

    fun setLine(index: Int,component: Component) {
        mc.playerList.players.forEach {
            it.connection.send(ClientboundSetScorePacket("$index",INTERNAL_OBJECTIVE_NAME,index,Optional.of(component), Optional.of(BlankFormat.INSTANCE)))
        }

        lines[index] = component
    }
}