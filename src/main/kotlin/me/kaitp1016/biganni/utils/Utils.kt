package me.kaitp1016.biganni.utils

import net.minecraft.core.BlockPos
import net.minecraft.world.level.Level

object Utils {
    fun Double.toIntCorrect(): Int {
        if (this > 0) return this.toInt()
        else return this.toInt() - 1
    }

    fun Level.isFullBlock(pos: BlockPos): Boolean {
        val state = getBlockState(pos)
        state.occlusionShape
        return state.getCollisionShape(this, pos).`moonrise$isFullBlock`()
    }
}