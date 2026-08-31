package me.kaitp1016.biganni.config

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import me.kaitp1016.biganni.game.AnniTeam
import me.kaitp1016.biganni.gson
import me.kaitp1016.biganni.mc
import me.kaitp1016.biganni.plugin
import me.kaitp1016.biganni.utils.LevelBlockPos
import me.kaitp1016.biganni.utils.MCUtils.toMC
import me.kaitp1016.biganni.utils.Utils.reloadWorld
import net.kyori.adventure.key.Key
import net.minecraft.core.registries.Registries
import net.minecraft.resources.Identifier
import net.minecraft.resources.ResourceKey
import net.minecraft.world.item.Item
import net.minecraft.world.item.Items
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.phys.AABB
import org.bukkit.Bukkit
import org.bukkit.Location
import java.io.File
import kotlin.jvm.optionals.getOrNull

object Config {
    val MAPS_DIRECTORY = File(plugin.dataFolder, "maps")

    data class TeamConfig(val name: String, val nexus: LevelBlockPos, val health: Int, val spawn: Location, val color: String, val baseArea: AABB, val nexusWarp: Location, val witchLocation: Location, val riftLocation: Location, val bossSpawn: Location, val teamWool: Item, val teamDoorBlock: Block) {
        companion object {
            fun fromJson(json: JsonObject): TeamConfig {
                val name = json.get("name").asString
                val nexus = parseLevelBlockPos(json.get("nexus").asJsonObject)
                val health = json.get("health").asInt
                val spawn = parseLocation(json.get("spawn").asJsonObject)
                val color = json.get("color").asString
                val baseArea = parseAABB(json.get("base_area").asJsonObject)
                val nexusWarp = parseLocation(json.get("nexus_warp").asJsonObject)
                val witchLocation = parseLocation(json.get("witch_location").asJsonObject)
                val riftLocation = parseLocation(json.get("rift_location").asJsonObject)
                val bossSpawn = parseLocation(json.get("boss_spawn").asJsonObject)
                val teamWool = getItem(json.get("team_wool").asString) ?: Items.WHITE_WOOL
                val teamDoorBlock = getBlock(json.get("team_door_block").asString) ?: Blocks.GLASS_PANE

                return TeamConfig(name, nexus, health, spawn, color, baseArea, nexusWarp, witchLocation, riftLocation, bossSpawn, teamWool, teamDoorBlock)
            }
        }

        fun create(): AnniTeam {
            return AnniTeam(name, nexus, health, color, spawn, baseArea, nexusWarp, witchLocation, riftLocation, bossSpawn, teamWool, teamDoorBlock)
        }
    }

    data class MapConfig(val name: String, val dimension: Identifier, val phaseTime: Int, val bossPortals: List<LevelBlockPos>, val bossLocation: Location, val teams: List<TeamConfig>, val blockedClasses: List<String>, val doubleNexusDamage: Boolean) {
        fun reloadWorld() {
            bossLocation.reloadWorld()

            bossPortals.forEach {
                it.reloadWorld()
            }

            teams.forEach {
                it.nexusWarp.reloadWorld()
                it.bossSpawn.reloadWorld()
                it.riftLocation.reloadWorld()
                it.spawn.reloadWorld()
                it.witchLocation.reloadWorld()
                it.nexus.reloadWorld()
            }
        }

