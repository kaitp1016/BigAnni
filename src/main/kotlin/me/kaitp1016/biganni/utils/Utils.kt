package me.kaitp1016.biganni.utils

import org.bukkit.event.block.Action

object Utils {
    fun Double.toIntCorrect(): Int {
        if (this > 0) return this.toInt()
        else return this.toInt() - 1
    }
}