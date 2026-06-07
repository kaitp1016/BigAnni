package me.kaitp1016.biganni.anniclass.impl

import com.destroystokyo.paper.event.server.ServerTickStartEvent
import me.kaitp1016.biganni.anniclass.AnniClass
import me.kaitp1016.biganni.utils.ItemUtils.addLore
import me.kaitp1016.biganni.utils.ItemUtils.getAnniId
import me.kaitp1016.biganni.utils.ItemUtils.setAnniItem
import me.kaitp1016.biganni.utils.MCUtils.toMC
import me.kaitp1016.biganni.utils.Utils.toIntCorrect
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.projectile.FishingHook
import net.minecraft.world.item.Items
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.FishHook
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.ProjectileHitEvent
import org.bukkit.event.player.PlayerFishEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.potion.PotionEffectType

object ScoutClass: AnniClass(), Listener {
    override val name = "Scout"
    override val shortName = "SCO"
    override val icon = Items.FISHING_ROD
    override val description = arrayOf(
        "高速で移動ができるグラップリングフックが初期装備に含まれている。", "戦闘中は使用できない。"
    )

    const val GRAPPLING_HOOK_ID = "scout_grappling_hook"

    override fun getDefaultItems(player: Player): MutableList<ItemStack> {
        return super.getDefaultItems(player).also {
            it.add(ItemStack(Material.FISHING_ROD).apply {
                addLore(Component.text("<").decoration(TextDecoration.ITALIC, false).color(NamedTextColor.GRAY).append(Component.text("Right Click").color(NamedTextColor.AQUA).append(Component.text(">").color(NamedTextColor.GRAY).append(Component.text(" Cast your grappling").color(NamedTextColor.DARK_AQUA)))))
                addLore(Component.text("hook").color(NamedTextColor.DARK_AQUA).decoration(TextDecoration.ITALIC, false))

                uniqueClassItem()
                soulbound()
                setAnniItem(GRAPPLING_HOOK_ID)

                editMeta {
                    it.isUnbreakable = true
                    it.itemName(Component.text("Grapple").color(NamedTextColor.YELLOW))
                }
            })

            it.removeIf { it.type == Material.WOODEN_SWORD }
            it.add(ItemStack(Material.GOLDEN_SWORD).uniqueClassItem().soulbound())
        }
    }

    override fun onUserTick(player: Player) {
        val mcPlayer = player.toMC()
        val fishing = mcPlayer.fishing ?: return
        val inv = player.inventory
        if (inv.itemInMainHand.getAnniId() == GRAPPLING_HOOK_ID || inv.itemInOffHand.getAnniId() == GRAPPLING_HOOK_ID) {
            fishing.deltaMovement = fishing.deltaMovement.multiply(0.95, 0.9, 0.95)
        }
    }

    data class Cooldown(val player: Player, var time: Int)

    val cooldowns = mutableListOf<Cooldown>()

    @EventHandler
    fun onFish(event: PlayerFishEvent) {
        val player = event.player
        if (!isSelected(player)) return

        val hand = event.hand ?: return
        val item = event.player.inventory.getItem(hand)
        if (item.getAnniId() != GRAPPLING_HOOK_ID) return

        val state = event.state
        if (state == PlayerFishEvent.State.IN_GROUND || state == PlayerFishEvent.State.REEL_IN) {
            if (cooldowns.any { it.player == player } || player.fireTicks > 0 || player.hasPotionEffect(PotionEffectType.SLOWNESS)) {
                event.isCancelled = true
                return
            }

            val hook = event.hook
            val world = hook.world
            if (!hook.isOnGround && world.getBlockAt(hook.x.toInt(), (hook.y + 1).toIntCorrect(), hook.z.toInt()).isPassable && world.getBlockAt(hook.x.toInt(), hook.y.toIntCorrect(), hook.z.toInt()).isPassable && world.getBlockAt(hook.x.toInt(), (hook.y - 1).toIntCorrect(), hook.z.toInt()).isPassable) {
                return
            }

            val velocity = hook.location.clone().subtract(player.location).apply {
                multiply(0.2)
            }

            player.velocity = player.velocity.add(velocity.toVector())
        }
        if (state == PlayerFishEvent.State.CAUGHT_ENTITY) {
            event.isCancelled = true
        }
        if (state == PlayerFishEvent.State.FISHING) {
            val mcPlayer = player.toMC()
            val fishing = mcPlayer.fishing ?: return
            val motion = fishing.deltaMovement.multiply(1.5, 1.5, 1.5)
            fishing.setDeltaMovement(motion.x, motion.y, motion.z)
        }
    }

    @EventHandler
    fun onTick(event: ServerTickStartEvent) {
        if (cooldowns.isEmpty()) return

        cooldowns.removeIf {
            it.time--
            return@removeIf it.time < 1
        }
    }

    @EventHandler
    fun onDamage(event: EntityDamageEvent) {
        val source = event.damageSource

        val entity = event.entity as? Player ?: return
        val causingEntity = source.causingEntity as? Player ?: return

        if (isSelected(causingEntity)) {
            addCooldown(causingEntity)
        }

        if (isSelected(entity)) {
            addCooldown(entity)
        }
    }

    @EventHandler
    fun onProjectileHit(event: ProjectileHitEvent) {
        val entity = event.entity
        if (entity !is FishHook) return

        val owner = entity.ownerUniqueId?.let { Bukkit.getEntity(it) } ?: return
        if (owner !is Player || !isSelected(owner)) return

        val inv = owner.inventory
        if (inv.itemInMainHand.getAnniId() == GRAPPLING_HOOK_ID || inv.itemInOffHand.getAnniId() == GRAPPLING_HOOK_ID) {
            event.isCancelled = true
        }
    }

    fun addCooldown(player: Player) {
        cooldowns.removeIf { it.player == player }

        cooldowns.add(Cooldown(player, 100))
    }
}