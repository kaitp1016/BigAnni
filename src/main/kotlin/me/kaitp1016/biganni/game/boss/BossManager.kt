package me.kaitp1016.biganni.game.boss

import me.kaitp1016.biganni.game.Game
import me.kaitp1016.biganni.mc
import me.kaitp1016.biganni.packetgui.ChestPacketGui
import me.kaitp1016.biganni.utils.ItemUtils.consumeItem
import me.kaitp1016.biganni.utils.ItemUtils.getAnniId
import me.kaitp1016.biganni.utils.ItemUtils.setAnniItem
import me.kaitp1016.biganni.utils.ItemUtils.soulbound
import me.kaitp1016.biganni.utils.MCUtils.toMC
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.minecraft.core.BlockPos
import net.minecraft.core.component.DataComponents
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.util.CommonColors
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.effect.MobEffects
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.MoverType
import net.minecraft.world.entity.boss.wither.WitherBoss
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.item.alchemy.PotionContents
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.Explosion
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.Vec3
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDeathEvent
import org.bukkit.event.player.PlayerInteractEvent
import java.util.Optional
import java.util.UUID

object BossManager: Listener {
    const val BOSS_BUFF_ITEM_ID = "boss_buff"

    var boss: UUID? = null

    fun spawn() {
        val bossLocation = Game.map.bossLocation
        val world = bossLocation.world
        val level = world.toMC()

        val wither = Boss(level).apply {
            this.setPos(bossLocation.x, bossLocation.y, bossLocation.z)
        }

        level.addFreshEntity(wither)

        boss = wither.uuid

        Bukkit.getOnlinePlayers().forEach {
            it.playSound(it, Sound.ENTITY_WITHER_SPAWN, 1f, 1f)
        }
    }

    @EventHandler
    fun onDeath(event: EntityDeathEvent) {
        val entity = event.entity
        if (event.entity.uniqueId != boss) return

        event.drops.clear()

        val level = entity.world.toMC()
        level.addFreshEntity(PickupableBossBuff(level, entity.x, entity.y, entity.z))

        val killer = event.damageSource.causingEntity as? Player
        killer?.giveExp(1385, true)
    }

    @EventHandler
    fun onInteract(event: PlayerInteractEvent) {
        val item = event.item
        if (item?.getAnniId() != BOSS_BUFF_ITEM_ID) return

        val player = event.player.toMC()
        BossBuffGui(player).open()
    }

    class Boss : WitherBoss {
        constructor(level: ServerLevel) : super(EntityType.WITHER, level) {

        }

        override fun move(moverType: MoverType, delta: Vec3) {
        }

        override fun shouldBlockExplode(explosion: Explosion, level: BlockGetter, pos: BlockPos, state: BlockState, power: Float): Boolean {
            return false
        }

        override fun shouldBeSaved(): Boolean {
            return false
        }
    }

    class PickupableBossBuff : ItemEntity {
        val serverLevel: ServerLevel

        constructor(level: ServerLevel, x: Double, y: Double, z: Double) : super(level, x, y, z, ItemStack(Items.NETHER_STAR)) {
            this.serverLevel = level
            this.pickupDelay = 10
        }


        override fun playerTouch(player: net.minecraft.world.entity.player.Player) {
            if (this.pickupDelay > 0) return

            kill(serverLevel)

            val team = player.teamColor
            val message = player.displayName.copy().append(net.minecraft.network.chat.Component.literal(" とそのチームメイトがボスバフを獲得した!").withColor(CommonColors.YELLOW))

            mc.playerList.players.forEach {
                if (it.teamColor == team) {
                    it.bukkitEntity.give(createBossBuffItem())
                }

                it.sendSystemMessage(message)
            }
        }
    }

    fun createBossBuffItem(): org.bukkit.inventory.ItemStack {
        return org.bukkit.inventory.ItemStack(Material.NETHER_STAR).apply {
            setAnniItem(BOSS_BUFF_ITEM_ID)

            editMeta {
                it.itemName(Component.text("Boss Buff").color(NamedTextColor.GOLD))
            }
        }
    }

    class BossBuffGui : ChestPacketGui {
        override val name = "Boss Buff"
        override val displayName = net.minecraft.network.chat.Component.literal("Boss Buff")

