package me.kaitp1016.biganni.anniclass.impl

import io.papermc.paper.datacomponent.DataComponentTypes
import io.papermc.paper.datacomponent.item.UseCooldown
import me.kaitp1016.biganni.PLUGIN_ID
import me.kaitp1016.biganni.anniclass.AnniClass
import me.kaitp1016.biganni.mc
import me.kaitp1016.biganni.utils.ItemUtils.getAnniId
import me.kaitp1016.biganni.utils.ItemUtils.setAnniItem
import me.kaitp1016.biganni.utils.MCUtils.toMC
import me.kaitp1016.biganni.utils.Scheduler
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.util.Mth
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.damagesource.DamageTypes
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.effect.MobEffects
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.projectile.Projectile
import net.minecraft.world.entity.projectile.Projectile.ProjectileFactory
import net.minecraft.world.entity.projectile.throwableitemprojectile.Snowball
import net.minecraft.world.item.Items
import net.minecraft.world.level.Level
import net.minecraft.world.phys.EntityHitResult
import net.minecraft.world.phys.HitResult
import net.minecraft.world.phys.Vec3
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityRemoveEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import kotlin.math.PI
import kotlin.random.Random
import net.minecraft.world.item.ItemStack as MCItemStack

object NinjaClass: AnniClass(), Listener {
    override val icon = Items.FIREWORK_STAR
    override val name = "Ninja"
    override val shortName = "NNJ"
    override val description = arrayOf(
        "常に跳躍の効果を獲得する。",
        "Smoke Bombを使用すると煙幕を投擲することができる。",
        "Shurikenを使用すると手裏剣を投げることができる。",
    )

    const val MASTERFUL_ASCENSION_ITEM_ID = "ninja_masterful_ascension"
    const val MASTERFUL_ASCENSION_COOLDOWN = 100
    val MASTERFUL_ASCENSION_COOLDOWN_GROUP = Key.key(PLUGIN_ID,"ninja_masterful_ascension")

    const val SMOKE_BOMB_ITEM_ID = "ninja_smoke_bomb"
    const val SMOKE_BOMB_COOLDOWN = 800
    val SMOKE_BOMB_COOLDOWN_GROUP = Key.key(PLUGIN_ID,"ninja_smoke_bomb")

    const val SHURIKEN_ITEM_ID = "ninja_shuriken"
    const val SHURIKEN_COOLDOWN = 40
    val SHURIKEN_COOLDOWN_GROUP = Key.key(PLUGIN_ID,"ninja_shuriken")

    override fun getDefaultItems(player: Player): MutableList<ItemStack> {
        return super.getDefaultItems(player).also {
            it.removeIf { it.type == Material.WOODEN_SWORD }
            it.add(ItemStack(Material.GOLDEN_SWORD).uniqueClassItem().soulbound())

            it.add(ItemStack(Material.RABBIT_FOOT).apply {
                uniqueClassItem()
                soulbound()
                setAnniItem(MASTERFUL_ASCENSION_ITEM_ID)

                setData(DataComponentTypes.USE_COOLDOWN, UseCooldown.useCooldown(MASTERFUL_ASCENSION_COOLDOWN / 20f).cooldownGroup(MASTERFUL_ASCENSION_COOLDOWN_GROUP).build())

                editMeta {
                    it.itemName(Component.text("Masterful Ascension").color(NamedTextColor.GOLD))
                }
            })

            it.add(ItemStack(Material.PRISMARINE_CRYSTALS).apply {
                uniqueClassItem()
                soulbound()
                setAnniItem(SHURIKEN_ITEM_ID)

                setData(DataComponentTypes.USE_COOLDOWN, UseCooldown.useCooldown(SHURIKEN_COOLDOWN / 20f).cooldownGroup(SHURIKEN_COOLDOWN_GROUP).build())

                editMeta {
                    it.itemName(Component.text("Shuriken").color(NamedTextColor.GOLD))
                }

                amount = 4
            })

            it.add(ItemStack(Material.FIREWORK_STAR).apply {
                uniqueClassItem()
                soulbound()
                setAnniItem(SMOKE_BOMB_ITEM_ID)

                setData(DataComponentTypes.USE_COOLDOWN, UseCooldown.useCooldown(SMOKE_BOMB_COOLDOWN / 20f).cooldownGroup(SMOKE_BOMB_COOLDOWN_GROUP).build())

                editMeta {
                    it.itemName(Component.text("Smoke Bomb").color(NamedTextColor.GOLD))
                }
            })
        }
    }

