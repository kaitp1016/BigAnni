package me.kaitp1016.biganni.anniclass.impl

import io.papermc.paper.datacomponent.DataComponentTypes
import io.papermc.paper.datacomponent.item.UseCooldown
import me.kaitp1016.biganni.PLUGIN_ID
import me.kaitp1016.biganni.anniclass.AnniClass
import me.kaitp1016.biganni.features.DelayingBlock
import me.kaitp1016.biganni.utils.BlockPosInfo
import me.kaitp1016.biganni.utils.ItemUtils.getAnniId
import me.kaitp1016.biganni.utils.ItemUtils.setAnniItem
import me.kaitp1016.biganni.utils.MCUtils.toMC
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.minecraft.core.BlockPos
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.tags.BlockTags
import net.minecraft.world.entity.MoverType
import net.minecraft.world.entity.item.PrimedTnt
import net.minecraft.world.item.Items
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.Explosion
import net.minecraft.world.level.ExplosionDamageCalculator
import net.minecraft.world.level.Level.ExplosionInteraction
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.material.FluidState
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.block.Block
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.entity.EntityRemoveEvent
import org.bukkit.event.player.PlayerInteractEntityEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import java.util.*
import java.util.function.IntPredicate

object EngineerClass: AnniClass(), Listener {
    override val name = "Enginner"
    override val shortName = "ENG"
    override val icon = Items.TNT
    override val description = arrayOf(
        "爆弾を設置することができ、敵が設置したブロックを破壊する。",
        "Evertoolは視点先のブロックに対応するツールになる。",
    )

    const val BUNKER_BUSTER_DROP_ITEM_ID = "engineer_bunker_buster"
    const val BUNKER_BUSTER_COOLDOWN = 600
    val BUNKER_BUSTER_COOLDOWN_GROUP = Key.key(PLUGIN_ID, "engineer_bunker_buster")

    const val EVERTOOL_ITEM_ID = "engineer_evertool"

    override fun getDefaultItems(player: Player): MutableList<ItemStack> {
        return super.getDefaultItems(player).also {
            it.removeIf { it.type == Material.WOODEN_PICKAXE }
            it.removeIf { it.type == Material.WOODEN_AXE }

            it.add(ItemStack(Material.BLAZE_ROD).apply {
                uniqueClassItem()
                soulbound()
                setAnniItem(EVERTOOL_ITEM_ID)

                editMeta {
                    it.itemName(Component.text("Evertool").color(NamedTextColor.GOLD))
                }
            })

            it.add(ItemStack(Material.TNT).apply {
                uniqueClassItem()
                soulbound()
                setAnniItem(BUNKER_BUSTER_DROP_ITEM_ID)

                setData(DataComponentTypes.USE_COOLDOWN, UseCooldown.useCooldown(BUNKER_BUSTER_COOLDOWN / 20f).cooldownGroup(BUNKER_BUSTER_COOLDOWN_GROUP).build())

                editMeta {
                    it.itemName(Component.text("Bunker Buster").color(NamedTextColor.GOLD))
                }
            })
        }
    }

    override fun onSelect(player: Player) {
        selectedExplosions[player] = ExplosionType.entries.first()

        super.onSelect(player)
    }

    override fun onUnselect(player: Player) {
        selectedExplosions.remove(player)

        super.onUnselect(player)
    }

    override fun onUserTick(player: Player) {
        val item = player.inventory.itemInMainHand
        if (item.getAnniId() != EVERTOOL_ITEM_ID) return

        val block = player.rayTraceBlocks(5.0)?.hitBlock
        val tool = getTool(block) ?: Material.BLAZE_ROD
        if (tool == item.type) return

        player.inventory.setItem(EquipmentSlot.HAND, item.withType(tool).apply {
            editMeta { it.isUnbreakable = true }
        })
    }

    val blockTags = listOf(
        BlockTags.MINEABLE_WITH_PICKAXE to Material.STONE_PICKAXE,
        BlockTags.MINEABLE_WITH_AXE to Material.STONE_AXE,
        BlockTags.MINEABLE_WITH_HOE to Material.STONE_HOE,
        BlockTags.MINEABLE_WITH_SHOVEL to Material.STONE_SHOVEL,
    )

    enum class ExplosionType(val radius: Float, val fuseTick: Int) {
        DYNMITE(radius = 2.2f, fuseTick = 19),
        C4(radius = 3.5f, fuseTick = 120),
        NUKE(radius = 10f, fuseTick = 200);

        fun next(): ExplosionType {
            val entries = entries
            val index = entries.indexOf(this)
            return entries.getOrNull(index + 1) ?: entries.first()
        }
    }

    // IntはTeamColor
    val placedBlocks = BlockPosInfo<Int>()
    val selectedExplosions = mutableMapOf<Player, ExplosionType>()

