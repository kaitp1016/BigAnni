package me.kaitp1016.biganni.packetgui.impl

import me.kaitp1016.biganni.game.Game
import me.kaitp1016.biganni.mc
import me.kaitp1016.biganni.packetgui.AbstractPacketGui
import me.kaitp1016.biganni.packetgui.ChestPacketGui
import net.minecraft.core.component.DataComponents
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.Style
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket
import net.minecraft.server.level.ServerPlayer
import net.minecraft.util.CommonColors
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.item.component.ItemLore
import org.bukkit.Sound

class BrewingShopGui: ChestPacketGui {
    override val displayName = Component.literal("Brewing Shop").withColor(0xFFAA00)
    override val name = "Brewing Shop"

    val shopItems = mapOf(
        10 to (ItemStack(Items.BREWING_STAND) to 10),
        12 to (ItemStack(Items.NETHER_WART) to 5),
        13 to (ItemStack(Items.REDSTONE) to 3),
        14 to (ItemStack(Items.FERMENTED_SPIDER_EYE) to 10),
        15 to (ItemStack(Items.MAGMA_CREAM) to 8),
        16 to (ItemStack(Items.SUGAR) to 10),

        19 to (ItemStack(Items.GLASS_BOTTLE).also { it.count = 3 } to 1),
        21 to (ItemStack(Items.GLISTERING_MELON_SLICE) to 8),
        22 to (ItemStack(Items.GHAST_TEAR) to 20),
        23 to (ItemStack(Items.GOLDEN_CARROT) to 10),
        24 to (ItemStack(Items.SPIDER_EYE) to 4),
    )

    constructor(player: ServerPlayer,parent: AbstractPacketGui? = null): super(player,36) {
        repeat(size) {
            items[it] = ItemStack(Items.GRAY_STAINED_GLASS_PANE).apply {
                this.set(DataComponents.ITEM_NAME, Component.empty())
            }
        }

        shopItems.forEach {
            setItem(it.key, it.value.first.copy().apply {
                set(DataComponents.LORE, ItemLore(listOf(Component.literal("§bCosts ${it.value.second} §6Gold Ingot").withStyle(Style.EMPTY.withItalic(false)).withColor(CommonColors.WHITE))))
            })
        }

        if (Game.canUseBlazePowder()) {
            setItem(BLAZE_POWDER_SLOT,ItemStack(Items.BLAZE_POWDER).apply {
                set(DataComponents.LORE, ItemLore(listOf(Component.literal("§bCosts ${BLAZE_POWDER_COST} §6Gold Ingot").withStyle(Style.EMPTY.withItalic(false)).withColor(CommonColors.WHITE))))
            })
        }
    }

    override fun onClick(packet: ServerboundContainerClickPacket) {
        update(false)

        mc.execute {
            val slot = packet.slotNum.toInt()
            if (slot == BLAZE_POWDER_SLOT && Game.canUseBlazePowder()) {
                buy(ItemStack(Items.BLAZE_POWDER), BLAZE_POWDER_COST)
                return@execute
            }

            val item = shopItems[slot]
            if (item != null) {
                buy(item.first, item.second)
            }
            return@execute
        }
    }

    fun buy(item: ItemStack,price: Int) {
        val golds = player.inventory.filter { it.item == Items.GOLD_INGOT }.sumOf { it.count }
        if (golds < price) return

        var price = price
        while (price > 0) {
            price--
            player.inventory.find { it.item == Items.GOLD_INGOT }?.count-- ?: break
        }

        if (price > 0) return
        player.bukkitEntity.give(item.bukkitStack)
        player.bukkitEntity.playSound(player.bukkitEntity, Sound.ENTITY_EXPERIENCE_ORB_PICKUP,1f,1f)

        player.inventoryMenu.sendAllDataToRemote()
   }

    companion object {
        const val BLAZE_POWDER_SLOT = 25
        const val BLAZE_POWDER_COST = 32
    }
}