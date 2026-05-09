package me.kaitp1016.biganni.game

import com.destroystokyo.paper.event.server.ServerTickStartEvent
import com.sun.jna.platform.win32.OaIdl
import io.papermc.paper.event.player.AsyncChatEvent
import me.kaitp1016.biganni.anniclass.AnniClassManager.getAnniClass
import me.kaitp1016.biganni.mc
import me.kaitp1016.biganni.packetgui.impl.AnniClassSelector
import me.kaitp1016.biganni.plugin
import me.kaitp1016.biganni.utils.ItemUtils.isAnniItem
import me.kaitp1016.biganni.utils.MCUtils.toBukkit
import me.kaitp1016.biganni.utils.MCUtils.toMC
import me.kaitp1016.biganni.utils.Scheduler
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.Component
import net.minecraft.util.CommonColors
import net.minecraft.world.phys.AABB
import org.bukkit.Bukkit
import org.bukkit.Location
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
import org.bukkit.event.entity.EntityDeathEvent
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.player.*
import kotlin.random.Random
import net.kyori.adventure.text.Component as BukkitComponent

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

        val world = Bukkit.getWorld(Key.key("sys","coastal"))
        teams.add(AnniTeam("Blue", BlockPos(-118,-51,190),"§9",Location(world,-108.5, -38.0, 180.5, -135f, 0f),AABB(-70.0, -256.0, 138.0,-140.0, 312.0,217.0),Location(world,-117.0, -52.0, 187.0),Location(world,6.5,-47.0,127.5)))
        teams.add(AnniTeam("Red", BlockPos(118,-51,-188),"§c", Location(world,109.5, -38.0, -177.5, 45f, 0f), AABB(77.0, -256.0, -133.0, 115.0, 312.0, -225.0),Location(world,118.0, -52.0, -185.0),Location(world,-5.5, -47.0, -124.5)))

        ScoreboardManager.reset()
        ScoreboardManager.setLine(0, Component.literal("§6apple.playit.plus"))
        ScoreboardManager.setLine(1, Component.empty())

        teams.forEach { updateNexusHealth(it) }

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

        BossBarManager.setTitle("Annihilation")
    }

    fun updateNexusHealth(team: AnniTeam) {
        val index = teams.indexOf(team) + 2
        val health = if (team.health < 1) "§c✘" else "§b${team.health}"
        ScoreboardManager.setLine(index,Component.literal("${team.color}${team.name} Nexus: ${health}"))
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

                if (phase == 3) {
                    teams.forEach { it.spawnWitch() }
                }

                if (phase == 4) {
                    BossManager.spawn()

                    val world = Bukkit.getWorld(Key.key("sys","coastal"))!!

                    repeat(9) {
                        // 95 -48 3
                        val x = 95 + it % 3
                        val y = -48
                        val z = 3 + it / 3
                        world.setBlockData(x,y,z,Material.END_PORTAL.createBlockData())
                    }

                    repeat(9) {
                        //-97 -48 -3
                        val x = -97 + it % 3
                        val y = -48
                        val z = -3 + it / 3
                        world.setBlockData(x,y,z,Material.END_PORTAL.createBlockData())
                    }
                }
            }
        }

        if (phase > 4) {
            BossBarManager.setTitle("Phase 5 - §cDouble §fNexus Damage!")
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

        val pos = BlockPos(block.x,block.y,block.z)
        val team = teams.find { it.nexus == pos } ?: return
        val player = event.player

        event.isCancelled = true

        if (phase < 2 || team.name.equals(player.toMC().team?.name,ignoreCase = true)) return

        val damage = if (phase < MAX_PHASE) 1 else 2
        team.health -= damage

        val pitch = Random.nextFloat() * 0.8f

        Bukkit.getOnlinePlayers().forEach {
            if (team.health < 1) it.playSound(it, Sound.ENTITY_GENERIC_EXPLODE,2f,0f)
            else if (block.location.distance(it.location) < 30) it.playSound(it, Sound.BLOCK_ANVIL_PLACE,2f,pitch)
            else if (it.toMC().team?.name?.equals(team.name,ignoreCase = true) == true) it.playSound(it, Sound.BLOCK_NOTE_BLOCK_HARP,2f,2f)
        }

        updateNexusHealth(team)

        if (team.health < 1) {
            block.world.setBlockData(block.location, Material.BEDROCK.createBlockData())
        }
        else {
            block.world.setBlockData(block.location, Material.AIR.createBlockData())

            Scheduler.scheduleTask(8) {
                block.world.setBlockData(block.location, Material.END_STONE.createBlockData())
            }
        }
    }

    @EventHandler
    fun onPortal(event: PlayerPortalEvent) {
        if (!isStarted) return

        if (event.cause == PlayerTeleportEvent.TeleportCause.NETHER_PORTAL) {
            val player = event.player
            AnniClassSelector(player.toMC()).open()
            event.isCancelled = true
        }
        if (event.cause == PlayerTeleportEvent.TeleportCause.END_PORTAL) {
            val player = event.player
            player.teleport(BossManager.BOSS_LOCATION)
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
            event.deathMessage(player.teamDisplayName().append(BukkitComponent.text("(${playerClass.deathMessageName})").append(BukkitComponent.text(" died.").color(NamedTextColor.GRAY))))
        }
        else {
            val reason = getDeathReason(source)
            val killerClass = killer.getAnniClass() ?: return
            event.deathMessage(killer.teamDisplayName().append(BukkitComponent.text("(${killerClass.deathMessageName})").append(BukkitComponent.text(" $reason ").color(NamedTextColor.GRAY).append(BukkitComponent.empty().color(NamedTextColor.WHITE).append(player.teamDisplayName().append(BukkitComponent.text("(${playerClass.deathMessageName})")))))))
        }
    }

    @EventHandler
    fun onEntityDeath(event: EntityDeathEvent) {
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

    fun getTeam(player: Player): AnniTeam? {
        val teamName = player.toMC().team?.name
        return teams.find { it.name.equals(teamName,true) }
    }
}