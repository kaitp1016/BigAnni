package me.kaitp1016.biganni.utils

import me.kaitp1016.biganni.utils.MCUtils.toMC
import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.block.state.BlockState
import org.bukkit.World

class LevelBlockPos {
    val world: World
    val level: ServerLevel
    val x: Int
    val y: Int
    val z: Int

    constructor(world: World,x: Int,y: Int,z: Int) {
        this.world = world
        this.level = world.toMC()
        this.x = x
        this.y = y
        this.z = z
    }

    constructor(level: ServerLevel,x: Int,y: Int,z: Int) {
        this.level = level
        this.world = level.world
        this.x = x
        this.y = y
        this.z = z
    }

    fun getBlock(): BlockState {
        return level.getBlockState(BlockPos(x,y,z))
    }

    fun toBlockPos(): BlockPos {
        return BlockPos(x,y,z)
    }

    override fun equals(other: Any?): Boolean {
        return other is LevelBlockPos && other.x == this.x && other.y == this.y && other.z == this.z
    }

    override fun hashCode(): Int {
        var result = x
        result = 31 * result + y
        result = 31 * result + z
        result = 31 * result + world.hashCode()
        result = 31 * result + level.hashCode()
        return result
    }
}