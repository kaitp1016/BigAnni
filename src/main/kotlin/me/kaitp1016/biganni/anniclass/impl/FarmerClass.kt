package me.kaitp1016.biganni.anniclass.impl

import com.destroystokyo.paper.event.server.ServerTickStartEvent
import io.papermc.paper.datacomponent.DataComponentTypes
import io.papermc.paper.datacomponent.item.UseCooldown
import me.kaitp1016.biganni.PLUGIN_ID
import me.kaitp1016.biganni.anniclass.AnniClass
import me.kaitp1016.biganni.utils.BlockPosInfo
import me.kaitp1016.biganni.utils.ItemUtils.getAnniId
import me.kaitp1016.biganni.utils.ItemUtils.setAnniItem
import me.kaitp1016.biganni.utils.MCUtils.toMC
import me.kaitp1016.biganni.utils.Scheduler
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.minecraft.core.BlockPos
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.item.Items
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.CropBlock
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.block.data.Ageable
import org.bukkit.entity.Item
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockDropItemEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerItemConsumeEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import kotlin.random.Random
import net.minecraft.world.item.ItemStack as MCItemStack

object FarmerClass: AnniClass(), Listener {
    override val name = "Farmer"
    override val shortName = "FAR"
    override val icon = Items.WHEAT_SEEDS
    override val description = arrayOf(
        "この職業で小麦を収穫すると自動で植えなおされ、その作物は高速で成長する。",
        "また、小麦を収穫すると確率でレアなアイテムがドロップするようになる。",
        "Feastを使用すると自身と周りにいる味方の満腹度を回復する。",
        "Famineを使用すると周りにいる敵の満腹度を減少させる。",
    )

    const val FEAST_ITEM_ID = "farmer_feast"
    const val FEAST_COOLDOWN = 600
    val FEAST_COOLDOWN_GROUP = Key.key(PLUGIN_ID, "farmer_feast")

    const val FAMINE_ITEM_ID = "farmer_famine"
    const val FAMINE_COOLDOWN = 600
    val FAMINE_COOLDOWN_GROUP = Key.key(PLUGIN_ID, "farmer_famine")

    override fun getDefaultItems(player: Player): MutableList<ItemStack> {
        return super.getDefaultItems(player).also {
            it.add(ItemStack(Material.STONE_HOE).uniqueClassItem().soulbound())
            it.add(ItemStack(Material.BONE_MEAL).uniqueClassItem().soulbound().also { it.amount = 15 })

            it.add(ItemStack(Material.GOLDEN_CARROT).apply {
                uniqueClassItem()
                soulbound()
                setAnniItem(FEAST_ITEM_ID)

                setData(DataComponentTypes.USE_COOLDOWN, UseCooldown.useCooldown(FEAST_COOLDOWN / 20f).cooldownGroup(FEAST_COOLDOWN_GROUP).build())

                editMeta {
                    it.itemName(Component.text("Feast").color(NamedTextColor.GOLD))
                }
            })

            it.add(ItemStack(Material.DEAD_BUSH).apply {
                uniqueClassItem()
                soulbound()
                setAnniItem(FAMINE_ITEM_ID)

                setData(DataComponentTypes.USE_COOLDOWN, UseCooldown.useCooldown(FAMINE_COOLDOWN / 20f).cooldownGroup(FAMINE_COOLDOWN_GROUP).build())

                editMeta {
                    it.itemName(Component.text("Famine").color(NamedTextColor.GOLD))
                }
            })
        }
    }

    val faminePlayers = mutableListOf<Player>()

    @EventHandler
    fun onInteract(event: PlayerInteractEvent) {
        if (event.action != Action.RIGHT_CLICK_BLOCK && event.action != Action.RIGHT_CLICK_AIR) return

        val player = event.player
        if (!isSelected(player)) return

        val item = event.item ?: return
        if (player.hasCooldown(item)) return

        val itemId = item.getAnniId()
        if (itemId == FEAST_ITEM_ID) {
            val world = player.world
            val team = player.toMC().teamColor

            world.getNearbyPlayers(player.location, 13.0).forEach { target ->
                if (target.toMC().teamColor != team) return@forEach

                target.removePotionEffect(PotionEffectType.HUNGER)
                target.foodLevel = 20
                target.saturation = 4f
                target.playSound(target, Sound.ENTITY_PLAYER_BURP, 1f, 1f)
            }

            player.setCooldown(FEAST_COOLDOWN_GROUP, FEAST_COOLDOWN)
            event.isCancelled = true
        }
        if (itemId == FAMINE_ITEM_ID) {
            val world = player.world
            val team = player.toMC().teamColor

            world.getNearbyPlayers(player.location, 13.0).forEach { target ->
                if (target.toMC().teamColor == team || BerserkerClass.isUsingAbility(target)) return@forEach

                target.addPotionEffect(PotionEffect(PotionEffectType.HUNGER, 600, 79))
                target.playSound(target, Sound.ENTITY_SKELETON_HORSE_DEATH, 1f, 1f)
                target.sendMessage(Component.text("You are now starving due to ").color(NamedTextColor.DARK_GREEN).append(player.teamDisplayName().append(Component.text("'s ").append(Component.text("famine ability!").color(NamedTextColor.DARK_GREEN)))))
                faminePlayers.add(target)
            }

            player.playSound(player, Sound.ENTITY_SKELETON_HORSE_DEATH, 1f, 1f)
            player.setCooldown(FAMINE_COOLDOWN_GROUP, FAMINE_COOLDOWN)
            event.isCancelled = true
        }
    }

