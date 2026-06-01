package me.kaitp1016.biganni.anniclass.impl

import io.papermc.paper.datacomponent.DataComponentTypes
import io.papermc.paper.datacomponent.item.UseCooldown
import me.kaitp1016.biganni.PLUGIN_ID
import me.kaitp1016.biganni.anniclass.AnniClass
import me.kaitp1016.biganni.utils.FullyInvisible
import me.kaitp1016.biganni.utils.ItemUtils.getAnniId
import me.kaitp1016.biganni.utils.ItemUtils.setAnniItem
import me.kaitp1016.biganni.utils.MCUtils.toMC
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.decoration.Mannequin
import net.minecraft.world.item.Items
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.ItemStack

object SpyClass: AnniClass(), Listener {
    override val name = "Spy"
    override val shortName = "SPY"
    override val icon = Items.SPYGLASS
    override val description = arrayOf(
        "スニークを一定時間続けると防具も含めた透明になる。",
        "一定距離移動するか、特定の行動をした後に解除される。",
        "アビリティを使用すると自身のコピーを召喚し、透明になる。"
    )

    const val FLEE_ITEM_ID = "spy_flee"
    const val FLEE_COOLDOWN = 800
    val FLEE_COOLDOWN_GROUP = Key.key(PLUGIN_ID, "spy_flee")

    const val MOVEABLE_DISTANCE = 3.0
    const val INVISBLE_COOLDOWN = 100

    override fun getDefaultItems(player: Player): MutableList<ItemStack> {
        return super.getDefaultItems(player).also {
            it.removeIf { it.type == Material.WOODEN_SWORD }
            it.add(ItemStack(Material.GOLDEN_SWORD).uniqueClassItem().soulbound())

            it.add(ItemStack(Material.SUGAR).apply {
                uniqueClassItem()
                soulbound()
                setAnniItem(FLEE_ITEM_ID)

                setData(DataComponentTypes.USE_COOLDOWN, UseCooldown.useCooldown(FLEE_COOLDOWN / 20f).cooldownGroup(FLEE_COOLDOWN_GROUP).build())

                editMeta {
                    it.itemName(Component.text("Flee").color(NamedTextColor.GOLD))
                }
            })
        }
    }

    data class SpyState(var sneakTick: Int) {
        var invisibleCooldown = 0
        var isInvisible = false
        var invisbleStart: Location? = null
    }

    val states = mutableMapOf<Player, SpyState>()

    override fun onUnselect(player: Player) {
        FullyInvisible.remove(player)
        states.remove(player)

        super.onUnselect(player)
    }

    override fun onUserTick(player: Player) {
        val state = states.getOrPut(player) { SpyState(0) }

        if (state.invisibleCooldown > 0) {
            state.invisibleCooldown--
        }

        if (state.isInvisible) {
            val start = state.invisbleStart
            if (start != null && start.distance(player.location) > MOVEABLE_DISTANCE) {
                state.invisibleCooldown = INVISBLE_COOLDOWN
                player.playSound(player, Sound.ENTITY_ZOMBIE_VILLAGER_CONVERTED, 1f, 1f)
            }
        }

        if (state.invisibleCooldown < 1 && player.isSneaking) {
            state.sneakTick++

            if (state.sneakTick > 50 && !state.isInvisible) {
                FullyInvisible.add(player, false)
                state.isInvisible = true
                state.invisbleStart = player.location
                player.world.playSound(player.location, Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1f)
            }
        } else {
            state.sneakTick = 0

            if (state.isInvisible) {
                FullyInvisible.remove(player)
                state.isInvisible = false
            }
        }
    }

    @EventHandler
    fun onInteract(event: PlayerInteractEvent) {
        val player = event.player
        if (!isSelected(player)) return

        states[player]?.let {
            if (it.isInvisible) {
                it.invisibleCooldown = INVISBLE_COOLDOWN
                player.playSound(player, Sound.ENTITY_ZOMBIE_VILLAGER_CONVERTED, 1f, 1f)
            }
        }

        val item = event.item ?: return
        if (item.getAnniId() != FLEE_ITEM_ID || player.hasCooldown(item)) return

        val mcPlayer = player.toMC()
        val level = mcPlayer.level()

        level.addFreshEntity(PlayerMannequin(level, mcPlayer).apply {
            setPos(mcPlayer.position())
            absSnapRotationTo(mcPlayer.yRot, mcPlayer.xRot)
        })

        FullyInvisible.add(player, 120)

        player.setCooldown(FLEE_COOLDOWN_GROUP, FLEE_COOLDOWN)
    }

    class PlayerMannequin : Mannequin {
        constructor(level: ServerLevel, player: ServerPlayer) : super(level) {
            this.profile = player.profile
            this.customName = player.name

            equipment.setAll(player.inventory.equipment)
        }

        override fun tick() {
            super.tick()

            if (tickCount > 100) {
                discard()
            }
        }

        override fun isInvulnerableTo(level: ServerLevel, source: DamageSource): Boolean {
            return true
        }

        override fun doHurtEquipment(damageSource: DamageSource, damage: Float, vararg slots: EquipmentSlot) {

        }

        override fun shouldBeSaved(): Boolean {
            return false
        }
    }
}