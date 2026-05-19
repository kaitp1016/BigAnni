package me.kaitp1016.biganni.anniclass.impl

import me.kaitp1016.biganni.anniclass.AnniClass
import me.kaitp1016.biganni.game.Game
import me.kaitp1016.biganni.game.Game.updateNexusHealth
import me.kaitp1016.biganni.utils.MCUtils.toMC
import net.kyori.adventure.text.Component
import net.minecraft.world.item.Items
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Player
import org.bukkit.event.Listener
import org.bukkit.inventory.ItemStack
import kotlin.random.Random

object HandymanClass: AnniClass(), Listener {
    override val icon = Items.ANVIL
    override val name = "Handyman"
    override val shortName = "HDY"
    override val description = arrayOf(
        "ネクサスを削るごとに自身が所属してるチームのネクサスの体力を確率で修復する。",
        "フェーズが進むごとに修復できる確率が下がっていく。",
    )

    override fun getDefaultItems(player: Player): MutableList<ItemStack> {
        return super.getDefaultItems(player).also {
            it.removeIf { it.type == Material.WOODEN_PICKAXE }
            it.add(ItemStack(Material.WOODEN_PICKAXE).apply {
                uniqueClassItem()
                soulbound()
                addEnchantment(Enchantment.EFFICIENCY,1)
            })

        }
    }

    fun onMineNexus(player: Player) {
        if (!isSelected(player)) return

        val chance = when(Game.phase) {
            2 -> 25
            3 -> 20
            4 -> 15
            5 -> 10
            else -> return
        }

        val team = Game.getTeam(player) ?: return

        if (Random.nextInt(0,100) <= chance) {
            team.health += 1
            updateNexusHealth(team)

            val message = Component.text("${player.name} §ehas repaired your nexus with the ${this.name} class")

            Bukkit.getOnlinePlayers().forEach { player ->
                if (player.toMC().team?.name?.equals(team.name,true) == true) {
                    player.sendMessage(message)
                    player.playSound(player, Sound.BLOCK_NOTE_BLOCK_HARP,1f,1.68f)
                }
            }
        }
    }
}