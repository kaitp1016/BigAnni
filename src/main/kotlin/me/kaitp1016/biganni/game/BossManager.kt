package me.kaitp1016.biganni.game

import me.kaitp1016.biganni.mc
import me.kaitp1016.biganni.packetgui.ChestPacketGui
import me.kaitp1016.biganni.utils.ItemUtils.consumeItem
import me.kaitp1016.biganni.utils.ItemUtils.getAnniId
import me.kaitp1016.biganni.utils.ItemUtils.setAnniItem
import me.kaitp1016.biganni.utils.ItemUtils.soulbound
import me.kaitp1016.biganni.utils.MCUtils.toMC
import net.kyori.adventure.key.Key
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
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Items
import net.minecraft.world.item.alchemy.PotionContents
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.Explosion
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.Vec3
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.enchantments.Enchantment
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDeathEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.ItemStack
import java.util.*

object BossManager: Listener {
    const val BOSS_BUFF_ITEM_ID = "boss_buff"
    val BOSS_LOCATION = Location(Bukkit.getWorld(Key.key("sys", "coastal")), 10000.0, 0.0, 0.0)

    var boss: UUID? = null

    fun spawn() {
        val world = BOSS_LOCATION.world
        val level = world.toMC()

        val wither = Boss(level).apply {
            this.setPos(BOSS_LOCATION.x,BOSS_LOCATION.y,BOSS_LOCATION.z)
        }

        level.addFreshEntity(wither)

        boss = wither.uuid

        Bukkit.broadcast(Component.text("ボスがスポーンしました!"))
    }

    @EventHandler
    fun onDeath(event: EntityDeathEvent) {
        val entity = event.entity
        if (event.entity.uniqueId != boss) return

        event.drops.clear()

        val level = entity.world.toMC()
        level.addFreshEntity(PickupableBossBuff(level,entity.x,entity.y,entity.z))

        val killer = event.damageSource.causingEntity as? org.bukkit.entity.Player
        killer?.giveExp(1385,true)
    }

    @EventHandler
    fun onInteract(event: PlayerInteractEvent) {
        val item = event.item
        if (item?.getAnniId() != BOSS_BUFF_ITEM_ID) return

        val player = event.player.toMC()
        BossBuffGui(player).open()
    }

