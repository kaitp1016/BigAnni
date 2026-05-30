package me.kaitp1016.biganni.anniclass.impl

import io.papermc.paper.datacomponent.DataComponentTypes
import io.papermc.paper.datacomponent.item.BlocksAttacks
import io.papermc.paper.datacomponent.item.Consumable
import io.papermc.paper.datacomponent.item.UseCooldown
import io.papermc.paper.datacomponent.item.consumable.ItemUseAnimation
import me.kaitp1016.biganni.PLUGIN_ID
import me.kaitp1016.biganni.anniclass.AnniClass
import me.kaitp1016.biganni.utils.ItemUtils.getAnniId
import me.kaitp1016.biganni.utils.ItemUtils.setAnniItem
import me.kaitp1016.biganni.utils.MCUtils.toMC
import me.kaitp1016.biganni.utils.Scheduler
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
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import kotlin.math.max
import kotlin.math.min

object TankClass: AnniClass(), Listener {
    override val name = "Tank"
    override val shortName = "TNK"
    override val icon = Items.SHIELD
    override val description = arrayOf(
        "盾を構えてる間は自身へのダメージを無効化し、",
        "周囲の味方の矢から受けるダメージを無効化する。",
        "アビリティを使用すると前に突進し、ダメージを与える。"
    )

    const val THE_SHIELD_ID = "tank_the_shield"
    const val THE_SHIELD_COOLDOWN = 200
    val THE_SHIELD_COOLDOWN_GROUP = Key.key(PLUGIN_ID,"tank_the_shield")

    const val SHIELD_CHARGE_ITEM_ID = "tank_shield_charge"
    const val SHIELD_CHARGE_COOLDOWN = 800
    val SHIELD_CHARGE_COOLDOWN_GROUP = Key.key(PLUGIN_ID,"tank_shield_charge")

    const val MAX_STAMINA = 30

    override fun getDefaultArmors(player: Player): MutableMap<EquipmentSlot, ItemStack> {
        return super.getDefaultArmors(player).apply {
            this[EquipmentSlot.OFF_HAND] = ItemStack(Material.SHIELD).apply {
                uniqueClassItem()
                soulbound()
                setAnniItem(THE_SHIELD_ID)

                setData(DataComponentTypes.BLOCKS_ATTACKS, BlocksAttacks.blocksAttacks().build())
                setData(DataComponentTypes.CONSUMABLE, Consumable.consumable().consumeSeconds(Float.MAX_VALUE).animation(ItemUseAnimation.BLOCK).build())
                setData(DataComponentTypes.USE_COOLDOWN, UseCooldown.useCooldown(THE_SHIELD_COOLDOWN / 20f).cooldownGroup(THE_SHIELD_COOLDOWN_GROUP).build())

                editMeta {
                    it.itemName(Component.text("The Shield").color(NamedTextColor.GOLD))
                }
            }
        }
    }

    override fun getDefaultItems(player: Player): MutableList<ItemStack> {
        return super.getDefaultItems(player).also {
            it.add(ItemStack(Material.TURTLE_SCUTE).apply {
                uniqueClassItem()
                soulbound()
                setAnniItem(SHIELD_CHARGE_ITEM_ID)

                setData(DataComponentTypes.USE_COOLDOWN, UseCooldown.useCooldown(SHIELD_CHARGE_COOLDOWN / 20f).cooldownGroup(SHIELD_CHARGE_COOLDOWN_GROUP).build())

                editMeta {
                    it.itemName(Component.text("Shield Charge").color(NamedTextColor.GOLD))
                }
            })
        }
    }

    data class ShieldState(var stamina: Int = 0) {
        var regenTick = 0
        var hasDoubleDamage = false
    }

    val states = mutableMapOf<Player, ShieldState>()

    override fun onUnselect(player: Player) {
        states.remove(player)
        super.onUnselect(player)
    }

    override fun onUserTick(player: Player) {
        val state = states.getOrPut(player) { ShieldState(0) }

        val mcPlayer = player.toMC()
        val useItem = mcPlayer.useItem
        if (useItem.bukkitStack.getAnniId() == THE_SHIELD_ID) {
            if (mcPlayer.ticksUsingItem > 100) {
                state.hasDoubleDamage = true
            }
        }
        else {
            state.regenTick++

            if (state.regenTick > 50 && state.stamina < MAX_STAMINA) {
                state.stamina = min(state.stamina + 3, MAX_STAMINA)
                state.regenTick = 0
                player.sendMessage("§7Stamina level: §6${state.stamina}")
            }
        }
    }

    @EventHandler
    fun onDamage(event: EntityDamageEvent) {
        val player = event.entity as? Player ?: return

        if (event.damageSource.damageType == DamageType.ARROW) {
            val world = player.world
            val team = player.toMC().teamColor
            if (world.getNearbyPlayers(player.location,5.0).any {
                val mcPlayer = it.toMC()
                return@any mcPlayer.teamColor == team && it.toMC().useItem.bukkitStack.getAnniId() == THE_SHIELD_ID && states.getOrPut(player) { ShieldState() }.stamina > 0
            }) {
                player.world.playSound(player.location, Sound.ENTITY_ARROW_HIT,2f,2f)
                event.isCancelled = true
            }
        }

        if (isSelected(player)) {
            val mcPlayer = player.toMC()
            if (mcPlayer.useItem.bukkitStack.getAnniId() == THE_SHIELD_ID) {
                val state = states.getOrPut(player) { ShieldState() }
                val damage = if (event.damage > 5) 10 else 5

                if (state.stamina > 0) {
                    state.stamina = max(state.stamina - damage ,0)
                    event.isCancelled = true
                    player.sendMessage("§7Stamina level: §6${state.stamina}")

                    if (state.stamina < 1) {
                        player.addPotionEffect(PotionEffect(PotionEffectType.SLOWNESS,60,10))
                        player.addPotionEffect(PotionEffect(PotionEffectType.NAUSEA,60,1))
                        player.world.playSound(player.location, Sound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR,1f,1f)
                        player.setCooldown(THE_SHIELD_COOLDOWN_GROUP,THE_SHIELD_COOLDOWN)
                    }
                    else {
                        player.world.playSound(player.location, Sound.ITEM_SHIELD_BLOCK,1f,1f)
                    }
                }
            }
        }

        val damager = event.damageSource.causingEntity
        if (damager is Player && isSelected(damager)) {
            if (states[player]?.hasDoubleDamage == true) {
                event.damage *= 2
                states[player]?.hasDoubleDamage = false
                damager.world.playSound(damager.location, Sound.ENTITY_ENDER_DRAGON_AMBIENT,1f,1f)
            }
        }
    }

    @EventHandler
    fun onInteract(event: PlayerInteractEvent) {
        val player = event.player
        if (!isSelected(player)) return

        val item = event.item ?: return
        if (item.getAnniId() != SHIELD_CHARGE_ITEM_ID || player.hasCooldown(item)) return

        val velocity = player.location.direction.clone().setY(0).normalize().multiply(3)
        val world = player.world

        player.velocity = velocity

        repeat(6) { count ->
            Scheduler.scheduleTask(count * 2) {
                val team = player.toMC().teamColor
                world.getNearbyPlayers(player.location,0.5).forEach {
                    if (it.toMC().teamColor != team) {
                        it.damage(2.5, player)
                        it.addPotionEffect(PotionEffect(PotionEffectType.BLINDNESS,100,0))
                    }
                }
            }
        }

        player.setCooldown(SHIELD_CHARGE_COOLDOWN_GROUP,SHIELD_CHARGE_COOLDOWN)
    }
}