package me.kaitp1016.biganni.utils

import net.minecraft.core.BlockPos
import net.minecraft.world.level.Level
import org.bukkit.Bukkit
import org.bukkit.Location

object Utils {
    fun Level.isFullBlock(pos: BlockPos): Boolean {
        val state = getBlockState(pos)
        state.occlusionShape
        return state.getCollisionShape(this, pos).`moonrise$isFullBlock`()
    }

    fun Location.reloadWorld() = apply {
        this.world = Bukkit.getWorld(world.key)
    }
}