package me.kaitp1016.biganni.features

import com.destroystokyo.paper.event.server.ServerTickStartEvent
import me.kaitp1016.biganni.mc
import net.minecraft.core.component.DataComponents
import net.minecraft.network.chat.Component
import net.minecraft.world.item.ItemStack
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import kotlin.jvm.optionals.getOrNull

object CooldownVisualizer: Listener {
    const val MINIUM_RENDER_COOLDOWN = 5f
    const val COOLDOWN_PREFFIX = "§|§_"

    @EventHandler
    fun onTick(event: ServerTickStartEvent) {
        mc.playerList.players.forEach { player ->
            val cooldowns = player.cooldowns
            val tickCount = cooldowns.tickCount

            player.inventory.forEach { item ->
                val useCooldown = item.get(DataComponents.USE_COOLDOWN) ?: return@forEach
                val group = useCooldown.cooldownGroup.getOrNull() ?: return@forEach
                val cooldown = cooldowns.cooldowns[group]
                if (useCooldown.seconds < MINIUM_RENDER_COOLDOWN) return@forEach

                if (cooldown == null || tickCount > cooldown.endTime) {
                    updateName(item, "READY", 0xFFAA00)
                } else {
                    val cooldown = cooldown.endTime - cooldowns.tickCount
                    updateName(item, (cooldown / 20).toString(), 0xFF5555)
                }
            }
        }
    }

    fun updateName(item: ItemStack, name: String, color: Int) {
        val dataComponent = if (item.has(DataComponents.CUSTOM_NAME)) DataComponents.CUSTOM_NAME else DataComponents.ITEM_NAME
        val component = item.get(dataComponent)?.copy() ?: return

        val siblings = component.siblings
        for (sibling in siblings) {
            val string = sibling.string
            if (!string.startsWith(COOLDOWN_PREFFIX)) continue
            if (string == name) return

            siblings.remove(sibling)
            break
        }

        component.append(Component.literal("$COOLDOWN_PREFFIX $name").withColor(color))

        item.set(dataComponent, component)
    }
}