package me.kaitp1016.biganni.anniclass.impl

import com.destroystokyo.paper.event.server.ServerTickStartEvent
import me.kaitp1016.biganni.anniclass.AnniClass
import me.kaitp1016.biganni.events.impl.PacketReciveEvent
import me.kaitp1016.biganni.mc
import net.minecraft.network.protocol.game.ServerboundPlayerAbilitiesPacket
import net.minecraft.world.item.Items
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.inventory.ItemStack
import org.bukkit.util.Vector
import kotlin.math.max

object AcrobatClass: AnniClass(), Listener {
    override val name = "Acrobat"
    override val icon = Items.FEATHER
    override val description = arrayOf(
        "常に落下ダメージを食らわなくなる。",
        "ダブルジャンプをすることができる。"
    )

    override fun getDefaultItems(player: Player): MutableList<ItemStack> {
        return super.getDefaultItems(player).also {
            it.add(ItemStack(Material.BOW).uniqueClassItem().soulbound())
            it.add(ItemStack(Material.ARROW).uniqueClassItem().soulbound().also { it.amount = 8 })
        }
    }

    data class AcrobatCooldown(val player: Player, var tick: Int)
    val cooldowns = mutableListOf<AcrobatCooldown>()

    override fun onSelect(player: Player) {
        player.allowFlight = true
        super.onSelect(player)
    }

    override fun onUnselect(player: Player) {
        player.allowFlight = false
        cooldowns.removeIf { it.player == player }

        super.onUnselect(player)
    }

    @EventHandler
    fun onPlayerTick(event: ServerTickStartEvent) {
        Bukkit.getOnlinePlayers().forEach { player ->
            if (!player.isFlying || !isSelected(player)) return@forEach

            if (!cooldowns.any { it.player == player }) {
                player.velocity = player.location.direction.clone().apply {
                    add(Vector(0.0,0.2,0.0))
                    normalize()
                    y = max(y,0.3)
                }

                player.playSound(player , Sound.ENTITY_WITHER_SHOOT,1f,2f)
                cooldowns.add(AcrobatCooldown(player,200))
            }

            player.allowFlight = false
        }
    }

    @EventHandler
    fun onTick(event: ServerTickStartEvent) {
        if (cooldowns.isEmpty()) return

        cooldowns.removeAll{
            it.tick--
            if (it.tick < 1) {
                it.player.allowFlight = true
                return@removeAll true
            }
            return@removeAll false
        }
    }
}