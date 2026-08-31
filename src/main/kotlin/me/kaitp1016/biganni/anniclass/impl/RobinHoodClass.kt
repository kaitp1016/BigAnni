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
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.entity.animal.equine.Horse
import net.minecraft.world.item.Items
import org.bukkit.Material
import org.bukkit.craftbukkit.event.CraftEventFactory
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDeathEvent
import org.bukkit.event.entity.EntityRegainHealthEvent
import org.bukkit.event.entity.EntityRemoveEvent
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType

object RobinHoodClass: AnniClass(), Listener {
    override val icon = Items.LEATHER_HORSE_ARMOR
    override val name = "Robin Hood"
    override val shortName = "RBH"
    override val description = arrayOf(
        "Steedを使用すると馬を召喚できる。",
        "Aerial Agilityを使用すると低速落下を獲得する。",
    )

    const val STEED_ITEM_ID = "robin_hood_steed"
    const val STEED_RESPAWN_COOLDOWN = 100
    const val STEED_DEATH_COOLDOWN = 1600
    val STEED_COOLDOWN_GROUP = Key.key(PLUGIN_ID,"robin_hood_steed")

    const val AERIAL_AGILITY_ITEM_ID = "robin_hood_aerial_agility"
    const val AERIAL_AGILITY_COOLDOWN = 600
    val AERIAL_AGILITY_COOLDOWN_GROUP = Key.key(PLUGIN_ID,"robin_hood_aerial_agility")

    override fun getDefaultItems(player: Player): MutableList<ItemStack> {
        return super.getDefaultItems(player).also {
            it.add(ItemStack(Material.WOODEN_SHOVEL).uniqueClassItem().soulbound())

            it.add(ItemStack(Material.OMINOUS_TRIAL_KEY).apply {
                uniqueClassItem()
                soulbound()
                setAnniItem(STEED_ITEM_ID)

                setData(DataComponentTypes.USE_COOLDOWN, UseCooldown.useCooldown(STEED_DEATH_COOLDOWN / 20f).cooldownGroup(STEED_COOLDOWN_GROUP).build())
                setData(DataComponentTypes.ITEM_MODEL, Key.key("minecraft:leather_horse_armor"))

                editMeta {
                    it.itemName(Component.text("Steed").color(NamedTextColor.GOLD))
                }
            })

            it.add(ItemStack(Material.PAPER).apply {
                uniqueClassItem()
                soulbound()
                setAnniItem(AERIAL_AGILITY_ITEM_ID)

                setData(DataComponentTypes.USE_COOLDOWN, UseCooldown.useCooldown(AERIAL_AGILITY_COOLDOWN / 20f).cooldownGroup(AERIAL_AGILITY_COOLDOWN_GROUP).build())

                editMeta {
                    it.itemName(Component.text("Aerial Agility").color(NamedTextColor.GOLD))
                }
            })
        }
    }

    override fun onUnselect(player: Player) {
        horses[player]?.discard()
        horses.remove(player)
        super.onUnselect(player)
    }

    val horses = mutableMapOf<Player, RobinHoodHorse>()

    @EventHandler
    fun onInteract(event: PlayerInteractEvent) {
        val player = event.player
        if (!isSelected(player)) return

        val item = event.item ?: return
        val id = item.getAnniId()
        if (id == STEED_ITEM_ID) {
            if (player.hasCooldown(item)) return

            val mcPlayer = player.toMC()
            val level = mcPlayer.level()
            val horse = horses.getOrPut(player) { RobinHoodHorse(level,mcPlayer) }

            if (horse.isSpawned) {
                val horse = horses[player]!!
                horse.remove(Entity.RemovalReason.DISCARDED)

                if (horse.isDeadOrDying) {
                    horses.remove(player)
                }
                else {
                    horses[player] = RobinHoodHorse(level,mcPlayer).apply {
                        this.health = horse.health
                    }
                }

                return
            }

            level.addFreshEntity(horse.apply {
                this.setPos(mcPlayer.position())
                this.setLevel(mcPlayer.level())
                this.isSpawned = true
            })

            player.setCooldown(STEED_COOLDOWN_GROUP,STEED_RESPAWN_COOLDOWN)
        }
        if (id == AERIAL_AGILITY_ITEM_ID) {
            val action = event.action
            if (action.isRightClick) {
                if (player.hasCooldown(item)) return

                player.addPotionEffect(PotionEffect(PotionEffectType.SLOW_FALLING,200,0))
                player.setCooldown(AERIAL_AGILITY_COOLDOWN_GROUP,AERIAL_AGILITY_COOLDOWN)
            }
            if (action.isLeftClick) {
                player.removePotionEffect(PotionEffectType.SLOW_FALLING)
            }
        }
    }

    @EventHandler
    fun onDeath(event: PlayerDeathEvent) {
        val player = event.player
        if (!isSelected(player)) return

        val level = player.world.toMC()
        horses[player]?.kill(level)
    }

    class RobinHoodHorse: Horse {
        val ownerPlayer: ServerPlayer
        var isSpawned = false

        constructor(level: ServerLevel,owner: ServerPlayer): super(EntityType.HORSE,level) {
            this.ownerPlayer = owner
            this.isTamed = true
            this.equipment.set(EquipmentSlot.SADDLE, Items.SADDLE.defaultInstance)

            getAttribute(Attributes.MOVEMENT_SPEED)!!.baseValue = 0.3
            getAttribute(Attributes.JUMP_STRENGTH)!!.baseValue = 0.8
            getAttribute(Attributes.MAX_HEALTH)!!.baseValue = 30.0
        }

        override fun mobInteract(player: net.minecraft.world.entity.player.Player, hand: InteractionHand): InteractionResult {
            if (player != ownerPlayer) return InteractionResult.SUCCESS

            return super.mobInteract(player, hand)
        }

        override fun die(source: DamageSource) {
            ownerPlayer.bukkitEntity.setCooldown(STEED_COOLDOWN_GROUP,STEED_DEATH_COOLDOWN)
            horses.remove(ownerPlayer.bukkitEntity)

            super.die(source)
        }

        override fun remove(reason: RemovalReason, eventCause: EntityRemoveEvent.Cause?) {
            super.remove(reason, eventCause)
        }

        override fun openCustomInventoryScreen(player: net.minecraft.world.entity.player.Player) {

        }

        override fun equipBodyArmor(player: net.minecraft.world.entity.player.Player, itemStack: net.minecraft.world.item.ItemStack) {

        }

        override fun isAffectedByPotions(): Boolean {
            return false
        }

        override fun dropAllDeathLoot(level: ServerLevel, source: DamageSource): EntityDeathEvent {
            return CraftEventFactory.callEntityDeathEvent(this, source)
        }

        override fun checkDespawn() {

        }

        override fun heal(heal: Float, regainReason: EntityRegainHealthEvent.RegainReason, isFastRegen: Boolean) {

        }

        override fun shouldBeSaved(): Boolean {
            return false
        }
    }
}