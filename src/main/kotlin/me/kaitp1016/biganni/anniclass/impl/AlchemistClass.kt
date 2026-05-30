package me.kaitp1016.biganni.anniclass.impl

import com.destroystokyo.paper.event.server.ServerTickStartEvent
import io.papermc.paper.datacomponent.DataComponentTypes
import io.papermc.paper.datacomponent.item.UseCooldown
import me.kaitp1016.biganni.PLUGIN_ID
import me.kaitp1016.biganni.anniclass.AnniClass
import me.kaitp1016.biganni.game.Game
import me.kaitp1016.biganni.utils.ItemUtils.getAnniId
import me.kaitp1016.biganni.utils.ItemUtils.setAnniItem
import me.kaitp1016.biganni.utils.MCUtils.toMC
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.minecraft.core.BlockPos
import net.minecraft.core.Holder
import net.minecraft.core.component.DataComponents
import net.minecraft.network.chat.Style
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.effect.MobEffects
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.LightningBolt
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.item.Item
import net.minecraft.world.item.Items
import net.minecraft.world.item.alchemy.Potion
import net.minecraft.world.item.alchemy.PotionContents
import net.minecraft.world.item.alchemy.Potions
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.entity.BrewingStandBlockEntity
import net.minecraft.world.phys.AABB
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.block.data.Levelled
import org.bukkit.craftbukkit.block.CraftBlock
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.entity.PotionSplashEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.ItemStack
import java.util.*
import kotlin.jvm.optionals.getOrNull
import kotlin.math.min
import kotlin.random.Random

object AlchemistClass: AnniClass(), Listener {
    override val name = "Alchemist"
    override val shortName = "ALC"
    override val icon = Items.BREWING_STAND
    override val description = arrayOf(
        "高速で醸造ができる醸造台が初期装備に含まれている。",
        "大釜を使用することで強化されたポーションを作ることができる。",
        "アビリティを使用すると確率でランダムなポーションの素材が手に入る。",
    )

    const val ALCHEMIST_STAND_ITEM_ID = "alchemist_stand"

    const val TOME_ITEM_ID = "alchemist_tome"
    const val TOME_COOLDOWN = 1800
    val TOME_COOLDOWN_GROUP = Key.key(PLUGIN_ID,"alchemist_tome")

    override fun getDefaultItems(player: Player): MutableList<ItemStack> {
        return super.getDefaultItems(player).also {
            it.add(createAlchemistStand())

            it.add(ItemStack(Material.ENCHANTED_BOOK).apply {
                uniqueClassItem()
                soulbound()
                setAnniItem(TOME_ITEM_ID)

                setData(DataComponentTypes.USE_COOLDOWN, UseCooldown.useCooldown(TOME_COOLDOWN / 20f).cooldownGroup(TOME_COOLDOWN_GROUP).build())

                editMeta {
                    it.itemName(Component.text("Alchemist's Tome").color(NamedTextColor.GOLD))
                }
            })

        }
    }

    data class PlacedStand(val level: Level, val pos: BlockPos, val owner: Player)

    val stands = mutableListOf<PlacedStand>()

    @EventHandler(priority = EventPriority.HIGH)
    fun onPlace(event: BlockPlaceEvent) {
        if (event.isCancelled) return

        val player = event.player
        if (!isSelected(player)) return

        val item = event.itemInHand
        if (item.getAnniId() != ALCHEMIST_STAND_ITEM_ID) return

        if (stands.any { it.owner == player }) {
            player.sendMessage("これは2個以上置けません!")
            event.isCancelled = true
            return
        }

        val block = event.block
        val level = player.toMC().level()
        val pos = BlockPos(block.x,block.y,block.z)
        level.setBlockAndUpdate(pos, Blocks.BREWING_STAND.defaultBlockState())

        stands.add(PlacedStand(level,pos,player))
    }

    @EventHandler
    fun onBreak(event: BlockBreakEvent) {
        val block = event.block
        if (block.type != Material.BREWING_STAND) return

        val pos = BlockPos(block.x,block.y,block.z)
        val level = block.world.toMC()
        val stand = stands.find { it.pos == pos && it.level == level } ?: return

        event.isCancelled = true

        level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState())

