package me.kaitp1016.biganni.anniclass.impl

import io.papermc.paper.datacomponent.DataComponentTypes
import io.papermc.paper.datacomponent.item.UseCooldown
import me.kaitp1016.biganni.PLUGIN_ID
import me.kaitp1016.biganni.anniclass.AnniClass
import me.kaitp1016.biganni.game.Game
import me.kaitp1016.biganni.utils.ItemUtils.getAnniId
import me.kaitp1016.biganni.utils.ItemUtils.setAnniItem
import me.kaitp1016.biganni.utils.MCUtils.toMC
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.minecraft.world.item.Items
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeModifier
import org.bukkit.damage.DamageType
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType

object ChargerClass: AnniClass(), Listener {
    override val icon = Items.NETHERITE_SWORD
    override val name = "Charger"
    override val description = arrayOf(
        "連打ができなくなり、連打が可能な場合は他の効果は発動しない。",
        "近接ダメージがクリティカルなら1.1倍、それ以外は1.3倍になる。",
        "アビリティを使用すると周囲の敵の移動速度を低下させる。",
    )

    const val FEARFUL_ITEM_ID = "charger_fearful"
    const val FEARFUL_COOLDOWN = 1200
    val FEARFUL_COOLDOWN_GROUP = Key.key(PLUGIN_ID,"charger_fearful")

    const val FEARFUL_TIME = 40

    override fun getDefaultItems(player: Player): MutableList<ItemStack> {
        return super.getDefaultItems(player).also {
            it.removeIf { it.type == Material.WOODEN_SWORD }
            it.add(ItemStack(Material.STONE_SWORD).uniqueClassItem().soulbound())

            it.add(ItemStack(Material.GHAST_TEAR).apply {
                uniqueClassItem()
                soulbound()
                setAnniItem(FEARFUL_ITEM_ID)

                setData(DataComponentTypes.USE_COOLDOWN, UseCooldown.useCooldown(FEARFUL_COOLDOWN / 20f).cooldownGroup(FEARFUL_COOLDOWN_GROUP).build())

                editMeta {
                    it.itemName(Component.text("Fearful").color(NamedTextColor.GOLD))
                }
            })
        }
    }

    override fun onSelect(player: Player) {
        player.getAttribute(Attribute.ATTACK_SPEED)?.removeModifier(Game.ATTACK_SPEED_MODIFIER)

        super.onSelect(player)
    }

    override fun onUnselect(player: Player) {
        val attackSpeed = player.getAttribute(Attribute.ATTACK_SPEED)
        if (attackSpeed?.getModifier(Game.ATTACK_SPEED_MODIFIER) == null) attackSpeed?.addModifier(AttributeModifier(Game.ATTACK_SPEED_MODIFIER,100000.0, AttributeModifier.Operation.ADD_NUMBER))

        super.onUnselect(player)
    }

    @EventHandler
    fun onInteract(event: PlayerInteractEvent) {
        val player = event.player
        if (!isSelected(player)) return

        val item = event.item ?: return
        if (item.getAnniId() != FEARFUL_ITEM_ID || player.hasCooldown(item)) return

        player.playSound(player.location, Sound.ENTITY_GHAST_WARN,2f,0.8f)

        val team = player.toMC().teamColor
        player.world.getNearbyPlayers(player.location,6.0).forEach { target ->
            if (target.toMC().teamColor == team || BerserkerClass.isUsingAbility(target)) return@forEach


            target.addPotionEffect(PotionEffect(PotionEffectType.SLOWNESS,FEARFUL_TIME,0))
        }

        player.setCooldown(FEARFUL_COOLDOWN_GROUP,FEARFUL_COOLDOWN)
    }

    @EventHandler
    fun onDamage(event: EntityDamageEvent) {
        val source = event.damageSource
        val player = source.causingEntity as? Player ?: return
        if (source.damageType != DamageType.PLAYER_ATTACK || !isSelected(player)) return

        val attackSpeed = player.getAttribute(Attribute.ATTACK_SPEED)?.value ?: 0.0
        if (attackSpeed >= 4.0) return

        event.damage *= if (source.toMC().isCritical) 1.1f else 1.3f
    }
}