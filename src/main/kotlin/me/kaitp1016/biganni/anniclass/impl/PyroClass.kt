package me.kaitp1016.biganni.anniclass.impl

import io.papermc.paper.datacomponent.DataComponentTypes
import io.papermc.paper.datacomponent.item.UseCooldown
import me.kaitp1016.biganni.PLUGIN_ID
import me.kaitp1016.biganni.anniclass.AnniClass
import me.kaitp1016.biganni.utils.ItemUtils.getAnniId
import me.kaitp1016.biganni.utils.ItemUtils.setAnniItem
import me.kaitp1016.biganni.utils.MCUtils.toMC
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.minecraft.world.item.Items
import org.bukkit.Material
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeModifier
import org.bukkit.damage.DamageSource
import org.bukkit.damage.DamageType
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.EquipmentSlotGroup
import org.bukkit.inventory.ItemStack
    import org.bukkit.inventory.meta.PotionMeta
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.bukkit.potion.PotionType
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin
import kotlin.random.Random

object PyroClass: AnniClass(), Listener {
    override val name = "Pyro"
    override val shortName = "PYR"
    override val icon = Items.FLINT_AND_STEEL
    override val description = arrayOf(
        "敵にダメージを与えると確率で燃やす。",
        "アビリティを使用すると周囲にいる敵を燃やす。",
    )

    const val FIRESTORM_ITEM_ID = "pyro_firestorm"
    const val FIRESTORM_COOLDOWN = 800
    val FIRESTORM_COOLDOWN_GROUP = Key.key(PLUGIN_ID,"pyro_firestorm")

    override fun getDefaultItems(player: Player): MutableList<ItemStack> {
        return super.getDefaultItems(player).also {
            it.add(ItemStack(Material.POTION).apply {
                uniqueClassItem()
                soulbound()

                editMeta {
                    (it as PotionMeta).basePotionType = PotionType.HEALING
                }
            })

            it.add(ItemStack(Material.FIRE_CHARGE).apply {
                uniqueClassItem()
                soulbound()
                setAnniItem(FIRESTORM_ITEM_ID)

                setData(DataComponentTypes.USE_COOLDOWN, UseCooldown.useCooldown(FIRESTORM_COOLDOWN / 20f).cooldownGroup(FIRESTORM_COOLDOWN_GROUP).build())

                editMeta {
                    it.itemName(Component.text("Firestorm").color(NamedTextColor.AQUA))
                    it.addAttributeModifier(Attribute.ATTACK_DAMAGE, AttributeModifier(AXE_ATTRIBUTE_MODIFIER_KEY,3.5, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.MAINHAND))
                }
            })
        }
    }

    override fun onSelect(player: Player) {
        player.addPotionEffect(PotionEffect(PotionEffectType.FIRE_RESISTANCE, PotionEffect.INFINITE_DURATION,0))
        super.onSelect(player)
    }

    override fun onUnselect(player: Player) {
        player.removePotionEffect(PotionEffectType.FIRE_RESISTANCE)
        super.onUnselect(player)
    }

    @EventHandler
    fun onInteract(event: PlayerInteractEvent) {
        if (!event.action.isRightClick) return

        val player = event.player
        if (!isSelected(player)) return

        val item = event.item ?: return
        if (item.getAnniId() != FIRESTORM_ITEM_ID || player.hasCooldown(item)) return

        event.isCancelled = true

        val team = player.toMC().teamColor
        val targets = player.world.getNearbyPlayers(player.location,5.0)

        targets.forEach { target ->
            if (target.toMC().teamColor == team) {
                target.fireTicks = 0
                target.addPotionEffect(PotionEffect(PotionEffectType.FIRE_RESISTANCE,200,0))
            }
            else {
                if (target.fireTicks > 0) {
                    val source = DamageSource.builder(DamageType.GENERIC_KILL)
                        .withDirectEntity(player)
                        .build()

                    target.damage(4.0,source)
                }
                else {
                    target.fireTicks = 120
                }
            }

            target.world.playSound(target.location, Sound.ENTITY_BLAZE_SHOOT,1f,1f)
        }

        player.world.playSound(player.location, Sound.ENTITY_BLAZE_SHOOT,1f,1f)

        val distance = 5.0
        val amount = 32
        val world = player.world

        repeat(amount) {
            val angle = 360f / amount * it * PI / 180f
            val x = player.x + distance * cos(angle)
            val z = player.z + distance * sin(angle)

            repeat(10) { dy ->
                val y = player.y + dy - 5

                Particle.FLAME.builder()
                    .location(world, x, y, z)
                    .receivers(32, true)
                    .count(0)
                    .offset(0.0, 0.0, 0.0)
                    .spawn()
            }
        }

        player.setCooldown(FIRESTORM_COOLDOWN_GROUP,FIRESTORM_COOLDOWN)
    }

    @EventHandler(priority = EventPriority.LOW)
    fun onDamage(event: EntityDamageEvent) {
        val source = event.damageSource
        val damager = source.causingEntity
        if (damager !is Player || !isSelected(damager)) return

        if (source.damageType == DamageType.PLAYER_ATTACK) {
            if (Random.nextInt(0,5) == 2) {
                val target = event.entity
                target.fireTicks = max(target.fireTicks,60)
                target.world.playSound(target.location, Sound.ENTITY_BLAZE_SHOOT,1f,1f)
            }
        }
        if (source.damageType == DamageType.ARROW) {
            val target = event.entity
            target.fireTicks = max(target.fireTicks,400)
            target.world.playSound(target.location, Sound.ENTITY_BLAZE_SHOOT,1f,1f)
        }
    }
}