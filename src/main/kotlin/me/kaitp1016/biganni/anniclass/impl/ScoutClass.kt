package me.kaitp1016.biganni.anniclass.impl

import com.destroystokyo.paper.event.server.ServerTickStartEvent
import me.kaitp1016.biganni.anniclass.AnniClass
import me.kaitp1016.biganni.utils.ItemUtils.addLore
import me.kaitp1016.biganni.utils.ItemUtils.getAnniId
import me.kaitp1016.biganni.utils.ItemUtils.setAnniItem
import me.kaitp1016.biganni.utils.Utils.toIntCorrect
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import net.minecraft.world.item.Items
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.player.PlayerFishEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.potion.PotionEffectType

object ScoutClass: AnniClass(), Listener {
    override val name = "Scout"
    override val deathMessageName = "SCO"
    override val icon = Items.FISHING_ROD
    override val description = arrayOf(
        "グラップリングフックが初期装備に含まれている。",
        "グラップリングフックは高速で移動ができる。",
        "戦闘中は使用できない。"
    )

    const val GRAPPLING_HOOK_ID = "scout_grappling_hook"

    override fun getDefaultItems(player: Player): MutableList<ItemStack> {
        return super.getDefaultItems(player).also {
            it.add(ItemStack(Material.FISHING_ROD).apply {
                addLore(Component.text("<").decoration(TextDecoration.ITALIC,false).color(NamedTextColor.GRAY).append(Component.text("Right Click").color(NamedTextColor.AQUA).append(Component.text(">").color(NamedTextColor.GRAY).append(Component.text(" Cast your grappling").color(NamedTextColor.DARK_AQUA)))))
                addLore(Component.text("hook").color(NamedTextColor.DARK_AQUA).decoration(TextDecoration.ITALIC,false))

                uniqueClassItem()
                soulbound()
                setAnniItem(GRAPPLING_HOOK_ID)

                editMeta {
                    it.isUnbreakable = true
                    it.itemName(Component.text("Grapple").color(NamedTextColor.YELLOW))
                }
            })

            it.removeIf { it.type == Material.WOODEN_SWORD }
            it.add(ItemStack(Material.GOLDEN_SWORD).uniqueClassItem().soulbound())
        }
    }

    data class Cooldown(val player: Player,var time: Int)

    val cooldowns = mutableListOf<Cooldown>()

    @EventHandler
    fun onFish(event: PlayerFishEvent) {
        if (event.state != PlayerFishEvent.State.IN_GROUND && event.state != PlayerFishEvent.State.REEL_IN) return

        val player = event.player
        if (!isSelected(player)) return

        val hand = event.hand ?: return
        val item = event.player.inventory.getItem(hand)
        if (item.getAnniId() != GRAPPLING_HOOK_ID) return

        if (cooldowns.any { it.player == player } || player.hasPotionEffect(PotionEffectType.SLOWNESS)) {
            event.isCancelled = true
            return
        }

        val hook = event.hook
        val world = hook.world
        if (!hook.isOnGround && !world.getBlockAt(hook.x.toInt(),hook.y.toIntCorrect(),hook.z.toInt()).isBuildable && !world.getBlockAt(hook.x.toInt(),(hook.y - 1).toIntCorrect(),hook.z.toInt()).isBuildable) {
            return
        }

        val velocity = player.location.clone().subtract(hook.location).apply {
            y *= 0.6
            multiply(-0.3)
        }

        player.velocity = player.velocity.add(velocity.toVector())
    }

    @EventHandler
    fun onTick(event: ServerTickStartEvent) {
        if (cooldowns.isEmpty()) return

        cooldowns.removeAll{
            it.time--
            return@removeAll it.time < 1
        }
    }

    @EventHandler
    fun onDamage(event: EntityDamageEvent) {
        val source = event.damageSource
        val causingEntity = source.causingEntity
        if (causingEntity !is Player) return

        if (isSelected(causingEntity)) {
            addCooldown(causingEntity)
        }

        val entity = event.entity
        if (entity is Player && isSelected(entity)) {
            addCooldown(entity)
        }
    }

    fun addCooldown(player: Player) {
        cooldowns.removeIf { it.player == player }

        cooldowns.add(Cooldown(player,100))
    }
}