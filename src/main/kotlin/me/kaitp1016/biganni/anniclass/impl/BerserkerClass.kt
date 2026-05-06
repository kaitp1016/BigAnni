package me.kaitp1016.biganni.anniclass.impl

import io.papermc.paper.datacomponent.DataComponentTypes
import io.papermc.paper.datacomponent.item.UseCooldown
import me.kaitp1016.biganni.PLUGIN_ID
import me.kaitp1016.biganni.anniclass.AnniClass
import me.kaitp1016.biganni.plugin
import me.kaitp1016.biganni.utils.ItemUtils.getAnniId
import me.kaitp1016.biganni.utils.ItemUtils.setAnniItem
import me.kaitp1016.biganni.utils.Scheduler
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.minecraft.world.item.Items
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.Sound
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeModifier
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import kotlin.math.min

object BerserkerClass: AnniClass(), Listener {
    override val name = "Berserker"
    override val icon = Items.NETHERITE_INGOT
    override val description = arrayOf(
        "プレイヤーを殺すごとに体力がハートが0.5個分増える。",
        "アビリティを使用することで移動速度が上昇し、クラスのアビリティーのデバフを受けなくなる。",
    )

    const val BERSERKER_ITEM_ID = "berserker_berserker"
    const val BERSERKER_COOLDOWN = 2400
    val BERSERKER_COOLDOWN_GROUP = Key.key(PLUGIN_ID,"berserker_berserker")
    val BERSERKER_HEALTH_PASSIVE_KEY = NamespacedKey(plugin,"berserker_health_passive")

    override fun getDefaultItems(player: Player): MutableList<ItemStack> {
        return super.getDefaultItems(player).also {
            it.add(ItemStack(Material.NETHERITE_INGOT).apply {
                uniqueClassItem()
                soulbound()
                setAnniItem(BERSERKER_ITEM_ID)

                setData(DataComponentTypes.USE_COOLDOWN, UseCooldown.useCooldown(BERSERKER_COOLDOWN / 20f).cooldownGroup(BERSERKER_COOLDOWN_GROUP).build())

                editMeta {
                    it.itemName(Component.text("Berserker").color(NamedTextColor.GOLD))
                }
            })

            it.removeIf { it.type == Material.WOODEN_SWORD }
            it.add(ItemStack(Material.STONE_SWORD).uniqueClassItem().soulbound())
        }
    }

    override fun onUnselect(player: Player) {
        super.onUnselect(player)

        val attribute = player.getAttribute(Attribute.MAX_HEALTH)!!
        attribute.removeModifier(BERSERKER_HEALTH_PASSIVE_KEY)
    }

    val abilityPlayers = mutableListOf<Player>()

    @EventHandler
    fun onInteract(event: PlayerInteractEvent) {
        val player = event.player
        if (!isSelected(player)) return

        val item = event.item ?: return
        if (item.getAnniId() != BERSERKER_ITEM_ID || player.hasCooldown(item)) return

        player.world.playSound(player,Sound.ENTITY_RAVAGER_ROAR,1f,1f)
        player.addPotionEffect(PotionEffect(PotionEffectType.SPEED,300,0))
        player.setCooldown(BERSERKER_COOLDOWN_GROUP,BERSERKER_COOLDOWN)
        abilityPlayers.add(player)

        Scheduler.scheduleTask(300) {
            player.world.playSound(player,Sound.ENTITY_WOLF_SHAKE,1f,2f)
            abilityPlayers.remove(player)
        }
    }

    @EventHandler
    fun onKill(event: PlayerDeathEvent) {
        val player = event.player
        if (isSelected(player)) {
            val attribute = player.getAttribute(Attribute.MAX_HEALTH)!!
            attribute.removeModifier(BERSERKER_HEALTH_PASSIVE_KEY)
        }

        val killer = event.damageSource.causingEntity as? Player ?: return
        if (isSelected(killer)) {
            val attribute = killer.getAttribute(Attribute.MAX_HEALTH)!!
            val amount = min(40.0,(attribute.getModifier(BERSERKER_HEALTH_PASSIVE_KEY)?.amount?.plus(1) ?: 1.0))

            attribute.removeModifier(BERSERKER_HEALTH_PASSIVE_KEY)
            attribute.addTransientModifier(AttributeModifier(BERSERKER_HEALTH_PASSIVE_KEY,amount, AttributeModifier.Operation.ADD_NUMBER))
        }
    }

    fun isUsingAbility(player: Player): Boolean {
        return abilityPlayers.contains(player)
    }
}