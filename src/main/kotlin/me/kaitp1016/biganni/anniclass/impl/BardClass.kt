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
import me.kaitp1016.biganni.utils.MCUtils.toMC
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.minecraft.core.BlockPos
import net.minecraft.core.Holder
import net.minecraft.core.component.DataComponents
import net.minecraft.network.chat.Style
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.effect.MobEffect
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.effect.MobEffects
import net.minecraft.world.item.Item
import net.minecraft.world.item.Items
import net.minecraft.world.item.JukeboxSong
import net.minecraft.world.item.component.ItemLore
import net.minecraft.world.item.component.TooltipDisplay
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.entity.JukeboxBlockEntity
import net.minecraft.world.phys.AABB
import org.bukkit.Material
import org.bukkit.Particle
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.ItemStack
import java.util.*
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

object BardClass: AnniClass(), Listener {
    override val name = "Bard"
    override val icon = Items.JUKEBOX
    override val description = arrayOf(
        "ジュークボックスを設置すると周囲に選択したバフかデバフを与える。",
    )

    const val BUFF_BOX_ITEM_ID = "bard_buffbox"
    const val BUFF_BOX_COOLDOWN = 200
    val BUFF_BOX_COOLDOWN_GROUP = Key.key(PLUGIN_ID,"bard_buff_box")

    override fun getDefaultItems(player: Player): MutableList<ItemStack> {
        return super.getDefaultItems(player).also {
            it.add(createBardBox())
        }
    }

    override fun onUnselect(player: Player) {
        buffboxes.removeIf {
            if (it.owner == player) {
                it.level.setBlockAndUpdate(it.pos, Blocks.AIR.defaultBlockState())
                return@removeIf true
            }
            return@removeIf false
        }

        super.onUnselect(player)
    }

    const val BUFF_BOX_RANGE = 15

    enum class Buff(val title: String, val description: String, val icon: Item, val effect: Holder<MobEffect>, val isBuff: Boolean) {
        INVIGORATE(title = "Invigorate",description = "味方に再生を付与する。",icon = Items.MUSIC_DISC_MALL,effect = MobEffects.REGENERATION, isBuff = true),
        ENLIGHTEN(title = "Enlighten",description = "味方に移動速度上昇を付与する。",icon = Items.MUSIC_DISC_FAR,effect = MobEffects.SPEED, isBuff = true),
        INTIMIDATE(title = "Intimidate",description = "敵に弱体化を付与する。",icon = Items.MUSIC_DISC_MELLOHI,effect = MobEffects.WEAKNESS, isBuff = false),
        SHACKLE(title = "Shackle",description = "敵に移動速度低下を付与する。",icon = Items.MUSIC_DISC_STAL,effect = MobEffects.SLOWNESS, isBuff = false),
    }

    data class Buffbox(val owner: Player,val level: Level, val pos: BlockPos,var buff: Buff?) {
        var tick: Int = 0
    }

    val buffboxes = mutableListOf<Buffbox>()

    @EventHandler(priority = EventPriority.HIGH)
    fun onPlace(event: BlockPlaceEvent) {
        if (event.isCancelled) return

        val player = event.player
        if (!isSelected(player)) return

        val item = event.itemInHand
        if (item.getAnniId() != BUFF_BOX_ITEM_ID) return

        if (buffboxes.any { it.owner == player }) {
            player.sendMessage("これは2個以上置けません!")
            event.isCancelled = true
            return
        }

        val block = event.block
        val level = player.toMC().level()
        val pos = BlockPos(block.x,block.y,block.z)

        level.setBlockAndUpdate(pos, Blocks.JUKEBOX.defaultBlockState())
        buffboxes.add(Buffbox(player,level,pos,null))
    }

    @EventHandler
    fun onTick(event: ServerTickStartEvent) {
        if (buffboxes.isEmpty()) return

        buffboxes.removeAll { box ->
            val state = box.level.getBlockState(box.pos)
            if (state.block != Blocks.JUKEBOX) {
                box.owner.give(createBardBox())
                return@removeAll true
            }

            box.tick++
            if (box.tick % 20 == 0) {
                val buff = box.buff ?: return@removeAll false

                val pos = box.pos
                val aabb = AABB((pos.x + BUFF_BOX_RANGE).toDouble(),(pos.y + BUFF_BOX_RANGE).toDouble(),(pos.z + BUFF_BOX_RANGE).toDouble(),(pos.x - BUFF_BOX_RANGE).toDouble(),(pos.y - BUFF_BOX_RANGE).toDouble(),(pos.z - BUFF_BOX_RANGE).toDouble())
                val centerPos = pos.center
                val team = box.owner.toMC().teamColor

                box.level.getEntitiesOfClass(ServerPlayer::class.java,aabb) { centerPos.distanceTo(it.position()) < BUFF_BOX_RANGE }
                    .forEach { target ->
                        val isTeammate = target.teamColor == team
                        if (isTeammate == buff.isBuff) {
                            target.addEffect(MobEffectInstance(buff.effect,60,0))
                        }
                    }

                val distance = box.tick % 160 / 20 * 1.3
                val amount = box.tick % 160 / 20 * 10
                val world = box.level.world

                repeat(amount) {
                    val angle = 360f / amount * it * PI / 180f
                    val x = pos.x + 0.5 + distance * cos(angle)
                    val z = pos.z + 0.5 + distance * sin(angle)
                    val y = pos.y + 0.5

                    Particle.NOTE.builder()
                        .location(world,x,y,z)
                        .receivers(32,true)
                        .spawn()
                }
            }

            return@removeAll false
        }
    }

