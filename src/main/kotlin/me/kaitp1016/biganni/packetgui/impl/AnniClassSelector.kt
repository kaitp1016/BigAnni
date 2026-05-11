package me.kaitp1016.biganni.packetgui.impl

import me.kaitp1016.biganni.anniclass.AnniClassManager.selectAnniClass
import me.kaitp1016.biganni.anniclass.AnniClasses
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
import net.minecraft.world.item.component.ItemLore
import org.bukkit.Bukkit

class   AnniClassSelector: ChestPacketGui {
    override val displayName = Component.literal("Class Selector")
    override val name = "Class Selector"

    constructor(player: ServerPlayer,parent: AbstractPacketGui? = null): super(player,54) {
        AnniClasses.ALL_CLASSES.forEachIndexed { index, anniClass ->
            val lore = anniClass.description.map { Component.literal(it).withStyle(Style.EMPTY.withItalic(false)).withColor(CommonColors.WHITE) }
            setItem(index, ItemStack(anniClass.icon).apply {
                set(DataComponents.ITEM_NAME, Component.literal(anniClass.name).withColor(CommonColors.WHITE))
                set(DataComponents.LORE, ItemLore(lore))
            })
        }
    }

    override fun onClick(packet: ServerboundContainerClickPacket) {
        val slot = packet.slotNum.toInt()
        val anniClass = AnniClasses.ALL_CLASSES.getOrNull(slot) ?: return

        mc.execute {
            val player = player.bukkitEntity
            if (Game.map.blockedClasses.any { anniClass.name == it }) {
                player.sendMessage("このクラスは使用できません!")
                update(false)
                return@execute
            }

            player.selectAnniClass(anniClass)

            player.openInventory(Bukkit.createInventory(player,27, net.kyori.adventure.text.Component.text("§o§rClass Item")).also { inv ->
                anniClass.getDefaultArmors(player).values.forEach { inv.addItem(it) }
                anniClass.getDefaultItems(player).forEach { inv.addItem(it) }
            })
        }
    }
}