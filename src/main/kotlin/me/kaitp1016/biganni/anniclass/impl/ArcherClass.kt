package me.kaitp1016.biganni.anniclass.impl

import me.kaitp1016.biganni.PLUGIN_ID
import me.kaitp1016.biganni.anniclass.AnniClass
import me.kaitp1016.biganni.utils.MCUtils.toMC
import me.kaitp1016.biganni.utils.Scheduler
import net.kyori.adventure.key.Key
import net.minecraft.core.component.DataComponents
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.effect.MobEffects
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.projectile.Projectile
import net.minecraft.world.entity.projectile.arrow.Arrow
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownSplashPotion
import net.minecraft.world.item.Items
import net.minecraft.world.item.alchemy.PotionContents
import net.minecraft.world.phys.HitResult
import net.minecraft.world.phys.Vec3
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.damage.DamageType
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.EntityShootBowEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.PotionMeta
import org.bukkit.potion.PotionType
import java.util.*
import kotlin.random.Random

object ArcherClass: AnniClass(), Listener {
    override val name = "Archer"
    override val shortName = "ARC"
    override val icon = Items.BOW
    override val description = arrayOf(
        "最初から弓と矢を持ち、矢が与えるダメージが常に増える。",
        "左クリックをするとアビリティを選択でき、それぞれのアビリティにはクールダウンがある。",
    )

    override fun getDefaultItems(player: Player): MutableList<ItemStack> {
        return super.getDefaultItems(player).also {
            it.add(ItemStack(Material.WOODEN_SHOVEL).uniqueClassItem().soulbound())

            it.add(ItemStack(Material.BOW).apply {
                uniqueClassItem()
                soulbound()

                addEnchantment(Enchantment.PUNCH, 1)
                addEnchantment(Enchantment.INFINITY, 1)
            })

            it.add(ItemStack(Material.ARROW).uniqueClassItem().soulbound())

            it.add(ItemStack(Material.POTION).apply {
                uniqueClassItem()
                soulbound()

                editMeta {
                    (it as PotionMeta).basePotionType = PotionType.HEALING
                }
            })
        }
    }

    override fun onSelect(player: Player) {
        selectedAbility[player] = AbilityType.entries.first()

        super.onSelect(player)
    }

    override fun onUnselect(player: Player) {
        selectedAbility.remove(player)

        super.onUnselect(player)
    }

    enum class AbilityType(val displayName: String, val cooldown: Key?, val cooldownTick: Int) {
        NONE("None", null, -1),
        RAIN_OF_ARROW("Rain of Arrow", RAIN_OF_ARROW_GROUP, 600),
        POISON_SHOT("Poison Shot", POISON_SHOT_GROUP, 1000);

        fun next(): AbilityType {
            val entries = entries
            val index = entries.indexOf(this)
            return entries.getOrNull(index + 1) ?: entries.first()
        }
    }

    val RAIN_OF_ARROW_GROUP = Key.key(PLUGIN_ID, "archer_rain_of_arrow")
    val POISON_SHOT_GROUP = Key.key(PLUGIN_ID, "archer_poison_shot")

    val selectedAbility = mutableMapOf<Player, AbilityType>()

    @EventHandler
    fun onInteract(event: PlayerInteractEvent) {
        val player = event.player
        if (!isSelected(player)) return

        val item = event.item ?: return
        if (item.type == Material.BOW && (event.action == Action.LEFT_CLICK_AIR || event.action == Action.LEFT_CLICK_BLOCK)) {
            val ability = selectedAbility.getOrPut(player) { AbilityType.entries.first() }.next()
            selectedAbility[player] = ability

            player.sendMessage("${ability.displayName} を選択しました!")
            player.playSound(player, Sound.UI_BUTTON_CLICK, 1f, 1f)

            return
        }
    }

    @EventHandler
    fun onShootBow(event: EntityShootBowEvent) {
        val player = event.entity as? Player ?: return
        if (!isSelected(player)) return

        val ability = selectedAbility[player] ?: AbilityType.NONE
        val cooldown = ability.cooldown ?: return
        if (player.getCooldown(cooldown) > 0) return

        event.projectile.toMC().setRemoved(Entity.RemovalReason.DISCARDED)

        val mcPlayer = player.toMC()
        val level = mcPlayer.level()
        val weapon = event.bow?.toMC() ?: net.minecraft.world.item.ItemStack(Items.BOW)
        val power = event.force * 3f

        val arrow = Projectile.spawnProjectileFromRotationDelayed({ level, shooter: LivingEntity, weapon: net.minecraft.world.item.ItemStack -> ArcherArrow(level, mcPlayer, event.consumable?.toMC() ?: net.minecraft.world.item.ItemStack(Items.ARROW), weapon, ability) }, level, weapon, mcPlayer, 1f, power, 1f)
        if (arrow.attemptSpawn()) {
            player.setCooldown(cooldown, ability.cooldownTick)
        }
    }

    @EventHandler
    fun onDamage(event: EntityDamageEvent) {
        val source = event.damageSource
        if (source.damageType != DamageType.ARROW) return

        val damager = source.causingEntity as? Player ?: return
        if (!isSelected(damager)) return

        event.damage += 1
    }

    class ArcherArrow : Arrow {
        var usedAbility = false
        val ability: AbilityType
        val player: ServerPlayer

        constructor(level: ServerLevel, owner: ServerPlayer, arrow: net.minecraft.world.item.ItemStack, weapon: net.minecraft.world.item.ItemStack, ability: AbilityType) : super(level, owner, arrow, weapon) {
            this.player = owner
            this.ability = ability
        }

        override fun onHit(hitResult: HitResult) {
            if (!usedAbility) {
                if (ability == AbilityType.RAIN_OF_ARROW) {
                    repeat(20) {
                        Scheduler.scheduleTask(it * 5) {
                            repeat(5) {
                                val x = this.x + Random.nextDouble(-0.8, 0.8)
                                val y = this.y + Random.nextDouble(-0.8, 0.8) + 5.0
                                val z = this.z + Random.nextDouble(-0.8, 0.8)

                                val arrow = Arrow(level(), x, y, z, net.minecraft.world.item.ItemStack(Items.ARROW), null).apply {
                                    pickup = Pickup.CREATIVE_ONLY
                                    lerpMotion(Vec3(Random.nextDouble(-0.05, 0.05), 0.5, Random.nextDouble(-0.05, 0.05)))
                                    setOwner(player, false)
                                }

                                level().addFreshEntity(arrow)
                            }
                        }
                    }
                }

                if (ability == AbilityType.POISON_SHOT) {
                    val potion = ThrownSplashPotion(level(), player, net.minecraft.world.item.ItemStack(Items.SPLASH_POTION).apply {
                        set(DataComponents.POTION_CONTENTS, PotionContents(Optional.empty(), Optional.empty(), listOf(MobEffectInstance(MobEffects.POISON, 200, 0)), Optional.empty()))
                        lerpMotion(Vec3(0.0, 0.3, 0.0))
                    })

                    level().addFreshEntity(potion)
                    potion.setPos(position().add(0.0, 0.5, 0.0))
                }

                usedAbility = true
            }

            super.onHit(hitResult)
        }

        override fun shouldBeSaved(): Boolean {
            return false
        }
    }
}