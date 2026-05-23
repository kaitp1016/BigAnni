package me.kaitp1016.biganni.features

import me.kaitp1016.biganni.utils.ItemUtils.getAnniId
import me.kaitp1016.biganni.utils.ItemUtils.setAnniItem
import me.kaitp1016.biganni.utils.MCUtils.toMC
import me.kaitp1016.biganni.utils.Scheduler
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.item.Items
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import org.bukkit.Sound
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import kotlin.math.abs

object TeamDoor: Listener {
    const val TEAM_DOOR_ITEM_ID = "anni_team_door"
    const val TEAM_DOOR_PLACE_DISTANCE = 5

    val teamsDoors = mutableMapOf<ServerLevel, HashMap<BlockPos, ServerPlayer>>()

    @EventHandler(priority = EventPriority.HIGH)
    fun onPlace(event: BlockPlaceEvent) {
        val item = event.itemInHand
        if (event.isCancelled || item.getAnniId() != TEAM_DOOR_ITEM_ID) return

        val block = event.block
        val player = event.player.toMC()
        val level = player.level()
        val pos = BlockPos(block.x,block.y,block.z)

        val doorsInLevel = teamsDoors.getOrPut(level) { HashMap() }
        if (doorsInLevel.any { it.key.distManhattan(pos) < TEAM_DOOR_PLACE_DISTANCE }) {
            player.bukkitEntity.sendMessage("近くにTeam Doorがあるため設置できません!")
            event.isCancelled = true
            return
        }

        event.isCancelled = true
        item.amount--
        event.hand

        val doorBlock = getTeamDoorBlock(player)

        Scheduler.scheduleTask(0) {
            level.setBlockAndUpdate(pos, Block.updateFromNeighbourShapes(doorBlock.defaultBlockState(),level,pos))
            level.setBlockAndUpdate(pos.offset(0,1,0), Block.updateFromNeighbourShapes(doorBlock.defaultBlockState(),level,pos.offset(0,1,0)))
        }

        doorsInLevel[pos] = player
    }

    @EventHandler
    fun onMove(event: PlayerMoveEvent) {
        val player = event.player
        val mcPlayer = player.toMC()
        val level = mcPlayer.level()
        val blocksInLevel = teamsDoors[level] ?: return

        val to = event.to
        val pos = BlockPos(to.blockX, to.blockY, to.blockZ)
        val teamDoor = blocksInLevel[pos] ?: return
        if (teamDoor.teamColor != mcPlayer.teamColor) return

        val direction = mcPlayer.position().subtract(pos.center).multiply(1.0, 0.0, 1.0).normalize()
        val isX = abs(direction.x) > abs(direction.z)
        val location = to.add(if (isX) direction.x * -1.5 else 0.0, 0.0, if (isX) 0.0 else direction.z * -1.5)
        if (!location.block.isPassable || !location.clone().add(0.0, 1.0, 0.0).block.isPassable) return

        player.teleport(location.toCenterLocation().add(0.0,-0.5,0.0))
        player.world.playSound(player.location, Sound.ENTITY_CHICKEN_EGG, 1f, 0f)
    }

    @EventHandler(priority = EventPriority.HIGH)
    fun onBreak(event: BlockBreakEvent) {
        if (event.isCancelled) return

        val player = event.player.toMC()
        val blocksInLevel = teamsDoors[player.level()] ?: return
        val block = event.block

        for (dy in -1..1) {
            val pos = BlockPos(block.x,block.y + dy,block.z)

            val miningBlock = blocksInLevel[pos]
            if (miningBlock != null) {
                blocksInLevel.remove(pos)

                val level = player.level()
                level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState())
                level.setBlockAndUpdate(pos.offset(0,1,0), Blocks.AIR.defaultBlockState())

                level.addFreshEntity(ItemEntity(player.level(),block.x + 0.5,block.y + 0.5,block.z + 0.5, createItem().toMC()!!).apply {
                    setDefaultPickUpDelay()
                })
            }
        }
    }

    @EventHandler
    fun onInteract(event: PlayerInteractEvent) {
        if (event.action != Action.LEFT_CLICK_BLOCK) return

        val block = event.clickedBlock ?: return
        val blocksInLevel = teamsDoors[block.world.toMC()] ?: return
        val pos = BlockPos(block.x,block.y,block.z)

        val miningBlock = blocksInLevel[pos] ?: blocksInLevel[pos.offset(0,-1,0)]
        if (miningBlock != null) {
            val player = event.player
            player.addPotionEffect(PotionEffect(PotionEffectType.MINING_FATIGUE,120,19))
            return
        }
    }

    fun createItem(): ItemStack {
        return net.minecraft.world.item.ItemStack(Items.IRON_DOOR).bukkitStack.apply {
            editMeta {
                it.itemName(Component.text("Team Door").color(NamedTextColor.AQUA))
            }

            setAnniItem(TEAM_DOOR_ITEM_ID)
        }
    }

    fun getTeamDoorBlock(player: ServerPlayer): Block {
        return when(player.team?.name?.lowercase()) {
            "red" -> Blocks.RED_STAINED_GLASS_PANE
            "blue" -> Blocks.BLUE_STAINED_GLASS_PANE
            "green" -> Blocks.LIME_STAINED_GLASS_PANE
            "yellow" -> Blocks.YELLOW_STAINED_GLASS_PANE
            "black" -> Blocks.BLACK_STAINED_GLASS_PANE
            "gray" -> Blocks.GRAY_STAINED_GLASS
            "brown" -> Blocks.BROWN_STAINED_GLASS_PANE
            else -> Blocks.WHITE_STAINED_GLASS_PANE
        }
    }
}