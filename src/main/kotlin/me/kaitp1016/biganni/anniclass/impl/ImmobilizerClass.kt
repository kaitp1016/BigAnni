package me.kaitp1016.biganni.anniclass.impl

import com.destroystokyo.paper.event.server.ServerTickStartEvent
import io.papermc.paper.datacomponent.DataComponentTypes
import io.papermc.paper.datacomponent.item.UseCooldown
import me.kaitp1016.biganni.PLUGIN_ID
import me.kaitp1016.biganni.anniclass.AnniClass
import me.kaitp1016.biganni.plugin
import me.kaitp1016.biganni.utils.FallDamageResistance
import me.kaitp1016.biganni.utils.ItemUtils.getAnniId
import me.kaitp1016.biganni.utils.ItemUtils.setAnniItem
import me.kaitp1016.biganni.utils.MCUtils.toMC
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.minecraft.world.item.Items
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeModifier
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import kotlin.math.max
import kotlin.math.min

object ImmobilizerClass: AnniClass(), Listener {
    override val name = "Immobilizer"
    override val shortName = "IMM"
    override val icon = Items.SLIME_BALL
    override val description = arrayOf(
        "左クリックでアビリティを使用すると周りの敵の移動速度を低下させる。",
        "右クリックでアビリティを使用すると自身と周りの敵を拘束させる。",
    )

    const val IMMOBILIZE_ITEM_ID = "imobilizer_immobilize"
    const val IMMOBILIZE_COOLDOWN = 400
    val IMMOBILIZE_COOLDOWN_GROUP = Key.key(PLUGIN_ID, "imobilizer_immobilize")

    const val IMMOBILIZE_RECEIVE_COOLDOWN = 400
    val IMMOBILIZE_JUMP_REDUCE_KEY = NamespacedKey(plugin, "immobilize_jump_reduce")

    override fun getDefaultItems(player: Player): MutableList<ItemStack> {
        return super.getDefaultItems(player).also {
            it.add(ItemStack(Material.SLIME_BALL).apply {
                uniqueClassItem()
                soulbound()
                setAnniItem(IMMOBILIZE_ITEM_ID)

                setData(DataComponentTypes.USE_COOLDOWN, UseCooldown.useCooldown(IMMOBILIZE_COOLDOWN / 20f).cooldownGroup(IMMOBILIZE_COOLDOWN_GROUP).build())

                editMeta {
                    it.itemName(Component.text("Immobilize").color(NamedTextColor.GOLD))
                }
            })
        }
    }

    data class TargetCooldown(val player: Player, var time: Int)
    data class Immobilize(val user: Player, val target: Player, var tick: Int)

    val immobilizes = mutableListOf<Immobilize>()
    val targetCooldown = mutableListOf<TargetCooldown>()

    @EventHandler
    fun onInteract(event: PlayerInteractEvent) {
        val player = event.player
        if (!isSelected(player)) return

        val item = event.item ?: return
        if (item.getAnniId() != IMMOBILIZE_ITEM_ID || player.hasCooldown(item)) return

        if (event.action == Action.RIGHT_CLICK_BLOCK || event.action == Action.RIGHT_CLICK_AIR) {
            val team = player.toMC().teamColor

            val targets = player.world.getNearbyPlayers(player.location, 5.0).filter { target -> target.toMC().teamColor != team && targetCooldown.none { it.player == target } && !BerserkerClass.isUsingAbility(target) }
            if (targets.isEmpty()) return

            var userEffectTick: Int = -1
            targets.forEach { target ->
                val effectTime = getEffectTime(target)
                userEffectTick = max(effectTime, userEffectTick)

                applyImmobilize(target, player, effectTime)
            }

            applyImmobilize(player, player, userEffectTick)

            player.setCooldown(IMMOBILIZE_COOLDOWN_GROUP, IMMOBILIZE_COOLDOWN)
        }

        if (event.action == Action.LEFT_CLICK_BLOCK || event.action == Action.LEFT_CLICK_AIR) {
            val team = player.toMC().teamColor

            val targets = player.world.getNearbyPlayers(player.location, 5.0).filter { it.toMC().teamColor != team && !BerserkerClass.isUsingAbility(it) }
            if (targets.isEmpty()) return

            targets.forEach { target ->
                target.addPotionEffect(PotionEffect(PotionEffectType.SLOWNESS, 99, 2))
                target.playSound(target, Sound.BLOCK_SLIME_BLOCK_BREAK, 2f, 2f)
                target.playSound(player, Sound.BLOCK_SLIME_BLOCK_BREAK, 2f, 2f)
                target.setCooldown(IMMOBILIZE_COOLDOWN_GROUP, IMMOBILIZE_COOLDOWN)
            }
        }
    }

    @EventHandler
    fun onTick(event: ServerTickStartEvent) {
        if (!targetCooldown.isEmpty()) {
            targetCooldown.removeIf { cooldown ->
                cooldown.time--
                return@removeIf cooldown.time < 1
            }
        }

        if (!immobilizes.isEmpty()) {
            immobilizes.removeIf { immobilize ->
                immobilize.tick--

                val user = immobilize.user
                val target = immobilize.target
                if (user.world != target.world) return@removeIf true

                val distance = user.location.distance(target.location)
                val location = user.location.clone().add(0.0, 1.25, 0.0)
                val delta = target.location.clone().subtract(user.location).toVector().normalize().multiply(0.1)

                val receivers = location.getNearbyPlayers(32.0)

                repeat((distance * 10).toInt()) {
                    location.add(delta)

                    Particle.CRIT.builder()
                        .location(location)
                        .count(0)
                        .offset(0.0, 0.0, 0.0)
                        .receivers(receivers)
                        .spawn()
                }

                if (immobilize.tick < 1) {
                    target.getAttribute(Attribute.JUMP_STRENGTH)?.removeModifier(IMMOBILIZE_JUMP_REDUCE_KEY)
                    return@removeIf true
                } else {
                    return@removeIf false
                }
            }
        }
    }

    private fun applyImmobilize(target: Player, user: Player, tick: Int) {
        target.addPotionEffect(PotionEffect(PotionEffectType.SLOWNESS, tick, 10))
        target.addPotionEffect(PotionEffect(PotionEffectType.MINING_FATIGUE, tick, 1))
        target.addPotionEffect(PotionEffect(PotionEffectType.ABSORPTION, tick, 1))
        target.addPotionEffect(PotionEffect(PotionEffectType.WEAKNESS, tick, 10))

        val jumpStrength = target.getAttribute(Attribute.JUMP_STRENGTH)
        if (jumpStrength?.getModifier(IMMOBILIZE_JUMP_REDUCE_KEY) == null) {
            jumpStrength?.addTransientModifier(AttributeModifier(IMMOBILIZE_JUMP_REDUCE_KEY, -10.0, AttributeModifier.Operation.ADD_NUMBER))
        }

        FallDamageResistance.add(target, tick)

        target.playSound(target, Sound.ENTITY_PLAYER_BIG_FALL, 2f, 0f)
        targetCooldown.add(TargetCooldown(target, IMMOBILIZE_RECEIVE_COOLDOWN))
        immobilizes.add(Immobilize(user, target, tick))
    }

    private fun getEffectTime(player: Player): Int {
        return min(max(((player.getAttribute(Attribute.ARMOR)?.value ?: 0.0) * 5).toInt(), 40), 100)
    }
}