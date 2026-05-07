package me.kaitp1016.biganni.anniclass.impl

import com.destroystokyo.paper.event.server.ServerTickStartEvent
import io.papermc.paper.datacomponent.DataComponentTypes
import io.papermc.paper.datacomponent.item.UseCooldown
import me.kaitp1016.biganni.PLUGIN_ID
import me.kaitp1016.biganni.anniclass.AnniClass
import me.kaitp1016.biganni.mc
import me.kaitp1016.biganni.packetgui.ChestPacketGui
import me.kaitp1016.biganni.utils.ItemUtils.getAnniId
import me.kaitp1016.biganni.utils.ItemUtils.setAnniItem
import me.kaitp1016.biganni.utils.MCUtils.toMC
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.minecraft.core.component.DataComponents
import net.minecraft.network.chat.Style
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.item.Items
import net.minecraft.world.item.component.ItemLore
import org.bukkit.Material
import org.bukkit.Particle
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.ItemStack

object RiftWalkerClass: AnniClass(), Listener {
    override val name = "Rift Walker"
    override val deathMessageName = "RFW"
    override val icon = Items.BLAZE_ROD
    override val description = arrayOf(
        "アビリティを使用することで、指定した味方にテレポートできる。",
        "周囲にいたスニークしている味方もテレポートできる。",
    )

    const val OPEN_RIFT_ITEM_ID = "riftwalker_open_rift"
    const val OPEN_RIFT_COOLDOWN = 1200
    val OPEN_RIFT_COOLDOWN_GROUP = Key.key(PLUGIN_ID,"riftwalker_open_rift")

    override fun getDefaultItems(player: Player): MutableList<ItemStack> {
        return super.getDefaultItems(player).also {
            it.add(ItemStack(Material.BLAZE_ROD).apply {
                uniqueClassItem()
                soulbound()
                setAnniItem(OPEN_RIFT_ITEM_ID)

                setData(DataComponentTypes.USE_COOLDOWN, UseCooldown.useCooldown(OPEN_RIFT_COOLDOWN / 20f).cooldownGroup(OPEN_RIFT_COOLDOWN_GROUP).build())

                editMeta {
                    it.itemName(Component.text("Open Rift").color(NamedTextColor.GOLD))
                }
            })
        }
    }

    override fun onUnselect(player: Player) {
        super.onUnselect(player)

        rifts.removeIf { it.rifter == player }
    }

    data class Rift(val rifter: Player,val target: Player) {
        var time: Int = 201

        fun tick(): Boolean {
            time--

            if (time % 20 != 0) return false

            val world = rifter.world
            arrayOf(3.0 to 3.0,-3.0 to 3.0,-3.0 to -3.0, 3.0 to -3.0,5.0 to 0.0,-5.0 to 0.0,0.0 to 5.0,0.0 to -5.0).forEach { (dx,dz) ->
                Particle.HAPPY_VILLAGER.builder()
                    .location(rifter.location.clone().add(dx,0.0,dz))
                    .offset(0.0,2.0,0.0)
                    .count(20)
                    .spawn()
            }

            if (time <= 0) {
                val players = world.getNearbyPlayers(rifter.location, 3.0, 3.0).filter { it == rifter || (it.toMC().teamColor == rifter.toMC().teamColor && it.isSneaking) }.sortedBy { it.location.distance(rifter.location) }.take(4)
                players.forEach {
                    it.teleport(target)
                }

                return true
            }

            world.getNearbyPlayers(rifter.location, 3.0, 3.0).filter { it.toMC().teamColor == rifter.toMC().teamColor }.sortedBy { it.location.distance(rifter.location) }.forEach {
                it.sendMessage(Component.text("Rift to ").color(NamedTextColor.GOLD).append(target.teamDisplayName().append(Component.text(" opens in ${time / 20}").color(NamedTextColor.GOLD))))
            }

            return false
        }
    }

    val rifts = mutableListOf<Rift>()

    @EventHandler
    fun onInteract(event: PlayerInteractEvent) {
        val player = event.player
        if (!isSelected(player)) return

        val item = event.item ?: return
        if (item.getAnniId() != OPEN_RIFT_ITEM_ID || player.hasCooldown(item)) return

        RiftSelectGui(player.toMC()).open()
    }

    @EventHandler
    fun onTick(event: ServerTickStartEvent) {
        if (rifts.isEmpty()) return

        rifts.removeAll(Rift::tick)
    }

    class RiftSelectGui: ChestPacketGui {
        override val name = "rift target selector"
        override val displayName = net.minecraft.network.chat.Component.literal("リフトする対象を選択してね")

        val players: List<ServerPlayer>

        constructor(player: ServerPlayer):super(player,27) {
            val team = player.teamColor
            this.players = mc.playerList.players.also { it.remove(player)  }.filter { it.teamColor == team }

            var i = -1
            players.forEach { player ->
                i++
                this.setItem(i, net.minecraft.world.item.ItemStack(Items.PLAYER_HEAD).apply {
                    this.set(DataComponents.PROFILE, net.minecraft.world.item.component.ResolvableProfile.createResolved(player.gameProfile))
                    this.set(DataComponents.LORE, ItemLore(listOf(net.minecraft.network.chat.Component.literal("リフトする!").withStyle(Style.EMPTY.withItalic(false)))))
                })
            }
        }

        override fun onClick(packet: ServerboundContainerClickPacket) {
            mc.execute {
                if (!isOpened) return@execute

                val target = players.getOrNull(packet.slotNum.toInt())?.bukkitEntity ?: return@execute
                val rifter = this.player.bukkitEntity

                rifts.add(Rift(rifter,target))
                rifter.setCooldown(OPEN_RIFT_COOLDOWN_GROUP,OPEN_RIFT_COOLDOWN)

                close()
            }
        }
    }
}