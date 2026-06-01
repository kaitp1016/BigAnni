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
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerExpChangeEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.ItemStack

object MinerClass: AnniClass(), Listener {
    override val icon = Items.STONE_PICKAXE
    override val name = "Miner"
    override val shortName = "MIN"
    override val description = arrayOf(
        "この職業の時は常に鉱石の獲得量が増える。",
        "アビリティを使用することで鉱石の獲得量がさらに増える。",
        "鉱石を掘ったときに溶鉱炉を獲得し、アビリティを使用してる場合は石炭を確率で獲得する。",
    )

    const val GOLD_RUSH_ITEM_ID = "miner_gold_rush"
    const val GOLD_RUSH_COOLDOWN = 2400
    val GOLD_RUSH_COOLDOWN_GROUP = Key.key(PLUGIN_ID,"miner_gold_rush")

    const val GOLD_RUSH_TIME = 400

    override fun getDefaultItems(player: Player): MutableList<ItemStack> {
        return super.getDefaultItems(player).also {
            it.add(ItemStack(Material.GOLD_NUGGET).apply {
                uniqueClassItem()
                soulbound()
                setAnniItem(GOLD_RUSH_ITEM_ID)

                setData(DataComponentTypes.USE_COOLDOWN, UseCooldown.useCooldown(GOLD_RUSH_COOLDOWN / 20f).cooldownGroup(GOLD_RUSH_COOLDOWN_GROUP).build())

                editMeta {
                    it.itemName(Component.text("Gold Rush").color(NamedTextColor.GOLD))
                }
            })

            it.removeIf { it.type == Material.WOODEN_PICKAXE }
            it.add(ItemStack(Material.STONE_PICKAXE).apply {
                uniqueClassItem()
                soulbound()
                addEnchantment(Enchantment.EFFICIENCY,1)
            })

        }
    }

    override fun onUnselect(player: Player) {
        super.onUnselect(player)

        blastFurnaceCounts.remove(player)
    }

    data class GoldRushAbility(val player: Player, var time: Int)
    data class BlastFurnaceCount(val player: Player, var minedCount: Int)

    val goldrushes = mutableListOf<GoldRushAbility>()
    val blastFurnaceCounts = mutableMapOf<Player,BlastFurnaceCount>()

    @EventHandler
    fun onInteract(event: PlayerInteractEvent) {
        val player = event.player
        if (!isSelected(player)) return

        val item = event.item ?: return
        if (item.getAnniId() != GOLD_RUSH_ITEM_ID || player.hasCooldown(item)) return

        goldrushes.add(GoldRushAbility(player,GOLD_RUSH_TIME))
        player.setCooldown(GOLD_RUSH_COOLDOWN_GROUP,GOLD_RUSH_COOLDOWN)
    }

    @EventHandler
    fun onTick(event: ServerTickStartEvent) {
        if (goldrushes.isEmpty()) return

        goldrushes.removeIf {
            it.time--
            return@removeIf it.time <= 0
        }
    }

    @EventHandler
    fun onExp(event:PlayerExpChangeEvent) {
        val player = event.player
        if (!isSelected(player)) return

        val multiply = if (goldrushes.any { it.player == player }) 3 else 2
        event.amount *= multiply
    }

    val ores = listOf(Material.DIAMOND_ORE, Material.COAL_ORE, Material.IRON_ORE, Material.LAPIS_ORE, Material.GOLD_ORE, Material.EMERALD_ORE, Material.REDSTONE_ORE, Material.COPPER_ORE,)

    @EventHandler
    fun onHit(event: PlayerInteractEvent) {
        val player = event.player
        if (event.action != Action.LEFT_CLICK_BLOCK || !isSelected(player) || !ores.contains(event.clickedBlock?.type) || goldrushes.none { it.player == player }) return

        val face = event.blockFace
        val pos = event.clickedBlock?.location?.apply {
            add(0.5,0.5,0.5)
            add(face.modX * 0.65, face.modY * 0.65,face.modZ * 0.65)
        } ?: return

        Particle.DUST.builder()
            .location(pos.world,pos.x,pos.y,pos.z)
            .receivers(12,true)
            .count(1)
            .color(255,220,220,50)
            .spawn()
    }

    fun getMultiply(player: Player): Int {
        if (!isSelected(player)) return 1
        if (goldrushes.any { it.player == player }) return 3
        return 2
    }

    fun onMine(player: Player) {
        if (!isSelected(player)) return

        if (goldrushes.any { it.player == player }) {
            player.give(ItemStack(Material.COAL))
        }

        val blastFurnaceCount = blastFurnaceCounts.getOrPut(player) {
            player.give(ItemStack(Material.BLAST_FURNACE))
            BlastFurnaceCount(player, 0)
        }

        blastFurnaceCount.minedCount++
        if (blastFurnaceCount.minedCount > 8) {
            player.give(ItemStack(Material.BLAST_FURNACE))
            blastFurnaceCount.minedCount = 0
        }
    }
}