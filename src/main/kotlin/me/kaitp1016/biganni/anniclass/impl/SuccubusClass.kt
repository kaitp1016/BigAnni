package me.kaitp1016.biganni.anniclass.impl

import com.destroystokyo.paper.event.server.ServerTickStartEvent
import io.papermc.paper.datacomponent.DataComponentTypes
import io.papermc.paper.datacomponent.item.UseCooldown
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
import org.bukkit.attribute.Attribute
import org.bukkit.damage.DamageSource
import org.bukkit.damage.DamageType
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerInteractEntityEvent
import org.bukkit.inventory.ItemStack
import java.util.*

object SuccubusClass: AnniClass(), Listener {
    override val name = "Succubus"
    override val deathMessageName = "SUC"
    override val icon = Items.RED_DYE
    override val description = arrayOf(
        "敵のHPが常に表示される。",
        "アビリティを使用すると敵のHPが30%未満なら即死させる。",
        "それ以外なら敵の体力分だけ貫通ダメージを受ける。"
    )

    const val DRAIN_ITEM_ID = "succubus_drain_item"
    const val DRAIN_COOLDOWN = 1200
    val DRAIN_COOLDOWN_GROUP = Key.key(PLUGIN_ID,"succubus_drain_item")

    const val INTERNAL_HP_OBJECTIVE = "HP"

    override fun getDefaultItems(player: Player): MutableList<ItemStack> {
        return super.getDefaultItems(player).also {
            it.add(ItemStack(Material.WOODEN_SHOVEL).uniqueClassItem().soulbound())

            it.add(ItemStack(Material.RED_DYE).apply {
                uniqueClassItem()
                soulbound()
                setAnniItem(DRAIN_ITEM_ID)

                setData(DataComponentTypes.USE_COOLDOWN, UseCooldown.useCooldown(DRAIN_COOLDOWN / 20f).cooldownGroup(DRAIN_COOLDOWN_GROUP).build())

                editMeta {
                    it.itemName(Component.text("Drain").color(NamedTextColor.GOLD))
                }
            })
        }
    }

    override fun onSelect(player: Player) {
        val mcPlayer = player.toMC()
        val objective = Objective(mc.scoreboard, INTERNAL_HP_OBJECTIVE, ObjectiveCriteria.DUMMY, net.minecraft.network.chat.Component.literal("HP"), ObjectiveCriteria.RenderType.INTEGER, false, null)
        mcPlayer.connection.send(ClientboundSetObjectivePacket(objective, ClientboundSetObjectivePacket.METHOD_ADD))
        mcPlayer.connection.send(ClientboundSetDisplayObjectivePacket(DisplaySlot.BELOW_NAME, objective))

        val team = mcPlayer.teamColor
        Bukkit.getOnlinePlayers().forEach {
            val target = it.toMC()
            if (target.teamColor != team) {
                mcPlayer.connection.send(ClientboundSetScorePacket(it.name, HealerClass.INTERNAL_HP_OBJECTIVE, target.health.toInt(), Optional.empty(), Optional.empty()))
            }
            else {
                mcPlayer.connection.send(ClientboundResetScorePacket(target.scoreboardName,objective.name))
            }
        }

        super.onSelect(player)
    }

    override fun onUnselect(player: Player) {
        val mcPlayer = player.toMC()
        val objective = Objective(mc.scoreboard, INTERNAL_HP_OBJECTIVE, ObjectiveCriteria.DUMMY, net.minecraft.network.chat.Component.literal("HP"), ObjectiveCriteria.RenderType.HEARTS,false, null)
        mcPlayer.connection.send(ClientboundSetDisplayObjectivePacket(DisplaySlot.BELOW_NAME,null))
        mcPlayer.connection.send(ClientboundSetObjectivePacket(objective, ClientboundSetObjectivePacket.METHOD_REMOVE))

        super.onUnselect(player)
    }

    @EventHandler
    fun onInteract(event: PlayerInteractEntityEvent) {
        val player = event.player
        if (!isSelected(player)) return

        val item = event.player.inventory.itemInMainHand
        if (item.getAnniId() != DRAIN_ITEM_ID || player.hasCooldown(item)) return

        val target = event.rightClicked as? Player ?: return
        val maxHealth = target.getAttribute(Attribute.MAX_HEALTH)?.value ?: return
        val health = target.health

        if (health / maxHealth > 0.3) {
            val source = DamageSource.builder(DamageType.GENERIC_KILL)
                .withCausingEntity(target)
                .withDirectEntity(target)
                .build()

            player.damage(health,source)
        }
        else {
            val source = DamageSource.builder(DamageType.GENERIC_KILL)
                .withCausingEntity(player)
                .withDirectEntity(player)
                .build()

            target.damage(10000.0,source)
        }

        player.playSound(player, Sound.ENTITY_BLAZE_AMBIENT,1f,1.4f)
        target.playSound(target, Sound.ENTITY_BLAZE_AMBIENT,1f,1.4f)
        player.setCooldown(DRAIN_COOLDOWN_GROUP, DRAIN_COOLDOWN)
    }

    @EventHandler
    fun onTick(event: ServerTickStartEvent) {
        Bukkit.getOnlinePlayers().forEach { player ->
            if (!isSelected(player)) return@forEach

            val mcPlayer = player.toMC()
            val team = mcPlayer.teamColor

            player.world.getNearbyPlayers(player.location,12.0).forEach {
                if (it.toMC().teamColor != team) {
                    mcPlayer.connection.send(ClientboundSetScorePacket(it.name,INTERNAL_HP_OBJECTIVE,it.health.toInt(),Optional.empty(), Optional.empty()))
                }
                else {
                    mcPlayer.connection.send(ClientboundResetScorePacket(it.name,INTERNAL_HP_OBJECTIVE))
                }
            }
        }
    }
}