        if (isSelected(stand.owner)) {
            stand.owner.give(createAlchemistStand())
        }
    }

    @EventHandler
    fun onTick(event: ServerTickStartEvent) {
        if (stands.isEmpty()) return

        stands.removeIf { stand ->
            val state = stand.level.getBlockState(stand.pos)
            if (state.block != Blocks.BREWING_STAND) return@removeIf true

            val entity = stand.level.getBlockEntity(stand.pos) as? BrewingStandBlockEntity ?: return@removeIf false
            BrewingStandBlockEntity.serverTick(stand.level,stand.pos,state,entity)

            return@removeIf false
        }
    }

    val tomeItems = arrayOf(
        32 to Material.NETHER_WART,
        32 to Material.FERMENTED_SPIDER_EYE,
        28 to Material.GLISTERING_MELON_SLICE,
        28 to Material.GOLDEN_CARROT,
        28 to Material.SUGAR,
        28 to Material.SPIDER_EYE,
        28 to Material.MAGMA_CREAM,
        15 to Material.GLOWSTONE_DUST,
        32 to Material.NETHER_WART,
        7 to Material.GHAST_TEAR,
        3 to Material.BLAZE_POWDER,
        3 to Material.GUNPOWDER,
        15 to Material.ROTTEN_FLESH,
        15 to Material.POISONOUS_POTATO,
        15 to Material.SNOWBALL,
        15 to Material.STRING,
    )

    data class EnhancedPotionData(val name:String,val requiredPotion: Holder.Reference<Potion>, val material: Item, val effects: List<MobEffectInstance>)

    val enhancedPotions = listOf(
        EnhancedPotionData("Speed",Potions.STRONG_SWIFTNESS , Items.SUGAR,listOf(MobEffectInstance(MobEffects.SPEED,1600,2))),
        EnhancedPotionData("Invisibility",Potions.INVISIBILITY, Items.GOLDEN_CARROT,listOf(MobEffectInstance(MobEffects.INVISIBILITY,24000,0))),
        EnhancedPotionData("Regeneration",Potions.STRONG_REGENERATION, Items.GHAST_TEAR,listOf(MobEffectInstance(MobEffects.REGENERATION,1600,1))),
        EnhancedPotionData("Slowness",Potions.SLOWNESS, Items.FERMENTED_SPIDER_EYE,listOf(MobEffectInstance(MobEffects.SLOWNESS,1800,2))),
        EnhancedPotionData("Strength",Potions.STRONG_STRENGTH, Items.BLAZE_POWDER,listOf(MobEffectInstance(MobEffects.STRENGTH,1600,2))),
        EnhancedPotionData("Weakness",Potions.WEAKNESS, Items.FERMENTED_SPIDER_EYE,listOf(MobEffectInstance(MobEffects.WEAKNESS,3600,2))),
    )

    @EventHandler
    fun onInteract(event: PlayerInteractEvent) {
        val block = event.clickedBlock
        // アルケミストスタンド
        if (event.action == Action.RIGHT_CLICK_BLOCK && block?.type == Material.BREWING_STAND) {
            val pos = BlockPos(block.x,block.y,block.z)
            val level = block.world.toMC()
            val stand = stands.find { it.pos == pos && it.level == level } ?: return

            val user = event.player
            val owner = stand.owner
            if (owner == user) return

            if (user.toMC().teamColor == owner.toMC().teamColor) {
                user.sendMessage("他の人のAlchemist's Standは使えません!")
                event.isCancelled = true
                return
            }
            else {
                user.breakBlock(block)
            }

            return
        }

        // 大釜クラフト
        val player = event.player
        if (!isSelected(player)) return

        if (event.action == Action.RIGHT_CLICK_BLOCK && block?.type == Material.WATER_CAULDRON) {
            val data = block.blockData
            if (data !is Levelled || data.level != data.maximumLevel) return

            val world = block.world
            val droppedItems = world.toMC().getEntitiesOfClass(ItemEntity::class.java, AABB((block as CraftBlock).position)).map { it.item }
            val potion = enhancedPotions.find { potion -> droppedItems.any { it.get(DataComponents.POTION_CONTENTS)?.potion?.getOrNull() == potion.requiredPotion} && droppedItems.any { it.item == potion.material } }
            if (potion == null) {
                return
            }

            val isSplash = droppedItems.any { it.item == Items.GUNPOWDER }

            val item = net.minecraft.world.item.ItemStack(if (isSplash) Items.SPLASH_POTION else Items.POTION).apply {
                set(DataComponents.POTION_CONTENTS, PotionContents(Optional.empty(),Optional.empty(),potion.effects,Optional.empty()))
                set(DataComponents.CUSTOM_NAME, net.minecraft.network.chat.Component.literal("§bEnhanced Potion of §6${potion.name}").withStyle(Style.EMPTY.withItalic(false)))
            }.bukkitStack.soulbound().uniqueClassItem()

            droppedItems.forEach { it.count = 0 }
            player.give(item)

            val level = world.toMC()
            level.addFreshEntity(LightningBolt(EntityType.LIGHTNING_BOLT,level).apply {
                visualOnly = true
                setPos(block.x + 0.5, block.y.toDouble(), block.z + 0.5)
            })

            event.isCancelled = true
        }

        // Alchemist Tome
        val item = event.item ?: return
        if (item.getAnniId() != TOME_ITEM_ID || player.hasCooldown(item)) return

        player.openInventory(Bukkit.createInventory(player,27,Component.text("Alchemist's Tome")).apply {
            tomeItems.forEach { item ->
                if (item.second == Material.BLAZE_POWDER && !Game.canUseBlazePowder()) return@forEach

                if (Random.nextInt(0,100) < item.first) setItem(Random.nextInt(0,26),ItemStack(item.second))
            }
        })

        player.setCooldown(TOME_COOLDOWN_GROUP,TOME_COOLDOWN)
    }

    @EventHandler
    fun onPotionSplash(event: PotionSplashEvent) {
        val potion = event.potion
        val throwerUUID = potion.ownerUniqueId ?: return
        val thrower = Bukkit.getPlayer(throwerUUID) ?: return
        if (!isSelected(thrower)) return

        val effects = potion.effects

        event.affectedEntities.forEach { target ->
            if (target !is Player || thrower != target) {
                target.addPotionEffects(effects.map { it.withDuration((it.duration * event.getIntensity(target)).toInt()).withAmplifier(min(1,it.amplifier)) })
            }
            else {
                target.addPotionEffects(effects.map { it.withDuration((it.duration * event.getIntensity(target)).toInt()) })
            }

            event.setIntensity(target,0.00001)
        }
    }

    fun createAlchemistStand(): ItemStack {
        return ItemStack(Material.BREWING_STAND).apply {
            uniqueClassItem()
            soulbound()
            setAnniItem(ALCHEMIST_STAND_ITEM_ID)

            editMeta {
                it.itemName(Component.text("Alchemist's Stand").color(NamedTextColor.AQUA))
            }
        }
    }
}