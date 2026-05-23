package me.kaitp1016.biganni.game

import com.destroystokyo.paper.event.server.ServerTickStartEvent
import io.papermc.paper.event.player.AsyncChatEvent
import me.kaitp1016.biganni.anniclass.AnniClassManager.getAnniClass
import me.kaitp1016.biganni.anniclass.impl.HandymanClass
import me.kaitp1016.biganni.config.Config
import me.kaitp1016.biganni.game.boss.BossManager
import me.kaitp1016.biganni.mc
import me.kaitp1016.biganni.packetgui.impl.AnniClassSelector
import me.kaitp1016.biganni.plugin
import me.kaitp1016.biganni.utils.ItemUtils.isAnniItem
import me.kaitp1016.biganni.utils.MCUtils.toMC
import me.kaitp1016.biganni.utils.Scheduler
import net.kyori.adventure.text.format.NamedTextColor
import net.minecraft.network.chat.Component
import net.minecraft.util.CommonColors
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.monster.Witch
import net.minecraft.world.level.Level
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.Sound
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeModifier
import org.bukkit.damage.DamageSource
import org.bukkit.damage.DamageType
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.entity.EntityRemoveEvent
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.player.*
import kotlin.random.Random
import net.kyori.adventure.text.Component as BukkitComponent

object Game: Listener {
    const val MAX_PHASE = 5
    val ATTACK_SPEED_MODIFIER = NamespacedKey(plugin,"anni_attack_speed_modifier")

    val teams = mutableListOf<AnniTeam>()
    var map = Config.MapConfig.default()

    var isStarted = false
    var phaseTime: Int = -1
    var phase: Int = -1

    fun start() {
        phaseTime = map.phaseTime
        phase = 1
        isStarted = true

        teams.clear()

        ScoreboardManager.reset()
        ScoreboardManager.setLine(0, Component.literal("§6apple.playit.plus"))
        ScoreboardManager.setLine(1, Component.empty())

        map.teams.forEach {
            teams.add(it.create())
        }

        teams.forEach { updateNexusHealth(it) }

        ScoreboardManager.setLine(teams.size + 2, Component.empty())
        ScoreboardManager.setLine(teams.size + 3, Component.literal("§6Map: §l${map.name}"))
        ScoreboardManager.setLine(teams.size + 4, Component.empty())

        Bukkit.getOnlinePlayers().forEach {
            it.kill()
        }
    }

    fun reset() {
        isStarted = false
        phase = -1
        teams.clear()

        BossBarManager.setTitle("Annihilation")
    }

    fun updateNexusHealth(team: AnniTeam) {
        val index = teams.indexOf(team) + 2
        val health = if (team.health < 1) "§c✘" else "§b${team.health}"
        ScoreboardManager.setLine(index,Component.literal("${team.color}${team.name} Nexus: $health"))
    }

    @EventHandler
    fun onTick(event: ServerTickStartEvent) {
        if (!isStarted) return

        if (phase < MAX_PHASE) {
            phaseTime--
            if (phaseTime < 1) {
                phase++
                phaseTime = map.phaseTime

                Bukkit.getOnlinePlayers().forEach {
                    it.sendMessage("§a--- §e§lPhase $phase §r§a---")
                }

                if (phase == 3) {
                    teams.forEach { it.spawnWitch() }
                }

                if (phase == 4) {
                    BossManager.spawn()

                    map.bossPortals.forEach { portal ->
                        repeat(9) {
                            val x = portal.x + it % 3
                            val y = portal.y
                            val z = portal.z + it / 3
                            portal.world.setBlockData(x,y,z,Material.END_PORTAL.createBlockData())
                        }
                    }
                }
            }
        }

        if (phase > 4) {
            BossBarManager.setTitle("Phase 5 - §c${if (map.doubleNexusDamage) "Double" else "Single"} §fNexus Damage!")
        }
        else {
            val min = phaseTime / 20 / 60
            val sec = phaseTime / 20 % 60

            BossBarManager.setTitle("Phase ${this.phase} - ${min}:${if (sec > 9) "$sec" else "0${sec}"}")
        }

        BossBarManager.onTick()
    }

    @EventHandler(priority = EventPriority.LOW)
    fun onBreakLowest(event: BlockBreakEvent) {
        val block = event.block
        if (block.type == Material.DIAMOND_ORE && isStarted && phase < 3) {
            event.isCancelled = true
        }
    }

