package me.kaitp1016.biganni.anniclass.impl

import me.kaitp1016.biganni.anniclass.AnniClass
import me.kaitp1016.biganni.utils.ItemUtils.getAnniId
import me.kaitp1016.biganni.utils.ItemUtils.setAnniItem
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.minecraft.world.item.Items
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeModifier
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryType
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.EquipmentSlotGroup
import org.bukkit.inventory.ItemStack

object CivilianClass: AnniClass(), Listener {
    override val icon = Items.CRAFTING_TABLE
    override val name = "Civilian"
    override val description = arrayOf(
        "アビリティを使用すると作業台が開く。",
    )

    const val CRAFT_O_MATIC_ITEM_ID = "civilatian_craft_o_matic"

    override fun getDefaultItems(player: Player): MutableList<ItemStack> {
        return super.getDefaultItems(player).also {
            it.removeIf { it.type == Material.WOODEN_AXE }
            it.removeIf { it.type == Material.WOODEN_PICKAXE }

            it.add(ItemStack(Material.STONE_PICKAXE).uniqueClassItem().soulbound())
            it.add(ItemStack(Material.STONE_AXE).soulbound().uniqueClassItem().apply {
                this.editMeta {
                    it.addAttributeModifier(Attribute.ATTACK_DAMAGE, AttributeModifier(AXE_ATTRIBUTE_MODIFIER_KEY,3.5, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.MAINHAND))
                }
            })
            it.add(ItemStack(Material.STONE_SHOVEL).uniqueClassItem().soulbound())

            it.add(ItemStack(Material.BRICK).apply {
                uniqueClassItem()
                soulbound()
                setAnniItem(CRAFT_O_MATIC_ITEM_ID)

                editMeta {
                    it.itemName(Component.text("Craft O' Matic").color(NamedTextColor.GOLD))
                }
            })
        }
    }

    @EventHandler
    fun onInteract(event: PlayerInteractEvent) {
        val player = event.player
        if (!isSelected(player)) return

        val item = event.item ?: return
        if (item.getAnniId() != CRAFT_O_MATIC_ITEM_ID || player.hasCooldown(item)) return

        player.openInventory(Bukkit.createInventory(player, InventoryType.WORKBENCH))
    }
}