    val plants = BlockPosInfo<Unit>()

    @EventHandler
    fun onBreak(event: BlockBreakEvent) {
        val block = event.block
        val player = event.player
        if (block.type != Material.WHEAT || !isSelected(player)) return

        val level = block.world.toMC()
        val pos = BlockPos(block.x, block.y, block.z)
        val data = block.blockData as? Ageable ?: return

        if (plants.remove(level, pos) == null && data.age != data.maximumAge) {
            return
        }

        Scheduler.scheduleTask(0) {
            level.setBlockAndUpdate(pos, Blocks.WHEAT.defaultBlockState())
        }

        plants[level, pos] = Unit
    }

    @EventHandler(priority = EventPriority.HIGH)
    fun onTick(event: ServerTickStartEvent) {
        plants.removeIf { level, pos, data ->
            val state = level.getBlockState(pos)
            if (state.block != Blocks.WHEAT) return@removeIf true

            if (Random.nextInt(0, 100) == 0) {
                val age = state.getValue(CropBlock.AGE)
                if (age >= CropBlock.MAX_AGE) return@removeIf true

                level.setBlockAndUpdate(pos, state.setValue(CropBlock.AGE, age + 1))
            }

            return@removeIf false
        }

        if (faminePlayers.isNotEmpty()) {
            faminePlayers.removeIf { player ->
                if (player.foodLevel < 7) {
                    player.removePotionEffect(PotionEffectType.HUNGER)
                    return@removeIf true
                }

                return@removeIf !player.hasPotionEffect(PotionEffectType.HUNGER)
            }
        }
    }

    @EventHandler
    fun onBlockDropItem(event: BlockDropItemEvent) {
        val block = event.blockState
        val player = event.player
        if (!isSelected(player)) return

        if (block.type == Material.SHORT_GRASS) {
            if (Random.nextInt(0, 5) == 2) {
                val level = block.world.toMC()
                event.items.add(ItemEntity(level, block.x + 0.5, block.y + 0.5, block.z + 0.5, MCItemStack(Items.CARROT)).apply {
                    setDefaultPickUpDelay()
                }.bukkitEntity as Item)
            }
        }
        if (block.type == Material.WHEAT) {
            val data = block.blockData as? Ageable ?: return
            if (data.age != data.maximumAge) return

            val level = block.world.toMC()

            if (Random.nextInt(0, 300) == 77) {
                event.items.add(ItemEntity(level, block.x + 0.5, block.y + 0.5, block.z + 0.5, MCItemStack(Items.APPLE)).bukkitEntity as Item)
            }
            if (Random.nextInt(0, 30) == 27) {
                event.items.add(ItemEntity(level, block.x + 0.5, block.y + 0.5, block.z + 0.5, MCItemStack(Items.RAW_GOLD)).bukkitEntity as Item)
            }
            if (Random.nextInt(0, 30) == 12) {
                event.items.add(ItemEntity(level, block.x + 0.5, block.y + 0.5, block.z + 0.5, MCItemStack(Items.RAW_IRON)).bukkitEntity as Item)
            }
            if (Random.nextInt(0, 70) == 45) {
                event.items.add(ItemEntity(level, block.x + 0.5, block.y + 0.5, block.z + 0.5, MCItemStack(Items.NETHER_WART)).bukkitEntity as Item)
            }
            if (Random.nextInt(0, 70) == 62) {
                event.items.add(ItemEntity(level, block.x + 0.5, block.y + 0.5, block.z + 0.5, MCItemStack(Items.SOUL_SAND)).bukkitEntity as Item)
            }
            if (Random.nextInt(0, 150) == 32) {
                event.items.add(ItemEntity(level, block.x + 0.5, block.y + 0.5, block.z + 0.5, MCItemStack(Items.GHAST_TEAR)).bukkitEntity as Item)
            }
            if (Random.nextInt(0, 150) == 53) {
                event.items.add(ItemEntity(level, block.x + 0.5, block.y + 0.5, block.z + 0.5, MCItemStack(Items.BOOK)).bukkitEntity as Item)
            }
            if (Random.nextInt(0, 200) == 132) {
                event.items.add(ItemEntity(level, block.x + 0.5, block.y + 0.5, block.z + 0.5, MCItemStack(Items.IRON_HOE)).bukkitEntity as Item)
            }
        }
    }

    @EventHandler
    fun onConsume(event: PlayerItemConsumeEvent) {
        val player = event.player
        if (!isSelected(player)) return

        val item = event.item
        if (!item.hasData(DataComponentTypes.FOOD)) return

        if (Random.nextInt(0, 100) < 30) {
            player.heal(2.0)
            player.saturation += 2f
        }
    }
}