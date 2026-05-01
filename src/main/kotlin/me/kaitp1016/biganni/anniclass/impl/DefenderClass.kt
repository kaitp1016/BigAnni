package me.kaitp1016.biganni.anniclass.impl

import io.papermc.paper.datacomponent.DataComponentTypes
import io.papermc.paper.datacomponent.item.UseCooldown
import me.kaitp1016.biganni.PLUGIN_ID
import me.kaitp1016.biganni.anniclass.AnniClass
import me.kaitp1016.biganni.utils.ItemUtils.getAnniId
import me.kaitp1016.biganni.utils.ItemUtils.setAnniItem
import me.kaitp1016.biganni.utils.MCUtils.toMC
import me.kaitp1016.biganni.utils.Scheduler
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.item.Items
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack

object DefenderClass: AnniClass(), Listener {
    override val name = "Defender"
    override val icon = Items.PRISMARINE_SHARD
    override val description = arrayOf(
        "アビリティを使用することでアラートを設置できる。",
        "自身が設置したアラートに敵が触れると音が鳴る。",
    )

    const val ALERT_ITEM_ID = "defender_alert_item"
    const val ALERT_COOLDOWN = 400
    val IMMOBILIZE_COOLDOWN_GROUP = Key.key(PLUGIN_ID,"defender_alert_item")

    override fun getDefaultArmors(player: Player): MutableMap<EquipmentSlot, ItemStack> {
        return super.getDefaultArmors(player).apply {
            this[EquipmentSlot.CHEST] = ItemStack(Material.CHAINMAIL_CHESTPLATE).soulbound().uniqueClassItem()
        }
    }

    override fun getDefaultItems(player: Player): MutableList<ItemStack> {
        return super.getDefaultItems(player).also {
            it.add(ItemStack(Material.WOODEN_SHOVEL).uniqueClassItem().soulbound())

            it.add(ItemStack(Material.PRISMARINE_SHARD).apply {
                uniqueClassItem()
                soulbound()
                setAnniItem(ALERT_ITEM_ID)

                setData(DataComponentTypes.USE_COOLDOWN, UseCooldown.useCooldown(ALERT_COOLDOWN / 20f).cooldownGroup(IMMOBILIZE_COOLDOWN_GROUP).build())

                editMeta {
                    it.itemName(Component.text("Alert Item").color(NamedTextColor.GOLD))
                }
            })
        }
    }

    @EventHandler
    fun onInteract(event: PlayerInteractEvent) {
        if (event.action != Action.RIGHT_CLICK_BLOCK && event.action != Action.RIGHT_CLICK_AIR) return

        val player = event.player
        if (!isSelected(player)) return

        val item = event.item ?: return
        if (item.getAnniId() != ALERT_ITEM_ID || player.hasCooldown(item)) return

        val level = player.world.toMC()

        val alertItem = AlertItem(player.toMC(),level,player.x,player.y,player.z)
        level.addFreshEntity(alertItem)

        player.setCooldown(IMMOBILIZE_COOLDOWN_GROUP, ALERT_COOLDOWN)
    }

    class AlertItem: ItemEntity {
        val spawner: net.minecraft.world.entity.player.Player
        val serverLevel: ServerLevel

        constructor(spawner: net.minecraft.world.entity.player.Player, level: ServerLevel, x: Double, y: Double, z: Double):super(level,x,y,z, net.minecraft.world.item.ItemStack(Items.PRISMARINE_SHARD)) {
            this.spawner = spawner
            this.serverLevel = level
            this.pickupDelay = 20
        }

        override fun playerTouch(player: net.minecraft.world.entity.player.Player) {
            if (player.teamColor == spawner.teamColor || this.pickupDelay > 0) return

            kill(serverLevel)

            repeat(10) {
                Scheduler.scheduleTask(it * 5) {
                    val spawner = spawner.bukkitEntity as Player
                    spawner.playSound(spawner,Sound.BLOCK_NOTE_BLOCK_PLING,1f,1f)
                }
            }
        }
    }
}