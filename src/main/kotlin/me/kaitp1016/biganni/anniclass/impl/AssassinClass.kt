package me.kaitp1016.biganni.anniclass.impl

import com.destroystokyo.paper.event.server.ServerTickStartEvent
import com.mojang.datafixers.util.Pair
import io.papermc.paper.datacomponent.DataComponentTypes
import io.papermc.paper.datacomponent.item.UseCooldown
import me.kaitp1016.biganni.PLUGIN_ID
import me.kaitp1016.biganni.anniclass.AnniClass
import me.kaitp1016.biganni.events.impl.PacketSendEvent
import me.kaitp1016.biganni.utils.ItemUtils.getAnniItemId
import me.kaitp1016.biganni.utils.ItemUtils.setAnniItem
import me.kaitp1016.biganni.utils.MCUtils.toMC
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.minecraft.network.protocol.game.ClientboundSetEquipmentPacket
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.item.Items
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.bukkit.util.Vector
import net.minecraft.world.entity.player.Player as MCPlayer
import kotlin.math.max

object AssassinClass: AnniClass(), Listener {
    override val name = "Assassin"
    override val icon = Items.FEATHER
    override val description = arrayOf(
        "常に落下ダメージが食らわなくなる。",
        "アビリティを使用すると前に飛び、6秒間の透明化を取得する。",
        "この透明化は防具も透明化される。"
    )

    const val LEAP_ITEM_ID = "assassin_leap"
    const val LEAP_COOLDOWN = 800
    val LEAP_COOLDOWN_GROUP = Key.key(PLUGIN_ID,"assassin_leap")

    override fun getDefaultItems(player: Player): MutableList<ItemStack> {
        return super.getDefaultItems(player).also {
            it.add(ItemStack(Material.FEATHER).apply {
                uniqueClassItem()
                soulbound()
                setAnniItem(LEAP_ITEM_ID)

                setData(DataComponentTypes.USE_COOLDOWN, UseCooldown.useCooldown(LEAP_COOLDOWN / 20f).cooldownGroup(LEAP_COOLDOWN_GROUP).build())

                editMeta {
                    it.itemName(Component.text("Leap").color(NamedTextColor.GOLD))
                }
            })
        }
    }

    val invisibleSlots = arrayOf(EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET,EquipmentSlot.OFFHAND,)

    data class InvisiblePlayer(val player: MCPlayer, val entityId: Int, var time: Int)

    const val LEAP_INVISIBLE_TIME = 120
    val invisiblePlayers = mutableListOf<InvisiblePlayer>()

    @EventHandler
    fun onInteract(event: PlayerInteractEvent) {
        if (event.action != Action.RIGHT_CLICK_BLOCK && event.action != Action.RIGHT_CLICK_AIR) return

        val player = event.player
        if (!isSelected(player)) return

        val item = event.item ?: return
        if (item.getAnniItemId() != LEAP_ITEM_ID || player.hasCooldown(item)) return

        player.addPotionEffect(PotionEffect(PotionEffectType.INVISIBILITY,max(LEAP_INVISIBLE_TIME,player.getPotionEffect(PotionEffectType.INVISIBILITY)?.duration ?: 0),0))
        invisiblePlayers.add(InvisiblePlayer(player.toMC(),player.entityId,LEAP_INVISIBLE_TIME))

        val mcPlayer = player.toMC()
        val slots = invisibleSlots.map { slot -> Pair(slot, net.minecraft.world.item.ItemStack.EMPTY) }
        val packet = ClientboundSetEquipmentPacket(player.entityId,slots)

        mcPlayer.`moonrise$getTrackedEntity`().seenBy.forEach {
            it.send(packet)
        }

        player.velocity = player.location.direction.clone().apply {
            add(Vector(0.0,0.2,0.0))
            normalize()
            multiply(2f)
        }

        player.world.playSound(player.location, Sound.ENTITY_WITHER_SHOOT,2f,2f)

        player.setCooldown(LEAP_COOLDOWN_GROUP,LEAP_COOLDOWN)
    }

    @EventHandler
    fun onDamage(event: EntityDamageEvent) {
        val player = event.entity as? Player ?: return
        val entityId = player.entityId
        if (!isSelected(player) || invisiblePlayers.none { it.entityId == entityId }) return

        player.removePotionEffect(PotionEffectType.INVISIBILITY)
        revealInvisible(player.toMC())
        invisiblePlayers.removeIf { it.entityId == entityId }
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

        player.`moonrise$getTrackedEntity`().seenBy.forEach {
            it.send(packet)
        }
    }
}