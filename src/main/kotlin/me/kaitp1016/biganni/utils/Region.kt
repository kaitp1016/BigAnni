package me.kaitp1016.biganni.utils

import me.kaitp1016.biganni.utils.MCUtils.toMC
import net.minecraft.resources.Identifier
import net.minecraft.world.level.Level
import org.bukkit.Location
import org.bukkit.World
import kotlin.math.max
import kotlin.math.min

class Region {
    val minX: Int
    val minY: Int
    val minZ: Int
    val maxX: Int
    val maxY: Int
    val maxZ: Int
    val dimension: Identifier

    constructor(minX: Int, minY: Int, minZ: Int, maxX: Int, maxY: Int, maxZ: Int, dimension: Identifier) {
        this.minX = min(minX,maxX)
        this.minY = min(minY,maxY)
        this.minZ = min(minZ,maxZ)
        this.maxX = max(minX,maxX)
        this.maxY = max(minY,maxY)
        this.maxZ = max(minZ,maxZ)
        this.dimension = dimension
    }

    fun contains(x: Int, y: Int, z: Int): Boolean {
        return x >= this.minX && x <= this.maxX && y >= this.minY && y <= this.maxY && z >= this.minZ && z <= this.maxZ
    }

    fun contains(level: Level, x: Int, y: Int, z: Int): Boolean {
        return isInDimension(level) && contains(x, y, z)
    }

    fun contains(world: World, x: Int, y: Int, z: Int): Boolean {
        return contains(world.toMC(), x, y, z)
    }

    fun contains(location: Location): Boolean {
        return contains(location.world, location.blockX, location.blockY, location.blockZ)
    }

    fun isInDimension(world: World): Boolean {
        return isInDimension(world.toMC())
    }

    fun isInDimension(level: Level): Boolean {
        return level.dimension().identifier() == dimension
    }
}