package me.kaitp1016.biganni.anniclass.impl

import com.destroystokyo.paper.event.server.ServerTickStartEvent
import io.papermc.paper.datacomponent.DataComponentTypes
import io.papermc.paper.datacomponent.item.UseCooldown
import me.kaitp1016.biganni.PLUGIN_ID
import me.kaitp1016.biganni.anniclass.AnniClass
import me.kaitp1016.biganni.utils.ItemUtils.getAnniItemId
import me.kaitp1016.biganni.utils.ItemUtils.setAnniItem
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
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
import kotlin.math.min

object WarriorClass: AnniClass(), Listener {
    override val icon = Items.STONE_SWORD
    override val name = "Warrior"
    override val description = arrayOf(
        "この職業の時は常に攻撃力が増える。",
        "アビリティを追加することで追加で攻撃力が増え、移動速度が増える。",
    )

    const val FRENZY_ITEM_ID = "warrior_frenzy"
    const val FRENZY_COOLDOWN = 1200
    val FRENZY_COOLDOWN_GROUP = Key.key(PLUGIN_ID,"warrior_frenzy")

    const val FRENZY_TIME = 240

    override fun getDefaultItems(player: Player): MutableList<ItemStack> {
        return super.getDefaultItems(player).also {
            it.removeIf { it.type == Material.WOODEN_SWORD }
            it.add(ItemStack(Material.GOLDEN_SWORD).uniqueClassItem().soulbound())

            it.add(ItemStack(Material.BLAZE_POWDER).apply {
                uniqueClassItem()
                soulbound()
                setAnniItem(FRENZY_ITEM_ID)

                setData(DataComponentTypes.USE_COOLDOWN, UseCooldown.useCooldown(FRENZY_COOLDOWN / 20f).cooldownGroup(FRENZY_COOLDOWN_GROUP).build())

                editMeta {
                    it.itemName(Component.text("Frenzy").color(NamedTextColor.GOLD))
                }
            })
        }
    }

    data class FrenzyAbility(val player: Player, var time: Int)

    val frenzies = mutableListOf<FrenzyAbility>()

    @EventHandler
    fun onInteract(event: PlayerInteractEvent) {
        val player = event.player
        if (!isSelected(player)) return

        val item = event.item ?: return
        if (item.getAnniItemId() != FRENZY_ITEM_ID || player.hasCooldown(item)) return

        player.addPotionEffect(PotionEffect(PotionEffectType.SPEED,FRENZY_TIME,0))
        player.playSound(player.location, Sound.ENTITY_POLAR_BEAR_WARNING,1f,1f)

        frenzies.add(FrenzyAbility(player,FRENZY_TIME))
        player.setCooldown(FRENZY_COOLDOWN_GROUP,FRENZY_COOLDOWN)
    }

    @EventHandler
    fun onTick(event: ServerTickStartEvent) {
        if (frenzies.isEmpty()) return

        frenzies.removeAll {
            it.time--
            return@removeAll it.time <= 0
        }
    }

    @EventHandler
    fun onDamage(event: EntityDamageEvent) {
        val source = event.damageSource
        val entity = event.entity
        if (entity is Player && isSelected(entity) && frenzies.any{ it.player == entity }) {
            event.damage = min(event.damage * 1.25,999999999.9)
        }

        val attacker = source.causingEntity
        if (attacker is Player && source.damageType == DamageType.PLAYER_ATTACK && isSelected(attacker)) {
            event.damage += 1

            if (frenzies.any { it.player == attacker }) {
                event.damage += 1
            }
        }
    }
}