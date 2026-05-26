package me.kaitp1016.biganni.game

import me.kaitp1016.biganni.anniclass.impl.HandymanClass
import me.kaitp1016.biganni.game.Game.isStarted
import me.kaitp1016.biganni.game.Game.map
import me.kaitp1016.biganni.game.Game.phase
import me.kaitp1016.biganni.game.Game.teams
import me.kaitp1016.biganni.game.boss.FinalBossFight
import me.kaitp1016.biganni.utils.MCUtils.toMC
import me.kaitp1016.biganni.utils.Scheduler
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor
import net.minecraft.world.entity.EquipmentSlot
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.block.Block
import org.bukkit.boss.BarColor
import org.bukkit.entity.Player
import kotlin.random.Random

object NexusHandler {
    const val NEXUS_RESPAWN_TICK = 7

    fun onMine(player: Player, block: Block, team: AnniTeam) {
        if (phase < 2 || team.name.equals(player.toMC().team?.name, ignoreCase = true)) return

        team.health -= getNexusDamage()

        val pitch = Random.nextFloat() * 0.8f
        val actionbar = player.teamDisplayName().append(Component.text(" damaged the").color(NamedTextColor.GRAY).append(Component.text(" ${team.color}${team.name} team's nexus!")))

        val world = block.world
        val blockLocation = block.location

        Bukkit.getOnlinePlayers().forEach {
            if (team.health < 1) it.playSound(it, Sound.ENTITY_GENERIC_EXPLODE, 2f, 0f)
            else if (world == it.world && blockLocation.toVector().distance(it.location.toVector()) < 30) it.playSound(it, Sound.BLOCK_ANVIL_PLACE, 2f, pitch)
            else if (it.toMC().team?.name?.equals(team.name, ignoreCase = true) == true) it.playSound(it, Sound.BLOCK_NOTE_BLOCK_HARP, 2f, 2f)

            it.sendActionBar(actionbar)
        }

        Game.updateNexusHealth(team)

        if (team.health < 1) {
            world.setBlockData(blockLocation, Material.BEDROCK.createBlockData())
        } else {
            world.setBlockData(blockLocation, Material.AIR.createBlockData())

            Scheduler.scheduleTask(NEXUS_RESPAWN_TICK) {
                world.setBlockData(blockLocation, Material.END_STONE.createBlockData())
            }
        }

        spawnParticle(blockLocation.toCenterLocation())

        HandymanClass.onMineNexus(player)

        val mcPlayer = player.toMC()
        mcPlayer.mainHandItem.hurtAndBreak(1, mcPlayer, EquipmentSlot.MAINHAND)

        if (team.health < 1) {
            onNexusBroken(team, player)
        }
    }

    fun getNexusDamage(): Int {
        if (phase > 4 && map.doubleNexusDamage) {
            return 2
        } else {
            return 1
        }
    }

    fun onNexusBroken(team: AnniTeam, breaker: Player) {
        sendDestructionMessage(team, breaker)

        if (teams.count { it.health > 1 } < 2) {
            sendBossFightMessage()

            isStarted = false
            BossBarManager.setTitle("Game Ended.")
            BossBarManager.setColor(BarColor.PURPLE)
            BossBarManager.setProgress(1.0)
            FinalBossFight.start()
        }
    }

    private fun spawnParticle(location: Location) {
        val world = location.world

        world.spawnParticle(Particle.LAVA, location, 75, 2.0, 2.0, 2.0)

        repeat(10) {
            world.spawnParticle(Particle.FIREWORK, location, 0, Random.nextDouble(-1.5, 1.5), Random.nextDouble(-1.5, 1.5), Random.nextDouble(-1.5, 1.5))
        }

        repeat(6) {
            world.spawnParticle(Particle.FIREWORK, location, 0, Random.nextDouble(-0.5, 0.5), Random.nextDouble(-0.5, 0.5), Random.nextDouble(-0.5, 0.5))
        }
    }

    private fun sendBossFightMessage() {
        val message = arrayOf(
            "119999911" to Component.text(""),
            "190000091" to Component.text(""),
            "900000009" to Component.text("        §6§lBoss Fight!"),
            "909000909" to Component.text("§aIt's time to go all in if you're brave."),
            "909909909" to Component.text("§aBe sneaking in 30s time,"),
            "900000009" to Component.text("§ato be teleported to the arena!"),
            "190909091" to Component.text(""),
            "190909091" to Component.text(""),
            "199999991" to Component.text(""),
        ) to mapOf(
            '0' to TextColor.color(255, 255, 255),
            '1' to TextColor.color(92, 90, 90),
            '9' to TextColor.color(0, 0, 0),
        )

        Game.sendPixelArtMessage(message)
    }

