package me.kaitp1016.biganni.anniclass.impl

import com.destroystokyo.paper.event.server.ServerTickStartEvent
import io.papermc.paper.datacomponent.DataComponentTypes
import io.papermc.paper.datacomponent.item.UseCooldown
import me.kaitp1016.biganni.PLUGIN_ID
import me.kaitp1016.biganni.anniclass.AnniClass
import me.kaitp1016.biganni.game.AnniTeam
import me.kaitp1016.biganni.game.Game
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
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.Particle
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.ItemStack

object RiftWalkerClass: AnniClass(), Listener {
    override val name = "Rift Walker"
    override val shortName = "RIF"
    override val icon = Items.BLAZE_ROD
    override val description = arrayOf(
        "アビリティを使用することで、指定した味方にテレポートできる。",
        "周囲にいたスニークしている味方もテレポートできる。",
    )

    const val OPEN_RIFT_ITEM_ID = "riftwalker_open_rift"
    const val OPEN_RIFT_COOLDOWN = 1200
    val OPEN_RIFT_COOLDOWN_GROUP = Key.key(PLUGIN_ID,"riftwalker_open_rift")

    val rifts = mutableListOf<Rift>()

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

    data class Rift(val rifter: Player,val target: RiftTarget,val location: Location) {
        var time: Int = 201

        fun tick(): Boolean {
            time--

            if (time % 20 != 0) return false

            val world = location.world
            arrayOf(3.0 to 3.0,-3.0 to 3.0,-3.0 to -3.0, 3.0 to -3.0,5.0 to 0.0,-5.0 to 0.0,0.0 to 5.0,0.0 to -5.0).forEach { (dx,dz) ->
                Particle.HAPPY_VILLAGER.builder()
                    .location(location.clone().add(dx,0.0,dz))
                    .offset(0.0,2.0,0.0)
                    .count(20)
                    .receivers(32,true)
                    .spawn()
            }

            if (time <= 0) {
                val players = world.getNearbyPlayers(location, 5.0, 5.0).filter { it != rifter && (it.toMC().teamColor == rifter.toMC().teamColor && it.isSneaking) }.sortedBy { if (it == rifter) 0.0 else it.location.distance(location) }.take(3) + rifter
                players.forEach {
                    it.teleport(target.getLocation())
                }

                return true
            }

            world.getNearbyPlayers(rifter.location, 5.0, 5.0).filter { it.toMC().teamColor == rifter.toMC().teamColor }.sortedBy { it.location.distance(rifter.location) }.forEach {
                it.sendMessage(Component.text("Rift to ").color(NamedTextColor.GOLD).append(target.getName().append(Component.text(" opens in ${time / 20}").color(NamedTextColor.GOLD))))
            }

            target.sendMessage(Component.text("${rifter.name}'s rift to you arrives in ${time / 20}!").color((NamedTextColor.GOLD)))

            return false
        }
    }

    abstract class RiftTarget {
        abstract fun getLocation(): Location
        abstract fun sendMessage(message: Component)
        abstract fun getName(): Component
        abstract fun getIcon(): net.minecraft.world.item.ItemStack
    }

    class PlayerRiftTarget(val player: Player): RiftTarget() {
        override fun getLocation(): Location {
            return player.location
        }

        override fun sendMessage(message: Component) {
            player.sendMessage(message)
        }

        override fun getIcon(): net.minecraft.world.item.ItemStack {
            return net.minecraft.world.item.ItemStack(Items.PLAYER_HEAD).apply {
                this.set(DataComponents.PROFILE, net.minecraft.world.item.component.ResolvableProfile.createResolved(player.toMC().gameProfile))
                this.set(DataComponents.LORE, ItemLore(listOf(net.minecraft.network.chat.Component.literal("リフトする!").withStyle(Style.EMPTY.withItalic(false)))))
            }
        }

        override fun getName(): Component {
            return player.name()
        }
    }

    class BaseRiftTarget(val team: AnniTeam): RiftTarget() {
        override fun getLocation(): Location {
            return team.spawn
        }

        override fun sendMessage(message: Component) {

        }

        override fun getIcon(): net.minecraft.world.item.ItemStack {
            return net.minecraft.world.item.ItemStack(Items.RED_BED).apply {
                set(DataComponents.ITEM_NAME, net.minecraft.network.chat.Component.literal("${team.color}Your Base"))
            }
        }

        override fun getName(): Component {
            return Component.text("${team.color}Your Base")
        }
    }

    class EnemyBaseRiftTarget(val team: AnniTeam): RiftTarget() {
        override fun getLocation(): Location {
            return team.riftLocation
        }

        override fun sendMessage(message: Component) {

        }

        override fun getIcon(): net.minecraft.world.item.ItemStack {
            val item = when(team.name.lowercase()) {
                "red" -> Items.RED_WOOL
                "blue" -> Items.BLUE_WOOL
                "green" -> Items.GREEN_WOOL
                "yellow" -> Items.YELLOW_WOOL
                "gray" -> Items.GRAY_WOOL
                "black" -> Items.BLACK_WOOL
                "white" -> Items.WHITE_WOOL
                else -> Items.WHITE_WOOL
            }
            return net.minecraft.world.item.ItemStack(item).apply {
                set(DataComponents.ITEM_NAME, net.minecraft.network.chat.Component.literal("${team.color}${team.name} Base"))
            }
        }

        override fun getName(): Component {
            return Component.text("${team.color}${team.name} Base")
        }
    }

    class RiftSelectGui: ChestPacketGui {
        override val name = "rift target selector"
        override val displayName = net.minecraft.network.chat.Component.literal("リフトする対象を選択してね")

        val targets: List<RiftTarget>

        constructor(player: ServerPlayer):super(player,27) {
            val team = player.team
            val targets = mutableListOf<RiftTarget>()

            Game.teams.find { it.name.equals(team?.name, true) }?.let { targets.add(BaseRiftTarget(it)) }
            targets.addAll(Game.teams.filter { !it.name.equals(team?.name, true)}.map { EnemyBaseRiftTarget(it) })
            targets.addAll(mc.playerList.players.also { it.remove(player) }.filter { it.team == team }.map{ PlayerRiftTarget(it.bukkitEntity) })

            this.targets = targets

            var i = -1
            targets.forEach { target ->
                i++
                this.setItem(i, target.getIcon())
            }
        }

        override fun onClick(packet: ServerboundContainerClickPacket) {
            mc.execute {
                if (!isOpened) return@execute

                val target = targets.getOrNull(packet.slotNum.toInt()) ?: return@execute
                val rifter = this.player.bukkitEntity

                rifts.add(Rift(rifter,target,rifter.location))
                rifter.setCooldown(OPEN_RIFT_COOLDOWN_GROUP,OPEN_RIFT_COOLDOWN)
                target.sendMessage(Component.text("${player.plainTextName} is attempting to rift to you!").color((NamedTextColor.GREEN)))

                close()
            }
        }
    }
}