    @EventHandler
    fun onInteract(event: PlayerInteractEvent) {
        val player = event.player
        if (!isSelected(player)) return

        val item = event.item ?: return
        if (item.getAnniId() != BUNKER_BUSTER_DROP_ITEM_ID) return

        if (event.action == Action.LEFT_CLICK_AIR || event.action == Action.LEFT_CLICK_BLOCK) {
            val explosion = selectedExplosions.getOrPut(player) { ExplosionType.entries.first() }.next()
            selectedExplosions[player] = explosion

            player.sendMessage("${explosion.name} を選択しました!")
            player.playSound(player, Sound.UI_BUTTON_CLICK, 1f, 1f)

            return
        }

        if (player.hasCooldown(item)) return

        val face = event.blockFace
        val pos = event.clickedBlock?.location?.apply {
            add(0.5, 0.5, 0.5)
            add(face.modX * 1.5, face.modY * 1.5, face.modZ * 1.5)
        } ?: return

        val mcPlayer = player.toMC()
        val level = mcPlayer.level()
        val type = selectedExplosions[player] ?: ExplosionType.entries.first()

        val tnt = BunkerBusterTNT(level, pos.x, pos.y, pos.z, mcPlayer, type)
        level.addFreshEntity(tnt)

        event.isCancelled = true
        player.setCooldown(BUNKER_BUSTER_COOLDOWN_GROUP, BUNKER_BUSTER_COOLDOWN)
    }

    @EventHandler
    fun onPlace(event: BlockPlaceEvent) {
        val player = event.player
        val mcPlayer = player.toMC()
        val block = event.block
        val level = mcPlayer.level()
        val pos = BlockPos(block.x, block.y, block.z)

        placedBlocks[level, pos] = mcPlayer.teamColor
    }

    @EventHandler
    fun onBreak(event: BlockBreakEvent) {
        val player = event.player
        val mcPlayer = player.toMC()
        val block = event.block
        val level = mcPlayer.level()
        val pos = BlockPos(block.x, block.y, block.z)

        placedBlocks.remove(level, pos)
    }

    @EventHandler
    fun onInteractEntity(event: PlayerInteractEntityEvent) {
        val target = event.rightClicked.toMC()
        if (target !is BunkerBusterTNT) return

        val player = event.player.toMC()
        if (target.spawner.teamColor == player.teamColor) return

        val level = event.player.toMC().level()
        target.kill(level)
    }

    class BunkerBusterTNT : PrimedTnt {
        val spawner: ServerPlayer
        val type: ExplosionType

        constructor(level: ServerLevel, x: Double, y: Double, z: Double, spawner: ServerPlayer, type: ExplosionType) : super(level, x, y, z, spawner) {
            this.spawner = spawner
            this.type = type
            this.fuse = type.fuseTick
            this.explosionPower = type.radius
        }

        override fun tick() {
            this.handlePortal()
            this.applyGravity()
            this.move(MoverType.SELF, this.deltaMovement)
            this.applyEffectsFromBlocks()
            if (this.level().paperConfig().fixes.tntEntityHeightNerf.test(IntPredicate { v: Int -> this.y > v.toDouble() })) {
                this.discard(EntityRemoveEvent.Cause.OUT_OF_WORLD)
            } else {
                this.deltaMovement = this.deltaMovement.scale(0.98)
                if (this.onGround()) {
                    this.deltaMovement = this.deltaMovement.multiply(0.7, -0.5, 0.7)
                }

                val fuse = this.fuse - 1
                this.fuse = fuse
                if (fuse <= 0) {
                    this.explode()
                    this.discard(EntityRemoveEvent.Cause.EXPLODE)
                } else {
                    this.updateFluidInteraction()
                    this.level().addParticle(ParticleTypes.SMOKE, this.x, this.y + 0.5, this.z, 0.0, 0.0, 0.0)
                }

                if (!this.isRemoved && this.wasTouchingWater && this.level().paperConfig().fixes.preventTntFromMovingInWater) {
                    this.hurtMarked = true
                    this.needsSync = true
                }
            }
        }

        fun explode() {
            this.level().explode(this, Explosion.getDefaultDamageSource(this.level(), this), ExplosionCalculator(spawner.teamColor), this.x, this.getY(0.0625), this.z, this.explosionPower, false, ExplosionInteraction.TNT)
        }

        class ExplosionCalculator(val team: Int) : ExplosionDamageCalculator() {
            override fun shouldBlockExplode(explosion: Explosion, level: BlockGetter, pos: BlockPos, state: BlockState, power: Float): Boolean {
                val level = (level as ServerLevel)
                val placedTeam = placedBlocks.get(level, pos)
                if (placedTeam == null || placedTeam == team || DelayingBlock.delayingBlocks.has(level, pos)) return false

                return super.shouldBlockExplode(explosion, level, pos, state, power)
            }

            override fun getBlockExplosionResistance(explosion: Explosion, level: BlockGetter, pos: BlockPos, block: BlockState, fluid: FluidState): Optional<Float> {
                val result = super.getBlockExplosionResistance(explosion, level, pos, block, fluid)
                if (result.isPresent) return Optional.of(1f)
                return result
            }
        }
    }

    fun getTool(block: Block?): Material? {
        if (block == null) return null

        val block = block.toMC()
        if (block.`is`(BlockTags.LEAVES) || block.`is`(BlockTags.WOOL)) return Material.SHEARS

        blockTags.find { block.`is`(it.first) }?.second?.let { return it }

        return null
    }
}