    private fun sendDestructionMessage(team: AnniTeam, destroyer: Player) {
        val message = when (team.name.lowercase()) {
            "red" -> (arrayOf(
                "000000000" to Component.text(""),
                "119999111" to Component.text(""),
                "229222922" to Component.text(""),
                "339333933" to Component.text(""),
                "449999444" to Component.text("${team.color}${team.name} §7team's "),
                "559595555" to Component.text("§7Nexus has been destroyed"),
                "669669666" to Component.text("§7by ").append(Component.empty().color(NamedTextColor.WHITE).append(destroyer.teamDisplayName())),
                "779777977" to Component.text(""),
                "888888888" to Component.text(""),
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
                "000000000" to Component.text(""),
                "119999111" to Component.text(""),
                "229222922" to Component.text(""),
                "339333933" to Component.text(""),
                "449999444" to Component.text("${team.color}${team.name} §7team's "),
                "559555955" to Component.text("§7Nexus has been destroyed"),
                "669555955" to Component.text("§7by ").append(Component.empty().color(NamedTextColor.WHITE).append(destroyer.teamDisplayName())),
                "779999777" to Component.text(""),
                "888888888" to Component.text(""),
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

            "green" -> (arrayOf(
                "000000000" to Component.text(""),
                "111999111" to Component.text(""),
                "229222922" to Component.text(""),
                "339333333" to Component.text(""),
                "449444444" to Component.text("${team.color}${team.name} §7team's "),
                "559559955" to Component.text("§7Nexus has been destroyed"),
                "669666966" to Component.text("§7by ").append(Component.empty().color(NamedTextColor.WHITE).append(destroyer.teamDisplayName())),
                "777999777" to Component.text(""),
                "888888888" to Component.text(""),
            ) to mapOf(
                '0' to TextColor.color(245, 255, 245),
                '1' to TextColor.color(235, 255, 235),
                '2' to TextColor.color(225, 255, 225),
                '3' to TextColor.color(215, 255, 215),
                '4' to TextColor.color(205, 255, 205),
                '5' to TextColor.color(195, 255, 195),
                '6' to TextColor.color(185, 255, 185),
                '7' to TextColor.color(175, 255, 175),
                '8' to TextColor.color(165, 255, 165),
                '9' to TextColor.color(0, 255, 0),
            ))

            "yellow" -> (arrayOf(
                "000000000" to Component.text(""),
                "119111911" to Component.text(""),
                "229222922" to Component.text(""),
                "333939333" to Component.text(""),
                "444494444" to Component.text("${team.color}${team.name} §7team's "),
                "555595555" to Component.text("§7Nexus has been destroyed"),
                "666696666" to Component.text("§7by ").append(Component.empty().color(NamedTextColor.WHITE).append(destroyer.teamDisplayName())),
                "777797777" to Component.text(""),
                "888888888" to Component.text(""),
            ) to mapOf(
                '0' to TextColor.color(255, 255, 245),
                '1' to TextColor.color(255, 255, 235),
                '2' to TextColor.color(255, 255, 225),
                '3' to TextColor.color(255, 255, 215),
                '4' to TextColor.color(255, 255, 205),
                '5' to TextColor.color(255, 255, 195),
                '6' to TextColor.color(255, 255, 185),
                '7' to TextColor.color(255, 255, 175),
                '8' to TextColor.color(255, 255, 165),
                '9' to TextColor.color(255, 255, 0),
            ))

            else -> (arrayOf(
                "000000000" to Component.text(""),
                "000000000" to Component.text(""),
                "000000000" to Component.text(""),
                "000000000" to Component.text(""),
                "000000000" to Component.text("${team.color}${team.name} §7team's "),
                "000000000" to Component.text("§7Nexus has been destroyed"),
                "000000000" to Component.text("§7by ").append(Component.empty().color(NamedTextColor.WHITE).append(destroyer.teamDisplayName())),
                "000000000" to Component.text(""),
                "000000000" to Component.text(""),
            ) to mapOf(
                '0' to TextColor.color(255, 255, 255),
            ))
        }

        Game.sendPixelArtMessage(message)
    }
}