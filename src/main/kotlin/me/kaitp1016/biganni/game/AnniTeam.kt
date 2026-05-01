package me.kaitp1016.biganni.game

import net.minecraft.core.BlockPos
import org.bukkit.Location

class AnniTeam {
    val name: String
    val nexus: BlockPos
    var health: Int
    val spawn: Location
    val color: String

    constructor(name: String, nexus: BlockPos,color: String,spawn: Location) {
        this.name = name
        this.nexus = nexus
        this.color = color
        this.spawn = spawn
        this.health = 150
    }
}