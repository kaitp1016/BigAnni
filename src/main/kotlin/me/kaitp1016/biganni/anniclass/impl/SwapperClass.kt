package me.kaitp1016.biganni.anniclass.impl

import com.destroystokyo.paper.event.server.ServerTickStartEvent
import io.papermc.paper.datacomponent.DataComponentTypes
import io.papermc.paper.datacomponent.item.UseCooldown
import me.kaitp1016.biganni.PLUGIN_ID
import me.kaitp1016.biganni.anniclass.AnniClass
import me.kaitp1016.biganni.plugin
import me.kaitp1016.biganni.utils.ItemUtils.getAnniId
import me.kaitp1016.biganni.utils.ItemUtils.setAnniItem
import me.kaitp1016.biganni.utils.MCUtils.toMC
import me.kaitp1016.biganni.utils.Scheduler
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.minecraft.tags.BlockTags
import net.minecraft.world.item.Items
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.Sound
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeModifier
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.bukkit.util.VoxelShape
import kotlin.math.max
import kotlin.math.min

object SwapperClass: AnniClass(), Listener {
    override val name = "Swapper"
    override val icon = Items.MUSIC_DISC_CAT
    override val description = arrayOf(
        "アビリティを使用すると視点先の敵と自身の位置を入れ替える。",
    )

    const val SWAPPER_ITEM_ID = "swapper_swapper"
    const val SWAPPER_COOLDOWN = 1200
    val SWAPPER_COOLDOWN_GROUP = Key.key(PLUGIN_ID,"swapper_swapper")

    val SWAPPER_FEATHER_FALLING = NamespacedKey(plugin,"swapper_feather_falling")

    override fun getDefaultItems(player: Player): MutableList<ItemStack> {
        return super.getDefaultItems(player).also {
            it.add(ItemStack(Material.MUSIC_DISC_CAT).apply {
                uniqueClassItem()
                soulbound()
                setAnniItem(SWAPPER_ITEM_ID)

                setData(DataComponentTypes.USE_COOLDOWN, UseCooldown.useCooldown(SWAPPER_COOLDOWN / 20f).cooldownGroup(SWAPPER_COOLDOWN_GROUP).build())

                editMeta {
                    it.itemName(Component.text("Swapper").color(NamedTextColor.GOLD))
                }
            })
        }
    }

    @EventHandler
    fun onInteract(event: PlayerInteractEvent) {
        val player = event.player
        if (!isSelected(player)) return

        val item = event.item ?: return
        if (item.getAnniId() != SWAPPER_ITEM_ID || player.hasCooldown(item)) return

        val world = player.world
        if (!world.getBlockState(player.location).block.isEmpty || !world.getBlockState(player.location.clone().add(0.0,1.0,0.0)).block.isEmpty || !world.getBlockState(player.location.clone().add(0.0,-1.0,0.0)).block.toMC().occlusionShape.`moonrise$isFullBlock`()) {
            player.sendMessage("ここでは使用できません!")
            return
        }

        val raytrace = player.rayTraceEntities(20)
        val target = raytrace?.hitEntity as? Player ?: return
        if (BerserkerClass.abilityPlayers.contains(target)) return

        val selfLocation = player.location
        player.teleport(target.location)
        target.teleport(selfLocation)

        player.playSound(player, Sound.ENTITY_ENDERMAN_TELEPORT,1f,1f)
        target.playSound(target, Sound.ENTITY_ENDERMAN_TELEPORT,1f,1f)

        target.addPotionEffect(PotionEffect(PotionEffectType.ABSORPTION,1,100))
        target.addPotionEffect(PotionEffect(PotionEffectType.SLOWNESS,0,60))
        target.addPotionEffect(PotionEffect(PotionEffectType.ABSORPTION,1,100))

        target.getAttribute(Attribute.FALL_DAMAGE_MULTIPLIER)?.addTransientModifier(AttributeModifier(SWAPPER_FEATHER_FALLING,-100000.0, AttributeModifier.Operation.ADD_NUMBER))

        Scheduler.scheduleTask(100) {
            target.getAttribute(Attribute.FALL_DAMAGE_MULTIPLIER)?.removeModifier(SWAPPER_FEATHER_FALLING)
        }

        player.setCooldown(SWAPPER_COOLDOWN_GROUP,SWAPPER_COOLDOWN)
    }
}