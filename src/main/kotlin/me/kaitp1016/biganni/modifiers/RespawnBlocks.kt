package me.kaitp1016.biganni.modifiers

import com.destroystokyo.paper.event.server.ServerTickStartEvent
import me.kaitp1016.biganni.anniclass.impl.LumberjackClass
import me.kaitp1016.biganni.anniclass.impl.MinerClass
import me.kaitp1016.biganni.plugin
import me.kaitp1016.biganni.utils.MCUtils.toMC
import net.minecraft.core.BlockPos
import net.minecraft.world.InteractionHand
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.state.BlockState
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.Material
import org.bukkit.enchantments.Enchantment
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.player.PlayerExpChangeEvent
import org.bukkit.inventory.ItemStack
import kotlin.random.Random

object RespawnBlocks: Listener {
    data class RespawnableBlock(val material: Material, val placeholder: Material, val respawnTime: Int, val experience: IntRange,val drops: List<Pair<Material, IntRange>>,val isOre: Boolean = false,val isWood:Boolean = false)

    val respawnableBlocks = hashMapOf(
        // 鉱石
        Material.DIAMOND_ORE to RespawnableBlock(Material.DIAMOND_ORE,Material.COBBLESTONE,700,10..20,listOf(Material.DIAMOND to 1..1),isOre = true),
        Material.COAL_ORE to RespawnableBlock(Material.COAL_ORE,Material.COBBLESTONE,350,3..6,listOf(Material.COAL to 1..1),isOre = true),
        Material.IRON_ORE to RespawnableBlock(Material.IRON_ORE,Material.COBBLESTONE,450,4..8,listOf(Material.RAW_IRON to 1..1),isOre = true),
        Material.LAPIS_ORE to RespawnableBlock(Material.LAPIS_ORE,Material.COBBLESTONE,600,5..15,listOf(Material.LAPIS_LAZULI to 3..7),isOre = true),
        Material.GOLD_ORE to RespawnableBlock(Material.GOLD_ORE,Material.COBBLESTONE,600,6..10,listOf(Material.RAW_GOLD to 1..1),isOre = true),
        Material.EMERALD_ORE to RespawnableBlock(Material.EMERALD_ORE,Material.COBBLESTONE,500,10..15,listOf(Material.EMERALD to 1..1),isOre = true),
        Material.REDSTONE_ORE to RespawnableBlock(Material.REDSTONE_ORE,Material.COBBLESTONE,500,7..13,listOf(Material.REDSTONE to 3..7),isOre = true),
        Material.COPPER_ORE to RespawnableBlock(Material.COPPER_ORE,Material.COBBLESTONE,400,3..5,listOf(Material.RAW_COPPER to 1..1),isOre = true),

        // 原木
        Material.OAK_LOG to RespawnableBlock(Material.OAK_LOG,Material.AIR,1000,0..0,listOf(Material.OAK_LOG to 1..1),isWood = true),
        Material.SPRUCE_LOG to RespawnableBlock(Material.SPRUCE_LOG,Material.AIR,1000,0..0,listOf(Material.SPRUCE_LOG to 1..1),isWood = true),
        Material.ACACIA_LOG to RespawnableBlock(Material.ACACIA_LOG,Material.AIR,1000,0..0,listOf(Material.ACACIA_LOG to 1..1),isWood = true),
        Material.JUNGLE_LOG to RespawnableBlock(Material.JUNGLE_LOG,Material.AIR,1000,0..0,listOf(Material.JUNGLE_LOG to 1..1),isWood = true),

        // その他
        Material.GRAVEL to RespawnableBlock(Material.GRAVEL,Material.COBBLESTONE,500,1..3,listOf(Material.BONE to -5..3,Material.STRING to -5..3,Material.FLINT to -5..3,Material.FEATHER to -5..3,)),
        Material.MELON to RespawnableBlock(Material.MELON,Material.AIR,400,1..3,listOf(Material.MELON_SLICE to 1..4,)),
        )

    data class RespawingBlock(val level: Level,val pos: BlockPos,val block: BlockState,var tick: Int)

    val placedBlocks = hashSetOf<Pair<Level,BlockPos>>()
    val respawingBlocks = mutableListOf<RespawingBlock>()

    @EventHandler
    fun onBreak(event: BlockBreakEvent) {
        val player = event.player
        if (player.gameMode == GameMode.CREATIVE || event.isCancelled) return

        val block = event.block
        val pos = BlockPos(block.x,block.y,block.z)

        if (respawingBlocks.any { it.pos == pos }) {
            event.isCancelled = true
            return
        }

        val respawnData = respawnableBlocks[block.type] ?: return
        val level = block.world.toMC()
        if (placedBlocks.contains(level to pos)) return

        val exp = respawnData.experience.random()

        if (exp > 1) {
            val event = PlayerExpChangeEvent(player,exp)
            plugin.server.pluginManager.callEvent(event)

            player.giveExp(event.amount,true)
        }

        respawnData.drops.forEach { (material,amount) ->
            var amount = amount.random()

            if (respawnData.isOre) {
                val fortune = player.inventory.itemInMainHand.enchantments[Enchantment.FORTUNE] ?: 0
                amount += (Random.nextFloat() * fortune).toInt() * amount + fortune / 2 * amount
                amount *= MinerClass.getMultiply(player)
                amount *= LumberjackClass.getMultiply(player)
            }

            if (amount > 0) {
                player.give(ItemStack(material).also { it.amount = amount })
            }
        }

        if (respawnData.isOre) {
            MinerClass.onMine(player)
        }

        val originalBlock = level.getBlockState(pos)
        block.world.setBlockData(pos.x,pos.y,pos.z, Bukkit.createBlockData(respawnData.placeholder))
        respawingBlocks.add(RespawingBlock(level,pos,originalBlock,respawnData.respawnTime))

        player.toMC().mainHandItem.hurtAndBreak(1,player.toMC(), InteractionHand.MAIN_HAND)

        event.isCancelled = true
    }

    @EventHandler
    fun onPlace(event: BlockPlaceEvent) {
        val player = event.player
        if (player.gameMode == GameMode.CREATIVE) return

        val block = event.block
        if (respawnableBlocks.contains(block.type)) {
            val level = block.world.toMC()
            placedBlocks.add(level to BlockPos(block.x,block.y,block.z))
        }
    }

    @EventHandler
    fun onTick(event: ServerTickStartEvent) {
        if (respawingBlocks.isEmpty()) return

        respawingBlocks.removeAll { block ->
            block.tick--

            if (block.tick < 1) {
                block.level.setBlockAndUpdate(block.pos,block.block)

                return@removeAll true
            }

            return@removeAll false
        }
    }
}