    val enabledPlayers = mutableListOf<Player>()

    override fun onUnselect(player: Player) {
        enabledPlayers.remove(player)
        player.removePotionEffect(PotionEffectType.JUMP_BOOST)

        super.onUnselect(player)
    }

    @EventHandler
    fun onInteract(event: PlayerInteractEvent) {
        val player = event.player
        if (!isSelected(player)) return

        val item = event.item ?: return
        if (player.hasCooldown(item)) return

        if (item.getAnniId() == MASTERFUL_ASCENSION_ITEM_ID) {
            event.isCancelled = true

            if (enabledPlayers.contains(player)) {
                enabledPlayers.remove(player)
                player.removePotionEffect(PotionEffectType.JUMP_BOOST)
                player.sendMessage(Component.text("Jump Boost disabled.").color(NamedTextColor.GREEN))
            }
            else {
                enabledPlayers.add(player)
                player.addPotionEffect(PotionEffect(PotionEffectType.JUMP_BOOST, PotionEffect.INFINITE_DURATION,1,false,false))
                player.sendMessage(Component.text("Jump Boost enabled.").color(NamedTextColor.GREEN))
                player.setCooldown(MASTERFUL_ASCENSION_COOLDOWN_GROUP,100)
            }

            player.playSound(player, Sound.UI_BUTTON_CLICK,1f,1f)

        }
        if (item.getAnniId() == SHURIKEN_ITEM_ID) {
            event.isCancelled = true

            val mcPlayer = player.toMC()
            val level = mcPlayer.level()
            val mcItem = item.toMC() ?: MCItemStack(Items.BOW)
            val isRightClick = event.action.isRightClick
            val snowball = Projectile.spawnProjectileFromRotationDelayed(ProjectileFactory { level: ServerLevel, mob: LivingEntity, item: MCItemStack -> ThrowShuriken(mcPlayer, level, mob,isRightClick) }, level, mcItem, mcPlayer, -5.0f, 1.5f, 1.0f)
            if (!snowball.attemptSpawn()) return

            player.world.playSound(player.location, Sound.ENTITY_ITEM_FRAME_REMOVE_ITEM,1f,1f)
            player.setCooldown(SHURIKEN_COOLDOWN_GROUP,SHURIKEN_COOLDOWN)
            item.amount--
        }
        if (item.getAnniId() == SMOKE_BOMB_ITEM_ID) {
            event.isCancelled = true

            val mcPlayer = player.toMC()
            val level = mcPlayer.level()
            val mcItem = item.toMC() ?: MCItemStack(Items.BOW)
            val isLeftClick = event.action.isLeftClick
            val power = if (isLeftClick) 0f else 1.5f
            val snowball = Projectile.spawnProjectileFromRotationDelayed(ProjectileFactory { level: ServerLevel, mob: LivingEntity, item: MCItemStack -> SmokeBomb(mcPlayer, level, mob) }, level, mcItem, mcPlayer, -5.0f, power, 1.0f)
            if (!snowball.attemptSpawn()) return

            player.world.playSound(player.location, Sound.BLOCK_LAVA_EXTINGUISH,1f,1f)
            player.setCooldown(SMOKE_BOMB_COOLDOWN_GROUP,SMOKE_BOMB_COOLDOWN)
        }
    }

    class ThrowShuriken : Snowball {
        val thrower: ServerPlayer
        val isRightClick: Boolean

        constructor(thrower: ServerPlayer, level: Level, mob: LivingEntity,isRightClick: Boolean) : super(level, mob, MCItemStack(Items.PRISMARINE_CRYSTALS)) {
            this.thrower = thrower
            this.isRightClick = isRightClick
        }

