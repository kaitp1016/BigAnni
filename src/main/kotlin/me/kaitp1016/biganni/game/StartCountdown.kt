package me.kaitp1016.biganni.game

import com.destroystokyo.paper.event.server.ServerTickStartEvent
import org.bukkit.Bukkit
import org.bukkit.Sound
import org.bukkit.boss.BarColor
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener

object StartCountdown: Listener {
    const val START_TICK = 2401

    var isStarted = false
    var tick = -1

    fun reset() {
        isStarted = false
        tick = -1
    }

    fun start() {
        isStarted = true
        tick = START_TICK

        BossBarManager.setColor(BarColor.GREEN)

        Bukkit.getOnlinePlayers().forEach {
            it.playSound(it, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 0f)
        }
    }

    @EventHandler
    fun onTick(event: ServerTickStartEvent) {
        if (!isStarted) return

        tick--

        val min = tick / 20 / 60
        val sec = tick / 20 % 60

        if (tick % 20 == 0) {
            BossBarManager.setTitle("Starting in ${min}:${if (sec > 9) "$sec" else "0${sec}"}")
            BossBarManager.setProgress(tick.toDouble() / START_TICK)

            if (tick in 21..201) {
                Bukkit.getOnlinePlayers().forEach {
                    it.playSound(it, Sound.UI_BUTTON_CLICK, 1f, 1f)
                }
            }
        }

        if (tick == 19) {
            Bukkit.getOnlinePlayers().forEach {
                it.playSound(it, Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.3f)
            }
        }

        if (tick < 1) {
            isStarted = false
            tick = -1

            Game.start()
        }
    }
}