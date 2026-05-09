package me.kaitp1016.biganni.game

import me.kaitp1016.biganni.plugin
import me.kaitp1016.biganni.utils.MCUtils.toMC
import net.minecraft.core.BlockPos
import net.minecraft.world.entity.EntityType
import net.minecraft.world.phys.AABB
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.NamespacedKey
import org.bukkit.entity.Witch
import org.bukkit.persistence.PersistentDataType
import java.util.UUID

class AnniTeam {
    val name: String
    val nexus: BlockPos
    var health: Int
    val spawn: Location
    val color: String
    val baseArea: AABB
    val nexusWarp: Location
    val witchLocation: Location
    var witch: UUID? = null

    constructor(name: String, nexus: BlockPos,color: String,spawn: Location, baseArea: AABB, nexusWarp: Location,witchLocation: Location) {
        this.name = name
        this.nexus = nexus
        this.color = color
        this.spawn = spawn
        this.health = 75
        this.baseArea = baseArea
        this.nexusWarp = nexusWarp
        this.witchLocation = witchLocation
    }

    fun spawnWitch() {
        val level = witchLocation.world.toMC()
        val witch = net.minecraft.world.entity.monster.Witch(EntityType.WITCH, level).apply {
            this.setPos(witchLocation.x,witchLocation.y,witchLocation.z)
            this.checkDespawn()
            this.persistenceRequired = true
        }

        level.addFreshEntity(witch)
        this.witch = witch.uuid
    }
}