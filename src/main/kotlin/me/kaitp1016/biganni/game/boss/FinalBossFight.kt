package me.kaitp1016.biganni.game.boss

import com.destroystokyo.paper.event.server.ServerTickStartEvent
import me.kaitp1016.biganni.game.Game
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.title.Title
import org.bukkit.Bukkit
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

object FinalBossFight: Listener {
    const val TELEPORT_TICK = 600

    var isWarping = false
    var warpingTick = -1

    fun reset() {
        isWarping = false
        warpingTick = -1
    }

    fun start() {
        isWarping = true
        warpingTick = TELEPORT_TICK
    }

    @EventHandler
    fun onTick(event: ServerTickStartEvent) {
        if (!isWarping) return
        warpingTick--

        if (warpingTick % 20 == 0) {
            Bukkit.getOnlinePlayers().forEach { player ->
                if (player.isSneaking || warpingTick > 200) {
                    player.sendActionBar(Component.text("Teleport commencing in ").color(NamedTextColor.GOLD).append(Component.text("${warpingTick / 20}").color(NamedTextColor.GREEN).append(Component.text("s").color(NamedTextColor.GREEN))))
                } else {
                    player.sendActionBar(Component.text("Teleport commencing in ").color(NamedTextColor.GOLD).append(Component.text("${warpingTick / 20}").color(NamedTextColor.GREEN).append(Component.text("s").color(NamedTextColor.GREEN).append(Component.text(" - ").color(NamedTextColor.GRAY).append(Component.text("SNEAK!").color(NamedTextColor.DARK_RED).decoration(TextDecoration.BOLD, true))))))
                    player.playSound(player, Sound.UI_BUTTON_CLICK, 1f, 1f)
                }

                val distance = 1.5
                val amount = 32
                val world = player.world

                repeat(amount) {
                    val angle = 360f / amount * it * PI / 180f
                    val x = player.x + distance * cos(angle)
                    val z = player.z + distance * sin(angle)
                    val y = player.y + 0.8

                    Particle.HAPPY_VILLAGER.builder().location(world, x, y, z).receivers(32, true).count(0).offset(0.0, 0.0, 0.0).spawn()
                }

            }
        }
        if (warpingTick < 1) {
            isWarping = false
            warpingTick = -1

            val boss = BossManager.boss
            if (boss != null) {
                Bukkit.getEntity(boss)?.remove()
            }

            BossManager.spawn()

            Bukkit.getOnlinePlayers().forEach { player ->
                if (player.isSneaking) {
                    player.teleport(Game.map.bossLocation)
                    player.showTitle(Title.title(Component.text("FIGHT!").decoration(TextDecoration.BOLD, true).color(NamedTextColor.GOLD), Component.text("Final Boss Fight.").color(NamedTextColor.GRAY)))
                }
            }
        }
    }
}