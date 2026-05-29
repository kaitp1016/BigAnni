package me.kaitp1016.biganni.packetgui

import me.kaitp1016.biganni.events.impl.PacketReceiveEvent
import me.kaitp1016.biganni.events.impl.PacketSendEvent
import me.kaitp1016.biganni.utils.Scheduler
import net.minecraft.core.NonNullList
import net.minecraft.core.component.DataComponents
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.Style
import net.minecraft.network.protocol.game.*
import net.minecraft.server.level.ServerPlayer
import net.minecraft.util.CommonColors
import net.minecraft.world.inventory.MenuType
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.item.component.ItemLore

abstract class ChestPacketGui: AbstractPacketGui {
    val syncId:Int
    val size: Int
    val menuType: MenuType<*>
    var openParentSlot: Int? = null
    open val items: NonNullList<ItemStack> by lazy { NonNullList.withSize(size, ItemStack.EMPTY) }

    abstract val displayName: Component

    constructor(player: ServerPlayer,size: Int):super(player) {
        this.syncId = player.nextContainerCounter()
        this.size = size
        this.menuType = menuTypeMap[size] ?: throw IllegalArgumentException("size is must be one of ${menuTypeMap.keys.joinToString()}.")
    }

    override fun onPacketRecive(event: PacketReceiveEvent) {
        val packet = event.packet

        if (packet is ServerboundContainerClickPacket) {
            val slot = packet.slotNum

            if (packet.slotNum < 0 || packet.slotNum > size) {
                return
            }

            if (packet.containerId != this.syncId) {
                this.close()
            }
            else {
                when (slot.toInt()) {
                    openParentSlot -> {
                        this.openParent()
                    }
                    else -> {
                        this.onClick(packet)
                        event.isCancelled = true
                    }
                }
            }
        }

        if (packet is ServerboundContainerClosePacket) {
            this.close()
        }
    }

    override fun onPacketSend(event: PacketSendEvent) {

    }

    open fun update(reopen:Boolean = false) {
        if (reopen) {
            Scheduler.scheduleTask(0) {
                this.open()
            }
        }
        else {
            player.connection.send(ClientboundContainerSetContentPacket(syncId,0,items, ItemStack.EMPTY))
        }
    }

    fun setItem(index: Int,item: ItemStack) {
        items[index] = item
    }

    fun getItem(index: Int): ItemStack {
        return items[index]
    }

    fun setOpenParentItem(index: Int) {
        if (parent == null) {
            this.setItem(index, ItemStack(Items.BARRIER).apply {
                this.set(DataComponents.ITEM_NAME, Component.literal("閉じる").withColor(CommonColors.SOFT_RED))
            })
        }
        else {
            this.setItem(index, ItemStack(Items.ARROW).apply {
                this.set(DataComponents.ITEM_NAME, Component.literal("前の画面に戻る").withColor(CommonColors.WHITE))
                this.set(DataComponents.LORE, ItemLore(listOf(Component.literal("(${parent!!.name})").withColor(CommonColors.GRAY).withStyle(Style.EMPTY.withItalic(false)))))
            })
        }

        this.openParentSlot = index
    }

    override fun open() {
        player.containerMenu = PacketGuiContainer(syncId,this)
        player.connection.send(ClientboundOpenScreenPacket(syncId, menuType,displayName))
        this.isOpened = true
        this.onOpen()

        update()
    }

    override fun close() {
        player.doCloseContainer()
        player.connection.send(ClientboundContainerClosePacket(this.syncId))

        this.isOpened = false
    }

    override fun onOpen() {

    }

    override fun onClose() {

    }

    abstract fun onClick(packet: ServerboundContainerClickPacket)

    companion object {
        val menuTypeMap = mapOf(
            9 to MenuType.GENERIC_9x1,
            18 to MenuType.GENERIC_9x2,
            27 to MenuType.GENERIC_9x3,
            36 to MenuType.GENERIC_9x4,
            45 to MenuType.GENERIC_9x5,
            54 to MenuType.GENERIC_9x6,
        )
    }
}