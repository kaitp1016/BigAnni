package me.kaitp1016.biganni.modifiers

import io.papermc.paper.inventory.BrewingSimpleContainerData
import me.kaitp1016.biganni.ItemKeys
import me.kaitp1016.biganni.utils.ItemUtils.addLore
import me.kaitp1016.biganni.utils.MCUtils.toMC
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import net.minecraft.core.BlockPos
import net.minecraft.world.level.block.entity.BrewingStandBlockEntity
import org.bukkit.Material
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.inventory.InventoryOpenEvent
import org.bukkit.inventory.BrewerInventory
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType

object BrewingStandModifier: Listener {
    @EventHandler
    fun onInventoryOpen(event: InventoryOpenEvent) {
        val inv = event.inventory
        if (inv !is BrewerInventory) return

        inv.fuel = ItemStack(Material.BLAZE_POWDER).apply {
            this.editMeta {
                it.persistentDataContainer.set(ItemKeys.SOULBOUND, PersistentDataType.BOOLEAN, true)
            }
        }
    }

    @EventHandler
    fun onBreak(event: BlockBreakEvent) {
        val block = event.block
        if (block.type != Material.BREWING_STAND) return

        val level = block.world.toMC()
        val blockEntity = level.getBlockEntity(BlockPos(block.x,block.y,block.z)) as? BrewingStandBlockEntity ?: return
        blockEntity.setItem(4, net.minecraft.world.item.ItemStack.EMPTY)
    }
}