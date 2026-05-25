package me.kaitp1016.biganni.game

import org.bukkit.Bukkit
import org.bukkit.boss.BarColor
import org.bukkit.boss.BarStyle
import org.bukkit.entity.Player

object BossBarManager {
    val bossbar = Bukkit.createBossBar("Annihilation", BarColor.BLUE, BarStyle.SOLID)

    fun onJoin(player: Player) {
        bossbar.addPlayer(player)
    }

    fun onQuit(player: Player) {
        bossbar.removePlayer(player)
    }

    fun setTitle(title: String) {
        bossbar.setTitle(title)
    }

    fun setColor(color: BarColor) {
        bossbar.color = color
    }

    fun setProgress(progress: Double) {
        bossbar.progress = progress
    }
}