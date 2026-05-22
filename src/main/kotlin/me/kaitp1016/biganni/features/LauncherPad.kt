package me.kaitp1016.biganni.features

import com.destroystokyo.paper.event.server.ServerTickStartEvent
import me.kaitp1016.biganni.utils.FallDamageResistance
import me.kaitp1016.biganni.utils.MCUtils.toMC
import net.minecraft.world.level.block.Blocks
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.util.Vector

object LauncherPad: Listener {
    const val LAUNCHER_PAD_COOLDOWN = 100

    data class LauncherPadData(val block: Material,val strength: Float,val upwards: Float = 0f)
    data class LauncherPadCooldown(val player: Player,var tick: Int)

    val launcherPadDatas = arrayOf(
        LauncherPadData(Material.IRON_BLOCK,strength = 2f),
        LauncherPadData(Material.REDSTONE_LAMP,strength = 3f),
        LauncherPadData(Material.DIAMOND_BLOCK,strength = 4f),

        LauncherPadData(Material.EMERALD_BLOCK,strength = 1.2f, upwards = 7f),
        LauncherPadData(Material.GOLD_BLOCK,strength = 1.4f, upwards = 9f),
    )

    val cooldowns = mutableListOf<LauncherPadCooldown>()

    @EventHandler
    fun onMove(event: PlayerMoveEvent) {
        val player = event.player.toMC()
        if (!player.onGround) return

        val pos = player.blockPosition()
        val level = player.level()
        if (level.getBlockState(pos).block != Blocks.STONE_PRESSURE_PLATE) return

        val underBlock = level.getBlockState(pos.offset(0, -1, 0)).bukkitMaterial
        val pad = launcherPadDatas.find { it.block == underBlock }
        if (pad == null) return

        val bukkitPlayer = player.bukkitEntity
        if (cooldowns.any { it.player == bukkitPlayer }) return

        bukkitPlayer.velocity = bukkitPlayer.location.direction.clone().apply {
            this.setY(0.0)
            this.normalize()
            this.multiply(pad.strength * 1.5f)
            this.add(Vector(0.0, 0.65 + pad.upwards * 0.1f, 0.0))
        }

        FallDamageResistance.add(bukkitPlayer, 200)
        bukkitPlayer.world.playSound(bukkitPlayer.location, Sound.ENTITY_WITHER_SHOOT, 1f, 2f)

        cooldowns.add(LauncherPadCooldown(bukkitPlayer,LAUNCHER_PAD_COOLDOWN))
    }

    @EventHandler
    fun onTick(event: ServerTickStartEvent) {
        if (cooldowns.isEmpty()) return

        cooldowns.removeAll {
            it.tick--
            return@removeAll it.tick < 0
        }
    }
}