package me.kaitp1016.biganni.anniclass.impl

import com.destroystokyo.paper.event.server.ServerTickStartEvent
import io.papermc.paper.datacomponent.DataComponentTypes
import io.papermc.paper.datacomponent.item.UseCooldown
import me.kaitp1016.biganni.PLUGIN_ID
import me.kaitp1016.biganni.anniclass.AnniClass
import me.kaitp1016.biganni.mc
import me.kaitp1016.biganni.packetgui.ChestPacketGui
import me.kaitp1016.biganni.utils.ItemUtils.getAnniId
import me.kaitp1016.biganni.utils.ItemUtils.setAnniItem
import me.kaitp1016.biganni.utils.LevelBlockPos
import me.kaitp1016.biganni.utils.MCUtils.toMC
import me.kaitp1016.biganni.utils.Utils.toIntCorrect
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.minecraft.core.BlockPos
import net.minecraft.core.component.DataComponents
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.damagesource.DamageTypes
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.effect.MobEffects
import net.minecraft.world.item.Item
import net.minecraft.world.item.Items
import org.bukkit.Material
import org.bukkit.Particle
import org.bukkit.block.data.BlockData
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.inventory.ItemStack

object HunterClass: AnniClass(), Listener {
    override val name = "Hunter"
    override val shortName = "HUN"
    override val icon = Items.LEAD
    override val description = arrayOf(
        "アビリティを使用するとトラップを設置できる。",
    )

    const val TRAP_SNARE_ITEM_ID = "hunter_trap_snare"
    const val TRAP_SNARE_COOLDOWN = 300
    val TRAP_SNARE_COOLDOWN_GROUP = Key.key(PLUGIN_ID,"hunter_trap_snare")
    
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
        }
    }

    override fun onUnselect(player: Player) {
        traps.removeAll { it.player == player }

        super.onUnselect(player)
    }

    data class Trap(val player: Player, val pos: LevelBlockPos, val type: TrapType)

    enum class TrapType(val displayName: String, val display: Item,val block: BlockData) {
        FREEZE(displayName = "Freeze",display = Items.ICE,block = Material.ICE.createBlockData()),
        BLAST(displayName = "Blast",display = Items.TNT,block = Material.TNT.createBlockData()),
        DECAY(displayName = "Decay",display = Items.COAL_BLOCK,block = Material.COAL_BLOCK.createBlockData()),
        LEVITATION(displayName = "Levitation",display = Items.EMERALD_BLOCK,block = Material.EMERALD_BLOCK.createBlockData());

        fun next(): TrapType {
            val entries = entries
            val index = entries.indexOf(this)
            return entries.getOrNull(index + 1) ?: entries.first()
        }
    }

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

            PlaceTrapGui(player.toMC(), LevelBlockPos(mcPlayer.level(),pos.x,pos.y,pos.z)).open()
        }
    }

    @EventHandler
    fun onMove(event: PlayerMoveEvent) {
        if (traps.isEmpty()) return

        val player = event.player.toMC()
        val loaction = event.to
        val pos = BlockPos(loaction.x.toIntCorrect(),loaction.y.toIntCorrect(),loaction.z.toIntCorrect())
        val level = player.level()
        val levelPos = LevelBlockPos(level,pos.x,pos.y,pos.z)

        val trap = traps.find { it.pos == levelPos } ?: return
        if (trap.player.toMC().teamColor == player.teamColor) return

        when(trap.type) {
            TrapType.FREEZE -> {
                player.addEffect(MobEffectInstance(MobEffects.SLOWNESS,200,1))
            }
            TrapType.BLAST -> {
                player.hurtMarked = true
                player.lerpMotion(player.position().subtract(pos.center).normalize().multiply(3.0,1.0,3.0))
                val source = DamageSource(mc.registryAccess().get(DamageTypes.PLAYER_EXPLOSION).get(),trap.player.toMC())

                player.hurtServer(level,source,10f)
            }
            TrapType.DECAY -> {
                player.addEffect(MobEffectInstance(MobEffects.WITHER,160,1))
            }
            TrapType.LEVITATION -> {
                player.addEffect(MobEffectInstance(MobEffects.LEVITATION,200,1))
                player.addEffect(MobEffectInstance(MobEffects.SLOWNESS,200,10))
            }
        }

        traps.remove(trap)
    }

    @EventHandler
    fun onBreak(event: BlockBreakEvent) {
        if (traps.isEmpty()) return

        val block = event.block
        val level = block.world.toMC()
        val pos = BlockPos(block.x,block.y,block.z)
        val levelPos = LevelBlockPos(level,pos.x,pos.y,pos.z)

        val trap = traps.find { it.pos == levelPos } ?: return
        traps.remove(trap)

        event.isCancelled = true
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
                .location(trap.pos.level.world,trap.pos.x.toDouble() + 0.5,trap.pos.y.toDouble() + 1.1,trap.pos.z.toDouble() + 0.5)
                .offset(0.3,0.0,0.3)
                .receivers(16, true)
                .count(6)
                .data(trap.type.block)

            builder.receivers(builder.receivers()!!.filter { it.toMC().teamColor == team })
                .spawn()
        }
    }

    class PlaceTrapGui: ChestPacketGui {
        override val name = "place trap"
        override val displayName = net.minecraft.network.chat.Component.literal("Place Trap")

        val types: List<TrapType>
        val pos: LevelBlockPos

        constructor(player: ServerPlayer,pos: LevelBlockPos):super(player,9) {
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
                val size = arrayOf(3,2,1).find { isPossibleToPlace(it) }
                val bukkitPlayer = player.bukkitEntity
                if (size == null) {
                    bukkitPlayer.sendMessage("ここにはおけません!")
                    close()
                    return@execute
                }

                traps.removeAll { it.player == bukkitPlayer }

                val level = pos.level
                val world = level.world

                repeat(size * size) {
                    val x = pos.x + it % size
                    val y = pos.y
                    val z = pos.z + it / size

                    traps.add(Trap(bukkitPlayer, LevelBlockPos(level,x,y,z),type))
                }

                bukkitPlayer.setCooldown(TRAP_SNARE_COOLDOWN_GROUP,TRAP_SNARE_COOLDOWN)
                close()
            }
        }

        fun isPossibleToPlace(size: Int): Boolean {
            val level = pos.level

            repeat(size * size) {
                val x = pos.x + it % size
                val z = pos.z + it / size

                if (!canPlaceAt(level,x,pos.y,z)) return false
            }

            return true
        }

        fun canPlaceAt(level: ServerLevel, x: Int,y: Int,z: Int): Boolean {
            return level.getBlockState(BlockPos(x,y,z)).occlusionShape.`moonrise$isFullBlock`() && level.getBlockState(BlockPos(x,y + 1,z)).isAir && level.getBlockState(BlockPos(x,y + 2,z)).isAir
        }
    }
}