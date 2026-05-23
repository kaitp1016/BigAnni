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
import net.kyori.adventure.text.format.TextDecoration
import net.minecraft.world.item.Items
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.damage.DamageType
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import kotlin.math.abs
import kotlin.random.Random

object VampireClass: AnniClass(), Listener {
    override val icon = Items.REDSTONE
    override val name = "Vampire"
    override val shortName = "VMP"
    override val description = arrayOf(
        "この職業の時は常に攻撃力が増える。",
        "敵を殴ることで確率で体力を回復する。",
        "Blood Senseを使用することで、周囲の敵を発光させる。",
        "Insidisious Dispatchを使用すると、自身に背を向けてる敵にテレポートできる。"
    )

    const val BLOOD_SENSE_ITEM_ID = "vampire_blood_sense"
    const val BLOOD_SENSE_COOLDOWN = 60
    val BLOOD_SENSE_COOLDOWN_GROUP = Key.key(PLUGIN_ID,"vampire_blood_sense")

    const val INSIDISIOUS_DISPATCH_ITEM_ID = "vampire_insidisious_dispatch"
    const val INSIDISIOUS_DISPATCH_COOLDOWN = 800
    val INSIDISIOUS_DISPATCH_COOLDOWN_GROUP = Key.key(PLUGIN_ID,"vampire_insidisious_dispatch")

    override fun getDefaultItems(player: Player): MutableList<ItemStack> {
        return super.getDefaultItems(player).also {
            it.add(ItemStack(Material.POTION).apply {
                uniqueClassItem()
                soulbound()
                setAnniItem(BLOOD_SENSE_ITEM_ID)

                setData(DataComponentTypes.USE_COOLDOWN, UseCooldown.useCooldown(BLOOD_SENSE_COOLDOWN / 20f).cooldownGroup(BLOOD_SENSE_COOLDOWN_GROUP).build())

                editMeta {
                    it.customName(Component.text("Blood Sense").color(NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false))
                }
            })

            it.add(ItemStack(Material.MUSIC_DISC_CHIRP).apply {
                uniqueClassItem()
                soulbound()
                setAnniItem(INSIDISIOUS_DISPATCH_ITEM_ID)

                setData(DataComponentTypes.USE_COOLDOWN, UseCooldown.useCooldown(INSIDISIOUS_DISPATCH_COOLDOWN / 20f).cooldownGroup(INSIDISIOUS_DISPATCH_COOLDOWN_GROUP).build())

                editMeta {
                    it.itemName(Component.text("Insidious Dispatch").color(NamedTextColor.GOLD))
                }
            })
        }
    }

    @EventHandler
    fun onInteract(event: PlayerInteractEvent) {
        val player = event.player
        if (!isSelected(player)) return

        val item = event.item ?: return
        if (player.hasCooldown(item)) return

        val id = item.getAnniId()
        if (id == BLOOD_SENSE_ITEM_ID) {
            event.isCancelled = true

            val world = player.world
            val team = player.toMC().teamColor

            val targets = world.getNearbyPlayers(player.location,8.0).filter { it.toMC().teamColor != team }
            if (targets.isEmpty()) return

            targets.forEach {
                it.addPotionEffect(PotionEffect(PotionEffectType.GLOWING,30,0))
            }

            player.setCooldown(BLOOD_SENSE_COOLDOWN_GROUP,BLOOD_SENSE_COOLDOWN)
        }
        if (id == INSIDISIOUS_DISPATCH_ITEM_ID) {
            event.isCancelled = true

            val target = player.rayTraceEntities(30)?.hitEntity ?: return
            if (target !is Player) return

            val distance = abs(target.yaw % 180 - player.yaw % 180)
            if (distance > 40 || player.toMC().teamColor == target.toMC().teamColor) return

            player.teleport(target)
            player.world.playSound(player.location, Sound.ENTITY_ENDERMAN_TELEPORT,1f,1f)

            player.setCooldown(INSIDISIOUS_DISPATCH_COOLDOWN_GROUP,INSIDISIOUS_DISPATCH_COOLDOWN)
        }
    }

    @EventHandler
    fun onDamage(event: EntityDamageEvent) {
        val source = event.damageSource
        val attacker = source.causingEntity

        if (attacker is Player && source.damageType == DamageType.PLAYER_ATTACK && isSelected(attacker)) {
            event.damage *= 1.25

            val world = attacker.world
            val chance = if (world.isDayTime) 15 else 30
            if (Random.nextInt(100) < chance) {
                attacker.heal(1.0)
                attacker.world.playSound(attacker.location, Sound.ENTITY_ZOMBIE_VILLAGER_CURE,1f,1f)
            }
        }
    }
}