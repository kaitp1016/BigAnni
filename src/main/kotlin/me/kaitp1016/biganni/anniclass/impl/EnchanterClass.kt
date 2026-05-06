package me.kaitp1016.biganni.anniclass.impl

import com.destroystokyo.paper.event.server.ServerTickStartEvent
import io.papermc.paper.datacomponent.DataComponentTypes
import io.papermc.paper.datacomponent.item.UseCooldown
import me.kaitp1016.biganni.PLUGIN_ID
import me.kaitp1016.biganni.anniclass.AnniClass
import me.kaitp1016.biganni.utils.ItemUtils.getAnniId
import me.kaitp1016.biganni.utils.ItemUtils.setAnniItem
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.minecraft.world.item.Items
import org.bukkit.Material
import org.bukkit.Particle
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.enchantment.EnchantItemEvent
import org.bukkit.event.player.PlayerExpChangeEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.ItemStack
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

object EnchanterClass: AnniClass(), Listener {
    override val icon = Items.ENCHANTING_TABLE
    override val name = "Enchanter"
    override val description = arrayOf(
        "この職業の時は常に経験値の獲得量が2倍になる。",
        "アビリティを使用することで経験値の獲得量が3倍になる。",
        "エンチャントをしたときにランダムなひとつのエンチャントの",
        "レベルが昇華する確率がある。",
    )

    const val INTENSIFIER_ITEM_ID = "enchanter_intensifier"
    const val INTENSIFIER_COOLDOWN = 2400
    val INTENSIFIER_COOLDOWN_GROUP = Key.key(PLUGIN_ID,"enchanter_intensifier")

    const val INTENSIFIER_TIME = 300

    override fun getDefaultItems(player: Player): MutableList<ItemStack> {
        return super.getDefaultItems(player).also {
            it.removeIf { it.type == Material.WOODEN_SWORD }
            it.add(ItemStack(Material.GOLDEN_SWORD).uniqueClassItem().soulbound())

            it.add(ItemStack(Material.LAPIS_LAZULI).apply {
                uniqueClassItem()
                soulbound()
                setAnniItem(INTENSIFIER_ITEM_ID)

                setData(DataComponentTypes.USE_COOLDOWN, UseCooldown.useCooldown(INTENSIFIER_COOLDOWN / 20f).cooldownGroup(INTENSIFIER_COOLDOWN_GROUP).build())

                editMeta {
                    it.itemName(Component.text("Intensifier").color(NamedTextColor.GOLD))
                }
            })
        }
    }

    data class IntensifierAbility(val player: Player,var time: Int)

    val intensifiers = mutableListOf<IntensifierAbility>()

    @EventHandler
    fun onEnchant(event: EnchantItemEvent) {
        val player = event.enchanter

        if (!isSelected(player) || Random.nextFloat() > 0.5f) return

        val enchant = event.enchantsToAdd.filter { it.key.maxLevel > it.value }.keys.randomOrNull() ?: return

        event.enchantsToAdd[enchant] = event.enchantsToAdd[enchant]!! + 1
        player.sendMessage(Component.text("エンチャンターの効果で ").color(NamedTextColor.GREEN).append(enchant.description().color(NamedTextColor.YELLOW).append(Component.text(" のレベルがあがった!").color(NamedTextColor.GREEN))))
    }

    @EventHandler
    fun onInteract(event: PlayerInteractEvent) {
        val player = event.player
        if (!isSelected(player)) return

        val item = event.item ?: return
        if (item.getAnniId() != INTENSIFIER_ITEM_ID || player.hasCooldown(item)) return

        intensifiers.add(IntensifierAbility(player,INTENSIFIER_TIME))
        player.setCooldown(INTENSIFIER_COOLDOWN_GROUP,INTENSIFIER_COOLDOWN)
    }

    @EventHandler
    fun onTick(event: ServerTickStartEvent) {
        if (intensifiers.isEmpty()) return

        intensifiers.removeAll {
            it.time--
            return@removeAll it.time <= 0
        }
    }

    @EventHandler
    fun onExp(event:PlayerExpChangeEvent) {
        val player = event.player
        if (!isSelected(player)) return

        val isIntensifier = intensifiers.any { it.player == player }
        val multiply = if (isIntensifier) 3 else 2
        event.amount *= multiply

        if (isIntensifier) {
            val distance = 1.5
            val amount = 32
            val world = player.world

            repeat(amount) {
                val angle = 360f / amount * it * PI / 180f
                val x = player.x + distance * cos(angle)
                val z = player.z + 0.5 + distance * sin(angle)
                val y = player.y + 1.0

                Particle.ENCHANT.builder().location(world, x, y, z).receivers(32, true).count(0).offset(0.0, 0.0, 0.0).spawn()
            }
        }
    }
}