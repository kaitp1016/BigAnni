package me.kaitp1016.biganni.game

import io.papermc.paper.world.PaperWorldLoader
import me.kaitp1016.biganni.mc
import me.kaitp1016.biganni.plugin
import me.kaitp1016.biganni.utils.MCUtils.toMC
import net.minecraft.core.registries.Registries
import net.minecraft.resources.ResourceKey
import net.minecraft.world.level.dimension.LevelStem
import org.bukkit.Bukkit
import org.bukkit.World

object WorldResetter {
    private val WORLDS_FOLDER = plugin.dataFolder.resolve("worlds")

    fun reset(world: World, id: String): Boolean {
        try {
            val teleportLocation = Bukkit.getWorld("world")!!.spawnLocation
            val dimension = world.toMC().dimension()

            Bukkit.getOnlinePlayers().forEach { player ->
                if (player.world == world) {
                    player.teleport(teleportLocation)
                }
            }

            val backupFolder = WORLDS_FOLDER.resolve(id)
            if (!backupFolder.exists()) {
                return false
            }

            val worldFolder = world.worldFolder

            Bukkit.unloadWorld(world, false)
            worldFolder.deleteRecursively()
            backupFolder.copyRecursively(worldFolder)

            val stem = mc.registryAccess().get(ResourceKey.create(Registries.LEVEL_STEM, dimension.identifier())).get()
            val loader = PaperWorldLoader.create(mc, id)

            val previousLevels = mc.allLevels.toList()

            loader::class.java.getDeclaredMethod("loadInitialWorld", LevelStem::class.java, Boolean::class.java).apply {
                isAccessible = true
            }.invoke(loader, stem.value(), true)

            val loadedLevel = mc.allLevels.find { it !in previousLevels }!!
            mc.prepareLevel(loadedLevel)

            Game.map.reloadWorld()

            return true
        }
        catch (e: Throwable) {
            e.printStackTrace()
            return false
        }
    }
}