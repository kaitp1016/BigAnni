package me.kaitp1016.biganni.anniclass.impl

import me.kaitp1016.biganni.anniclass.AnniClass
import me.kaitp1016.biganni.utils.ItemUtils.getAnniId
import me.kaitp1016.biganni.utils.ItemUtils.isAnniItem
import me.kaitp1016.biganni.utils.ItemUtils.removeAnniItem
import me.kaitp1016.biganni.utils.ItemUtils.setAnniItem
import me.kaitp1016.biganni.utils.MCUtils.toMC
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.projectile.Projectile
import net.minecraft.world.entity.projectile.arrow.Arrow
import net.minecraft.world.item.Items
import org.bukkit.Material
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.entity.EntityRemoveEvent
import org.bukkit.event.entity.EntityShootBowEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import kotlin.random.Random

object SniperClass: AnniClass(), Listener {
    override val name = "Sniper"
    override val shortName = "SNI"
    override val icon = Items.ARROW
    override val description = arrayOf(
        "他の能力がない弓を左クリックすると特殊な弓になる。",
        "その弓で発射された矢は高速で発射され、重力の影響を受けない。",
    )

    override fun getDefaultItems(player: Player): MutableList<ItemStack> {
        return super.getDefaultItems(player).also {
            it.add(ItemStack(Material.BOW).apply {
                uniqueClassItem()
                soulbound()
            })

            it.add(ItemStack(Material.ARROW).uniqueClassItem().soulbound().apply {
                amount = 32
            })

            it.add(ItemStack(Material.WOODEN_SHOVEL).uniqueClassItem().soulbound())
        }
    }

    override fun onUnselect(player: Player) {
        player.inventory.forEach { item ->
            if (item?.getAnniId() == COMPOUND_BOW_ID) {
                item.removeAnniItem()

                item.editMeta {
                    it.itemName(null)
                }
            }
        }

        super.onUnselect(player)
    }

    const val COMPOUND_BOW_ID = "sniper_compound_bow"

    @EventHandler
    fun onInteract(event: PlayerInteractEvent) {
        val player = event.player
        if (!isSelected(player)) return

        val item = event.item ?: return
        if (item.type == Material.BOW && (event.action == Action.LEFT_CLICK_AIR || event.action == Action.LEFT_CLICK_BLOCK)) {
            if (item.isAnniItem()) {
                if (item.getAnniId() == COMPOUND_BOW_ID) {
                    item.removeAnniItem()
                    item.editMeta {
                        it.itemName(null)
                    }
                    player.playSound(player, Sound.UI_BUTTON_CLICK, 1f, 1f)
                }
                return
            }

            item.setAnniItem(COMPOUND_BOW_ID)

            item.editMeta {
                it.itemName(Component.text("Compound Bow").color(NamedTextColor.GREEN))
            }

            player.playSound(player, Sound.UI_BUTTON_CLICK, 1f, 1f)
        }
    }

    @EventHandler
    fun onShootBow(event: EntityShootBowEvent) {
        val player = event.entity as? Player ?: return
        if (!isSelected(player) || event.bow?.getAnniId() != COMPOUND_BOW_ID) return

        if (event.force < 1f) {
            event.isCancelled = true
            player.playSound(player, Sound.ITEM_BUNDLE_INSERT_FAIL, 1f, 1f)
            return
        }

        event.projectile.toMC().setRemoved(Entity.RemovalReason.DISCARDED)

        val mcPlayer = player.toMC()
        val level = mcPlayer.level()
        val weapon = event.bow?.toMC() ?: net.minecraft.world.item.ItemStack(Items.BOW)
        val power = event.force * 5f

        val arrow = Projectile.spawnProjectileFromRotationDelayed({ level, shooter: LivingEntity, weapon: net.minecraft.world.item.ItemStack -> SniperArrow(level, mcPlayer, event.consumable?.toMC() ?: net.minecraft.world.item.ItemStack(Items.ARROW), weapon) }, level, weapon, mcPlayer, 0f, power, 1f)
        if (!arrow.attemptSpawn()) return

        player.setRotation(player.yaw + Random.nextFloat() * 10 - 5, player.pitch + Random.nextFloat() * 10 - 5)

        val hand = if (event.hand == EquipmentSlot.HAND) net.minecraft.world.entity.EquipmentSlot.MAINHAND else net.minecraft.world.entity.EquipmentSlot.OFFHAND
        event.bow?.toMC()?.hurtAndBreak(10, mcPlayer, hand)
    }

    class SniperArrow : Arrow {
        val player: ServerPlayer

        constructor(level: ServerLevel, owner: ServerPlayer, arrow: net.minecraft.world.item.ItemStack, weapon: net.minecraft.world.item.ItemStack) : super(level, owner, arrow, weapon) {
            this.player = owner
            this.isNoGravity = true
        }

        override fun tick() {
            super.tick()

            Particle.FIREWORK.builder()
                .location(bukkitEntity.world, x, y, z)
                .count(0)
                .offset(0.0, 0.0, 0.0)
                .receivers(32, true)
                .spawn()

            if (tickCount > 200) {
                this.discard(EntityRemoveEvent.Cause.DESPAWN)
            }
        }

        override fun shouldBeSaved(): Boolean {
            return false
        }
    }
}