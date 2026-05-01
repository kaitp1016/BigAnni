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
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.LightningBolt
import net.minecraft.world.item.Items
import org.bukkit.Material
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeModifier
import org.bukkit.damage.DamageSource
import org.bukkit.damage.DamageType
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.EquipmentSlotGroup
import org.bukkit.inventory.ItemStack
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType

object ThorClass: AnniClass(), Listener {
    override val name = "Thor"
    override val icon = Items.GOLDEN_AXE
    override val description = arrayOf(
        "アビリティを使用すると周りの敵に魔法ダメージを与え、耐性を獲得する",
    )

    const val HAMMER_ITEM_ID = "thor_hammer"
    const val HAMMER_COOLDOWN = 800
    val HAMMER_COOLDOWN_GROUP = Key.key(PLUGIN_ID,"thor_hammer")

    override fun getDefaultItems(player: Player): MutableList<ItemStack> {
        return super.getDefaultItems(player).also {
            it.add(ItemStack(Material.GOLDEN_AXE).apply {
                uniqueClassItem()
                soulbound()
                setAnniItem(HAMMER_ITEM_ID)

                setData(DataComponentTypes.USE_COOLDOWN, UseCooldown.useCooldown(HAMMER_COOLDOWN / 20f).cooldownGroup(HAMMER_COOLDOWN_GROUP).build())

                editMeta {
                    it.itemName(Component.text("Hammer").color(NamedTextColor.GOLD))
                    it.addAttributeModifier(Attribute.ATTACK_DAMAGE, AttributeModifier(AXE_ATTRIBUTE_MODIFIER_KEY,4.5, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.MAINHAND))
                }

                addUnsafeEnchantment(Enchantment.UNBREAKING,10)
                addUnsafeEnchantment(Enchantment.KNOCKBACK,1)
            })
        }
    }

    @EventHandler
    fun onInteract(event: PlayerInteractEvent) {
        if (event.action != Action.RIGHT_CLICK_BLOCK && event.action != Action.RIGHT_CLICK_AIR) return

        val player = event.player
        if (!isSelected(player)) return

        val item = event.item ?: return
        if (item.getAnniId() != HAMMER_ITEM_ID || player.hasCooldown(item)) return

        val team = player.toMC().teamColor

        player.world.getNearbyPlayers(player.location,5.0).forEach { target ->
            if (target.toMC().teamColor != team) {
                val source = DamageSource.builder(DamageType.MAGIC).withDirectEntity(player).withCausingEntity(player).build()
                target.damage(5.0, source)

                val level = target.world.toMC()
                level.addFreshEntity(LightningBolt(EntityType.LIGHTNING_BOLT, level).apply {
                    visualOnly = true
                    flashes = 1
                    setPos(target.x, target.y, target.z)
                })
            }
        }

        player.addPotionEffect(PotionEffect(PotionEffectType.RESISTANCE,400,0))

        player.setCooldown(HAMMER_COOLDOWN_GROUP,HAMMER_COOLDOWN)
    }
}