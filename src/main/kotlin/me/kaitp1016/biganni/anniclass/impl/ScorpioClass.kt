package me.kaitp1016.biganni.anniclass.impl

import io.papermc.paper.datacomponent.DataComponentTypes
import io.papermc.paper.datacomponent.item.UseCooldown
import me.kaitp1016.biganni.PLUGIN_ID
import me.kaitp1016.biganni.anniclass.AnniClass
import me.kaitp1016.biganni.utils.ItemUtils.getAnniId
import me.kaitp1016.biganni.utils.ItemUtils.setAnniItem
import me.kaitp1016.biganni.utils.MCUtils.toMC
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.minecraft.core.BlockPos
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.util.Mth
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.projectile.Projectile
import net.minecraft.world.entity.projectile.Projectile.ProjectileFactory
import net.minecraft.world.entity.projectile.throwableitemprojectile.Snowball
import net.minecraft.world.item.Items
import net.minecraft.world.level.ClipContext
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.gameevent.GameEvent
import net.minecraft.world.phys.EntityHitResult
import net.minecraft.world.phys.HitResult
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.entity.EntityRemoveEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.ItemStack
import kotlin.math.PI
import kotlin.math.min
import net.minecraft.world.item.ItemStack as MCItemStack

object ScorpioClass: AnniClass(), Listener {
    override val icon = Items.NETHER_STAR
    override val name = "Scropio"
    override val description = arrayOf(
        "右クリックでアビリティを使用すると、当った敵を自身の視点の先にテレポートできる。",
        "左クリックでアビリティを使用すると、当たった味方に向かって引っ張られる。",
    )

    const val SCORPIO_HOOK_ITEM_ID = "scorpio_hook"
    const val SCORPIO_HOOK_COOLDOWN = 60
    val SCORPIO_HOOK_COOLDOWN_GROUP = Key.key(PLUGIN_ID, "scorpio_hook")

    override fun getDefaultItems(player: Player): MutableList<ItemStack> {
        return super.getDefaultItems(player).also {
            it.removeIf { it.type == Material.WOODEN_SWORD }
            it.add(ItemStack(Material.STONE_SWORD).uniqueClassItem().soulbound())

            it.add(ItemStack(Material.NETHER_STAR).apply {
                uniqueClassItem()
                soulbound()
                setAnniItem(SCORPIO_HOOK_ITEM_ID)

                setData(DataComponentTypes.USE_COOLDOWN, UseCooldown.useCooldown(SCORPIO_HOOK_COOLDOWN / 20f).cooldownGroup(SCORPIO_HOOK_COOLDOWN_GROUP).build())

                editMeta {
                    it.itemName(Component.text("Hook").color(NamedTextColor.GOLD))
                }
            })
        }
    }

    enum class HookType {
        PULL_SELF, PULL_OTHER,
    }

    @EventHandler
    fun onInteract(event: PlayerInteractEvent) {
        val player = event.player
        if (!isSelected(player)) return

        val item = event.item ?: return
        if (item.getAnniId() != SCORPIO_HOOK_ITEM_ID || player.hasCooldown(item)) return

        val action = event.action
        val mcPlayer = player.toMC()
        val mcItem = item.toMC() ?: return
        val level = mcPlayer.level()

        if (action == Action.RIGHT_CLICK_BLOCK || action == Action.RIGHT_CLICK_AIR) {
            val snowball = Projectile.spawnProjectileFromRotationDelayed(ProjectileFactory { level: ServerLevel, mob: LivingEntity, item: MCItemStack -> Hook(mcPlayer, HookType.PULL_OTHER, level, mob) }, level, mcItem, mcPlayer, -10.0f, 1.0f, 1.0f)
            if (!snowball.attemptSpawn()) return
        } else if (action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK) {
            val snowball = Projectile.spawnProjectileFromRotationDelayed(ProjectileFactory { level: ServerLevel, mob: LivingEntity, item: MCItemStack -> Hook(mcPlayer, HookType.PULL_SELF, level, mob) }, level, mcItem, mcPlayer, -10.0f, 1.0f, 1.0f)
            if (!snowball.attemptSpawn()) return
        } else {
            return
        }

        player.setCooldown(SCORPIO_HOOK_COOLDOWN_GROUP, SCORPIO_HOOK_COOLDOWN)
        player.world.playSound(player.location, Sound.ENTITY_SNOWBALL_THROW, 2f, 1f)

        event.isCancelled = true
    }

    class Hook : Snowball {
        val thrower: ServerPlayer
        val type: HookType

        constructor(thrower: ServerPlayer, type: HookType, level: Level, mob: LivingEntity) : super(level, mob, MCItemStack(Items.NETHER_STAR)) {
            this.thrower = thrower
            this.type = type
        }

        override fun onHitEntity(hitResult: EntityHitResult) {
            val target = hitResult.entity
            if (target !is ServerPlayer) return

            val team = thrower.teamColor
            if (type == HookType.PULL_SELF) {
                if (team != target.teamColor) return

                thrower.deltaMovement = target.position().subtract(thrower.position()).multiply(1.0, 0.0, 1.0).normalize().multiply(9.0, 0.0, 9.0).add(0.0, 0.5, 0.0)
                thrower.hurtMarked = true

                this.level().broadcastEntityEvent(this, 3.toByte())
                this.discard(EntityRemoveEvent.Cause.HIT)

                bukkitEntity.world.playSound(bukkitEntity.location, Sound.BLOCK_WOODEN_DOOR_OPEN, 2f, 0f)
            }
            if (type == HookType.PULL_OTHER) {
                if (team == target.teamColor || BerserkerClass.abilityPlayers.contains(target.bukkitEntity)) return

                val pos = thrower.getRayTrace(1, ClipContext.Fluid.ANY).location

                val level = thrower.level()
                val blockPos = BlockPos(pos.x.toInt(), pos.y.toInt(), pos.z.toInt())
                if (!level.getBlockState(blockPos.offset(0, 0, 0)).canBeReplaced()   || !level.getBlockState(blockPos.offset(0, 1, 0)).canBeReplaced()) return

                var y = min(blockPos.y, 256)
                var isVoid = true

                while (y > -64) {
                    val state = level.getBlockState(BlockPos(blockPos.x, y, blockPos.z))
                    if (!state.isAir && state.block != Blocks.STRUCTURE_VOID) {
                        isVoid = false
                        break
                    }

                    y--
                }

                if (isVoid) return

                target.teleportTo(pos.x, pos.y, pos.z)

                level().broadcastEntityEvent(this, 3.toByte())
                discard(EntityRemoveEvent.Cause.HIT)
            }
        }

        override fun onHit(hitResult: HitResult) {
            if (hitResult is EntityHitResult) {
                this.onHitEntity(hitResult)
                this.level().gameEvent(GameEvent.PROJECTILE_LAND, hitResult.location, GameEvent.Context.of(this, null))
                return
            }

            super.onHit(hitResult)
        }

        override fun tick() {
            thrower.connection.send(ClientboundLevelParticlesPacket(ParticleTypes.FIREWORK, false, true, this.x, this.y, this.z, 0f, 0f, 0f, 0f, 1))

            super.tick()
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
    }
}