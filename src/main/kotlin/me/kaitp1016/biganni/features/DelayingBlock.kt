package me.kaitp1016.biganni.features

import me.kaitp1016.biganni.utils.ItemUtils.getAnniId
import me.kaitp1016.biganni.utils.ItemUtils.setAnniItem
import me.kaitp1016.biganni.utils.MCUtils.toMC
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.effect.MobEffects
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.item.Items
import net.minecraft.world.level.block.Blocks
import org.bukkit.Sound
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.inventory.ItemStack

object DelayingBlock: Listener {
    const val DELAYING_BLOCK_ID = "anni_delaying_block"
    const val DELAY_EFFECT_DISTANCE = 5
    const val DELAY_PLACE_DISTANCE = 10

    // IntはTeamColor
    val delayingBlocks = mutableMapOf<ServerLevel, HashMap<BlockPos, ServerPlayer>>()

    @EventHandler(priority = EventPriority.HIGH)
    fun onPlace(event: BlockPlaceEvent) {
        val item = event.itemInHand
        if (event.isCancelled || item.getAnniId() != DELAYING_BLOCK_ID) return

        val block = event.block
        val player = event.player.toMC()
        val level = player.level()
        val pos = BlockPos(block.x,block.y,block.z)

        val blocksInLevel = delayingBlocks.getOrPut(level) { HashMap() }
        val team = player.teamColor
        if (blocksInLevel.any { it.value.teamColor == team && it.key.distManhattan(pos) < DELAY_PLACE_DISTANCE }) {
            player.bukkitEntity.sendMessage("近くにDelaying Blockがあるため設置できません!")
            event.isCancelled = true
            return
        }

        blocksInLevel[pos] = player
    }

    @EventHandler(priority = EventPriority.HIGH)
    fun onBreak(event: BlockBreakEvent) {
        if (event.isCancelled) return

        val player = event.player.toMC()
        val blocksInLevel = delayingBlocks[player.level()] ?: return
        val block = event.block
        val pos = BlockPos(block.x,block.y,block.z)
        val team = player.teamColor

        val miningBlock = blocksInLevel[pos]
        if (miningBlock != null) {
            if (miningBlock != player && miningBlock.teamColor == team) {
                event.isCancelled = true
                return
            }

            blocksInLevel.remove(pos)
            event.isCancelled = true

            val level = player.level()
            level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState())
            level.addFreshEntity(ItemEntity(player.level(),block.x + 0.5,block.y + 0.5,block.z + 0.5, createItem().toMC()!!).apply {
                setDefaultPickUpDelay()
            })

            return
        }

        if (!blocksInLevel.any { it.value.teamColor != team && it.key.distManhattan(pos) < DELAY_EFFECT_DISTANCE }) return

        val bukkitPlayer = player.bukkitEntity
        player.addEffect(MobEffectInstance(MobEffects.MINING_FATIGUE,140,1))
        bukkitPlayer.world.playSound(bukkitPlayer, Sound.ENTITY_ELDER_GUARDIAN_CURSE,1f,2f)
    }

    fun isDelayingBlock(level:ServerLevel,pos: BlockPos): Boolean {
        return delayingBlocks[level]?.contains(pos) == true
    }

    fun createItem(): ItemStack {
        return net.minecraft.world.item.ItemStack(Items.SEA_LANTERN).bukkitStack.apply {
            editMeta {
                it.itemName(Component.text("Delaying Block").color(NamedTextColor.AQUA))
            }

            setAnniItem(DELAYING_BLOCK_ID)
        }
    }
}