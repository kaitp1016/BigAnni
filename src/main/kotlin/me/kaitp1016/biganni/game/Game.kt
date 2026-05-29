package me.kaitp1016.biganni.game

import com.destroystokyo.paper.event.server.ServerTickStartEvent
import io.papermc.paper.event.player.AsyncChatEvent
import me.kaitp1016.biganni.anniclass.AnniClassManager.getAnniClass
import me.kaitp1016.biganni.anniclass.AnniClassManager.selectAnniClass
import me.kaitp1016.biganni.anniclass.AnniClasses
import me.kaitp1016.biganni.config.Config
import me.kaitp1016.biganni.game.boss.BossManager
import me.kaitp1016.biganni.game.boss.FinalBossFight
import me.kaitp1016.biganni.mc
import me.kaitp1016.biganni.packetgui.impl.AnniClassSelector
import me.kaitp1016.biganni.plugin
import me.kaitp1016.biganni.utils.ItemUtils.isAnniItem
import me.kaitp1016.biganni.utils.MCUtils.toMC
import me.kaitp1016.biganni.utils.Scheduler
import net.kyori.adventure.text.TextComponent
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor
import net.minecraft.network.chat.Component
import net.minecraft.util.CommonColors
import net.minecraft.world.entity.monster.Witch
import net.minecraft.world.level.Level
import org.bukkit.*
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeModifier
import org.bukkit.boss.BarColor
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
    const val BLAZE_POWDER_USABLE_PHASE = 4
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

        BossBarManager.setColor(BarColor.BLUE)

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

        Bukkit.getOnlinePlayers().forEach { player ->
            player.totalExperience = 0
            player.enchantmentSeed = Random.nextInt()
            player.clearActivePotionEffects()

            if (player.getAnniClass() == null) {
                player.selectAnniClass(AnniClasses.CIVILIAN)
            }

            if (player.gameMode != GameMode.SPECTATOR) {
                player.inventory.clear()
                player.enderChest.clear()
                player.kill()
            }
        }

        Scheduler.scheduleTask(5) {
            sendPhaseMessage(1)
        }
    }

    fun reset() {
        FinalBossFight.reset()
        StartCountdown.reset()

        isStarted = false
        phase = -1
        teams.clear()

        BossBarManager.setTitle("Annihilation")
    }

    @EventHandler
    fun onTick(event: ServerTickStartEvent) {
        if (!isStarted) return

        if (phase < MAX_PHASE) {
            phaseTime--
            if (phaseTime < 1) {
                phase++
                phaseTime = map.phaseTime

                sendPhaseMessage(phase)

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

        if (phase >= MAX_PHASE) {
            val multiplier = when(val multiplier = NexusHandler.getNexusDamage()) {
                1 -> "SINGLE"
                2 -> "DOUBLE"
                3 -> "TRIPLE"
                else -> "${multiplier}x"
            }

            BossBarManager.setTitle("Phase 5 - §c${multiplier} §fNexus Damage!")
            BossBarManager.setProgress(0.0)
        }
        else {
            val min = phaseTime / 20 / 60
            val sec = phaseTime / 20 % 60

            BossBarManager.setTitle("Phase ${this.phase} - ${min}:${if (sec > 9) "$sec" else "0${sec}"}")
            BossBarManager.setProgress(phaseTime.toDouble() / map.phaseTime)
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    fun onBreakLow(event: BlockBreakEvent) {
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

        NexusHandler.damage(player,block,team)
    }

    @EventHandler
    fun onPortal(event: PlayerPortalEvent) {
        if (!isStarted) return

        if (event.cause == PlayerTeleportEvent.TeleportCause.NETHER_PORTAL) {
            val player = event.player
            val team = getTeam(player)

            if (team != null) {
                val bossLocation = map.bossLocation
                if (bossLocation.world == player.world && bossLocation.distance(player.location) < 125.0) {
                    player.teleport(team.spawn)
                    event.isCancelled = true
                }
                else {
                    val spawn = teams.filter { it.spawn.world == player.world }.minByOrNull { it.spawn.distance(player.location) }?.spawn
                    if (spawn != null) {
                        player.teleport(spawn)

                        Scheduler.scheduleTask(5) {
                            AnniClassSelector(player.toMC()).open()
                        }

                        event.isCancelled = true
                    }
                }
            }
        }
        if (event.cause == PlayerTeleportEvent.TeleportCause.END_PORTAL) {
            val player = event.player
            val team = getTeam(player)
            val location = team?.bossSpawn ?: map.bossLocation
            player.teleport(location)
            event.isCancelled = true
        }
    }

    @EventHandler
    fun onJoin(event: PlayerJoinEvent) {
        val player = event.player
        val mcPlayer = player.toMC()

        player.respawnLocation = null

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
        if (item.type == Material.BLAZE_POWDER && !canUseBlazePowder() && !item.isAnniItem()) {
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

    fun updateNexusHealth(team: AnniTeam) {
        val index = teams.indexOf(team) + 2
        val health = if (team.health < 1) "§c✘" else "§b${team.health}"
        ScoreboardManager.setLine(index,Component.literal("${team.color}${team.name} Nexus: $health"))
    }

    fun sendPixelArtMessage(message: Pair<Array<Pair<String, TextComponent>>, Map<Char, TextColor>>) {
        message.first.map { line ->
            var component = BukkitComponent.empty()
            line.first.map {
                return@map message.second[it] ?: throw IllegalArgumentException()
            }.forEach { component = component.append(BukkitComponent.text("█").color(it)) }

            return@map component.append(BukkitComponent.text(" ").append(line.second))
        }.forEach {
            Bukkit.broadcast(it)
        }
    }

    fun getTeam(player: Player): AnniTeam? {
        val teamName = player.toMC().team?.name
        return teams.find { it.name.equals(teamName,true) }
    }

    fun canUseBlazePowder(): Boolean {
        return !isStarted || phase >= BLAZE_POWDER_USABLE_PHASE
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

    private fun sendPhaseMessage(phase: Int) {
        val message = when (phase) {
            1 -> (arrayOf(
                "113110131" to BukkitComponent.text(""),
                "120292323" to BukkitComponent.text(""),
                "019991230" to BukkitComponent.text(""),
                "131393231" to BukkitComponent.text(""),
                "323292023" to BukkitComponent.text("§7The game has §6started!"),
                "321391112" to BukkitComponent.text("§7The Nexus is §6invincible"),
                "012293011" to BukkitComponent.text(""),
                "119999933" to BukkitComponent.text(""),
                "221212212" to BukkitComponent.text(""),
            ) to mapOf(
                '0' to TextColor.color(180, 177, 129),
                '1' to TextColor.color(216, 224, 160),
                '2' to TextColor.color(208, 216, 149),
                '3' to TextColor.color(210, 215, 156),
                '9' to TextColor.color(0, 136, 13),
            ))
            2 -> (arrayOf(
                "213120232" to BukkitComponent.text(""),
                "230999303" to BukkitComponent.text(""),
                "019210930" to BukkitComponent.text(""),
                "131333901" to BukkitComponent.text(""),
                "311309112" to BukkitComponent.text("§6Phase 2 §7has §6started"),
                "321391113" to BukkitComponent.text("§7The Nexus has §6lost §7its §6invincibility"),
                "013913012" to BukkitComponent.text(""),
                "129999933" to BukkitComponent.text(""),
                "132322131" to BukkitComponent.text(""),
            ) to mapOf(
                '0' to TextColor.color(185, 174, 127),
                '1' to TextColor.color(193, 194, 134),
                '2' to TextColor.color(219, 226, 168),
                '3' to TextColor.color(222, 230, 168),
                '9' to TextColor.color(7, 206, 233),
            ))
            3 -> (arrayOf(
                "212112202" to BukkitComponent.text(""),
                "220999212" to BukkitComponent.text(""),
                "219110902" to BukkitComponent.text(""),
                "201110923" to BukkitComponent.text(""),
                "200999123" to BukkitComponent.text("§6Phase 3 §7has §6started"),
                "211111903" to BukkitComponent.text("§bDiamonds §7have spawned in the middle"),
                "209020903" to BukkitComponent.text("§bWitches §7have also spawned"),
                "200999223" to BukkitComponent.text(""),
                "232332322" to BukkitComponent.text(""),
            ) to mapOf(
                '0' to TextColor.color(116, 249, 255),
                '1' to TextColor.color(136, 221, 206),
                '2' to TextColor.color(26, 191, 191),
                '3' to TextColor.color(11, 153, 155),
                '9' to TextColor.color(0, 0, 0),
            ))
            4 -> (arrayOf(
                "333332322" to BukkitComponent.text(""),
                "322329222" to BukkitComponent.text(""),
                "322299223" to BukkitComponent.text(""),
                "323929223" to BukkitComponent.text(""),
                "339339333" to BukkitComponent.text(" §6Phase 4 §7has §6started"),
                "309999923" to BukkitComponent.text(" §6Blaze Powder §7is now available"),
                "322339223" to BukkitComponent.text(" §5The Wither §7has spawned"),
                "322329223" to BukkitComponent.text(""),
                "322213311" to BukkitComponent.text(""),
            ) to mapOf(
                '0' to TextColor.color(60, 73, 90),
                '1' to TextColor.color(30, 30, 39),
                '2' to TextColor.color(20, 26, 36),
                '3' to TextColor.color(11, 17, 20),
                '9' to TextColor.color(255, 255, 255),
            ))
            5 -> (arrayOf(
                "120172101" to BukkitComponent.text(""),
                "109999920" to BukkitComponent.text(""),
                "319083102" to BukkitComponent.text(""),
                "209080101" to BukkitComponent.text(""),
                "019999217" to BukkitComponent.text("§6Phase 5 §7has §6started"),
                "078731980" to BukkitComponent.text("§c${NexusHandler.getNexusDamage()}x §6Nexus damage"),
                "780800912" to BukkitComponent.text(""),
                "819999800" to BukkitComponent.text(""),
                "212181303" to BukkitComponent.text(""),
            ) to mapOf(
                '0' to TextColor.color(222, 230, 168),
                '1' to TextColor.color(219, 228, 163),
                '2' to TextColor.color(193, 194, 134),
                '3' to TextColor.color(185, 174, 127),
                '7' to TextColor.color(134, 132, 134),
                '8' to TextColor.color(52, 52, 52),
                '9' to TextColor.color(108, 1, 3),
            ))

            else -> throw IllegalArgumentException()
        }

        sendPixelArtMessage(message)
    }
}