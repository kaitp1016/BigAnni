package me.kaitp1016.biganni.modifiers

import com.destroystokyo.paper.event.server.ServerTickStartEvent
import me.kaitp1016.biganni.PLUGIN_ID
import me.kaitp1016.biganni.utils.MCUtils.toMC
import net.minecraft.resources.Identifier
import net.minecraft.world.entity.ai.attributes.AttributeModifier
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.block.Blocks
import org.bukkit.Sound
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.util.Vector

object LauncherPad: Listener {
    val LAUNCHER_PAD_FEATHER_FALLING_IDENTFIER = Identifier.fromNamespaceAndPath(PLUGIN_ID,"launcehr_pad_feather_falling")

    data class FeatherFalling(val player: Player,var tick: Int = 200)

    val featherFallings = mutableListOf<FeatherFalling>()

    @EventHandler
    fun onMove(event: PlayerMoveEvent) {
        val player = event.player.toMC()
        if (!player.onGround) return

        val pos = player.blockPosition()
        val level = player.level()

        if (level.getBlockState(pos).block == Blocks.STONE_PRESSURE_PLATE && level.getBlockState(pos.offset(0,-1,0)).block == Blocks.IRON_BLOCK) {

            val bukkitPlayer = player.bukkitEntity

            bukkitPlayer.velocity = bukkitPlayer.location.direction.clone().apply {
                this.setY(0.0)
                this.normalize()
                this.multiply(3f)
                this.add(Vector(0.0,1.0,0.0))
            }

            val fallDamageAttribute = player.getAttribute(Attributes.FALL_DAMAGE_MULTIPLIER)

            if (fallDamageAttribute?.hasModifier(LAUNCHER_PAD_FEATHER_FALLING_IDENTFIER) == false) {
                fallDamageAttribute.addTransientModifier(AttributeModifier(LAUNCHER_PAD_FEATHER_FALLING_IDENTFIER,-1000.0, AttributeModifier.Operation.ADD_VALUE))
            }

            bukkitPlayer.playSound(bukkitPlayer,Sound.ENTITY_WITHER_SHOOT,1f,2f)

            val original = featherFallings.find { it.player == player }
            if (original != null) {
                original.tick = 200
            }
            else {
                featherFallings.add(FeatherFalling(player,200))
            }
        }
    }

    @EventHandler
    fun onTick(evnet: ServerTickStartEvent) {
        if (featherFallings.isEmpty()) return

        featherFallings.removeAll{
            it.tick--
            if (it.tick < 1) {
                it.player.getAttribute(Attributes.FALL_DAMAGE_MULTIPLIER)?.removeModifier(LAUNCHER_PAD_FEATHER_FALLING_IDENTFIER)
                return@removeAll true
            }
            return@removeAll false
        }
    }
}