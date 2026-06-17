package me.kaitp1016.biganni.anniclass.impl

import com.destroystokyo.paper.event.server.ServerTickStartEvent
import io.papermc.paper.datacomponent.DataComponentTypes
import io.papermc.paper.datacomponent.item.UseCooldown
import me.kaitp1016.biganni.PLUGIN_ID
import me.kaitp1016.biganni.anniclass.AnniClass
import me.kaitp1016.biganni.utils.BlockPosInfo
import me.kaitp1016.biganni.utils.ItemUtils.getAnniId
import me.kaitp1016.biganni.utils.ItemUtils.setAnniItem
import me.kaitp1016.biganni.utils.MCUtils.toMC
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.minecraft.core.BlockPos
import net.minecraft.world.item.Items
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeModifier
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.EquipmentSlotGroup
import org.bukkit.inventory.ItemStack
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType

object NeptuneClass: AnniClass(), Listener {
    override val icon = Items.TRIDENT
    override val name = "Neptune"
    override val shortName = "NEP"
    override val description = arrayOf(
        "周囲の液体を凍らせることができる。",
        "トライデントは2個のモードを切り替えることができる。",
    )

    const val TOGGLE_GROUND_FREEZE_ITEM_ID = "neptune_toggle_frost_walker"
    const val TIDEBRINGER_ITEM_ID = "neptune_tidebringer"
    const val TIDEBRINGER_COOLDOWN = 400
    val TIDEBRINGE_COOLDOWN_GROUP = Key.key(PLUGIN_ID, "neptune_tidebringer")

    override fun getDefaultItems(player: Player): MutableList<ItemStack> {
        return super.getDefaultItems(player).also {
            it.removeIf { it.type == Material.WOODEN_SWORD }
            it.add(ItemStack(Material.STONE_SWORD).uniqueClassItem().soulbound())

            it.add(ItemStack(Material.ICE).apply {
                uniqueClassItem()
                soulbound()
                setAnniItem(TOGGLE_GROUND_FREEZE_ITEM_ID)

                editMeta {
                    it.itemName(Component.text("Ground Freeze").color(NamedTextColor.GOLD))
                }
            })

            it.add(ItemStack(Material.TRIDENT).apply {
                uniqueClassItem()
                soulbound()
                setAnniItem(TIDEBRINGER_ITEM_ID)

                setData(DataComponentTypes.USE_COOLDOWN, UseCooldown.useCooldown(TIDEBRINGER_COOLDOWN / 20f).cooldownGroup(TIDEBRINGE_COOLDOWN_GROUP).build())

                editMeta {
                    it.itemName(Component.text("Tidebringer").color(NamedTextColor.GOLD))
                    it.addAttributeModifier(Attribute.ATTACK_DAMAGE, AttributeModifier(AXE_ATTRIBUTE_MODIFIER_KEY, 4.0, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.MAINHAND))
                }

                addEnchantment(Enchantment.LOYALTY, 3)
            })

            it.add(ItemStack(Material.LILY_PAD).uniqueClassItem().soulbound().also { item -> item.amount = 10 })
        }
    }

    data class FrozenBlock(val frozenBlock: Block, val unfrozenBlock: Block, var tick: Int = 20)

    val enabledPlayers = mutableListOf<Player>()
    val frozenBlocks = BlockPosInfo<FrozenBlock>()

    override fun onSelect(player: Player) {
        player.addPotionEffect(PotionEffect(PotionEffectType.WATER_BREATHING, PotionEffect.INFINITE_DURATION, 0))
        super.onSelect(player)
    }

    override fun onUnselect(player: Player) {
        player.removePotionEffect(PotionEffectType.WATER_BREATHING)
        enabledPlayers.remove(player)

        super.onUnselect(player)
    }

    override fun onRespawn(player: Player) {
        player.addPotionEffect(PotionEffect(PotionEffectType.WATER_BREATHING, PotionEffect.INFINITE_DURATION, 0))

        super.onRespawn(player)
    }


    @EventHandler
    fun onInteract(event: PlayerInteractEvent) {
        val player = event.player
        if (!isSelected(player)) return

        val item = event.item ?: return
        val id = item.getAnniId()

        if (id == TOGGLE_GROUND_FREEZE_ITEM_ID) {
            if (enabledPlayers.contains(player)) {
                enabledPlayers.remove(player)
                player.sendMessage(Component.text("Ground Freeze").color(NamedTextColor.AQUA).append(Component.text(" Disabled").color(NamedTextColor.RED)))
            } else {
                enabledPlayers.add(player)
                player.sendMessage(Component.text("Ground Freeze").color(NamedTextColor.AQUA).append(Component.text(" Enabled").color(NamedTextColor.GREEN)))
            }

            player.playSound(player, Sound.UI_BUTTON_CLICK, 1f, 1f)

            event.isCancelled = true
        }
        if (id == TIDEBRINGER_ITEM_ID) {
            if (!event.action.isLeftClick) return

            if (item.containsEnchantment(Enchantment.RIPTIDE)) {
                item.removeEnchantment(Enchantment.RIPTIDE)
                item.addEnchantment(Enchantment.LOYALTY, 3)

                player.sendMessage(Component.text("Curse of the Sea に切り替えました").color(NamedTextColor.GREEN))
            } else {
                item.removeEnchantment(Enchantment.LOYALTY)
                item.addEnchantment(Enchantment.RIPTIDE, 1)

                player.sendMessage(Component.text("Riptide に切り替えました").color(NamedTextColor.GREEN))
            }

            player.playSound(player, Sound.UI_BUTTON_CLICK, 1f, 1f)
        }
    }

    @EventHandler
    fun onTick(event: ServerTickStartEvent) {
        frozenBlocks.removeIf { level, pos, block ->
            block.tick--

            if (block.tick < 1) {
                if (level.getBlockState(pos).block == block.frozenBlock) {
                    level.setBlockAndUpdate(pos, block.unfrozenBlock.defaultBlockState())
                }
                return@removeIf true
            }

            false
        }
    }

    override fun onUserTick(player: Player) {
        if (!enabledPlayers.contains(player)) return

        val level = player.world.toMC()
        val pos = BlockPos.containing(player.x, player.y, player.z).below()

        for (dx in -2..2) {
            for (dz in -2..2) {
                val pos = pos.offset(dx, 0, dz)
                val block = level.getBlockState(pos)
                if (block.block == Blocks.WATER && block.fluidState.isSource && level.getBlockState(pos.offset(0, 1, 0)).isAir) {
                    level.setBlockAndUpdate(pos, Blocks.ICE.defaultBlockState())
                    frozenBlocks[level, pos] = FrozenBlock(Blocks.ICE, Blocks.WATER)
                }
                if (block.block == Blocks.LAVA && block.fluidState.isSource && level.getBlockState(pos.offset(0, 1, 0)).isAir) {
                    level.setBlockAndUpdate(pos, Blocks.MAGMA_BLOCK.defaultBlockState())
                    frozenBlocks[level, pos] = FrozenBlock(Blocks.MAGMA_BLOCK, Blocks.LAVA)
                }

                frozenBlocks[level, pos]?.let {
                    it.tick = 20
                }
            }
        }
    }
}