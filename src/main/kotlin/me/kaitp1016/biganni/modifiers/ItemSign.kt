package me.kaitp1016.biganni.modifiers

import me.kaitp1016.biganni.ItemKeys
import me.kaitp1016.biganni.utils.ItemUtils.addLore
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Material
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.enchantment.EnchantItemEvent
import org.bukkit.persistence.PersistentDataType

object ItemSign: Listener {
    val signableItems = mapOf(
        Material.WOODEN_PICKAXE to "Pickaxe",
        Material.WOODEN_AXE to "Axe",
        Material.WOODEN_SHOVEL to "Shovel",
        Material.WOODEN_HOE to "Hoe",
        Material.WOODEN_SWORD to "Sword",
        Material.WOODEN_SPEAR to "Spear",

        Material.STONE_PICKAXE to "Pickaxe",
        Material.STONE_AXE to "Axe",
        Material.STONE_SHOVEL to "Shovel",
        Material.STONE_HOE to "Hoe",
        Material.STONE_SWORD to "Sword",
        Material.STONE_SPEAR to "Spear",

        Material.COPPER_PICKAXE to "Pickaxe",
        Material.COPPER_AXE to "Axe",
        Material.COPPER_SHOVEL to "Shovel",
        Material.COPPER_HOE to "Hoe",
        Material.COPPER_SWORD to "Sword",
        Material.COPPER_SPEAR to "Spear",

        Material.COPPER_HELMET to "Helmet",
        Material.COPPER_CHESTPLATE to "Chestplate",
        Material.COPPER_LEGGINGS to "Leggings",
        Material.COPPER_BOOTS to "Boots",

        Material.GOLDEN_PICKAXE to "Pickaxe",
        Material.GOLDEN_AXE to "Axe",
        Material.GOLDEN_SHOVEL to "Shovel",
        Material.GOLDEN_HOE to "Hoe",
        Material.GOLDEN_SWORD to "Sword",
        Material.GOLDEN_SPEAR to "Spear",

        Material.GOLDEN_HELMET to "Helmet",
        Material.GOLDEN_CHESTPLATE to "Chestplate",
        Material.GOLDEN_LEGGINGS to "Leggings",
        Material.GOLDEN_BOOTS to "Boots",

        Material.IRON_PICKAXE to "Pickaxe",
        Material.IRON_AXE to "Axe",
        Material.IRON_SHOVEL to "Shovel",
        Material.IRON_HOE to "Hoe",
        Material.IRON_SWORD to "Sword",
        Material.IRON_SPEAR to "Spear",

        Material.IRON_HELMET to "Helmet",
        Material.IRON_CHESTPLATE to "Chestplate",
        Material.IRON_LEGGINGS to "Leggings",
        Material.IRON_BOOTS to "Boots",

        Material.DIAMOND_PICKAXE to "Pickaxe",
        Material.DIAMOND_AXE to "Axe",
        Material.DIAMOND_SHOVEL to "Shovel",
        Material.DIAMOND_HOE to "Hoe",
        Material.DIAMOND_SWORD to "Sword",
        Material.DIAMOND_SPEAR to "Spear",

        Material.DIAMOND_HELMET to "Helmet",
        Material.DIAMOND_CHESTPLATE to "Chestplate",
        Material.DIAMOND_LEGGINGS to "Leggings",
        Material.DIAMOND_BOOTS to "Boots",

        Material.NETHERITE_PICKAXE to "Pickaxe",
        Material.NETHERITE_AXE to "Axe",
        Material.NETHERITE_SHOVEL to "Shovel",
        Material.NETHERITE_HOE to "Hoe",
        Material.NETHERITE_SWORD to "Sword",
        Material.NETHERITE_SPEAR to "Spear",

        Material.NETHERITE_HELMET to "Helmet",
        Material.NETHERITE_CHESTPLATE to "Chestplate",
        Material.NETHERITE_LEGGINGS to "Leggings",
        Material.NETHERITE_BOOTS to "Boots",

        Material.BOW to "Bow",
        Material.CROSSBOW to "Crossbow",
        Material.SHIELD to "Shield",
        Material.MACE to "Mace",
        Material.ELYTRA to "Elytra",
        Material.TRIDENT to "Trident",
    )

    @EventHandler
    fun onEnchant(event: EnchantItemEvent) {
        val item = event.item
        if (item.persistentDataContainer.has(ItemKeys.SIGNED_ITEM)) return

        val name = signableItems[item.type] ?: return
        item.addLore(Component.empty().decoration(TextDecoration.ITALIC, false).color(NamedTextColor.WHITE).append(event.enchanter.teamDisplayName().append(Component.text("'s $name").color(NamedTextColor.GRAY))))

        item.editMeta {
            it.persistentDataContainer.set(ItemKeys.SIGNED_ITEM, PersistentDataType.BOOLEAN, true)
        }
    }
}