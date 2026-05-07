package me.kaitp1016.biganni.modifiers

import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.potion.PotionEffectType

object InvisibleModifier: Listener {
    @EventHandler
    fun onDamage(event: EntityDamageEvent) {
        val player = event.entity as? Player ?: return
        if (!player.hasPotionEffect(PotionEffectType.INVISIBILITY)) return

        player.removePotionEffect(PotionEffectType.INVISIBILITY)
        player.playSound(player.location, Sound.ENTITY_ZOMBIE_VILLAGER_CONVERTED,1f,1f)
    }

    @EventHandler
    fun onBreak(event: BlockBreakEvent) {
        val player = event.player
        if (!player.hasPotionEffect(PotionEffectType.INVISIBILITY)) return

        player.removePotionEffect(PotionEffectType.INVISIBILITY)
        player.playSound(player.location, Sound.ENTITY_ZOMBIE_VILLAGER_CONVERTED,1f,1f)
    }
}