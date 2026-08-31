package me.kaitp1016.biganni.anniclass.impl

import com.destroystokyo.paper.event.server.ServerTickStartEvent
import io.papermc.paper.datacomponent.DataComponentTypes
import io.papermc.paper.datacomponent.item.UseCooldown
import it.unimi.dsi.fastutil.ints.IntList
import me.kaitp1016.biganni.PLUGIN_ID
import me.kaitp1016.biganni.anniclass.AnniClass
import me.kaitp1016.biganni.game.Game
import me.kaitp1016.biganni.mc
import me.kaitp1016.biganni.packetgui.ChestPacketGui
import me.kaitp1016.biganni.utils.ItemUtils.getAnniId
import me.kaitp1016.biganni.utils.ItemUtils.setAnniItem
import me.kaitp1016.biganni.utils.LevelBlockPos
import me.kaitp1016.biganni.utils.MCUtils.toMC
import me.kaitp1016.biganni.utils.Utils.isFullBlock
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.minecraft.core.BlockPos
import net.minecraft.core.Vec3i
import net.minecraft.core.component.DataComponents
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.damagesource.DamageTypes
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.effect.MobEffects
import net.minecraft.world.entity.projectile.FireworkRocketEntity
import net.minecraft.world.item.Item
import net.minecraft.world.item.Items
import net.minecraft.world.item.component.FireworkExplosion
import net.minecraft.world.item.component.Fireworks
import net.minecraft.world.phys.Vec3
import org.bukkit.Material
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.block.data.BlockData
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.inventory.ItemStack

object HunterClass: AnniClass(), Listener {
    override val name = "Hunter"
    override val shortName = "HUN"
    override val icon = Items.LEAD
    override val description = arrayOf(
        "トラップを設置できる。",
    )

    const val TRAP_SNARE_ITEM_ID = "hunter_trap_snare"
    const val TRAP_SNARE_COOLDOWN = 800
    val TRAP_SNARE_COOLDOWN_GROUP = Key.key(PLUGIN_ID, "hunter_trap_snare")

    override fun getDefaultItems(player: Player): MutableList<ItemStack> {
        return super.getDefaultItems(player).also {
            it.add(ItemStack(Material.LEAD).apply {
                uniqueClassItem()
                soulbound()
                setAnniItem(TRAP_SNARE_ITEM_ID)

                setData(DataComponentTypes.USE_COOLDOWN, UseCooldown.useCooldown(TRAP_SNARE_COOLDOWN / 20f).cooldownGroup(TRAP_SNARE_COOLDOWN_GROUP).build())

                editMeta {
                    it.itemName(Component.text("Trap Snare").color(NamedTextColor.GOLD))
                }
            })

            it.add(ItemStack(Material.WOODEN_SHOVEL).uniqueClassItem().soulbound())
            it.add(ItemStack(Material.WOODEN_SHOVEL).uniqueClassItem().soulbound())
        }
    }

    override fun onUnselect(player: Player) {
        traps.removeIf { it.player == player }

        super.onUnselect(player)
    }

    // TODO ミサイル

    data class Trap(val player: Player, val pos: LevelBlockPos, val type: TrapType)

    enum class TrapType(val displayName: String, val display: Item, val block: BlockData) {
        FREEZE(displayName = "Freeze", display = Items.ICE, block = Material.ICE.createBlockData()), BLAST(displayName = "Blast", display = Items.TNT, block = Material.TNT.createBlockData()), DECAY(displayName = "Decay", display = Items.COAL_BLOCK, block = Material.COAL_BLOCK.createBlockData()), LEVITATION(displayName = "Levitation", display = Items.EMERALD_BLOCK, block = Material.EMERALD_BLOCK.createBlockData());

        fun next(): TrapType {
            val entries = entries
            val index = entries.indexOf(this)
            return entries.getOrNull(index + 1) ?: entries.first()
        }
    }

