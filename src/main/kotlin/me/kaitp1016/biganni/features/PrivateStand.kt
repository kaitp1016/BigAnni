package me.kaitp1016.biganni.features

import me.kaitp1016.biganni.utils.BlockPosInfo
import me.kaitp1016.biganni.utils.ItemUtils.getAnniId
import me.kaitp1016.biganni.utils.ItemUtils.setAnniItem
import me.kaitp1016.biganni.utils.MCUtils.toMC
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.item.Items
import net.minecraft.world.level.block.Blocks
import org.bukkit.Material
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.ItemStack

object PrivateStand: Listener {
    const val PRIVATE_STAND_ID = "anni_private_stand"

    val privateStands = BlockPosInfo<ServerPlayer>()

    @EventHandler(priority = EventPriority.HIGH)
    fun onPlace(event: BlockPlaceEvent) {
        val item = event.itemInHand
        if (event.isCancelled || item.getAnniId() != PRIVATE_STAND_ID) return

        val block = event.block
        val player = event.player.toMC()
        val level = player.level()
        val pos = BlockPos(block.x, block.y, block.z)

        privateStands[level, pos] = player
    }

    @EventHandler(priority = EventPriority.HIGH)
    fun onBreak(event: BlockBreakEvent) {
        if (event.isCancelled) return

        val player = event.player.toMC()
        val block = event.block
        val pos = BlockPos(block.x, block.y, block.z)
        val level = player.level()

        val miningBlock = privateStands[level, pos] ?: return
        val team = player.teamColor

        if (miningBlock != player && miningBlock.teamColor == team) {
            event.isCancelled = true
            return
        }

        privateStands.remove(level, pos)
        event.isCancelled = true

        level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState())

        level.addFreshEntity(ItemEntity(player.level(), block.x + 0.5, block.y + 0.5, block.z + 0.5, createItem().toMC()!!).apply {
            setDefaultPickUpDelay()
        })
    }

    @EventHandler
    fun onInteract(event: PlayerInteractEvent) {
        val block = event.clickedBlock
        if (event.action == Action.RIGHT_CLICK_BLOCK && block?.type == Material.BREWING_STAND) {
            val pos = BlockPos(block.x, block.y, block.z)
            val level = block.world.toMC()
            val owner = privateStands[level, pos] ?: return

            val user = event.player
            val mcUser = user.toMC()
            if (owner == mcUser) return

            if (mcUser.teamColor == owner.teamColor) {
                user.sendMessage("他の人のPrivate Standは使えません!")
                event.isCancelled = true
                return
            } else {
                user.breakBlock(block)
            }

            return
        }
    }

    fun createItem(): ItemStack {
        return net.minecraft.world.item.ItemStack(Items.BREWING_STAND).bukkitStack.apply {
            editMeta {
                it.itemName(Component.text("Private Stand").color(NamedTextColor.AQUA))
            }

            setAnniItem(PRIVATE_STAND_ID)
        }
    }
}