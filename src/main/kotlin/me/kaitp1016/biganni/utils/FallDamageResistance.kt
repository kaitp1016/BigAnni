package me.kaitp1016.biganni.utils

import com.destroystokyo.paper.event.server.ServerTickStartEvent
import me.kaitp1016.biganni.plugin
import org.bukkit.NamespacedKey
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeModifier
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import kotlin.math.max

object FallDamageResistance: Listener {
    val FALL_DAMAGE_RESISTANCE_KEY = NamespacedKey(plugin, "anni_fall_damage_resistance")

    data class ResistancePlayer(val player: Player, var time: Int)

    private val resistancePlayers = mutableListOf<ResistancePlayer>()

    fun add(player: Player, tick: Int) {
        if (resistancePlayers.any {
                if (it.player == player) {
                    it.time = max(it.time, tick)
                    return@any true
                }
                false
            }) return

        player.getAttribute(Attribute.FALL_DAMAGE_MULTIPLIER)?.addTransientModifier(AttributeModifier(FALL_DAMAGE_RESISTANCE_KEY, -1000.0, AttributeModifier.Operation.ADD_NUMBER))
        resistancePlayers.add(ResistancePlayer(player, tick))
    }

    @EventHandler
    fun onTick(event: ServerTickStartEvent) {
        resistancePlayers.removeAll{
            it.time--
            if (it.time < 1) {
                it.player.getAttribute(Attribute.FALL_DAMAGE_MULTIPLIER)?.removeModifier(FALL_DAMAGE_RESISTANCE_KEY)
                return@removeAll true
            }

            return@removeAll false
        }
    }
}