        override fun onHitEntity(hitResult: EntityHitResult) {
            val target = hitResult.entity
            if (target !is LivingEntity || target.teamColor == thrower.teamColor) return

            val level = target.level() as ServerLevel
            val source = DamageSource(mc.registryAccess().get(DamageTypes.ARROW).get(),this,thrower)
            target.hurtServer(level,source,1f)

            if (isRightClick) {
                val velocity = this.position().subtract(target.position()).multiply(-1.0,0.0,-1.0).normalize().add(0.0,0.4,0.0)
                target.lerpMotion(velocity)
                target.hurtMarked = true
            }
            else {
                if (Random.nextBoolean()) {
                    target.addEffect(MobEffectInstance(MobEffects.SLOWNESS,60,0))
                }
                else {
                    target.addEffect(MobEffectInstance(MobEffects.POISON,40,0))
                }
            }

            level().broadcastEntityEvent(this, 3.toByte())
            discard(EntityRemoveEvent.Cause.HIT)
        }

        override fun shootFromRotation(source: Entity, xRot: Float, yRot: Float, yOffset: Float, pow: Float, uncertainty: Float) {
            val xd = -Mth.sin(yRot * (PI / 180f)) * Mth.cos(xRot * (PI / 180f))
            val yd = -Mth.sin((xRot + yOffset) * (PI / 180f))
            val zd = Mth.cos(yRot * (PI / 180f)) * Mth.cos(xRot * (PI / 180f))

            shoot(xd.toDouble(), yd.toDouble(), zd.toDouble(), pow, uncertainty)
        }

        override fun isInWater(): Boolean {
            return false
        }

        override fun shouldBeSaved(): Boolean {
            return false
        }
    }

    class SmokeBomb : Snowball {
        val thrower: ServerPlayer

        constructor(thrower: ServerPlayer, level: Level, mob: LivingEntity) : super(level, mob, MCItemStack(Items.FIREWORK_STAR)) {
            this.thrower = thrower
        }

        override fun onHit(hitResult: HitResult) {
            val pos = hitResult.location

            repeat(12) {
                Scheduler.scheduleTask(it * 5) {
                    smoke(pos)
                }
            }

            level().broadcastEntityEvent(this, 3.toByte())
            discard(EntityRemoveEvent.Cause.HIT)
        }

        private fun smoke(position: Vec3) {
            val world = level().world

            repeat(30) {
                world.spawnParticle(Particle.LARGE_SMOKE,position.x + Random.nextDouble(-4.0,4.0),position.y + Random.nextDouble(-4.0,4.0),position.z + Random.nextDouble(-4.0,4.0),0,Random.nextDouble(-0.3,0.3),Random.nextDouble(-0.3,0.3),Random.nextDouble(-0.3,0.3))
            }

            val team = thrower.teamColor
            world.getNearbyPlayers(Location(world,position.x,position.y,position.z),4.0).forEach { target ->
                if (target.toMC().teamColor == team) {
                    target.addPotionEffect(PotionEffect(PotionEffectType.SPEED,40,1))
                }
                else {
                    target.addPotionEffect(PotionEffect(PotionEffectType.BLINDNESS,40,0))
                }
            }
        }

        override fun shootFromRotation(source: Entity, xRot: Float, yRot: Float, yOffset: Float, pow: Float, uncertainty: Float) {
            val xd = -Mth.sin(yRot * (PI / 180f)) * Mth.cos(xRot * (PI / 180f))
            val yd = -Mth.sin((xRot + yOffset) * (PI / 180f))
            val zd = Mth.cos(yRot * (PI / 180f)) * Mth.cos(xRot * (PI / 180f))

            shoot(xd.toDouble(), yd.toDouble(), zd.toDouble(), pow, uncertainty)
        }

        override fun isInWater(): Boolean {
            return false
        }

        override fun shouldBeSaved(): Boolean {
            return false
        }
    }
}