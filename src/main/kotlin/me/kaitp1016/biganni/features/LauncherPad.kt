package me.kaitp1016.biganni.features

import com.destroystokyo.paper.event.server.ServerTickStartEvent
import me.kaitp1016.biganni.utils.FallDamageResistance
import me.kaitp1016.biganni.utils.MCUtils.toMC
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.level.block.Blocks
import org.bukkit.Sound
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.util.Vector

object LauncherPad: Listener {
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
                this.add(Vector(0.0, 1.0, 0.0))
            }

            FallDamageResistance.add(bukkitPlayer,200)
            bukkitPlayer.playSound(bukkitPlayer, Sound.ENTITY_WITHER_SHOOT,1f,2f)
        }
    }
}