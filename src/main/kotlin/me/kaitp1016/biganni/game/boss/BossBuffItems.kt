package me.kaitp1016.biganni.game.boss

import com.destroystokyo.paper.event.server.ServerTickStartEvent
import io.papermc.paper.datacomponent.DataComponentTypes
import io.papermc.paper.datacomponent.item.UseCooldown
import me.kaitp1016.biganni.PLUGIN_ID
import me.kaitp1016.biganni.mc
import me.kaitp1016.biganni.plugin
import me.kaitp1016.biganni.utils.ItemUtils.addLore
import me.kaitp1016.biganni.utils.ItemUtils.getAnniId
import me.kaitp1016.biganni.utils.ItemUtils.setAnniItem
import me.kaitp1016.biganni.utils.ItemUtils.soulbound
import me.kaitp1016.biganni.utils.MCUtils.toMC
import me.kaitp1016.biganni.utils.Scheduler
import net.kyori.adventure.key.Key
import net.minecraft.core.component.DataComponents
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.Style
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.effect.MobEffects
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.projectile.Projectile
import net.minecraft.world.entity.projectile.arrow.Arrow
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.item.enchantment.Enchantments
import net.minecraft.world.phys.EntityHitResult
import org.bukkit.Bukkit
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.EntityShootBowEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType

object BossBuffItems: Listener {
    abstract class BossBuffItem: Listener {
        val id: String

        constructor(id: String) {
            this.id = id
        }

        abstract fun create():ItemStack
    }

    object HelmetOfExtingushment: BossBuffItem("HELMET_OF_EXTINGUSHMENT") {
        override fun create(): ItemStack {
            return ItemStack(Items.CHAINMAIL_HELMET).apply {
                enchant(mc.registryAccess().get(Enchantments.PROTECTION).get(),4)
                enchant(mc.registryAccess().get(Enchantments.UNBREAKING).get(),3)
                enchant(mc.registryAccess().get(Enchantments.MENDING).get(),3)

                set(DataComponents.ITEM_NAME, Component.literal("§6Helmet of Extingushment"))
            }.bukkitStack.apply {
                addLore(Component.literal("§7消火").withStyle(Style.EMPTY.withItalic(false)))
                soulbound()
                setAnniItem(id)
            }.toMC()!!
        }

        @EventHandler
        fun onTick(event: ServerTickStartEvent) {
            Bukkit.getOnlinePlayers().forEach {
                if (it.fireTicks > 0 && it.inventory.getItem(EquipmentSlot.HEAD).getAnniId() == id) {
                    it.fireTicks = 0
                }
            }
        }
    }

    object ChestplateOfExtingushment: BossBuffItem("CHESTPLATE_OF_EXTINGUSHMENT") {
        override fun create(): ItemStack {
            return ItemStack(Items.CHAINMAIL_CHESTPLATE).apply {
                enchant(mc.registryAccess().get(Enchantments.PROTECTION).get(),4)
                enchant(mc.registryAccess().get(Enchantments.UNBREAKING).get(),3)
                enchant(mc.registryAccess().get(Enchantments.MENDING).get(),3)

                set(DataComponents.ITEM_NAME, Component.literal("§6Chestplate of Extingushment"))
            }.bukkitStack.apply {
                addLore(Component.literal("§7再生 I (ダメージを与えることで発動)").withStyle(Style.EMPTY.withItalic(false)))
                soulbound()
                setAnniItem(id)
            }.toMC()!!
        }

        @EventHandler
        fun onHit(event: EntityDamageEvent) {
            val source = event.damageSource
            val causingEntity = source.causingEntity
            if (causingEntity !is Player || causingEntity.inventory.getItem(EquipmentSlot.CHEST).getAnniId() != id) return

            causingEntity.addPotionEffect(PotionEffect(PotionEffectType.REGENERATION, 100, 0))
        }
    }

    object LeggingsOfExtingushment: BossBuffItem("LEGGINGS_OF_EXTINGUSHMENT") {
        override fun create(): ItemStack {
            return ItemStack(Items.CHAINMAIL_LEGGINGS).apply {
                enchant(mc.registryAccess().get(Enchantments.PROTECTION).get(),4)
                enchant(mc.registryAccess().get(Enchantments.UNBREAKING).get(),3)
                enchant(mc.registryAccess().get(Enchantments.MENDING).get(),3)

                set(DataComponents.ITEM_NAME, Component.literal("§6Leggings of Extingushment"))
            }.bukkitStack.apply {
                addLore(Component.literal("§7俊敏 I (ダメージを与えることで発動)").withStyle(Style.EMPTY.withItalic(false)))
                soulbound()
                setAnniItem(id)
            }.toMC()!!
        }

        @EventHandler
        fun onHit(event: EntityDamageEvent) {
            val source = event.damageSource
            val causingEntity = source.causingEntity
            if (causingEntity !is Player || causingEntity.inventory.getItem(EquipmentSlot.LEGS).getAnniId() != id) return

            causingEntity.addPotionEffect(PotionEffect(PotionEffectType.SPEED, 100, 0))
        }
    }

    object BootsOfExtingushment: BossBuffItem("BOOTS_OF_EXTINGUSHMENT") {
        override fun create(): ItemStack {
            return ItemStack(Items.CHAINMAIL_BOOTS).apply {
                enchant(mc.registryAccess().get(Enchantments.PROTECTION).get(),4)
                enchant(mc.registryAccess().get(Enchantments.UNBREAKING).get(),3)
                enchant(mc.registryAccess().get(Enchantments.MENDING).get(),3)

                set(DataComponents.ITEM_NAME, Component.literal("§6Boots of Extingushment"))
            }.bukkitStack.apply {
                addLore(Component.literal("§7優雅 I").withStyle(Style.EMPTY.withItalic(false)))
                soulbound()
                setAnniItem(id)
            }.toMC()!!
        }