    class Boss: WitherBoss {
        constructor(level: ServerLevel):super(EntityType.WITHER,level) {

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

    class PickupableBossBuff: ItemEntity {
        val serverLevel: ServerLevel

        constructor(level: ServerLevel,x: Double,y: Double,z: Double):super(level,x,y,z, net.minecraft.world.item.ItemStack(Items.NETHER_STAR)) {
            this.serverLevel = level
            this.pickupDelay = 10
        }


        override fun playerTouch(player: Player) {
            if (this.pickupDelay > 0) return

            kill(serverLevel)

            val team = player.teamColor
            val message = player.name.copy().append(net.minecraft.network.chat.Component.literal(" とそのチームメイトがボスバフを獲得した!").withColor(CommonColors.YELLOW))

            mc.playerList.players.forEach {
                if (it.teamColor == team) {
                    it.bukkitEntity.give(createBossBuffItem())
                }

                it.sendSystemMessage(message)
            }
        }
    }

    fun createBossBuffItem(): ItemStack {
        return ItemStack(Material.NETHER_STAR).apply {
            setAnniItem(BOSS_BUFF_ITEM_ID)

            editMeta {
                it.itemName(Component.text("Boss Buff").color(NamedTextColor.GOLD))
            }
        }
    }

    class BossBuffGui: ChestPacketGui {
        override val name = "Boss Buff"
        override val displayName = net.minecraft.network.chat.Component.literal("Boss Buff")

        constructor(player: ServerPlayer):super(player,54) {
            items.fill(net.minecraft.world.item.ItemStack(Items.GRAY_STAINED_GLASS_PANE).apply {
                this.set(DataComponents.ITEM_NAME, net.minecraft.network.chat.Component.empty())
            })

            setItem(10, net.minecraft.world.item.ItemStack(Items.CHAINMAIL_HELMET))
            setItem(19, net.minecraft.world.item.ItemStack(Items.CHAINMAIL_CHESTPLATE))
            setItem(28, net.minecraft.world.item.ItemStack(Items.CHAINMAIL_LEGGINGS))
            setItem(37, net.minecraft.world.item.ItemStack(Items.CHAINMAIL_BOOTS))

            setItem(12, net.minecraft.world.item.ItemStack(Items.GOLDEN_PICKAXE).bukkitStack.apply {
                addUnsafeEnchantment(Enchantment.UNBREAKING,10)
                addUnsafeEnchantment(Enchantment.EFFICIENCY,3)
            }.toMC()!!)

            setItem(13, net.minecraft.world.item.ItemStack(Items.GOLDEN_PICKAXE).bukkitStack.apply {
                addUnsafeEnchantment(Enchantment.UNBREAKING,10)
                addUnsafeEnchantment(Enchantment.FORTUNE,3)
            }.toMC()!!)

            setItem(21, net.minecraft.world.item.ItemStack(Items.BOW).bukkitStack.apply {
                addUnsafeEnchantment(Enchantment.POWER,3)
                addUnsafeEnchantment(Enchantment.FLAME,1)
            }.toMC()!!)

            setItem(22, net.minecraft.world.item.ItemStack(Items.BOW).bukkitStack.apply {
                addUnsafeEnchantment(Enchantment.INFINITY,1)
            }.toMC()!!)

            setItem(30, net.minecraft.world.item.ItemStack(Items.DIAMOND_SWORD).bukkitStack.apply {
                addUnsafeEnchantment(Enchantment.UNBREAKING,1)
            }.toMC()!!)

            setItem(31, net.minecraft.world.item.ItemStack(Items.DIAMOND_SWORD).bukkitStack.apply {
                addUnsafeEnchantment(Enchantment.UNBREAKING,10)
                addUnsafeEnchantment(Enchantment.FIRE_ASPECT,3)
            }.toMC()!!)

            setItem(39, net.minecraft.world.item.ItemStack(Items.GOLDEN_SWORD).bukkitStack.apply {
                addUnsafeEnchantment(Enchantment.LOOTING,1)
            }.toMC()!!)

            setItem(16, net.minecraft.world.item.ItemStack(Items.SPLASH_POTION).apply {
                this.set(DataComponents.POTION_CONTENTS, PotionContents(Optional.empty(),Optional.empty(),listOf(MobEffectInstance(MobEffects.STRENGTH,3600,0)),Optional.empty()))
                this.set(DataComponents.CUSTOM_NAME, net.minecraft.network.chat.Component.literal("§9Splash Potion of §6Strength"))
            })

            setItem(17, net.minecraft.world.item.ItemStack(Items.SPLASH_POTION).apply {
                this.set(DataComponents.POTION_CONTENTS, PotionContents(Optional.empty(),Optional.empty(),listOf(MobEffectInstance(MobEffects.REGENERATION,900,0)),Optional.empty()))
                this.set(DataComponents.CUSTOM_NAME, net.minecraft.network.chat.Component.literal("§9Splash Potion of §6Regeneration"))
            })

            setItem(25, net.minecraft.world.item.ItemStack(Items.SPLASH_POTION).apply {
                this.set(DataComponents.POTION_CONTENTS, PotionContents(Optional.empty(),Optional.empty(),listOf(MobEffectInstance(MobEffects.INVISIBILITY,9600,0)),Optional.empty()))
                this.set(DataComponents.CUSTOM_NAME, net.minecraft.network.chat.Component.literal("§9Splash Potion of §6Invisibility"))
            })

            setItem(26, net.minecraft.world.item.ItemStack(Items.POTION).apply {
                this.set(DataComponents.POTION_CONTENTS, PotionContents(Optional.empty(),Optional.empty(),listOf(MobEffectInstance(MobEffects.HASTE,600,1)),Optional.empty()))
                this.set(DataComponents.CUSTOM_NAME, net.minecraft.network.chat.Component.literal("§9Potion of §6Haste"))
            })

            setItem(43, net.minecraft.world.item.ItemStack(Items.SPLASH_POTION).apply {
                this.set(DataComponents.POTION_CONTENTS, PotionContents(Optional.empty(),Optional.empty(),listOf(MobEffectInstance(MobEffects.POISON,900,0)),Optional.empty()))
                this.set(DataComponents.CUSTOM_NAME, net.minecraft.network.chat.Component.literal("§9Splash Potion of §6Poison"))
            })
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
                player.give(item.bukkitStack.soulbound())
            }
        }
    }
}