    val trapSizes = arrayOf(
        arrayOf(Vec3i(1, 0, 1), Vec3i(1, 0, 0), Vec3i(1, 0, -1), Vec3i(0, 0, 1), Vec3i(0, 0, 0), Vec3i(0, 0, -1), Vec3i(-1, 0, 1), Vec3i(-1, 0, 0), Vec3i(-1, 0, -1),),
        arrayOf(Vec3i(1, 0, 1), Vec3i(1, 0, 0), Vec3i(0, 0, 1), Vec3i(0, 0, 0),),
        arrayOf(Vec3i(-1, 0, 1), Vec3i(-1, 0, 0), Vec3i(0, 0, 1), Vec3i(0, 0, 0),),
        arrayOf(Vec3i(1, 0, -1), Vec3i(0, 0, -1), Vec3i(1, 0, 0), Vec3i(0, 0, 0),),
        arrayOf(Vec3i(-1, 0, -1), Vec3i(0, 0, -1), Vec3i(-1, 0, 0), Vec3i(0, 0, 0),),
        arrayOf(Vec3i(0, 0, 0),),
    )

    val traps = mutableListOf<Trap>()

    @EventHandler
    fun onInteract(event: PlayerInteractEvent) {
        val player = event.player
        if (!isSelected(player)) return

        val item = event.item ?: return
        if (player.hasCooldown(item)) return

        val anniId = item.getAnniId()
        if (anniId == TRAP_SNARE_ITEM_ID) {
            val pos = event.clickedBlock ?: return
            val mcPlayer = player.toMC()

            PlaceTrapGui(player.toMC(), LevelBlockPos(mcPlayer.level(), pos.x, pos.y, pos.z)).open()
        }
    }

    @EventHandler
    fun onMove(event: PlayerMoveEvent) {
        if (traps.isEmpty()) return

        val player = event.player.toMC()
        val location = event.to
        val pos = BlockPos.containing(location.x, location.y, location.z)
        val level = player.level()
        val levelPos = LevelBlockPos(level, pos.x, pos.y, pos.z)

        val trap = traps.find { it.pos == levelPos } ?: return
        if (trap.player.toMC().teamColor == player.teamColor) return

        when (trap.type) {
            TrapType.FREEZE -> {
                player.addEffect(MobEffectInstance(MobEffects.SLOWNESS, 200, 1))
            }

            TrapType.BLAST -> {
                player.hurtMarked = true
                player.lerpMotion(player.position().subtract(pos.center).normalize().multiply(3.0, 1.0, 3.0))
                val source = DamageSource(mc.registryAccess().get(DamageTypes.PLAYER_EXPLOSION).get(), trap.player.toMC())

                player.bukkitEntity.playSound(player.bukkitEntity.location, Sound.ENTITY_GENERIC_EXPLODE, 1f, 1f)
                player.hurtServer(level, source, 10f)
            }

            TrapType.DECAY -> {
                player.addEffect(MobEffectInstance(MobEffects.WITHER, 160, 1))
                player.bukkitEntity.playSound(player.bukkitEntity.location, Sound.ENTITY_WITHER_SHOOT, 1f, 1f)
            }

            TrapType.LEVITATION -> {
                player.bukkitEntity.playSound(player.bukkitEntity.location, Sound.ENTITY_FIREWORK_ROCKET_TWINKLE, 1f, 1f)
                player.addEffect(MobEffectInstance(MobEffects.LEVITATION, 200, 1))
                player.addEffect(MobEffectInstance(MobEffects.SLOWNESS, 200, 10))
            }
        }

        player.level().addFreshEntity(FireworkRocketEntity(level, player.x,player.y + 2.0,player.z,net.minecraft.world.item.ItemStack(Items.FIREWORK_ROCKET).apply {
            set(DataComponents.FIREWORKS, Fireworks(0,listOf(FireworkExplosion(FireworkExplosion.Shape.CREEPER, IntList.of(255,255,255),IntList.of(255,255,255,255),true,false))) )
        }))

        val owner = trap.player
        val type = trap.type

        traps.removeIf {
            it.player == owner && it.type == type
        }
    }

