package me.kaitp1016.biganni.modifiers

import me.kaitp1016.biganni.utils.Scheduler
import org.bukkit.GameMode
import org.bukkit.Material
import org.bukkit.enchantments.Enchantment
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.enchantment.EnchantItemEvent
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.event.inventory.InventoryOpenEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.inventory.EnchantingInventory
import org.bukkit.inventory.ItemStack
import kotlin.random.Random

object EnchantModifier: Listener {
    @EventHandler
    fun onInventoryOpen(event: InventoryOpenEvent) {
        val inv = event.inventory as? EnchantingInventory ?: return

        inv.secondary = ItemStack(Material.LAPIS_LAZULI).apply {
            this.amount = 64
        }
    }

    @EventHandler
    fun onInventoryClick(event: InventoryClickEvent) {
        val inv = event.inventory

        if (inv is EnchantingInventory) {
            if (event.currentItem?.type == Material.LAPIS_LAZULI) {
                event.isCancelled = true
            }
        }

        val item = event.currentItem
        if (item != null && (item.enchantments[Enchantment.DEPTH_STRIDER] ?: 0) > 1) {
            item.addUnsafeEnchantment(Enchantment.DEPTH_STRIDER,1)
        }
    }

    @EventHandler
    fun onInventoryClose(event: InventoryCloseEvent) {
        val inv = event.inventory
        if (inv is EnchantingInventory) {
            inv.secondary = ItemStack.empty()
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onEnchant(event: EnchantItemEvent) {
        if (event.isCancelled) return

        if (event.expLevelCost == 30 && event.enchanter.gameMode != GameMode.CREATIVE) {
            Scheduler.scheduleTask(1) {
                event.enchanter.level -= 2
            }
        }

        if (event.enchantsToAdd.contains(Enchantment.DEPTH_STRIDER)) {
            event.enchantsToAdd[Enchantment.DEPTH_STRIDER] = 1
        }
    }

    @EventHandler
    fun onJoin(event: PlayerJoinEvent) {
        event.player.enchantmentSeed = Random.nextInt()
    }
}