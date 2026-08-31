package me.kaitp1016.biganni.game

import io.papermc.paper.world.PaperWorldLoader
import me.kaitp1016.biganni.anniclass.AnniClasses
import me.kaitp1016.biganni.features.DelayingBlock
import me.kaitp1016.biganni.features.PrivateStand
import me.kaitp1016.biganni.features.RespawnBlocks
import me.kaitp1016.biganni.features.TeamDoor
import me.kaitp1016.biganni.mc
import me.kaitp1016.biganni.plugin
import net.minecraft.core.registries.Registries
import net.minecraft.resources.Identifier
import net.minecraft.resources.ResourceKey
import net.minecraft.world.level.dimension.LevelStem
import org.bukkit.Bukkit
import org.bukkit.World

object WorldResetter {
    private val WORLDS_FOLDER = plugin.dataFolder.resolve("worlds")

    fun reset(world: World, id: String): Boolean {
        try {
            val teleportLocation = mc.overworld().world.spawnLocation
            val dimension = ResourceKey.create(Registries.DIMENSION, Identifier.fromNamespaceAndPath(world.key.namespace,world.key.key))

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
            resetCustomBlocks()

            return true
        }
        catch (e: Throwable) {
            e.printStackTrace()
            return false
        }
    }

    private fun resetCustomBlocks() {
        AnniClasses.ALL_CLASSES.forEach {
            it.resetBlocks()
        }

        DelayingBlock.resetBlocks()
        PrivateStand.resetBlocks()
        RespawnBlocks.resetBlocks()
        TeamDoor.resetBlocks()
    }
}