package me.kaitp1016.biganni.anniclass.impl

import com.destroystokyo.paper.event.server.ServerTickStartEvent
import me.kaitp1016.biganni.anniclass.AnniClass
import me.kaitp1016.biganni.utils.ItemUtils.getAnniId
import me.kaitp1016.biganni.utils.ItemUtils.setAnniItem
import me.kaitp1016.biganni.utils.MCUtils.toMC
import me.kaitp1016.biganni.utils.Utils.isFullBlock
import me.kaitp1016.biganni.utils.Utils.toIntCorrect
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.util.Mth
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.projectile.Projectile
import net.minecraft.world.entity.projectile.throwableitemprojectile.Snowball
import net.minecraft.world.item.Items
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.VineBlock
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.HitResult
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.damage.DamageType
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockDropItemEvent
import org.bukkit.event.entity.EntityRemoveEvent
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import kotlin.math.PI
import net.minecraft.world.item.ItemStack as MCItemStack

object SpiderClass: AnniClass(), Listener {
    override val icon = Items.COBWEB
    override val name = "Spider"
    override val shortName = "SPI"
    override val description = arrayOf(
        "ツタを周囲の壁に設置することができる。",
        "投げることができる蜘蛛の巣が初期装備に含まれている。",
        "落下ダメージでは死なない。",
    )

    const val TOGGLE_WALL_CLIMB_ITEM_ID = "spider_toggle_wall_climb"
    const val THROWABLE_WEB_ITEM_ID = "spider_throwable_web"

    override fun getDefaultItems(player: Player): MutableList<ItemStack> {
        return super.getDefaultItems(player).also {
            it.add(ItemStack(Material.WOODEN_SHOVEL).uniqueClassItem().soulbound())

            it.add(ItemStack(Material.VINE).apply {
                uniqueClassItem()
                soulbound()
                setAnniItem(TOGGLE_WALL_CLIMB_ITEM_ID)

                editMeta {
                    it.itemName(Component.text("Toggle Wall Climb").color(NamedTextColor.GOLD))
                }
            })

            it.add(ItemStack(Material.COBWEB).apply {
                uniqueClassItem()
                soulbound()
                setAnniItem(THROWABLE_WEB_ITEM_ID)

                editMeta {
                    it.itemName(Component.text("Throwable Web").color(NamedTextColor.GOLD))
                }

                amount = 5
            })
        }
    }

    const val WEB_LIMIT = 15

    data class PlacedVine(val level: Level, val pos: BlockPos, var tick:Int = 200)
    data class PlacedWeb(val player: Player, val level: Level, val pos: BlockPos)

    val enabledPlayers = mutableListOf<Player>()
    val placedVines = mutableListOf<PlacedVine>()

    val placedWebs = mutableListOf<PlacedWeb>()

    override fun onUnselect(player: Player) {
        enabledPlayers.remove(player)

        val webs = placedWebs.filter { it.player == player }
        placedWebs.removeAll(webs)

        webs.forEach {
            if (it.level.getBlockState(it.pos).block == Blocks.COBWEB) {
                it.level.setBlockAndUpdate(it.pos,Blocks.AIR.defaultBlockState())
            }
        }

        super.onUnselect(player)
    }

    @EventHandler
    fun onInteract(event: PlayerInteractEvent) {
        val player = event.player
        if (!isSelected(player)) return

        val item = event.item ?: return
        if (item.getAnniId() == TOGGLE_WALL_CLIMB_ITEM_ID) {
            event.isCancelled = true

            if (enabledPlayers.contains(player)) {
                enabledPlayers.remove(player)
                player.sendMessage(Component.text("Wall Climb disabled.").color(NamedTextColor.DARK_GREEN))
            }
            else {
                enabledPlayers.add(player)
                player.sendMessage(Component.text("Wall Climb enabled.").color(NamedTextColor.DARK_GREEN))
            }

            player.playSound(player, Sound.UI_BUTTON_CLICK,1f,1f)

        }
        if (item.getAnniId() == THROWABLE_WEB_ITEM_ID) {
            event.isCancelled = true

            val mcPlayer = player.toMC()
            val level = mcPlayer.level()
            val mcItem = item.toMC() ?: MCItemStack(Items.COBWEB)
            val power = if (event.action.isRightClick) 1.0f else -0.75f
            val snowball = Projectile.spawnProjectileFromRotationDelayed({ level: ServerLevel, mob: LivingEntity, item: MCItemStack -> ThrownWeb(mcPlayer, level, mob) }, level, mcItem, mcPlayer, -3.0f, power, 1.0f)
            if (!snowball.attemptSpawn()) return

            item.amount--
        }
    }

    @EventHandler
    fun onTick(event: ServerTickStartEvent) {
        if (!placedVines.isEmpty()) {
            placedVines.removeIf { vine ->
                vine.tick--

                if (vine.tick < 1) {
                    if (vine.level.getBlockState(vine.pos).block == Blocks.VINE) {
                        vine.level.setBlockAndUpdate(vine.pos, Blocks.AIR.defaultBlockState())
                    }
                    return@removeIf true
                }

                false
            }
        }
    }

    @EventHandler
    fun onBlockDropItem(event: BlockDropItemEvent) {
        val block = event.block
        val pos = BlockPos(block.x,block.y,block.z)
        val level = block.world.toMC()

        if (placedWebs.removeIf { it.pos == pos && it.level == level }) {
            event.items.clear()
        }
    }

