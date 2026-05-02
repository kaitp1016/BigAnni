package me.kaitp1016.biganni.game

import com.destroystokyo.paper.event.server.ServerTickStartEvent
import me.kaitp1016.biganni.packetgui.impl.AnniClassSelector
import me.kaitp1016.biganni.plugin
import me.kaitp1016.biganni.utils.ItemUtils.isAnniItem
import me.kaitp1016.biganni.utils.MCUtils.toMC
import net.kyori.adventure.key.Key
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.Component
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.Sound
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeModifier
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.player.*
import kotlin.random.Random

object Game: Listener {
    const val PHASE_TIME = 9600
    const val MAX_PHASE = 5
    val ATTACK_SPEED_MODIFIER = NamespacedKey(plugin,"anni_attack_speed_modifier")

    val teams = mutableListOf<AnniTeam>()

    var isStarted = false
    var phaseTime: Int = -1
    var phase: Int = -1

    fun start() {
        phaseTime = PHASE_TIME
        phase = 1
        isStarted = true

        teams.clear()
        teams.add(AnniTeam("Red", BlockPos(118,-51,-188),"§c", Location(Bukkit.getWorld(Key.key("sys","coastal")),109.5, -38.0, -177.5, 45f, 0f)))
        teams.add(AnniTeam("Blue", BlockPos(-118,-51,190),"§9",Location(Bukkit.getWorld(Key.key("sys","coastal")),-108.5, -38.0, 180.5, -135f, 0f)))

        ScoreboardManager.reset()
        ScoreboardManager.setLine(0, Component.literal("§6apple.playit.plus"))
        ScoreboardManager.setLine(1, Component.empty())

        teams.forEachIndexed { index,team ->
            ScoreboardManager.setLine(2 + index, Component.literal("${team.color}${team.name} Nexus: §b${team.health}"))
        }

        ScoreboardManager.setLine(teams.size + 2, Component.empty())
        ScoreboardManager.setLine(teams.size + 3, Component.literal("§6Map: §lCoastal"))
        ScoreboardManager.setLine(teams.size + 4, Component.empty())

        Bukkit.getOnlinePlayers().forEach {
            it.kill()
        }
    }

    fun reset() {
        isStarted = false
        phase = -1
        teams.clear()
    }

    @EventHandler
    fun onTick(event: ServerTickStartEvent) {
        if (!isStarted) return

        if (phase < MAX_PHASE) {
            phaseTime--
            if (phaseTime < 1) {
                phase++
                phaseTime = PHASE_TIME

                Bukkit.getOnlinePlayers().forEach {
                    it.sendMessage("§a--- §e§lPhase $phase §r§a---")
                }
            }
        }

        BossBarManager.onTick()
    }

    @EventHandler(priority = EventPriority.LOW)
    fun onBreakLowest(event: BlockBreakEvent) {
        val block = event.block
        if (block.type == Material.DIAMOND_ORE && phase < 3) {
            event.isCancelled = true
        }
    }

    @EventHandler
    fun onBreak(event: BlockBreakEvent) {
        val block = event.block
        if (block.type != Material.END_STONE) return

        val pos = BlockPos(block.x,block.y,block.z)
        val team = teams.find { it.nexus == pos } ?: return
        val player = event.player

        event.isCancelled = true

        if (phase < 2 || team.name.equals(player.toMC().team?.name,ignoreCase = true)) return

        val damage = if (phase < MAX_PHASE) 1 else 2
        team.health -= damage

        val pitch = Random.nextFloat() * 0.8f

        player.playSound(player, Sound.BLOCK_ANVIL_PLACE,2f,pitch)

        Bukkit.getOnlinePlayers().forEach {
            if (it.toMC().team?.name.equals(team.name,ignoreCase = true)) it.playSound(it, Sound.BLOCK_ANVIL_PLACE,2f,pitch)
        }

        val index = teams.indexOf(team) + 2
        ScoreboardManager.setLine(index,Component.literal("${team.color}${team.name} Nexus: §b${team.health}"))
    }

    @EventHandler
    fun onPortal(event: PlayerPortalEvent) {
        if (event.cause != PlayerTeleportEvent.TeleportCause.NETHER_PORTAL) return

        val player = event.player
        AnniClassSelector(player.toMC()).open()
    }

    @EventHandler
    fun onJoin(event: PlayerJoinEvent) {
        val player = event.player
        val mcPlayer = player.toMC()

        BossBarManager.onJoin(player)
        ScoreboardManager.onJoin(mcPlayer)


        val attackSpeed = player.getAttribute(Attribute.ATTACK_SPEED)
        if (attackSpeed?.getModifier(ATTACK_SPEED_MODIFIER) == null) {
            attackSpeed?.addModifier(AttributeModifier(ATTACK_SPEED_MODIFIER,100000.0, AttributeModifier.Operation.ADD_NUMBER))
        }
    }

    @EventHandler
    fun onQuit(event: PlayerQuitEvent) {
        val player = event.player
        BossBarManager.onQuit(player)
    }

    @EventHandler
    fun onRespawn(event: PlayerRespawnEvent) {
        if (!isStarted) return

        val player = event.player
        val teamName = player.toMC().team?.name ?: return
        val team = teams.find { it.name.equals(teamName,ignoreCase = true) } ?: return

        event.respawnLocation = team.spawn
    }

    @EventHandler
    fun onClick(event: InventoryClickEvent) {
        val item = event.currentItem ?: return
        if (isStarted && phase < 4 && item.type == Material.BLAZE_POWDER && !item.isAnniItem()) {
            event.isCancelled = true
            event.whoClicked.sendMessage("このアイテムはPhase 4まで使用できません!")
        }
    }
}