package me.kaitp1016.biganni.game

import org.bukkit.Bukkit
import org.bukkit.boss.BarColor
import org.bukkit.boss.BarStyle
import org.bukkit.entity.Player

object BossBarManager {
    val bossbar = Bukkit.createBossBar("Annihilation", BarColor.RED, BarStyle.SOLID)

    fun onJoin(player: Player) {
        bossbar.addPlayer(player)
    }

    fun onQuit(player: Player) {
        bossbar.removePlayer(player)
    }

    fun onTick() {
        if (!Game.isStarted) return

        bossbar.progress = Game.phaseTime.toDouble() / Game.PHASE_TIME
        bossbar.setTitle("§aPhase ${Game.phase}")
    }
}