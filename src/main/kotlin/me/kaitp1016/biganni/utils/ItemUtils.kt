package me.kaitp1016.biganni.utils

import me.kaitp1016.biganni.ItemKeys
import net.kyori.adventure.text.Component
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
}