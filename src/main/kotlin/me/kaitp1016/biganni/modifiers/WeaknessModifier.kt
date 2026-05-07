package me.kaitp1016.biganni.modifiers

import me.kaitp1016.biganni.plugin
import org.bukkit.NamespacedKey
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeModifier
import org.bukkit.damage.DamageType
import org.bukkit.entity.LivingEntity
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.EntityPotionEffectEvent
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType

object WeaknessModifier: Listener {
    val WEAKNESS_MODIFIER_KEY = NamespacedKey(plugin,"weakness_modifier")

    @EventHandler
    fun onPotionEffect(event: EntityPotionEffectEvent) {
        if (event.modifiedType != PotionEffectType.WEAKNESS) return

        val entity = event.entity as? LivingEntity ?: return
        val effect = event.newEffect?.amplifier
        if (effect == null) {
            entity.getAttribute(Attribute.ATTACK_DAMAGE)?.removeModifier(WEAKNESS_MODIFIER_KEY)
            return
        }

        val attribute = entity.getAttribute(Attribute.ATTACK_DAMAGE) ?: return
        attribute.removeModifier(WEAKNESS_MODIFIER_KEY)
        attribute.addTransientModifier(AttributeModifier(WEAKNESS_MODIFIER_KEY,getVanillaWeaknessReduction(effect).toDouble(), AttributeModifier.Operation.ADD_NUMBER))
    }

    @EventHandler
    fun onDamage(event: EntityDamageEvent) {
        val source = event.damageSource
        val directEntity = source.directEntity ?: return
        if ((source.damageType != DamageType.PLAYER_ATTACK && source.damageType != DamageType.MOB_ATTACK && source.damageType != DamageType.MOB_ATTACK_NO_AGGRO) || directEntity !is LivingEntity) return

        val weakness = directEntity.getPotionEffect(PotionEffectType.WEAKNESS)?.amplifier ?: return
        event.damage -= getNewWeaknessReduction(weakness,event.damage)
    }

    fun getNewWeaknessReduction(level: Int,damage: Double): Double {
        return (level + 1) * (damage / 10)
    }

    fun getVanillaWeaknessReduction(level: Int): Int {
        return (level + 1) * 4
    }
}