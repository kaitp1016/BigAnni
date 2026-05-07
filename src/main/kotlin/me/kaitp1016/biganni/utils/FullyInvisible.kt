package me.kaitp1016.biganni.utils

import com.destroystokyo.paper.event.server.ServerTickStartEvent
import com.mojang.datafixers.util.Pair
import me.kaitp1016.biganni.anniclass.impl.AssassinClass
import me.kaitp1016.biganni.events.impl.PacketSendEvent
import me.kaitp1016.biganni.utils.MCUtils.toMC
import net.minecraft.network.protocol.game.ClientboundSetEquipmentPacket
import net.minecraft.world.entity.EquipmentSlot
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import kotlin.math.max
import net.minecraft.world.entity.player.Player as MCPlayer

object FullyInvisible: Listener {
    val invisibleSlots = arrayOf(EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET,EquipmentSlot.OFFHAND,)

    data class InvisiblePlayer(val player: MCPlayer, val entityId: Int, var time: Int)
    val invisiblePlayers = mutableListOf<InvisiblePlayer>()

    fun add(player: Player,tick: Int) {
        player.addPotionEffect(PotionEffect(PotionEffectType.INVISIBILITY,max(tick,player.getPotionEffect(PotionEffectType.INVISIBILITY)?.duration ?: 0),0))
        invisiblePlayers.add(InvisiblePlayer(player.toMC(),player.entityId,tick))

        val mcPlayer = player.toMC()
        val slots = invisibleSlots.map { slot -> Pair(slot, net.minecraft.world.item.ItemStack.EMPTY) }
        val packet = ClientboundSetEquipmentPacket(player.entityId,slots)

        mcPlayer.`moonrise$getTrackedEntity`()?.seenBy?.forEach {
            it.send(packet)
        }
    }

    @EventHandler
    fun onDamage(event: EntityDamageEvent) {
        val player = event.entity as? Player ?: return
        val entityId = player.entityId
        if (invisiblePlayers.none { it.entityId == entityId }) return

        player.removePotionEffect(PotionEffectType.INVISIBILITY)
        revealInvisible(player.toMC())
        invisiblePlayers.removeIf { it.entityId == entityId }
        player.playSound(player.location, Sound.ENTITY_ZOMBIE_VILLAGER_CONVERTED,1f,1f)
    }

    @EventHandler
    fun onBreak(event: BlockBreakEvent) {
        val player = event.player
        val id = player.entityId
        if (invisiblePlayers.none { it.entityId == id }) return

        player.removePotionEffect(PotionEffectType.INVISIBILITY)
        revealInvisible(player.toMC())
        invisiblePlayers.removeIf { it.entityId == id }
    }

    @EventHandler
    fun onTick(event: ServerTickStartEvent) {
        if (invisiblePlayers.isEmpty()) return

        invisiblePlayers.removeAll { player ->
            player.time--

            if (player.time < 1) {
                revealInvisible(player.player)
                return@removeAll true
            }
            return@removeAll false
        }
    }

    @EventHandler
    fun onPacketSend(event: PacketSendEvent) {
        if (invisiblePlayers.isEmpty()) return

        val packet = event.packet
        if (packet !is ClientboundSetEquipmentPacket) return

        val entityId = packet.entity
        if (invisiblePlayers.none { it.entityId == entityId }) return

        val equipments = packet.slots.map { if (it.first == EquipmentSlot.MAINHAND) it else Pair(it.first, net.minecraft.world.item.ItemStack.EMPTY) }
        event.packet = ClientboundSetEquipmentPacket(entityId, equipments)
    }

    private fun revealInvisible(player: MCPlayer) {
        val equipment = player.inventory.equipment
        val slots = invisibleSlots.map { slot -> Pair(slot,equipment.get(slot)) }
        val packet = ClientboundSetEquipmentPacket(player.id,slots)

        player.`moonrise$getTrackedEntity`()?.seenBy?.forEach {
            it.send(packet)
        }
    }
}