package me.kaitp1016.biganni.anniclass

import me.kaitp1016.biganni.ItemKeys
import me.kaitp1016.biganni.anniclass.AnniClassManager.isAnniClass
import me.kaitp1016.biganni.plugin
import me.kaitp1016.biganni.utils.ItemUtils.addLore
import me.kaitp1016.biganni.utils.MCUtils.toMC
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import net.minecraft.core.component.DataComponents
import net.minecraft.world.item.Item
import net.minecraft.world.item.component.DyedItemColor
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeModifier
import org.bukkit.craftbukkit.inventory.CraftItemStack
import org.bukkit.entity.Player
import org.bukkit.event.Listener
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.EquipmentSlotGroup
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType

abstract class AnniClass {
    abstract val name: String
    abstract val deathMessageName: String
    abstract val icon: Item
    abstract val description: Array<String>

    open fun register() {
        if (this is Listener) {
            plugin.server.pluginManager.registerEvents(this, plugin)
        }
    }

    open fun onRespawn(player: Player) {
        val armors = getDefaultArmors(player)
        armors.forEach { (slot, item) ->
            player.inventory.setItem(slot,item)
        }

        val items = getDefaultItems(player)
        items.forEach {
            player.give(it)
        }
    }

    open fun onUnselect(player: Player) {
        player.inventory.forEach { item ->
            if (item != null && item.persistentDataContainer.has(ItemKeys.UNIQUE_CLASS_ITEM)) {
                item.amount = 0
            }
        }
    }

    open fun onSelect(player: Player) {

    }

    open fun onUserTick(player: Player) {

    }

    open fun getDefaultArmors(player: Player): MutableMap<EquipmentSlot,ItemStack> {
        return mutableMapOf(
            EquipmentSlot.FEET to ItemStack(Material.LEATHER_BOOTS).color(player).soulbound(),
            EquipmentSlot.CHEST to ItemStack(Material.LEATHER_CHESTPLATE).color(player).soulbound(),
            EquipmentSlot.LEGS to ItemStack(Material.LEATHER_LEGGINGS).color(player).soulbound(),
            EquipmentSlot.HEAD to ItemStack(Material.LEATHER_HELMET).color(player).soulbound(),
        )
    }

    open fun getDefaultItems(player: Player): MutableList<ItemStack> {
        return mutableListOf(
            ItemStack(Material.WOODEN_AXE).soulbound().apply {
                this.editMeta {
                    it.addAttributeModifier(Attribute.ATTACK_DAMAGE, AttributeModifier(AXE_ATTRIBUTE_MODIFIER_KEY,3.5, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.MAINHAND))
                }
            },
            ItemStack(Material.WOODEN_PICKAXE).soulbound(),
            ItemStack(Material.WOODEN_SWORD).soulbound(),
        )
    }

    fun isSelected(player: Player):Boolean {
        return player.isAnniClass(this)
    }

    protected fun ItemStack.color(player: Player): ItemStack {
        val color = player.toMC().teamColor
        val item = CraftItemStack.asNMSCopy(this)

        item!!.set(DataComponents.DYED_COLOR, DyedItemColor(color))

        return item.bukkitStack
    }

    protected fun ItemStack.soulbound() = apply {
        this.editMeta {
            it.persistentDataContainer.set(ItemKeys.SOULBOUND, PersistentDataType.BOOLEAN, true)
        }

        addLore(Component.text("Undroppable").color(NamedTextColor.GOLD).decoration(TextDecoration.ITALIC,false))
    }

    protected fun ItemStack.uniqueClassItem() = apply {
        this.editMeta {
            it.persistentDataContainer.set(ItemKeys.UNIQUE_CLASS_ITEM, PersistentDataType.BOOLEAN, true)
        }

        addLore(Component.text("Class Item").color(NamedTextColor.GOLD).decoration(TextDecoration.ITALIC,false))
    }

    companion object {
        val AXE_ATTRIBUTE_MODIFIER_KEY = NamespacedKey(plugin,"axe_attribute_modifier")
    }
}