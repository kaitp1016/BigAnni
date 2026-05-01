package me.kaitp1016.biganni.modifier

import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerItemConsumeEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import java.awt.desktop.QuitEvent
import java.util.UUID

object GoldenAppleModifier: Listener {
    data class GappleCooldown(val player: UUID, var count: Int = 0, var lastEat: Long)

    const val GAPPLE_COOLDOWN = 180000

    val gappleStates = mutableListOf<GappleCooldown>()

    @EventHandler
    fun onConsume(event: PlayerItemConsumeEvent) {
        val item = event.item
        val player = event.player
        if (item.type == Material.ENCHANTED_GOLDEN_APPLE) {
            player.addPotionEffect(PotionEffect(PotionEffectType.REGENERATION,600,2))

            val time = System.currentTimeMillis()

            val cooldown = gappleStates.find { it.player == player.uniqueId } ?: run {
                val cooldown = GappleCooldown(player.uniqueId,0, time)
                gappleStates.add(cooldown)
                cooldown
            }

            if (cooldown.lastEat + GAPPLE_COOLDOWN < time) {
                cooldown.count = 0
            }

            cooldown.lastEat = time
            cooldown.count++

            if (cooldown.count >= 3) {
                player.setCooldown(item,3600)
            }
        }
    }

    @EventHandler
    fun onQuit(event: PlayerQuitEvent) {
        val player = event.player.uniqueId
        gappleStates.removeIf { it.player == player }
    }
}