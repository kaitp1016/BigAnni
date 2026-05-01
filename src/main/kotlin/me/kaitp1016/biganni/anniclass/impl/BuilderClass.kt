package me.kaitp1016.biganni.anniclass.impl

import io.papermc.paper.datacomponent.DataComponentTypes
import io.papermc.paper.datacomponent.item.UseCooldown
import me.kaitp1016.biganni.PLUGIN_ID
import me.kaitp1016.biganni.anniclass.AnniClass
import me.kaitp1016.biganni.utils.ItemUtils.getAnniItemId
import me.kaitp1016.biganni.utils.ItemUtils.setAnniItem
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.minecraft.world.item.Items
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.ItemStack
import kotlin.random.Random

object BuilderClass: AnniClass(), Listener {
    override val name = "Builder"
    override val icon = Items.BRICK
    override val description = arrayOf(
        "アビリティを使用することで建築資材が手に入る。",
    )

    const val RESOURCE_DROP_ITEM_ID = "builder_resource_drop"
    const val RESOURCE_DROP_COOLDOWN = 1800
    val RESOURCE_DROP_COOLDOWN_GROUP = Key.key(PLUGIN_ID,"builder_resource_drop")

    override fun getDefaultItems(player: Player): MutableList<ItemStack> {
        return super.getDefaultItems(player).also {
            it.add(ItemStack(Material.BOOK).apply {
                uniqueClassItem()
                soulbound()
                setAnniItem(RESOURCE_DROP_ITEM_ID)

                setData(DataComponentTypes.USE_COOLDOWN, UseCooldown.useCooldown(RESOURCE_DROP_COOLDOWN / 20f).cooldownGroup(RESOURCE_DROP_COOLDOWN_GROUP).build())

                editMeta {
                    it.itemName(Component.text("Resource Drop").color(NamedTextColor.GOLD))
                }
            })

            it.add(ItemStack(Material.WOODEN_SHOVEL).uniqueClassItem().soulbound())
        }
    }

    @EventHandler
    fun onInteract(event: PlayerInteractEvent) {
        val player = event.player
        if (!isSelected(player)) return

        val item = event.item ?: return
        if (item.getAnniItemId() != RESOURCE_DROP_ITEM_ID || player.hasCooldown(item)) return

        player.openInventory(Bukkit.createInventory(player,27,Component.text("Resource Drop")).apply {
            setItem(Random.nextInt(0,26),ItemStack(Material.WHITE_WOOL).also { it.amount = Random.nextInt(10,64) })
            setItem(Random.nextInt(0,26),ItemStack(Material.OAK_PLANKS).also { it.amount = Random.nextInt(10,64) })
            setItem(Random.nextInt(0,26),ItemStack(Material.STONE).also { it.amount = Random.nextInt(10,64) })
            setItem(Random.nextInt(0,26),ItemStack(Material.DIRT).also { it.amount = Random.nextInt(10,64) })
            setItem(Random.nextInt(0,26),ItemStack(Material.BRICKS).also { it.amount = Random.nextInt(10,64) })
            setItem(Random.nextInt(0,26),ItemStack(Material.GLASS).also { it.amount = Random.nextInt(10,64) })
            setItem(Random.nextInt(0,26),ItemStack(Material.OAK_FENCE).also { it.amount = Random.nextInt(10,64) })
        })

        player.setCooldown(RESOURCE_DROP_COOLDOWN_GROUP,RESOURCE_DROP_COOLDOWN)
    }
}