        constructor(player: ServerPlayer) : super(player, 54) {
            items.fill(ItemStack(Items.GRAY_STAINED_GLASS_PANE).apply {
                this.set(DataComponents.ITEM_NAME, net.minecraft.network.chat.Component.empty())
            })

            setItem(10, BossBuffItems.HelmetOfExtingushment.create())
            setItem(19, BossBuffItems.ChestplateOfExtingushment.create())
            setItem(28, BossBuffItems.LeggingsOfExtingushment.create())
            setItem(37, BossBuffItems.BootsOfExtingushment.create())

            setItem(12, ItemStack(Items.GOLDEN_PICKAXE).bukkitStack.apply {
                addUnsafeEnchantment(Enchantment.UNBREAKING, 10)
                addUnsafeEnchantment(Enchantment.EFFICIENCY, 3)
            }.toMC()!!)

            setItem(13, ItemStack(Items.GOLDEN_PICKAXE).bukkitStack.apply {
                addUnsafeEnchantment(Enchantment.UNBREAKING, 10)
                addUnsafeEnchantment(Enchantment.FORTUNE, 3)
            }.toMC()!!)

            setItem(21, ItemStack(Items.BOW).bukkitStack.apply {
                addUnsafeEnchantment(Enchantment.POWER, 3)
                addUnsafeEnchantment(Enchantment.FLAME, 1)
            }.toMC()!!)

            setItem(22, BossBuffItems.BowOfAether.create())

            setItem(30, BossBuffItems.SwordOfVenom.create())

            setItem(31, ItemStack(Items.DIAMOND_SWORD).bukkitStack.apply {
                addUnsafeEnchantment(Enchantment.UNBREAKING, 10)
                addUnsafeEnchantment(Enchantment.FIRE_ASPECT, 3)
            }.toMC()!!)

            setItem(39, ItemStack(Items.GOLDEN_SWORD).bukkitStack.apply {
                addUnsafeEnchantment(Enchantment.LOOTING, 1)
            }.toMC()!!)

            setItem(16, ItemStack(Items.SPLASH_POTION).apply {
                this.set(DataComponents.POTION_CONTENTS, PotionContents(Optional.empty(), Optional.empty(), listOf(MobEffectInstance(MobEffects.STRENGTH, 3600, 0)), Optional.empty()))
                this.set(DataComponents.CUSTOM_NAME, net.minecraft.network.chat.Component.literal("§9Splash Potion of §6Strength"))
            })

            setItem(17, ItemStack(Items.SPLASH_POTION).apply {
                this.set(DataComponents.POTION_CONTENTS, PotionContents(Optional.empty(), Optional.empty(), listOf(MobEffectInstance(MobEffects.REGENERATION, 900, 0)), Optional.empty()))
                this.set(DataComponents.CUSTOM_NAME, net.minecraft.network.chat.Component.literal("§9Splash Potion of §6Regeneration"))
            })

            setItem(25, ItemStack(Items.SPLASH_POTION).apply {
                this.set(DataComponents.POTION_CONTENTS, PotionContents(Optional.empty(), Optional.empty(), listOf(MobEffectInstance(MobEffects.INVISIBILITY, 9600, 0)), Optional.empty()))
                this.set(DataComponents.CUSTOM_NAME, net.minecraft.network.chat.Component.literal("§9Splash Potion of §6Invisibility"))
            })

            setItem(26, ItemStack(Items.POTION).apply {
                this.set(DataComponents.POTION_CONTENTS, PotionContents(Optional.empty(), Optional.empty(), listOf(MobEffectInstance(MobEffects.HASTE, 600, 1)), Optional.empty()))
                this.set(DataComponents.CUSTOM_NAME, net.minecraft.network.chat.Component.literal("§9Potion of §6Haste"))
            })

            setItem(43, ItemStack(Items.SPLASH_POTION).apply {
                this.set(DataComponents.POTION_CONTENTS, PotionContents(Optional.empty(), Optional.empty(), listOf(MobEffectInstance(MobEffects.POISON, 900, 0)), Optional.empty()))
                this.set(DataComponents.CUSTOM_NAME, net.minecraft.network.chat.Component.literal("§9Splash Potion of §6Poison"))
            })

            items.forEach {
                if (!it.isEmpty && it.item != Items.GRAY_STAINED_GLASS_PANE) it.bukkitStack.soulbound()
            }
        }

        override fun onClick(packet: ServerboundContainerClickPacket) {
            mc.execute {
                val index = packet.slotNum.toInt()
                val item = items.getOrNull(index)
                if (item == null || item.item == Items.GRAY_STAINED_GLASS_PANE) {
                    update(false)
                    return@execute
                }

                val player = player.bukkitEntity
                if (!player.consumeItem(BOSS_BUFF_ITEM_ID)) {
                    player.sendMessage("ボスバフが見つかりませんでした!")
                    close()
                    return@execute
                }

                close()
                player.give(item.bukkitStack)
            }
        }
    }
}