package me.kaitp1016.biganni.game

import com.destroystokyo.paper.event.server.ServerTickStartEvent
import io.papermc.paper.event.player.AsyncChatEvent
import me.kaitp1016.biganni.anniclass.AnniClassManager.getAnniClass
import me.kaitp1016.biganni.anniclass.AnniClassManager.selectAnniClass
import me.kaitp1016.biganni.anniclass.AnniClasses
import me.kaitp1016.biganni.anniclass.impl.HandymanClass
import me.kaitp1016.biganni.config.Config
import me.kaitp1016.biganni.game.boss.BossManager
import me.kaitp1016.biganni.game.boss.FinalBossFight
import me.kaitp1016.biganni.mc
import me.kaitp1016.biganni.packetgui.impl.AnniClassSelector
import me.kaitp1016.biganni.plugin
import me.kaitp1016.biganni.utils.ItemUtils.isAnniItem
import me.kaitp1016.biganni.utils.MCUtils.toMC
import me.kaitp1016.biganni.utils.Scheduler
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor
import net.minecraft.network.chat.Component
import net.minecraft.util.CommonColors
import net.minecraft.world.entity.EquipmentSlot
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
            val multiplier = when(val multiplier = getNexusDamage()) {
                1 -> "Single"
                2 -> "Double"
                3 -> "Triple"
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

        team.health -= getNexusDamage()

        val pitch = Random.nextFloat() * 0.8f
        val actionbar = player.teamDisplayName().append(BukkitComponent.text(" damaged the").color(NamedTextColor.GRAY).append(BukkitComponent.text(" ${team.color}${team.name} team's nexus!")))

        val world = block.world
        val blockLocation = block.location

        Bukkit.getOnlinePlayers().forEach {
            if (team.health < 1) it.playSound(it, Sound.ENTITY_GENERIC_EXPLODE, 2f, 0f)
            else if (world == it.world && blockLocation.toVector().distance(it.location.toVector()) < 30) it.playSound(it, Sound.BLOCK_ANVIL_PLACE, 2f, pitch)
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

        val particleLocation = blockLocation.toCenterLocation()

        world.spawnParticle(Particle.LAVA,particleLocation,75,2.0,2.0,2.0)

        repeat(10) {
            world.spawnParticle(Particle.FIREWORK,particleLocation,0, Random.nextDouble(-1.5,1.5),Random.nextDouble(-1.5,1.5),Random.nextDouble(-1.5,1.5))
        }

        repeat(6) {
            world.spawnParticle(Particle.FIREWORK,particleLocation,0, Random.nextDouble(-0.5,0.5),Random.nextDouble(-0.5,0.5),Random.nextDouble(-0.5,0.5))
        }

        HandymanClass.onMineNexus(player)

        val mcPlayer = player.toMC()
        mcPlayer.mainHandItem.hurtAndBreak(1, mcPlayer, EquipmentSlot.MAINHAND)

        if (team.health < 1) {
            sendDestructionMessage(team,"${getTeam(player)?.color ?: ""}${player.name}")

            if (teams.count { it.health > 1 } < 2) {
                val message = arrayOf(
                    "119999911" to "",
                    "190000091" to "",
                    "900000009" to "        §6§lBoss Fight!",
                    "909000909" to "§aIt's time to go all in if you're brave.",
                    "909909909" to "§aBe sneaking in 30s time,",
                    "900000009" to "§ato be teleported to the arena!",
                    "190909091" to "",
                    "190909091" to "",
                    "199999991" to "",
                ) to mapOf(
                    '0' to TextColor.color(255, 255, 255),
                    '1' to TextColor.color(92, 90, 90),
                    '9' to TextColor.color(0, 0, 0),
                )

                sendPixelArtWithMessage(message)

                isStarted = false
                BossBarManager.setTitle("Game Ended.")
                BossBarManager.setColor(BarColor.PURPLE)
                BossBarManager.setProgress(1.0)
                FinalBossFight.start()
            }
        }
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

    private fun getDeathReason(source: DamageSource): String {
        if (source.damageType == DamageType.ARROW) return "shot"

        return "killed"
    }

    private fun sendPhaseMessage(phase: Int) {
        val message = when (phase) {
            1 -> (arrayOf(
                "113110131" to "",
                "120292323" to "",
                "019991230" to "",
                "131393231" to "",
                "323292023" to "§7The game has §6started!",
                "321391112" to "§7The Nexus is §6invincible",
                "012293011" to "",
                "119999933" to "",
                "221212212" to "",
            ) to mapOf(
                '0' to TextColor.color(180, 177, 129),
                '1' to TextColor.color(216, 224, 160),
                '2' to TextColor.color(208, 216, 149),
                '3' to TextColor.color(210, 215, 156),
                '9' to TextColor.color(0, 136, 13),
            ))
            2 -> (arrayOf(
                "213120232" to "",
                "230999303" to "",
                "019210930" to "",
                "131333901" to "",
                "311309112" to "§6Phase 2 §7has §6started",
                "321391113" to "§7The Nexus has §6lost its §6invincibility",
                "013913012" to "",
                "129999933" to "",
                "132322131" to "",
            ) to mapOf(
                '0' to TextColor.color(185, 174, 127),
                '1' to TextColor.color(193, 194, 134),
                '2' to TextColor.color(219, 226, 168),
                '3' to TextColor.color(222, 230, 168),
                '9' to TextColor.color(7, 206, 233),
            ))
            3 -> (arrayOf(
                "212112202" to "",
                "220999212" to "",
                "219110902" to "",
                "201110923" to "",
                "200999123" to "§6Phase 3 §7has §6started",
                "211111903" to "§bDiamonds §7have spawned in the middle",
                "209020903" to "§bWitches §7have also spawned!",
                "200999223" to "",
                "232332322" to "",
            ) to mapOf(
                '0' to TextColor.color(116, 249, 255),
                '1' to TextColor.color(136, 221, 206),
                '2' to TextColor.color(26, 191, 191),
                '3' to TextColor.color(11, 153, 155),
                '9' to TextColor.color(0, 0, 0),
            ))
            4 -> (arrayOf(
                "333332322" to "",
                "322329222" to "",
                "322299223" to "",
                "323929223" to "",
                "339339333" to " §6Phase 4 §7has §6started",
                "309999923" to " §6Blaze Powder §7is now available",
                "322339223" to " §5The Wither §7has spawned",
                "322329223" to "",
                "322213311" to "",
            ) to mapOf(
                '0' to TextColor.color(60, 73, 90),
                '1' to TextColor.color(30, 30, 39),
                '2' to TextColor.color(20, 26, 36),
                '3' to TextColor.color(11, 17, 20),
                '9' to TextColor.color(255, 255, 255),
            ))
            5 -> (arrayOf(
                "120172101" to "",
                "109999920" to "",
                "319083102" to "",
                "209080101" to "",
                "019999217" to "§6Phase 5 §7has §6started",
                "078731980" to "§c${getNexusDamage()}x §6Nexus damage",
                "780800912" to "",
                "819999800" to "",
                "212181303" to "",
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

        sendPixelArtWithMessage(message)
    }

    private fun sendDestructionMessage(team: AnniTeam,destroyer: String) {
        val message = when(team.name.lowercase()) {
            "red" -> (arrayOf(
                "000000000" to "",
                "119999111" to "",
                "229222922" to "",
                "339333933" to "",
                "449999444" to "${team.color}${team.name} §7team's ",
                "559595555" to "§7Nexus has been destroyed",
                "669669666" to "§7by $destroyer",
                "779777977" to "",
                "888888888" to "",
            ) to mapOf(
                '0' to TextColor.color(255, 245, 245),
                '1' to TextColor.color(255, 235, 235),
                '2' to TextColor.color(255, 225, 225),
                '3' to TextColor.color(255, 215, 215),
                '4' to TextColor.color(255, 205, 205),
                '5' to TextColor.color(255, 195, 195),
                '6' to TextColor.color(255, 185, 185),
                '7' to TextColor.color(255, 175, 175),
                '8' to TextColor.color(255, 165, 165),
                '9' to TextColor.color(255, 0, 0),
            ))
            "blue" -> (arrayOf(
                "000000000" to "",
                "119999111" to "",
                "229222922" to "",
                "339333933" to "",
                "449999444" to "${team.color}${team.name} §7team's ",
                "559555955" to "§7Nexus has been destroyed",
                "669555955" to "§7by $destroyer",
                "779999777" to "",
                "888888888" to "",
            ) to mapOf(
                '0' to TextColor.color(245, 245, 255),
                '1' to TextColor.color(235, 235, 255),
                '2' to TextColor.color(225, 225, 255),
                '3' to TextColor.color(215, 215, 255),
                '4' to TextColor.color(205, 205, 255),
                '5' to TextColor.color(195, 195, 255),
                '6' to TextColor.color(185, 185, 255),
                '7' to TextColor.color(175, 175, 255),
                '8' to TextColor.color(165, 165, 255),
                '9' to TextColor.color(0, 0, 255),
            ))
            else -> {
                Bukkit.broadcast(BukkitComponent.text("${team.color}${team.name} §7チームのネクサスは ${destroyer} §7によって破壊された!"))
                return
            }
        }

        sendPixelArtWithMessage(message)
    }

    fun sendPixelArtWithMessage(message: Pair<Array<Pair<String, String>>, Map<Char, TextColor>>) {
        message.first.map { line ->
            var component = BukkitComponent.empty()
            line.first.map {
                return@map message.second[it] ?: throw IllegalArgumentException()
            }.forEach { component = component.append(BukkitComponent.text("█").color(it)) }

            return@map component.append(BukkitComponent.text(" ${line.second}"))
        }.forEach {
            Bukkit.broadcast(it)
        }
    }

    private fun getNexusDamage(): Int {
        if (phase > 4 && map.doubleNexusDamage) {
            return 2
        }
        else {
            return 1
        }
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

    fun canUseBlazePowder(): Boolean {
        return !isStarted || phase >= BLAZE_POWDER_USABLE_PHASE
    }
}