    @EventHandler
    fun onBreak(event: BlockBreakEvent) {
        val block = event.block
        if (block.type != Material.END_STONE) return

        val team = teams.find { it.nexus.world == block.world && it.nexus.x == block.x && it.nexus.y == block.y && it.nexus.z == block.z } ?: return
        val player = event.player

        event.isCancelled = true

        if (phase < 2 || team.name.equals(player.toMC().team?.name, ignoreCase = true)) return

        val damage = if (phase >= MAX_PHASE && map.doubleNexusDamage) 2 else 1
        team.health -= damage

        val pitch = Random.nextFloat() * 0.8f
        val actionbar = player.teamDisplayName().append(BukkitComponent.text(" damaged the").color(NamedTextColor.GRAY).append(BukkitComponent.text(" ${team.color}${team.name} team's nexus!")))

        Bukkit.getOnlinePlayers().forEach {
            if (team.health < 1) it.playSound(it, Sound.ENTITY_GENERIC_EXPLODE, 2f, 0f)
            else if (block.world == it.world && block.location.distance(it.location) < 30) it.playSound(it, Sound.BLOCK_ANVIL_PLACE, 2f, pitch)
            else if (it.toMC().team?.name?.equals(team.name, ignoreCase = true) == true) it.playSound(it, Sound.BLOCK_NOTE_BLOCK_HARP, 2f, 2f)

            it.sendActionBar(actionbar)
        }

        updateNexusHealth(team)

        if (team.health < 1) {
            block.world.setBlockData(block.location, Material.BEDROCK.createBlockData())
        } else {
            block.world.setBlockData(block.location, Material.AIR.createBlockData())

            Scheduler.scheduleTask(8) {
                block.world.setBlockData(block.location, Material.END_STONE.createBlockData())
            }
        }

        HandymanClass.onMineNexus(player)

        val mcPlayer = player.toMC()
        mcPlayer.mainHandItem.hurtAndBreak(1, mcPlayer, EquipmentSlot.MAINHAND)
    }

    @EventHandler
    fun onPortal(event: PlayerPortalEvent) {
        if (!isStarted) return

        if (event.cause == PlayerTeleportEvent.TeleportCause.NETHER_PORTAL) {
            val player = event.player
            val team = getTeam(player)
            if (team != null) player.teleport(team.spawn)
            AnniClassSelector(player.toMC()).open()
            event.isCancelled = true
        }
        if (event.cause == PlayerTeleportEvent.TeleportCause.END_PORTAL) {
            val player = event.player
            player.teleport(map.bossLocation)
            event.isCancelled = true
        }
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

        attackSpeed?.baseValue = 4.0
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

    @EventHandler
    fun onChat(event: AsyncChatEvent) {
        val bukkitPlayer = event.player
        val player = bukkitPlayer.toMC()
        if (player.team == null) return

        if (event.message().toMC().string.startsWith("!")) {
            event.message(BukkitComponent.text(event.message().toMC().string.removePrefix("!")))
            return
        }

        val message = Component.literal("§7[").append(Component.literal("Team").withColor(player.teamColor).append(Component.literal("§7]§f ").withColor(CommonColors.WHITE).append(player.plainTextName).append(Component.literal("§7: §f").withColor(CommonColors.WHITE).append(event.message().toMC()))))

        mc.playerList.players.forEach {
            if (it.teamColor == player.teamColor) it.sendSystemMessage(message)
        }

        event.isCancelled = true
    }

    @EventHandler
    fun onDeath(event: PlayerDeathEvent) {
        val source = event.damageSource
        val player = event.player
        val playerClass = player.getAnniClass() ?: return
        val killer = source.causingEntity as? Player

        if (killer == null) {
            event.deathMessage(player.teamDisplayName().append(BukkitComponent.text("(${playerClass.shortName})").append(BukkitComponent.text(" died.").color(NamedTextColor.GRAY))))
        }
        else {
            val reason = getDeathReason(source)
            val killerClass = killer.getAnniClass() ?: return
            event.deathMessage(killer.teamDisplayName().append(BukkitComponent.text("(${killerClass.shortName})").append(BukkitComponent.text(" $reason ").color(NamedTextColor.GRAY).append(BukkitComponent.empty().color(NamedTextColor.WHITE).append(player.teamDisplayName().append(BukkitComponent.text("(${playerClass.shortName})")))))))
        }
    }

    @EventHandler
    fun onRemoveEntity(event: EntityRemoveEvent) {
        val entity = event.entity
        if (entity.type != EntityType.WITCH) return

        val team = teams.find { it.witch == entity.uniqueId }
        if (team == null) return

        Scheduler.scheduleTask(600) {
            team.spawnWitch()
        }
    }

    private fun getDeathReason(source: DamageSource): String {
        if (source.damageType == DamageType.ARROW) return "shot"

        return "killed"
    }

    class GameWitch: Witch {
        constructor(level: Level):super(net.minecraft.world.entity.EntityType.WITCH,level)

        override fun shouldBeSaved(): Boolean {
            return false
        }
    }

    fun getTeam(player: Player): AnniTeam? {
        val teamName = player.toMC().team?.name
        return teams.find { it.name.equals(teamName,true) }
    }
}