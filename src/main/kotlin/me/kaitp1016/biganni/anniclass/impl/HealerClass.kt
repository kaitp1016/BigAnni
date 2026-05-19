package me.kaitp1016.biganni.anniclass.impl

import io.papermc.paper.datacomponent.DataComponentTypes
import io.papermc.paper.datacomponent.item.UseCooldown
import io.papermc.paper.event.player.PrePlayerAttackEntityEvent
import me.kaitp1016.biganni.PLUGIN_ID
import me.kaitp1016.biganni.anniclass.AnniClass
import me.kaitp1016.biganni.mc
import me.kaitp1016.biganni.utils.ItemUtils.getAnniId
import me.kaitp1016.biganni.utils.ItemUtils.setAnniItem
import me.kaitp1016.biganni.utils.MCUtils.toMC
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.minecraft.network.protocol.game.ClientboundResetScorePacket
import net.minecraft.network.protocol.game.ClientboundSetDisplayObjectivePacket
import net.minecraft.network.protocol.game.ClientboundSetObjectivePacket
import net.minecraft.network.protocol.game.ClientboundSetScorePacket
import net.minecraft.world.item.Items
import net.minecraft.world.scores.DisplaySlot
import net.minecraft.world.scores.Objective
import net.minecraft.world.scores.criteria.ObjectiveCriteria
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import java.util.Optional

object HealerClass: AnniClass(), Listener {
    override val name = "Healer"
    override val shortName = "HEA"
    override val icon = Items.GOLDEN_APPLE
    override val description = arrayOf(
        "味方の体力が常に見える。",
        "左クリックでアビリティを使用すると単体の味方を大幅に回復できる。",
        "右クリックでアビリティを使用すると周囲にいる味方を回復できる。",
    )

    const val BANDAGE_ITEM_ID = "healer_bandage"
    const val BANDAGE_COOLDOWN = 400
    val BANDAGE_COOLDOWN_GROUP = Key.key(PLUGIN_ID,"healer_bandage")

    override fun getDefaultItems(player: Player): MutableList<ItemStack> {
        return super.getDefaultItems(player).also {
            it.add(ItemStack(Material.PAPER).apply {
                uniqueClassItem()
                soulbound()
                setAnniItem(BANDAGE_ITEM_ID)

                setData(DataComponentTypes.USE_COOLDOWN, UseCooldown.useCooldown(BANDAGE_COOLDOWN / 20f).cooldownGroup(BANDAGE_COOLDOWN_GROUP).build())

                editMeta {
                    it.itemName(Component.text("Bandage").color(NamedTextColor.GOLD))
                }
            })

            it.add(ItemStack(Material.WOODEN_SHOVEL).uniqueClassItem().soulbound())
        }
    }

    const val INTERNAL_HP_OBJECTIVE = "HP"

    override fun onSelect(player: Player) {
        val mcPlayer = player.toMC()
        val objective = Objective(mc.scoreboard, INTERNAL_HP_OBJECTIVE, ObjectiveCriteria.DUMMY, net.minecraft.network.chat.Component.literal("HP"), ObjectiveCriteria.RenderType.HEARTS, false, null)
        mcPlayer.connection.send(ClientboundSetObjectivePacket(objective, ClientboundSetObjectivePacket.METHOD_ADD))
        mcPlayer.connection.send(ClientboundSetDisplayObjectivePacket(DisplaySlot.BELOW_NAME, objective))

        val team = mcPlayer.teamColor
        Bukkit.getOnlinePlayers().forEach {
            val target = it.toMC()
            if (target.teamColor == team) {
                mcPlayer.connection.send(ClientboundSetScorePacket(it.name, INTERNAL_HP_OBJECTIVE, target.health.toInt(), Optional.empty(), Optional.empty()))
            }
            else {
                mcPlayer.connection.send(ClientboundResetScorePacket(target.scoreboardName,objective.name))
            }
        }

        super.onSelect(player)
    }

    override fun onUnselect(player: Player) {
        val mcPlayer = player.toMC()
        val objective = Objective(mc.scoreboard, INTERNAL_HP_OBJECTIVE, ObjectiveCriteria.DUMMY, net.minecraft.network.chat.Component.literal("HP"), ObjectiveCriteria.RenderType.INTEGER,false, null)
        mcPlayer.connection.send(ClientboundSetDisplayObjectivePacket(DisplaySlot.BELOW_NAME,null))
        mcPlayer.connection.send(ClientboundSetObjectivePacket(objective, ClientboundSetObjectivePacket.METHOD_REMOVE))

        super.onUnselect(player)
    }

    @EventHandler
    fun onInteract(event: PlayerInteractEvent) {
        if (event.action != Action.RIGHT_CLICK_BLOCK && event.action != Action.RIGHT_CLICK_AIR) return

        val player = event.player
        if (!isSelected(player)) return

        val item = event.item ?: return
        if (item.getAnniId() != BANDAGE_ITEM_ID || player.hasCooldown(item)) return

        val team = player.toMC().teamColor

        player.world.getNearbyPlayers(player.location,5.0).forEach {
            if (it.toMC().teamColor == team) {
                it.addPotionEffect(PotionEffect(PotionEffectType.REGENERATION,80,2))
                it.world.playSound(it.location, Sound.ENTITY_ZOMBIE_VILLAGER_CONVERTED,1f,1f)
            }
        }

        player.setCooldown(BANDAGE_COOLDOWN_GROUP,BANDAGE_COOLDOWN)
    }

    @EventHandler
    fun onAttak(event: PrePlayerAttackEntityEvent) {
        val player = event.player
        if (!isSelected(player)) return

        val item = player.inventory.itemInMainHand
        if (item.getAnniId() != BANDAGE_ITEM_ID || player.hasCooldown(item)) return

        val target = event.attacked
        if (target !is Player || target.toMC().teamColor != player.toMC().teamColor) return

        target.heal(20.0)
        target.world.playSound(target.location, Sound.ENTITY_ZOMBIE_VILLAGER_CONVERTED,1f,1f)

        player.setCooldown(BANDAGE_COOLDOWN_GROUP,BANDAGE_COOLDOWN)

    }

    override fun onUserTick(player: Player) {
        if (!isSelected(player)) return

        val mcPlayer = player.toMC()
        val team = mcPlayer.teamColor

        player.world.getNearbyPlayers(player.location,12.0).forEach {
            if (it.toMC().teamColor == team) {
                mcPlayer.connection.send(ClientboundSetScorePacket(it.name, INTERNAL_HP_OBJECTIVE,it.health.toInt(),Optional.empty(), Optional.empty()))
            }
            else {
                mcPlayer.connection.send(ClientboundResetScorePacket(it.name, INTERNAL_HP_OBJECTIVE))
            }
        }
    }
}