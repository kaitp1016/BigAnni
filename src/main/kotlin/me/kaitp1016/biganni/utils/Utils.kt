package me.kaitp1016.biganni.utils

object Utils {
    fun Double.toIntCorrect(): Int {
        if (this > 0) return this.toInt()
        else return this.toInt() - 1
    }
}