package me.kaitp1016.biganni.game

import net.minecraft.core.BlockPos
import net.minecraft.world.level.Level
import org.bukkit.GameMode
import org.bukkit.Location
import org.bukkit.World
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockExplodeEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.entity.EntityExplodeEvent
import org.bukkit.event.player.PlayerBucketEmptyEvent
import org.bukkit.event.player.PlayerBucketFillEvent

object MapProtector: Listener {
    @EventHandler
    fun onPlace(event: BlockPlaceEvent) {
        val block = event.blockPlaced
        if (isProtected(block.location) && !canBypass(event.player)) {
            event.isCancelled = true
        }
    }

    @EventHandler
    fun onBreak(event: BlockBreakEvent) {
        val block = event.block
        if (isProtected(block.location) && !canBypass(event.player)) {
            event.isCancelled = true
        }
    }

    @EventHandler
    fun onExplode(event: EntityExplodeEvent) {
        event.blockList().removeIf { block -> isProtected(block.location) }
    }

    @EventHandler
    fun onBucketEmpty(event: PlayerBucketEmptyEvent) {
        val block = event.block

        if (isProtected(block.location) && !canBypass(event.player)) {
            event.isCancelled = true
        }
    }

    @EventHandler
    fun onBucketFill(event: PlayerBucketFillEvent) {
        val block = event.block

        if (isProtected(block.location) && !canBypass(event.player)) {
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

    private fun canBypass(player: Player): Boolean {
        return player.gameMode == GameMode.CREATIVE
    }
}