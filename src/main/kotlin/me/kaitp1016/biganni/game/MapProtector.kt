package me.kaitp1016.biganni.game

import net.minecraft.core.BlockPos
import net.minecraft.world.level.Level
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.Location
import org.bukkit.World
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockPlaceEvent

object MapProtector: Listener {
    @EventHandler
    fun onPlace(event: BlockPlaceEvent) {
        val block = event.blockPlaced
        if (isProtected(block.location) && event.player.gameMode != GameMode.CREATIVE) {
            event.isCancelled = true
        }
    }

    @EventHandler
    fun onBreak(event: BlockBreakEvent) {
        val block = event.block
        if (isProtected(block.location) && event.player.gameMode != GameMode.CREATIVE) {
            event.isCancelled = true
        }
    }

    fun isProtected(location: Location, ignoreOutbound: Boolean = false): Boolean {
        return isProtected(location.world, location.blockX, location.blockY, location.blockZ, ignoreOutbound)
    }

    fun isProtected(level: Level,pos: BlockPos, ignoreOutbound: Boolean = false): Boolean {
        return isProtected(level.world, pos.x, pos.y, pos.z, ignoreOutbound)
    }

    fun isProtected(world: World, x: Int, y: Int, z: Int, ignoreOutbound: Boolean = false): Boolean {
        val map = Game.map
        val mapRegion = map.mapRegion
        if (!ignoreOutbound && mapRegion.isInDimension(world) && !mapRegion.contains(x, y, z)) return true

        return map.protectedRegions.any { region -> region.contains(world, x, y, z) }
    }
}