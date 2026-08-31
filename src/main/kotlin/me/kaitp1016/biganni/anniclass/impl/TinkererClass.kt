package me.kaitp1016.biganni.anniclass.impl

import me.kaitp1016.biganni.anniclass.AnniClass
import me.kaitp1016.biganni.utils.BlockPosInfo
import me.kaitp1016.biganni.utils.MCUtils.toMC
import net.minecraft.core.BlockPos
import net.minecraft.tags.ItemTags
import net.minecraft.world.item.Items
import net.minecraft.world.level.block.Blocks
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.inventory.PrepareItemCraftEvent
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType

object TinkererClass: AnniClass(), Listener {
    override val name = "Tinkerer"
    override val shortName = "TIN"
    override val icon = Items.REDSTONE_BLOCK
    override val description = arrayOf(
        "一部の鉱石ブロックを設置するとバフを獲得するブロックになる。",
        "ツールを本1個とクラフトすると、エンチャントを本に移せる。",
    )

    override fun getDefaultItems(player: Player): MutableList<ItemStack> {
        return super.getDefaultItems(player).also {
            it.removeIf { it.type == Material.WOODEN_SWORD }
            it.add(ItemStack(Material.STONE_SWORD).uniqueClassItem().soulbound())

            it.add(ItemStack(Material.REDSTONE_BLOCK).uniqueClassItem().soulbound())
            it.add(ItemStack(Material.COAL_BLOCK).uniqueClassItem().soulbound())
            it.add(ItemStack(Material.BOOK).uniqueClassItem().soulbound().also { it.amount = 10 })
        }
    }

    data class PadType(val effect: PotionEffect,val drop: Material)

    val padTypes = mapOf(
        Material.REDSTONE_BLOCK to PadType(PotionEffect(PotionEffectType.SPEED,900,0),Material.REDSTONE),
        Material.COAL_BLOCK to PadType(PotionEffect(PotionEffectType.HASTE,900,0),Material.COAL),
        Material.DIAMOND_BLOCK to PadType(PotionEffect(PotionEffectType.SPEED,400,1),Material.DIAMOND),
        Material.GOLD_BLOCK to PadType(PotionEffect(PotionEffectType.HASTE,300,1),Material.GOLD_INGOT),
        Material.EMERALD_BLOCK to PadType(PotionEffect(PotionEffectType.ABSORPTION,400,0),Material.EMERALD),
    )

    data class PlacedPad(val owner: Player, val type: PadType)

    val pads = BlockPosInfo<PlacedPad>()

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onPlace(event: BlockPlaceEvent) {
        if (event.isCancelled) return

        val player = event.player
        if (!isSelected(player)) return

        val block = event.block
        val type = padTypes[block.type] ?: return

        val pos = BlockPos(block.x,block.y,block.z)
        val level = block.world.toMC()
        if (!level.getBlockState(pos.offset(0,1,0)).canBeReplaced()) {
            return
        }

        pads[level,pos] = PlacedPad(player,type)
        level.setBlockAndUpdate(pos.offset(0,1,0), Blocks.STONE_PRESSURE_PLATE.defaultBlockState())
    }

    @EventHandler
    fun onMove(event: PlayerMoveEvent) {
        val from = event.from
        val to = event.to
        if (from.x.toInt() == to.x.toInt() && from.y.toInt() == to.y.toInt() && from.z.toInt() == to.z.toInt()) return

        val player = event.player.toMC()
        val pos = player.blockPosition().offset(0,-1,0)
        val level = player.level()

        val pad = pads[level,pos] ?: return
        val bukkitPlayer = event.player
        bukkitPlayer.addPotionEffect(pad.type.effect)
        bukkitPlayer.world.playSound(bukkitPlayer.location, Sound.ENTITY_BLAZE_AMBIENT,1f,1f)
    }

    @EventHandler
    fun onBreak(event: BlockBreakEvent) {
        val block = event.block
        val level = block.world.toMC()
        val pos = BlockPos(block.x,block.y,block.z)

        val pad = pads[level,pos] ?: return
        pad.owner.give(ItemStack(pad.type.drop).also { it.amount = 4 })
        event.player.give(ItemStack(pad.type.drop).also { it.amount = 4 })

        block.world.setBlockData(pos.x,pos.y + 1,pos.z, Material.AIR.createBlockData())
        block.world.setBlockData(pos.x,pos.y ,pos.z, Material.AIR.createBlockData())

        pads.remove(level,pos)
        event.isCancelled = true
    }

    // 剣のエンチャントをはがす
    val allowedTags = listOf(ItemTags.PICKAXES,ItemTags.SWORDS,ItemTags.HOES,ItemTags.SHOVELS,ItemTags.AXES)

    @EventHandler
    fun onPrepareCraft(event: PrepareItemCraftEvent) {
        val player = event.view.player
        if (player !is Player || !isSelected(player)) return

        val matrix = event.inventory.matrix.filter { it?.type != null && !it.isEmpty }
        if (matrix.size != 2 || matrix.none { it?.type == Material.BOOK && it.amount == 1 }) return

        val tool = matrix.find { allowedTags.any { tag -> it?.toMC()?.`is`(tag) == true } && it?.enchantments?.isNotEmpty() == true && it.toMC()?.isDamaged == false } ?: return

        val book = ItemStack(Material.ENCHANTED_BOOK).also { book ->
            tool.enchantments.forEach {
                book.addUnsafeEnchantment(it.key,it.value)
            }
        }

        event.inventory.result = book
    }

    override fun resetBlocks() {
        pads.forEach { level, pos, pad ->
            level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState())
        }

        pads.clear()
    }
}