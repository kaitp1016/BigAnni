package me.kaitp1016.biganni.packetgui

import me.kaitp1016.biganni.events.impl.PacketReceiveEvent
import me.kaitp1016.biganni.events.impl.PacketSendEvent
import net.minecraft.server.level.ServerPlayer

abstract class AbstractPacketGui {
    open var parent: AbstractPacketGui? = null
    var isOpened = false
    val player: ServerPlayer
    val depth: Int get() = if (parent == null) 0 else parent!!.depth + 1

    abstract val name: String

    constructor(player: ServerPlayer) {
        this.player = player
    }

    abstract fun onPacketRecive(event: PacketReceiveEvent)
    abstract fun onPacketSend(event: PacketSendEvent)

    abstract fun onOpen()
    abstract fun onClose()

    open fun onTick() {

    }

    abstract fun open()
    abstract fun close()

    fun openParent(): Boolean {
        if (parent == null) {
            this.close()
            return false
        }

        parent!!.open()
        return true
    }
}