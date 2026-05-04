package me.kaitp1016.biganni.features

import me.kaitp1016.biganni.utils.ItemUtils.getAnniId
import me.kaitp1016.biganni.utils.ItemUtils.setAnniItem
import me.kaitp1016.biganni.utils.MCUtils.toMC
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.TextColor
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.effect.MobEffects
import net.minecraft.world.entity.item.ItemEntity
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.craftbukkit.inventory.CraftItemStack
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.inventory.ItemStack

object DelayingBlock: Listener {
    const val DELAYING_BLOCK_ID = "anni_delaying_block"
    const val DELAY_DISTANCE = 10

    // IntはTeamColor
    val delayingBlocks = mutableMapOf<ServerLevel, HashMap<BlockPos, ServerPlayer>>()

    @EventHandler(priority = EventPriority.LOWEST)
    fun onPlace(event: BlockPlaceEvent) {
        val item = event.itemInHand
        if (event.isCancelled || item.getAnniId() != DELAYING_BLOCK_ID) return

        val block = event.block
        val player = event.player.toMC()
        val level = player.level()
        val pos = BlockPos(block.x,block.y,block.z)

        val blocksInLevel = delayingBlocks.getOrPut(level) { HashMap() }
        blocksInLevel[pos] = player
    }

    @EventHandler
    fun onBreak(event: BlockBreakEvent) {
        val player = event.player.toMC()
        val blocksInLevel = delayingBlocks[player.level()] ?: return
        val block = event.block
        val pos = BlockPos(block.x,block.y,block.z)
        val team = player.teamColor

        val miningBlock = blocksInLevel[pos]
        if (miningBlock != null && miningBlock != player) {
            if (miningBlock.teamColor == team) {
                event.isCancelled = true
                return
            }
            blocksInLevel.remove(pos)
            event.isCancelled = true

            player.level().addFreshEntity(ItemEntity(player.level(),block.x + 0.5,block.y + 0.5,block.z + 0.5, CraftItemStack.asNMSCopy(createItem() as CraftItemStack)))

            return
        }

        if (!blocksInLevel.any { it.value.teamColor != team && it.key.distManhattan(pos) < DELAY_DISTANCE }) return

        player.addEffect(MobEffectInstance(MobEffects.MINING_FATIGUE,140,1))
        player.bukkitEntity.playSound(player.bukkitEntity, Sound.ENTITY_ELDER_GUARDIAN_CURSE,1f,1f)
    }

    fun isDelayingBlock(level:ServerLevel,pos: BlockPos): Boolean {
        return delayingBlocks[level]?.contains(pos) == true
    }

    fun createItem(): ItemStack {
        return ItemStack(Material.SEA_LANTERN).apply {
            editMeta {
                it.itemName(Component.text("Delaying Block").color(NamedTextColor.AQUA))
            }

            setAnniItem(DELAYING_BLOCK_ID)
        }
    }
}