package me.kaitp1016.biganni.config

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import me.kaitp1016.biganni.game.AnniTeam
import me.kaitp1016.biganni.gson
import me.kaitp1016.biganni.plugin
import me.kaitp1016.biganni.utils.LevelBlockPos
import net.kyori.adventure.key.Key
import net.minecraft.world.phys.AABB
import org.bukkit.Bukkit
import org.bukkit.Location
import java.io.File

object Config {
    val MAPS_DIRECTORY = File(plugin.dataFolder,"maps")

    data class TeamConfig(val name: String, val nexus: LevelBlockPos, val health: Int,val spawn: Location,val color: String,val baseArea: AABB,val nexusWarp: Location,val witchLocation: Location) {
        companion object {
            fun fromJson(json: JsonObject): TeamConfig {
                val name = json.get("name").asString
                val nexus = parseLevelBlockPos(json.get("nexus").asJsonObject)
                val health = json.get("health").asInt
                val spawn = parseLocation(json.get("spawn").asJsonObject)
                val color = json.get("color").asString
                val baseArea = parseAABB(json.get("baseArea").asJsonObject)
                val nexusWarp = parseLocation(json.get("nexusWarp").asJsonObject)
                val witchLocation = parseLocation(json.get("witchLocation").asJsonObject)

                return TeamConfig(name,nexus,health,spawn,color,baseArea,nexusWarp,witchLocation)
            }
        }

        fun create(): AnniTeam {
            return AnniTeam(name,nexus,health,color,spawn,baseArea,nexusWarp,witchLocation)
        }
    }

    data class MapConfig(val name: String, val phaseTime: Int, val bossPortals: List<LevelBlockPos>, val bossLocation: Location,val teams: List<TeamConfig>) {
        companion object {
            fun fromJson(json: JsonObject): MapConfig {
                val name = json.get("name").asString
                val phaseTime = json.get("phaseTime").asInt
                val bossPortals = parseLevelBlockPoses(json.get("bossPortals").asJsonArray)
                val bossLocation = parseLocation(json.get("bossLocation").asJsonObject)
                val teams = parseTeams(json.get("teams").asJsonArray)

                return MapConfig(name,phaseTime,bossPortals,bossLocation,teams)
            }

            fun default(): MapConfig {
                val name = "Coastal"
                val phaseTime = 9600
                val world = Bukkit.getWorld(Key.key("sys:coastal"))!!
                val bossPortals = listOf(LevelBlockPos(world,95, -48, 3), LevelBlockPos(world,-97, -48 ,-3))
                val bossLocation = Location(world, 10000.0, 0.0, 0.0)
                val teams = listOf(
                    TeamConfig("Blue", LevelBlockPos(world,-118,-51,190),75,Location(world,-108.5, -38.0, 180.5, -135f, 0f),"§9",AABB(-70.0, -256.0, 138.0,-140.0, 312.0,217.0),Location(world,-117.0, -52.0, 187.0),Location(world,6.5,-47.0,127.5)))
                    TeamConfig("Red", LevelBlockPos(world,118,-51,-188),75, Location(world,109.5, -38.0, -177.5, 45f, 0f),"§c", AABB(77.0, -256.0, -133.0, 115.0, 312.0, -225.0),Location(world,118.0, -52.0, -185.0),Location(world,-5.5, -47.0, -124.5))

                return MapConfig(name,phaseTime,bossPortals,bossLocation,teams)
            }
        }
    }

    private fun parseLevelBlockPos(json: JsonObject): LevelBlockPos {
        val worldName = json.get("world").asString
        val world = Bukkit.getWorld(Key.key(worldName))!!
        val x = json.get("x").asInt
        val y = json.get("y").asInt
        val z = json.get("z").asInt
        return LevelBlockPos(world,x,y,z)
    }

    private fun parseLocation(json: JsonObject): Location {
        val world = Bukkit.getWorld(Key.key(json.get("world").asString))!!
        val x = json.get("x").asDouble
        val y = json.get("y").asDouble
        val z = json.get("z").asDouble
        val yaw = json.get("yaw")?.asFloat ?: 0f
        val pitch = json.get("pitch")?.asFloat ?: 0f
        return Location(world,x,y,z,yaw,pitch)
    }

    private fun parseAABB(json: JsonObject): AABB {
        val minX = json.get("minX").asDouble
        val minY = json.get("minY").asDouble
        val minZ = json.get("minZ").asDouble
        val maxX = json.get("maxX").asDouble
        val maxY = json.get("maxY").asDouble
        val maxZ = json.get("maxZ").asDouble

        return AABB(minX,minY,minZ,maxX,maxY,maxZ)
    }

    private fun parseLevelBlockPoses(json: JsonArray): List<LevelBlockPos> {
        return json.map { parseLevelBlockPos(it.asJsonObject) }
    }

    private fun parseTeams(json: JsonArray):List<TeamConfig> {
        return json.map { TeamConfig.fromJson(it.asJsonObject) }
    }

    fun getMap(name: String): MapConfig? {
        val json: JsonObject

        try {
            json = gson.fromJson(File(MAPS_DIRECTORY, "${name}.json").readText(), JsonObject::class.java)
        }
        catch (e: Throwable) {
            return null
        }

        return MapConfig.fromJson(json)
    }

    fun getMapNames():List<String> {
        return MAPS_DIRECTORY.listFiles().filter { it.isFile }.map{ it.nameWithoutExtension }
    }
}