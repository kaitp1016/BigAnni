package me.kaitp1016.biganni.packetgui.impl

import me.kaitp1016.biganni.features.DelayingBlock
import me.kaitp1016.biganni.features.TeamDoor
import me.kaitp1016.biganni.mc
import me.kaitp1016.biganni.packetgui.AbstractPacketGui
import me.kaitp1016.biganni.packetgui.ChestPacketGui
import me.kaitp1016.biganni.utils.MCUtils.toMC
import net.minecraft.core.component.DataComponents
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.Style
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket
import net.minecraft.server.level.ServerPlayer
import net.minecraft.util.CommonColors
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.item.component.ItemLore
import net.minecraft.world.item.component.UseCooldown
import org.bukkit.Sound
import java.util.*

class WeaponShopGui: ChestPacketGui {
    override val displayName = Component.literal("Weapon Shop").withColor(0xFFAA00)
    override val name = "Weapon Shop"

    val shopItems = mapOf(
        10 to (ItemStack(Items.IRON_HELMET) to 3),
        19 to (ItemStack(Items.IRON_CHESTPLATE) to 5),
        28 to (ItemStack(Items.IRON_LEGGINGS) to 5),
        37 to (ItemStack(Items.IRON_BOOTS) to 3),

        12 to (ItemStack(Items.IRON_SWORD) to 1),
        30 to (ItemStack(Items.BOW) to 1),
        39 to (ItemStack(Items.ARROW).also { it.count = 16 } to 1),

        14 to (ItemStack(Items.COOKED_BEEF).also { it.count = 10 } to 5),
        15 to (ItemStack(Items.CAKE) to 1),
        16 to (ItemStack(Items.COBWEB) to 1),
        23 to (ItemStack(Items.EXPERIENCE_BOTTLE).also { it.count = 3 } to 2),
        24 to (ItemStack(Items.ENDER_PEARL).also { it.set(DataComponents.USE_COOLDOWN, UseCooldown(10f, Optional.empty())) } to 35),
        25 to (ItemStack(Items.MILK_BUCKET) to 5),

        41 to (TeamDoor.createItem().toMC()!! to 10),
        42 to (ItemStack(Items.SPONGE) to 5),
        43 to (DelayingBlock.createItem().toMC()!! to 20),
        )

    constructor(player: ServerPlayer,parent: AbstractPacketGui? = null): super(player,54) {
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
    }

    override fun onClick(packet: ServerboundContainerClickPacket) {
        update(false)

        mc.execute {
            val slot = packet.slotNum.toInt()
            val item = shopItems[slot]
            if (item != null){
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
        player.bukkitEntity.give(item.copy().bukkitStack)
        player.bukkitEntity.playSound(player.bukkitEntity, Sound.ENTITY_EXPERIENCE_ORB_PICKUP,1f,1f)

        player.inventoryMenu.sendAllDataToRemote()
    }
}