package me.kaitp1016.biganni.modifier

import org.bukkit.GameMode
import org.bukkit.Material
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.ItemStack
import kotlin.math.min

object FastCraft: Listener {
    val woods = mapOf(
        Material.OAK_LOG to Material.OAK_PLANKS,
        Material.ACACIA_LOG to Material.ACACIA_PLANKS,
        Material.BIRCH_LOG to Material.BIRCH_PLANKS,
        Material.DARK_OAK_LOG to Material.DARK_OAK_PLANKS,
        Material.JUNGLE_LOG to Material.JUNGLE_PLANKS,
        Material.SPRUCE_LOG to Material.SPRUCE_PLANKS,
        Material.CHERRY_LOG to Material.CHERRY_PLANKS,
        Material.MANGROVE_LOG to Material.MANGROVE_PLANKS,
        Material.PALE_OAK_LOG to Material.PALE_OAK_PLANKS,
    )

    @EventHandler
    fun onInteract(event: PlayerInteractEvent) {
        if (event.action != Action.RIGHT_CLICK_BLOCK && event.action != Action.RIGHT_CLICK_AIR) return

        val hand = event.hand ?: return
        val player = event.player
        if (player.gameMode == GameMode.CREATIVE) return

        val item = player.inventory.getItem(hand)
        val replacement = woods[item.type] ?: return

        var count = item.amount * 4

        player.inventory.setItem(hand, ItemStack(replacement).apply {
            this.amount = min(count, 64)
        })

        count -= 64

        while (count > 0) {
            player.give(ItemStack(replacement).apply {
                this.amount = min(count, 64)
            })

            count -= 64
        }

        event.isCancelled = true
    }
}