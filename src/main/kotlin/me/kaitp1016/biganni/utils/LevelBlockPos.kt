package me.kaitp1016.biganni.utils

import me.kaitp1016.biganni.utils.MCUtils.toMC
import net.minecraft.server.level.ServerLevel
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
}