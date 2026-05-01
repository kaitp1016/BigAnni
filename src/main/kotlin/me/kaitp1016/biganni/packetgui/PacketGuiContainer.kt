package me.kaitp1016.biganni.packetgui

import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.inventory.MenuType
import net.minecraft.world.item.ItemStack
import org.bukkit.craftbukkit.inventory.CraftInventoryView
import org.bukkit.inventory.InventoryView

class PacketGuiContainer: AbstractContainerMenu {
    val packetGui: AbstractPacketGui

    constructor(syncId: Int,packetGui: AbstractPacketGui):super(MenuType.BEACON,syncId) {
        this.packetGui = packetGui
    }

    override fun getBukkitView(): InventoryView {
        val bukkitPlayer = packetGui.player.bukkitEntity
        return CraftInventoryView(bukkitPlayer, bukkitPlayer.inventory, this);
    }

    override fun quickMoveStack(player: Player, slot: Int): ItemStack {
        return ItemStack.EMPTY
    }

    override fun removed(player: Player) {
        packetGui.onClose()
        super.removed(player)
    }

    override fun stillValid(player: Player): Boolean {
        return packetGui.isOpened
    }
}