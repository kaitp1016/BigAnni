package me.kaitp1016.biganni.utils

import me.kaitp1016.biganni.ItemKeys
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType

object ItemUtils {
    fun ItemStack.addLore(component: Component) = apply {
        val lines = lore() ?: mutableListOf()
        lines.add(component)

        lore(lines)
    }

    fun ItemStack.setAnniItem(id: String) = apply {
        this.editMeta {
            it.persistentDataContainer.set(ItemKeys.ANNI_ITEM, PersistentDataType.STRING,id)
        }
    }

    fun ItemStack.isAnniItem(): Boolean {
        return persistentDataContainer.has(ItemKeys.ANNI_ITEM)
    }

    fun ItemStack.getAnniId(): String? {
        return persistentDataContainer.get(ItemKeys.ANNI_ITEM, PersistentDataType.STRING)
    }

    fun ItemStack.soulbound() = apply {
        this.editMeta {
            it.persistentDataContainer.set(ItemKeys.SOULBOUND, PersistentDataType.BOOLEAN, true)
        }

        addLore(Component.text("Soulbound").color(NamedTextColor.GOLD).decoration(TextDecoration.ITALIC,false))
    }

    fun Player.consumeItem(id: String, amount: Int = 1): Boolean {
        val inventory = inventory

        val mainHand = inventory.itemInMainHand
        if (mainHand.getAnniId() == id && mainHand.amount >= amount) {
            mainHand.amount--
            return true
        }

        val offHand = inventory.itemInOffHand
        if (offHand.getAnniId() == id && offHand.amount >= amount) {
            offHand.amount--
            return true
        }

        inventory.forEach { item ->
            if (item.getAnniId() == id && item.amount >= amount) {
                item.amount--
                return true
            }
        }

        return false
    }

}