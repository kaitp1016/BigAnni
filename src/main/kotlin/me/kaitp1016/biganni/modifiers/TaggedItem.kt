package me.kaitp1016.biganni.modifiers

import me.kaitp1016.biganni.ItemKeys
import org.bukkit.Sound
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.enchantment.EnchantItemEvent
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.inventory.InventoryAction
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryType
import org.bukkit.event.inventory.PrepareItemCraftEvent
import org.bukkit.event.player.PlayerDropItemEvent
import org.bukkit.inventory.CraftingInventory
import org.bukkit.inventory.ItemStack

object TaggedItem: Listener {
    @EventHandler
    fun onDrop(event: PlayerDropItemEvent) {
        val item = event.itemDrop
        if (item.itemStack.persistentDataContainer.has(ItemKeys.SOULBOUND)) {
            val player = event.player
            item.itemStack = ItemStack.empty()
            player.playSound(player, Sound.ITEM_SHIELD_BREAK,1f,2f)
        }
    }

    @EventHandler
    fun onDeath(event: PlayerDeathEvent) {
        event.drops.removeAll {item ->
            return@removeAll item.persistentDataContainer.has(ItemKeys.SOULBOUND)
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    fun onCraft(event: PrepareItemCraftEvent) {
        val matrix = event.inventory.matrix
        if (matrix.any { it?.persistentDataContainer?.has(ItemKeys.SOULBOUND) == true }) {
            event.inventory.result = null
        }
    }

    @EventHandler
    fun onEnchant(event: EnchantItemEvent) {
        val item = event.item
        if (item.persistentDataContainer.has(ItemKeys.SOULBOUND)) {
            event.isCancelled = true
        }
    }

    @EventHandler
    fun onInventoryClick(event: InventoryClickEvent) {
        val inv = event.inventory

        if (inv.type == InventoryType.PLAYER || inv is CraftingInventory || (inv.type == InventoryType.CHEST && inv.location == null)) return

        if (event.currentItem?.persistentDataContainer?.has(ItemKeys.SOULBOUND) == true) {
            event.isCancelled = true
            return
        }

        if (event.action == InventoryAction.HOTBAR_SWAP && event.whoClicked.inventory.getItem(event.hotbarButton)?.persistentDataContainer?.has(ItemKeys.SOULBOUND) == true) {
            event.isCancelled = true
        }
    }
}