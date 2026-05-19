package me.kaitp1016.biganni.anniclass.impl

import com.destroystokyo.paper.event.server.ServerTickStartEvent
import io.papermc.paper.datacomponent.DataComponentTypes
import io.papermc.paper.datacomponent.item.UseCooldown
import it.unimi.dsi.fastutil.ints.IntList
import me.kaitp1016.biganni.PLUGIN_ID
import me.kaitp1016.biganni.anniclass.AnniClass
import me.kaitp1016.biganni.utils.ItemUtils.getAnniId
import me.kaitp1016.biganni.utils.ItemUtils.setAnniItem
import me.kaitp1016.biganni.utils.MCUtils.toMC
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.minecraft.core.component.DataComponents
import net.minecraft.world.entity.projectile.FireworkRocketEntity
import net.minecraft.world.item.Items
import net.minecraft.world.item.component.FireworkExplosion
import net.minecraft.world.item.component.Fireworks
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.player.PlayerInteractEntityEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType

object MercenaryClass: AnniClass(), Listener {
    override val icon = Items.SKELETON_SKULL
    override val name = "Mercenary"
    override val shortName = "MER"
    override val description = arrayOf(
        "アビリティを使用すると敵をマークすることができる。",
        "マークされたプレイヤーは発光し、受けるダメージが増える。",
    )

    const val MARK_ITEM_ID = "mercenary_mark"
    const val MARK_COOLDOWN = 600
    val MARK_COOLDOWN_GROUP = Key.key(PLUGIN_ID,"mercenary_mark")

    const val MARK_TIME = 200

    override fun getDefaultItems(player: Player): MutableList<ItemStack> {
        return super.getDefaultItems(player).also {
            it.add(ItemStack(Material.SKELETON_SKULL).apply {
                uniqueClassItem()
                soulbound()
                setAnniItem(MARK_ITEM_ID)

                setData(DataComponentTypes.USE_COOLDOWN, UseCooldown.useCooldown(MARK_COOLDOWN / 20f).cooldownGroup(MARK_COOLDOWN_GROUP).build())

                editMeta {
                    it.itemName(Component.text("Mark").color(NamedTextColor.GOLD))
                }
            })
        }
    }

    data class MarkedPlayer(val player: Player, var time: Int)
    data class MarkCooldown(val player: Player, var time: Int)

    val marks = mutableListOf<MarkedPlayer>()
    val markCooldowns = mutableListOf<MarkCooldown>()

    @EventHandler
    fun onInteract(event: PlayerInteractEntityEvent) {
        val player = event.player
        if (!isSelected(player)) return

        val item = event.player.inventory.getItem(event.hand)
        if (item.getAnniId() != MARK_ITEM_ID || player.hasCooldown(item)) return

        event.isCancelled = true

        val target = event.rightClicked as? Player ?: return
        if (target.toMC().teamColor == player.toMC().teamColor || markCooldowns.any { it.player == target }) return

        target.addPotionEffect(PotionEffect(PotionEffectType.GLOWING,MARK_TIME,0))

        val world = target.world
        val level = world.toMC()

        target.world.addEntity(FireworkRocketEntity(level, target.x,target.y + 2.0,target.z,net.minecraft.world.item.ItemStack(Items.FIREWORK_ROCKET).apply {
            set(DataComponents.FIREWORKS, Fireworks(0,listOf(FireworkExplosion(FireworkExplosion.Shape.CREEPER, IntList.of(255,255,255),IntList.of(255,255,255,255),true,false))) )
        }).bukkitEntity)

        marks.add(MarkedPlayer(target,MARK_TIME))
        markCooldowns.add(MarkCooldown(target,MARK_COOLDOWN))

        player.setCooldown(MARK_COOLDOWN_GROUP,MARK_COOLDOWN)
    }

    @EventHandler
    fun onTick(event: ServerTickStartEvent) {
        if (!markCooldowns.isEmpty()) {
            markCooldowns.removeAll {
                it.time--
                return@removeAll it.time <= 0
            }
        }

        if (!marks.isEmpty()) {
            marks.removeAll {
                it.time--
                return@removeAll it.time <= 0
            }
        }
    }

    @EventHandler
    fun onDamage(event: EntityDamageEvent) {
        val entity = event.entity
        if (entity is Player && marks.any { it.player == entity }) {
            event.damage *= 1.3
        }
    }
}