    @EventHandler
    fun onDeath(event: PlayerDeathEvent) {
        val player = event.player
        if (!isSelected(player)) return

        if (event.damageSource.damageType == DamageType.FALL) {
            event.isCancelled = true

            player.addPotionEffect(PotionEffect(PotionEffectType.REGENERATION, 400, 0))
            player.addPotionEffect(PotionEffect(PotionEffectType.ABSORPTION, 200, 0))
            player.health = 0.1
            player.world.playSound(player.location, Sound.ITEM_TOTEM_USE, 1f, 1f)

            return
        }

        val webs = placedWebs.filter { it.player == player }
        placedWebs.removeAll(webs)

        webs.forEach {
            if (it.level.getBlockState(it.pos).block == Blocks.COBWEB) {
                it.level.setBlockAndUpdate(it.pos,Blocks.AIR.defaultBlockState())
            }
        }
    }

    override fun onUserTick(player: Player) {
        if (!enabledPlayers.contains(player)) return

        val level = player.world.toMC()
        val pos = BlockPos(player.x.toIntCorrect(), (player.y).toIntCorrect(), player.z.toIntCorrect())

        for (dx in -2..2) {
            for (dz in -2..2) {
                for (dy in -2..2) {
                    val pos = pos.offset(dx, dy, dz)
                    val block = level.getBlockState(pos)
                    if (canPlaceVine(level,pos)) {
                        level.setBlockAndUpdate(pos, getVineAt(level,pos))
                        placedVines.add(PlacedVine(level, pos))
                    }
                    if (block.block == Blocks.VINE) {
                        placedVines.find { it.pos == pos }?.tick = 200
                    }
                }
            }
        }
    }

    val directions = arrayOf(
        intArrayOf(1,0,0),
        intArrayOf(-1,0,0),
        intArrayOf(0,0,1),
        intArrayOf(0,0,-1),
    )

    val properties = mapOf(
        intArrayOf(1,0,0) to VineBlock.EAST,
        intArrayOf(-1,0,0) to VineBlock.WEST,
        intArrayOf(0,1,0) to VineBlock.UP,
        intArrayOf(0,0,1) to VineBlock.SOUTH,
        intArrayOf(0,0,-1) to VineBlock.NORTH,
        )

    val webPlaceOffsets = arrayOf(
        intArrayOf(1,0,0),
        intArrayOf(-1,0,0),
        intArrayOf(0,1,0),
        intArrayOf(0,0,0),
        intArrayOf(0,-1,0),
        intArrayOf(0,0,1),
        intArrayOf(0,0,-1),
    )

    fun canPlaceVine(level: ServerLevel, pos: BlockPos): Boolean {
        return level.getBlockState(pos).isAir && directions.any { direction -> level.isFullBlock(pos.offset(direction[0],direction[1],direction[2])) }
    }

    fun getVineAt(level: ServerLevel,pos: BlockPos): BlockState {
        var vine = Blocks.VINE.defaultBlockState()
        properties.forEach { (direction, property) ->
            vine = vine.setValue(property,level.isFullBlock(pos.offset(direction[0],direction[1],direction[2])))
        }

        return vine
    }

    class ThrownWeb : Snowball {
        val thrower: ServerPlayer

        constructor(thrower: ServerPlayer, level: Level, mob: LivingEntity) : super(level, mob, MCItemStack(Items.COBWEB)) {
            this.thrower = thrower
        }

        override fun onHit(hitResult: HitResult) {
            val position = blockPosition()
            val level = level()
            val bukkitPlayer = thrower.bukkitEntity

            webPlaceOffsets.forEach { offset ->
                val pos = position.offset(offset[0],offset[1],offset[2])
                if (level.getBlockState(pos).isAir) {
                    level.setBlockAndUpdate(pos, Blocks.COBWEB.defaultBlockState())
                    placedWebs.add(PlacedWeb(bukkitPlayer,level,pos))
                }
            }

            val player = thrower.bukkitEntity

            while (placedWebs.filter { it.player == player }.size >= WEB_LIMIT) {
                val web = placedWebs.find { it.player == player } ?: break
                placedWebs.remove(web)
                if (web.level.getBlockState(web.pos).block == Blocks.COBWEB) {
                    web.level.setBlockAndUpdate(web.pos,Blocks.AIR.defaultBlockState())
                }
            }

            this.level().broadcastEntityEvent(this, 3)
            this.discard(EntityRemoveEvent.Cause.HIT)
        }

        override fun shootFromRotation(source: Entity, xRot: Float, yRot: Float, yOffset: Float, pow: Float, uncertainty: Float) {
            val xd = -Mth.sin(yRot * (PI / 180f)) * Mth.cos(xRot * (PI / 180f))
            val yd = -Mth.sin((xRot + yOffset) * (PI / 180f))
            val zd = Mth.cos(yRot * (PI / 180f)) * Mth.cos(xRot * (PI / 180f))

            shoot(xd.toDouble(), yd.toDouble(), zd.toDouble(), pow, uncertainty)
        }

        override fun isInWater(): Boolean {
            return false
        }

        override fun shouldBeSaved(): Boolean {
            return false
        }
    }
}