package me.kaitp1016.biganni.game

import me.kaitp1016.biganni.utils.LevelBlockPos
import me.kaitp1016.biganni.utils.MCUtils.toMC
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.LightningBolt
import net.minecraft.world.entity.monster.Witch
import net.minecraft.world.phys.AABB
import org.bukkit.Location
import java.util.*

class AnniTeam {
    val name: String
    val nexus: LevelBlockPos
    var health: Int
    val spawn: Location
    val color: String
    val baseArea: AABB
    val nexusWarp: Location
    val witchLocation: Location
    var witch: UUID? = null

    constructor(name: String, nexus: LevelBlockPos, health:Int,color: String,spawn: Location, baseArea: AABB, nexusWarp: Location,witchLocation: Location) {
        this.name = name
        this.nexus = nexus
        this.color = color
        this.spawn = spawn
        this.health = health
        this.baseArea = baseArea
        this.nexusWarp = nexusWarp
        this.witchLocation = witchLocation
    }

    fun spawnWitch() {
        val level = witchLocation.world.toMC()

        val witch = Witch(EntityType.WITCH, level).apply {
            this.setPos(witchLocation.x,witchLocation.y,witchLocation.z)
            this.persistenceRequired = true
        }

        level.addFreshEntity(witch)

        level.addFreshEntity(LightningBolt(EntityType.LIGHTNING_BOLT, level).apply {
            visualOnly = true
            flashes = 1
            this.setPos(witchLocation.x,witchLocation.y,witchLocation.z)
        })

        this.witch = witch.uuid
    }
}