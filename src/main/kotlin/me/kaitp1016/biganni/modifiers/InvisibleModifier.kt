package me.kaitp1016.biganni.modifiers

import me.kaitp1016.biganni.game.boss.BossBuffItems.BootsOfExtingushment
import me.kaitp1016.biganni.game.boss.BossBuffItems.ChestplateOfExtingushment
import me.kaitp1016.biganni.game.boss.BossBuffItems.HelmetOfExtingushment
import me.kaitp1016.biganni.game.boss.BossBuffItems.LeggingsOfExtingushment
import me.kaitp1016.biganni.utils.ItemUtils.getAnniId
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.potion.PotionEffectType

object InvisibleModifier: Listener {
    @EventHandler
    fun onDamage(event: EntityDamageEvent) {
        val player = event.entity as? Player ?: return
        if (!player.hasPotionEffect(PotionEffectType.INVISIBILITY)) return

        val inv = player.inventory
        if (inv.getItem(EquipmentSlot.HEAD).getAnniId() == HelmetOfExtingushment.id && inv.getItem(EquipmentSlot.CHEST).getAnniId() == ChestplateOfExtingushment.id && inv.getItem(EquipmentSlot.LEGS).getAnniId() == LeggingsOfExtingushment.id && inv.getItem(EquipmentSlot.FEET).getAnniId() == BootsOfExtingushment.id) {
            return
        }

        player.removePotionEffect(PotionEffectType.INVISIBILITY)
        player.world.playSound(player.location, Sound.ENTITY_ZOMBIE_VILLAGER_CONVERTED,1f,1f)
    }

    @EventHandler
    fun onBreak(event: BlockBreakEvent) {
        val player = event.player
        if (!player.hasPotionEffect(PotionEffectType.INVISIBILITY)) return

        val inv = player.inventory
        if (inv.getItem(EquipmentSlot.HEAD).getAnniId() == HelmetOfExtingushment.id && inv.getItem(EquipmentSlot.CHEST).getAnniId() == ChestplateOfExtingushment.id && inv.getItem(EquipmentSlot.LEGS).getAnniId() == LeggingsOfExtingushment.id && inv.getItem(EquipmentSlot.FEET).getAnniId() == BootsOfExtingushment.id) {
            return
        }

        player.removePotionEffect(PotionEffectType.INVISIBILITY)
        player.world.playSound(player.location, Sound.ENTITY_ZOMBIE_VILLAGER_CONVERTED,1f,1f)
    }
}