    @EventHandler
    fun onInteract(event: PlayerInteractEvent) {
        val block = event.clickedBlock

        if (event.action == Action.RIGHT_CLICK_BLOCK && block?.type == Material.JUKEBOX) {
            val pos = BlockPos(block.x,block.y,block.z)
            val level = block.world.toMC()
            val box = buffboxes.find { it.pos == pos && it.level == level } ?: return

            val user = event.player
            val owner = box.owner
            if (owner != user) return

            BuffSelectorGui(box,user.toMC()).open()

            return
        }
    }

    fun createBardBox(): ItemStack {
        return net.minecraft.world.item.ItemStack(Items.JUKEBOX).bukkitStack.apply {
            uniqueClassItem()
            soulbound()
            setAnniItem(BUFF_BOX_ITEM_ID)
            setData(DataComponentTypes.USE_COOLDOWN, UseCooldown.useCooldown(BUFF_BOX_COOLDOWN / 20f).cooldownGroup(BUFF_BOX_COOLDOWN_GROUP).build())

            editMeta {
                it.itemName(Component.text("Buffbox").color(NamedTextColor.AQUA))
            }
        }
    }

    class BuffSelectorGui: ChestPacketGui {
        override val name = "biff selector"
        override val displayName = net.minecraft.network.chat.Component.literal("Buff Selector")

        val box: Buffbox
        val buffs: List<Buff>

        constructor(box: Buffbox, player: ServerPlayer):super(player,9) {
            this.box = box
            this.buffs = Buff.entries

            buffs.forEachIndexed { slot,buff ->
                this.setItem(slot, net.minecraft.world.item.ItemStack(buff.icon).apply {
                    set(DataComponents.ITEM_NAME, net.minecraft.network.chat.Component.literal(buff.title).withColor(0xFFAA00))
                    set(DataComponents.LORE, ItemLore(listOf(net.minecraft.network.chat.Component.literal(buff.description).withStyle(Style.EMPTY.withItalic(false).withColor(0x55FF55)))))
                    set(DataComponents.TOOLTIP_DISPLAY, TooltipDisplay(false,linkedSetOf(DataComponents.JUKEBOX_PLAYABLE)))
                })
            }

            setItem(buffs.size, net.minecraft.world.item.ItemStack(Items.JUKEBOX).apply {
                set(DataComponents.ITEM_NAME, net.minecraft.network.chat.Component.literal("Reclaim Buffbox").withColor(0xFFAA00))
                set(DataComponents.LORE, ItemLore(listOf(net.minecraft.network.chat.Component.literal("Buffboxを回収する。").withStyle(Style.EMPTY.withItalic(false).withColor(0x55FF55)))))
            })
        }

        override fun onClick(packet: ServerboundContainerClickPacket) {
            mc.execute {
                if (!isOpened) return@execute

                if (packet.slotNum.toInt() == buffs.size) {
                    player.bukkitEntity.give(createBardBox())
                    player.bukkitEntity.setCooldown(BUFF_BOX_COOLDOWN_GROUP,BUFF_BOX_COOLDOWN)

                    buffboxes.remove(box)
                    box.level.setBlockAndUpdate(box.pos,Blocks.AIR.defaultBlockState())
                    close()

                    return@execute
                }

                val buff = buffs.getOrNull(packet.slotNum.toInt()) ?: return@execute
                box.buff = buff

                val level = box.level
                val pos = box.pos
                val jukebox = level.getBlockEntity(pos) as? JukeboxBlockEntity ?: return@execute
                val song = JukeboxSong.fromStack(net.minecraft.world.item.ItemStack(buff.icon))

                jukebox.songPlayer.play(level,song.get())
                jukebox.onSongChanged()

                close()
            }
        }
    }
}