        companion object {
            fun fromJson(json: JsonObject): MapConfig {
                val name = json.get("name").asString
                val phaseTime = json.get("phase_time").asInt
                val bossPortals = parseLevelBlockPoses(json.get("boss_portals").asJsonArray)
                val bossLocation = parseLocation(json.get("boss_location").asJsonObject)
                val teams = parseTeams(json.get("teams").asJsonArray)
                val blockedClasses = json.get("blocked_classes").asJsonArray.map { it.asString }
                val doubleNexusDamage = json.get("double_nexus_damage")?.asBoolean ?: true
                val dimension = json.get("dimension")?.asString?.let { Identifier.parse(it) } ?: bossLocation.world.toMC().dimension().identifier()

                return MapConfig(name, dimension, phaseTime, bossPortals, bossLocation, teams, blockedClasses, doubleNexusDamage)
            }

            fun default(): MapConfig {
                getMap("default")?.let { return it }

                val name = "Coastal"
                val dimension = Identifier.parse("sys:coastal")
                val phaseTime = 9600
                val world = Bukkit.getWorld(Key.key("sys:coastal"))!!
                val bossPortals = listOf(LevelBlockPos(world, 95, -48, 3), LevelBlockPos(world, -97, -48, -3))
                val bossLocation = Location(world, 10000.0, 0.0, 0.0)
                val blockedClasses = listOf<String>()
                val teams = listOf(
                    TeamConfig("Blue", LevelBlockPos(world, -118, -51, 190), 75, Location(world, -108.5, -38.0, 180.5, -135f, 0f), "§9", AABB(-70.0, -256.0, 138.0, -140.0, 312.0, 217.0), Location(world, -117.0, -52.0, 187.0), Location(world, 6.5, -47.0, 127.5), Location(world, 60.5, -48.0, 140.0, 90f, 0f), Location(world, 10000.0, 0.0, 0.0), Items.BLUE_WOOL, Blocks.BLUE_STAINED_GLASS_PANE),
                    TeamConfig("Red", LevelBlockPos(world, 118, -51, -188), 75, Location(world, 109.5, -38.0, -177.5, 45f, 0f), "§c", AABB(77.0, -256.0, -133.0, 115.0, 312.0, -225.0), Location(world, 118.0, -52.0, -185.0), Location(world, -5.5, -47.0, -124.5), Location(world, -60.5, -48.0, -140.0, -90f, 0f), Location(world, 10000.0, 0.0, 0.0), Items.RED_WOOL, Blocks.RED_STAINED_GLASS_PANE)
                )

                val doubleNexusDamage = false

                return MapConfig(name, dimension, phaseTime, bossPortals, bossLocation, teams, blockedClasses, doubleNexusDamage)
            }
        }
    }

    private fun getItem(id: String): Item? {
        return mc.registryAccess().get(ResourceKey.create(Registries.ITEM, Identifier.parse(id))).getOrNull()?.value()
    }

    private fun getBlock(id: String): Block? {
        return mc.registryAccess().get(ResourceKey.create(Registries.BLOCK, Identifier.parse(id))).getOrNull()?.value()
    }

    private fun parseLevelBlockPos(json: JsonObject): LevelBlockPos {
        val worldName = json.get("world").asString
        val world = Bukkit.getWorld(Key.key(worldName))!!
        val x = json.get("x").asInt
        val y = json.get("y").asInt
        val z = json.get("z").asInt
        return LevelBlockPos(world, x, y, z)
    }

    private fun parseLocation(json: JsonObject): Location {
        val world = Bukkit.getWorld(Key.key(json.get("world").asString))!!
        val x = json.get("x").asDouble
        val y = json.get("y").asDouble
        val z = json.get("z").asDouble
        val yaw = json.get("yaw")?.asFloat ?: 0f
        val pitch = json.get("pitch")?.asFloat ?: 0f
        return Location(world, x, y, z, yaw, pitch)
    }

    private fun parseAABB(json: JsonObject): AABB {
        val minX = json.get("min_x").asDouble
        val minY = json.get("min_y").asDouble
        val minZ = json.get("min_z").asDouble
        val maxX = json.get("max_x").asDouble
        val maxY = json.get("max_y").asDouble
        val maxZ = json.get("max_z").asDouble

        return AABB(minX, minY, minZ, maxX, maxY, maxZ)
    }

    private fun parseLevelBlockPoses(json: JsonArray): List<LevelBlockPos> {
        return json.map { parseLevelBlockPos(it.asJsonObject) }
    }

    private fun parseTeams(json: JsonArray): List<TeamConfig> {
        return json.map { TeamConfig.fromJson(it.asJsonObject) }
    }

    fun getMap(name: String): MapConfig? {
        val json: JsonObject

        try {
            json = gson.fromJson(File(MAPS_DIRECTORY, "${name}.json").readText(), JsonObject::class.java)
        } catch (e: Throwable) {
            return null
        }

        return MapConfig.fromJson(json)
    }

    fun getMapNames(): List<String> {
        return MAPS_DIRECTORY.listFiles().filter { it.isFile }.map { it.nameWithoutExtension }
    }
}