        @EventHandler
        fun onHit(event: EntityDamageEvent) {
            val entity = event.entity
            if (entity !is Player || event.cause != EntityDamageEvent.DamageCause.FALL) return

            val item = entity.inventory.getItem(EquipmentSlot.FEET)
            if (item.getAnniId() != id) return

            item.toMC()!!.hurtAndBreak(event.damage.toInt() + 1,entity.toMC(), net.minecraft.world.entity.EquipmentSlot.FEET)
            event.isCancelled = true
        }
    }

    object BowOfAether: BossBuffItem("BOW_OF_AETHER") {
        const val COOLDOWN_TICK = 100
        val COOLDOWN_GROUP = Key.key(PLUGIN_ID,"bow_of_aether_cooldown")

        override fun create(): ItemStack {
            return ItemStack(Items.BOW).apply {
                enchant(mc.registryAccess().get(Enchantments.INFINITY).get(),1)

                set(DataComponents.ITEM_NAME, Component.literal("§6Bow of Aether"))
            }.bukkitStack.apply {
                addLore(Component.literal("§7浮遊 I").withStyle(Style.EMPTY.withItalic(false)))
                soulbound()
                setAnniItem(id)

                setData(DataComponentTypes.USE_COOLDOWN, UseCooldown.useCooldown(COOLDOWN_TICK / 20f).cooldownGroup(COOLDOWN_GROUP).build())
            }.toMC()!!
        }

        @EventHandler
        fun onShootBow(event: EntityShootBowEvent) {
            val player = event.entity as? Player ?: return
            val bow = event.bow
            if (bow?.getAnniId() != id) return

            event.projectile.toMC().setRemoved(Entity.RemovalReason.DISCARDED)

            if (event.force < 1f) {
                event.isCancelled = true
                player.playSound(player, Sound.ITEM_BUNDLE_INSERT_FAIL,1f,1f)


                Scheduler.scheduleTask(1) {
                    player.setCooldown(COOLDOWN_GROUP,0)
                }
                return
            }

            val mcPlayer = player.toMC()
            val level = mcPlayer.level()
            val weapon = event.bow?.toMC() ?: ItemStack(Items.BOW)
            val power = event.force * 3

            val arrow = Projectile.spawnProjectileFromRotationDelayed({ level,shooter: LivingEntity,weapon: ItemStack -> AetherArrow(level,mcPlayer,event.consumable?.toMC() ?: ItemStack(Items.ARROW),weapon) },level,weapon,mcPlayer,1f,power,1f)
            arrow.attemptSpawn()
        }

        class AetherArrow: Arrow {
            val player: ServerPlayer

            constructor(level: ServerLevel,owner: ServerPlayer,arrow: ItemStack,weapon: ItemStack):super(level,owner,arrow,weapon) {
                this.player = owner
            }

            override fun onHitEntity(hitResult: EntityHitResult) {
                val target = hitResult.entity as? LivingEntity
                target?.addEffect(MobEffectInstance(MobEffects.LEVITATION, 70, 0))

                super.onHitEntity(hitResult)
            }

            override fun shouldBeSaved(): Boolean {
                return false
            }
        }
    }

    object SwordOfVenom: BossBuffItem("SWORD_OF_VENOM") {
        override fun create(): ItemStack {
            return ItemStack(Items.DIAMOND_SWORD).apply {
                set(DataComponents.ITEM_NAME, Component.literal("§6Sword of Venom"))
            }.bukkitStack.apply {
                addLore(Component.literal("§7毒 I ").withStyle(Style.EMPTY.withItalic(false)))
                soulbound()
                setAnniItem(id)
            }.toMC()!!
        }

        @EventHandler
        fun onHit(event: EntityDamageEvent) {
            val source = event.damageSource
            val causingEntity = source.causingEntity
            if (causingEntity !is Player || causingEntity.inventory.getItem(EquipmentSlot.HAND).getAnniId() != id) return

            (event.entity as? org.bukkit.entity.LivingEntity)?.addPotionEffect(PotionEffect(PotionEffectType.POISON, 80, 0))
        }
    }

    var tick = 0

    @EventHandler
    fun onTick(event: ServerTickStartEvent) {
        tick++
        if (tick < 20) return

        tick = 0

        Bukkit.getOnlinePlayers().forEach { player ->
            val inv = player.inventory
            if (inv.getItem(EquipmentSlot.HEAD).getAnniId() == HelmetOfExtingushment.id && inv.getItem(EquipmentSlot.CHEST).getAnniId() == ChestplateOfExtingushment.id && inv.getItem(EquipmentSlot.LEGS).getAnniId() == LeggingsOfExtingushment.id && inv.getItem(EquipmentSlot.FEET).getAnniId() == BootsOfExtingushment.id) {
                player.addPotionEffect(PotionEffect(PotionEffectType.INVISIBILITY,40,0))
            }
        }
    }

    fun register() {
        arrayOf(HelmetOfExtingushment, ChestplateOfExtingushment, LeggingsOfExtingushment, BootsOfExtingushment, BowOfAether, SwordOfVenom).forEach {
            plugin.server.pluginManager.registerEvents(it,plugin)
        }
    }
}