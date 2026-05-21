package me.kaitp1016.biganni.features

import me.kaitp1016.biganni.utils.MCUtils.toMC
import net.minecraft.tags.BlockTags
import net.minecraft.tags.ItemTags
import net.minecraft.world.item.Items
import org.bukkit.GameMode
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent

object RequiredTool: Listener {
    val blockTags = listOf(
        BlockTags.MINEABLE_WITH_PICKAXE,
        BlockTags.MINEABLE_WITH_AXE,
        BlockTags.MINEABLE_WITH_HOE,
        BlockTags.MINEABLE_WITH_SHOVEL,
    )

    val itemTags = mapOf(
        BlockTags.MINEABLE_WITH_PICKAXE to ItemTags.PICKAXES,
        BlockTags.MINEABLE_WITH_AXE to ItemTags.AXES,
        BlockTags.MINEABLE_WITH_HOE to ItemTags.HOES,
        BlockTags.MINEABLE_WITH_SHOVEL to ItemTags.SHOVELS,
    )

    @EventHandler(priority = EventPriority.LOWEST)
    fun onBreak(event: BlockBreakEvent) {
        val player = event.player
        if (player.gameMode == GameMode.CREATIVE) return

        val block = event.block.toMC()
        val item = event.player.inventory.itemInMainHand.toMC()

        if (block.`is`(BlockTags.WOOL)) {
            if (item?.item != Items.SHEARS) {
                event.isCancelled = true
            }
            return
        }

        if (item?.`is`(ItemTags.SWORDS) == true && block.`is`(BlockTags.SWORD_EFFICIENT)) {
            return
        }

        val blockTag = blockTags.find { block.`is`(it) } ?: return
        val itemTag = itemTags[blockTag] ?: return

        if (item?.`is`(itemTag) != true) {
            event.isCancelled = true
        }
    }
}