    @EventHandler
    fun onBreak(event: BlockBreakEvent) {
        if (traps.isEmpty()) return

        val block = event.block
        val level = block.world.toMC()
        val pos = BlockPos(block.x, block.y, block.z)
        val levelPos = LevelBlockPos(level, pos.x, pos.y, pos.z)

        val trap = traps.find { it.pos == levelPos } ?: return
        val owner = trap.player
        val type = trap.type

        traps.removeIf {
            it.player == owner && it.type == type
        }

        event.isCancelled = true
    }

    @EventHandler
    fun onDeath(event: PlayerDeathEvent) {
        val player = event.player
        if (!isSelected(player)) return

        traps.removeIf {
            it.player == player
        }
    }

    @EventHandler
    fun onQuit(event: PlayerQuitEvent) {
        val player = event.player

        traps.removeIf {
            it.player == player
        }
    }

    var tick = 0

    @EventHandler
    fun onTick(event: ServerTickStartEvent) {
        if (traps.isEmpty()) return

        tick++
        if (tick < 19) return
        tick = 0

        traps.forEach { trap ->
            val team = trap.player.toMC().teamColor

            val builder = Particle.BLOCK_CRUMBLE.builder()
                .location(trap.pos.level.world, trap.pos.x.toDouble() + 0.5, trap.pos.y.toDouble() + 1.1, trap.pos.z.toDouble() + 0.5)
                .offset(0.3, 0.0, 0.3)
                .receivers(16, true)
                .count(6)
                .data(trap.type.block)

            builder.receivers(builder.receivers()!!.filter { it.toMC().teamColor == team }).spawn()
        }
    }

    class PlaceTrapGui : ChestPacketGui {
        override val name = "place trap"
        override val displayName = net.minecraft.network.chat.Component.literal("Place Trap")

        val types: List<TrapType>
        val pos: LevelBlockPos

        constructor(player: ServerPlayer, pos: LevelBlockPos) : super(player, 9) {
            this.types = TrapType.entries.reversed()
            this.pos = pos

            types.forEachIndexed { slot, buff ->
                this.setItem(slot, net.minecraft.world.item.ItemStack(buff.display).apply {
                    set(DataComponents.ITEM_NAME, net.minecraft.network.chat.Component.literal(buff.displayName).withColor(0xFFAA00))
                })
            }
        }

        override fun onClick(packet: ServerboundContainerClickPacket) {
            mc.execute {
                if (!isOpened) return@execute

                val type = types.getOrNull(packet.slotNum.toInt()) ?: return@execute
                val level = pos.level
                val size = trapSizes.find { canPlace(it) }
                val bukkitPlayer = player.bukkitEntity
                if (size == null) {
                    bukkitPlayer.sendMessage("ここにはおけません!")
                    close()
                    return@execute
                }

                val position = Vec3(pos.x.toDouble(),pos.y.toDouble(),pos.z.toDouble())
                if (Game.teams.any { it.nexus.level == level && it.nexus.distanceTo(position) < 15.0 }) {
                    bukkitPlayer.sendMessage("ここには設置できません!")
                    close()
                    return@execute
                }

                traps.removeIf { it.player == bukkitPlayer && it.type == type }

                size.forEach {
                    traps.add(Trap(bukkitPlayer, LevelBlockPos(level, pos.x + it.x, pos.y + it.y, pos.z + it.z), type))
                }

                bukkitPlayer.setCooldown(TRAP_SNARE_COOLDOWN_GROUP, TRAP_SNARE_COOLDOWN)
                close()
            }
        }

        fun canPlace(size: Array<Vec3i>): Boolean {
            val level = pos.level
            val pos = pos.toBlockPos()

            return size.all { canPlaceAt(level, pos.offset(it)) }
        }

        fun canPlaceAt(level: ServerLevel, pos: BlockPos): Boolean {
            return level.isFullBlock(pos) && level.getBlockState(pos.offset(0, 1, 0)).isAir && level.getBlockState(pos.offset(0, 2, 0)).isAir
        }
    }
}