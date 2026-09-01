package me.kaitp1016.biganni.game

import com.destroystokyo.paper.event.server.ServerTickStartEvent
import me.kaitp1016.biganni.anniclass.AnniClassManager.isClassSelected
import me.kaitp1016.biganni.anniclass.AnniClassManager.selectAnniClass
import me.kaitp1016.biganni.anniclass.AnniClasses
import me.kaitp1016.biganni.mc
import me.kaitp1016.biganni.packetgui.ChestPacketGui
import me.kaitp1016.biganni.utils.ItemUtils.getAnniId
import me.kaitp1016.biganni.utils.ItemUtils.setAnniItem
import me.kaitp1016.biganni.utils.MCUtils.toMC
import me.kaitp1016.biganni.utils.Scheduler
import net.kyori.adventure.text.format.NamedTextColor
import net.minecraft.core.component.DataComponents
import net.minecraft.network.chat.ClickEvent
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.HoverEvent
import net.minecraft.network.chat.Style
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.item.ItemStack
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.enchantments.Enchantment
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerDropItemEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerRespawnEvent

object TeamSelector: Listener {
    const val TEAM_SELECTOR_ID = "team_selector"

    @EventHandler(priority = EventPriority.HIGH)
    fun onJoin(event: PlayerJoinEvent) {
        if (!Game.isStarted) return

        val player = event.player
        if (player.toMC().team != null) return

        player.give(createTeamSelectorItem())


        Scheduler.scheduleTask(1) {
            Bukkit.getOnlinePlayers().forEach {
                if (!it.isOp) return@forEach

                val mcPlayer = it.toMC()
                var teamMessage = Component.literal("§7 - §b手動チーム選択 ")

                Game.teams.forEach { team ->
                    teamMessage = teamMessage.append(Component.literal("§f[${team.color}${team.name}§f] ").setStyle(Style.EMPTY.withClickEvent(ClickEvent.RunCommand("/anni setteam ${it.name} ${team.name}")).withHoverEvent(HoverEvent.ShowText(Component.literal("クリックして${team.name}チームに入れる!")))))
                }

                mcPlayer.sendSystemMessage(teamMessage)
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    fun onRespawn(event: PlayerRespawnEvent) {
        if (!Game.isStarted) return

        val player = event.player
        if (player.toMC().team != null) return

        player.give(createTeamSelectorItem())
    }

    @EventHandler
    fun onInteract(event: PlayerInteractEvent) {
        val item = event.item ?: return
        if (item.getAnniId() == TEAM_SELECTOR_ID) {
            val player = event.player.toMC()
            TeamSelectorGui(player).open()
        }
    }

    @EventHandler
    fun onDrop(event: PlayerDropItemEvent) {
        val item = event.itemDrop
        if (item.itemStack.getAnniId() == TEAM_SELECTOR_ID) {
            event.isCancelled = true
        }
    }

    private fun createTeamSelectorItem(): org.bukkit.inventory.ItemStack {
        return org.bukkit.inventory.ItemStack(Material.NETHER_STAR).apply {
            setAnniItem(TEAM_SELECTOR_ID)

            editMeta {
                it.itemName(net.kyori.adventure.text.Component.text("チーム選択").color(NamedTextColor.GOLD))
            }

            addUnsafeEnchantment(Enchantment.VANISHING_CURSE, 1)
        }
    }

    class TeamSelectorGui: ChestPacketGui {
        override val name = "Team Selector"
        override val displayName = Component.literal("Team Selector")

        val teams: List<AnniTeam>

        constructor(player: ServerPlayer):super(player, 9) {
            this.teams = Game.teams.toList()

            teams.forEachIndexed { index, team ->
                setItem(index, ItemStack(team.teamWool).apply {
                    set(DataComponents.ITEM_NAME, Component.literal("${team.color}${team.name}"))
                })
            }
        }

        override fun onClick(packet: ServerboundContainerClickPacket) {
            mc.execute {
                val slot = packet.slotNum.toInt()
                val team = teams.getOrNull(slot) ?: return@execute update(false)

                val mcTeam = mc.scoreboard.playerTeams.find { it.name.equals(team.name, true) }
                if (mcTeam == null) {
                    player.sendSystemMessage(Component.literal("§cエラーが発生しました!"))
                    update()
                    return@execute
                }

                val teamPlayers = countPlayers(team)
                if (teams.any { countPlayers(it) < teamPlayers }) {
                    player.sendSystemMessage(Component.literal("§c人数が多いチームには参加できません!"))
                    update()
                    return@execute
                }

                mc.scoreboard.addPlayerToTeam(player.scoreboardName, mcTeam)
                player.kill(player.level())

                if (!player.bukkitEntity.isClassSelected()) {
                    player.bukkitEntity.selectAnniClass(AnniClasses.CIVILIAN)
                }
            }
        }

        private fun countPlayers(team: AnniTeam): Int {
            val team = mc.scoreboard.playerTeams.find { it.name.equals(team.name, true) }
            if (team == null) throw NoSuchElementException()

            return mc.playerList.players.count { it.team == team }
        }
    }
}