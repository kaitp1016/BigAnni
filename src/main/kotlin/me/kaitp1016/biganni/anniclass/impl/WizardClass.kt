package me.kaitp1016.biganni.anniclass.impl

import com.destroystokyo.paper.ParticleBuilder
import io.papermc.paper.datacomponent.DataComponentTypes
import io.papermc.paper.datacomponent.item.UseCooldown
import me.kaitp1016.biganni.PLUGIN_ID
import me.kaitp1016.biganni.anniclass.AnniClass
import me.kaitp1016.biganni.mc
import me.kaitp1016.biganni.packetgui.ChestPacketGui
import me.kaitp1016.biganni.utils.FallDamageResistance
import me.kaitp1016.biganni.utils.ItemUtils.getAnniId
import me.kaitp1016.biganni.utils.ItemUtils.setAnniItem
import me.kaitp1016.biganni.utils.MCUtils.toMC
import me.kaitp1016.biganni.utils.Utils.toIntCorrect
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.component.DataComponents
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.util.Mth
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.projectile.Projectile
import net.minecraft.world.entity.projectile.throwableitemprojectile.Snowball
import net.minecraft.world.item.Item
import net.minecraft.world.item.Items
import net.minecraft.world.item.component.TooltipDisplay
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.phys.HitResult
import org.bukkit.Material
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.damage.DamageSource
import org.bukkit.damage.DamageType
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityRemoveEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import kotlin.math.PI
import kotlin.random.Random

object WizardClass: AnniClass(), Listener {
    override val name = "Wizard"
    override val shortName = "WZR"
    override val icon = Items.STICK
    override val description = arrayOf(
        "Spellbookを使用すると魔法を選択できる。",
        "Wandを使用することで選択した魔法を使用できる。",
    )

    const val WAND_ITEM_ID = "wizard_wand"
    const val WAND_COOLDOWN = 300
    val WAND_COOLDOWN_GROUP = Key.key(PLUGIN_ID,"wizard_wand")

    const val SPELLBOOK_ITEM_ID = "wizard_spellbook"

    override fun getDefaultItems(player: Player): MutableList<org.bukkit.inventory.ItemStack> {
        return super.getDefaultItems(player).also {
            it.add(org.bukkit.inventory.ItemStack(Material.STICK).apply {
                uniqueClassItem()
                soulbound()
                setAnniItem(WAND_ITEM_ID)

                setData(DataComponentTypes.USE_COOLDOWN, UseCooldown.useCooldown(WAND_COOLDOWN / 20f).cooldownGroup(WAND_COOLDOWN_GROUP).build())

                editMeta {
                    it.itemName(Component.text("Wand").color(NamedTextColor.GOLD))
                }
            })

            it.add(org.bukkit.inventory.ItemStack(Material.ENCHANTED_BOOK).apply {
                uniqueClassItem()
                soulbound()
                setAnniItem(SPELLBOOK_ITEM_ID)
                
                editMeta {
                    it.itemName(Component.text("Spellbook").color(NamedTextColor.GOLD))
                }
            })
        }
    }

    override fun onUnselect(player: Player) {
        selectedSpell.remove(player)

        super.onUnselect(player)
    }

    enum class SpellType(val displayName: String, val display: Item, val particle: ParticleBuilder,val distance: Double) {
        INFERNO(displayName = "§cInferno",display = Items.NETHERRACK, particle = Particle.FLAME.builder(),1.0),
        VOID_BOLT(displayName = "§4Void Bolt",display = Items.PURPLE_WOOL, particle = Particle.ENTITY_EFFECT.builder().color(255,255,255),1.0),
        ARCANE_BOLT(displayName = "§3Arcane Bolt",display = Items.DIAMOND_SWORD, particle = Particle.SWEEP_ATTACK.builder(),1.0),
        GLACIAL_NOVA(displayName = "§bGlacial Nova",display = Items.ICE, particle = Particle.ITEM_SNOWBALL.builder(),2.0),
        WHRLWIND(displayName = "§7Whirlwind",display = Items.COBWEB, particle = Particle.CLOUD.builder(),3.0);

        fun next(): SpellType {
            val entries = entries
            val index = entries.indexOf(this)
            return entries.getOrNull(index + 1) ?: entries.first()
        }
    }

    val selectedSpell = mutableMapOf<Player, SpellType>()

    @EventHandler
    fun onInteract(event: PlayerInteractEvent) {
        val player = event.player
        if (!isSelected(player)) return

        val item = event.item ?: return
        val anniId = item.getAnniId()

        if (anniId == WAND_ITEM_ID) {
            if (event.action.isLeftClick) {
                val spell = selectedSpell.getOrPut(player) { SpellType.entries.first() }.next()
                selectedSpell[player] = spell

                player.sendMessage("${spell.displayName} §rを選択しました!")
                player.playSound(player, Sound.UI_BUTTON_CLICK,1f,1f)
            }
            else {
                if (player.hasCooldown(item)) return

                val spell = selectedSpell[player]
                if (spell == null) {
                    player.sendMessage("スペルを選択していません!")
                    return
                }

                val mcPlayer = player.toMC()
                val level = mcPlayer.level()
                val mcItem = item.toMC() ?: return
                val direction = mcPlayer.direction

                val snowball = Projectile.spawnProjectileFromRotationDelayed({ level: ServerLevel, mob: net.minecraft.world.entity.LivingEntity, aa: net.minecraft.world.item.ItemStack -> Bullet(level,mcPlayer, spell,direction) }, level, mcItem, mcPlayer, 0.0f, 1.0f, 1.0f)
                if (!snowball.attemptSpawn()) return

                player.setCooldown(WAND_COOLDOWN_GROUP,WAND_COOLDOWN)
            }
        }
        if (anniId == SPELLBOOK_ITEM_ID) {
            val mcPlayer = player.toMC()
            SpellSelectorGui(mcPlayer).open()
        }
    }

    class Bullet : Snowball {
        val thrower: ServerPlayer
        val bulletDirection: Direction
        val spell: SpellType
        var tick = 0

        constructor(level: ServerLevel, thrower: ServerPlayer, spell: SpellType, direction: Direction):super(level,thrower, net.minecraft.world.item.ItemStack(spell.display)) {
            this.thrower = thrower
            this.bulletDirection = direction
            this.spell = spell

            isNoGravity = true
        }

        override fun onHit(hitResult: HitResult) {
            val bukkit = bukkitEntity

            repeat(30) {
                spell.particle.clone()
                    .count(0)
                    .offset(0.0,0.0,0.0)
                    .location(bukkit.location.clone().add(Random.nextDouble(-2.5,2.5),Random.nextDouble(-2.5,2.5),Random.nextDouble(-2.5,2.5),))
                    .receivers(32,true)
                    .spawn()
            }

            val world = bukkit.world
            val team = thrower.teamColor
            val targets = world.getNearbyPlayers(bukkit.location,spell.distance * 2,spell.distance * 2,spell.distance * 2).filter { it.toMC().teamColor != team }

            when(spell) {
                SpellType.INFERNO -> {
                    targets.forEach { target ->
                        target.fireTicks = 200
                        val pos = BlockPos(target.x.toInt(),target.y.toIntCorrect(),target.z.toInt())
                        val world = target.world
                        val level = world.toMC()
                        if (world.toMC().getBlockState(pos).canBeReplaced()) {
                            level.setBlockAndUpdate(pos, Blocks.FIRE.defaultBlockState())
                        }
                    }
                }
                SpellType.VOID_BOLT -> {
                    targets.forEach { target ->
                        target.addPotionEffect(PotionEffect(PotionEffectType.WITHER,100,0))
                        target.addPotionEffect(PotionEffect(PotionEffectType.BLINDNESS,100,0))
                    }
                }
                SpellType.ARCANE_BOLT -> {
                    val source = DamageSource.builder(DamageType.PLAYER_ATTACK)
                        .withDirectEntity(bukkit)
                        .withCausingEntity(thrower.bukkitEntity)
                        .build()

                    targets.forEach { target ->
                        target.damage(6.0,source)
                    }
                }
                SpellType.GLACIAL_NOVA -> {
                    targets.forEach { target ->
                        target.addPotionEffect(PotionEffect(PotionEffectType.SLOWNESS,100,2))
                        target.addPotionEffect(PotionEffect(PotionEffectType.MINING_FATIGUE,100,0))
                    }
                }
                SpellType.WHRLWIND -> {
                    targets.forEach { target ->
                        val knockback = bukkit.location.clone().subtract(target.location).toVector().normalize()
                        target.velocity = knockback

                        FallDamageResistance.add(target,75)
                    }
                }
            }

            discard(EntityRemoveEvent.Cause.PLUGIN)
        }

        override fun tick() {
            spell.particle.clone()
                .location(bukkitEntity.location.clone().add(Random.nextDouble(-0.5,0.5),Random.nextDouble(-0.5,0.5),Random.nextDouble(-0.5,0.5)))
                .receivers(32,true)
                .offset(0.0,0.0,0.0)
                .count(0)
                .spawn()

            tick += 1
            if (tick > 200) {
                discard()
            }

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

        override fun shouldBeSaved(): Boolean {
            return false
        }
    }

    class SpellSelectorGui: ChestPacketGui {
        override val name = "spell selector"
        override val displayName = net.minecraft.network.chat.Component.literal("Select Spell")

        val spells: List<SpellType>

        constructor(player: ServerPlayer):super(player,9) {
            this.spells = SpellType.entries

            spells.forEachIndexed { slot,buff ->
                this.setItem(slot, net.minecraft.world.item.ItemStack(buff.display).apply {
                    set(DataComponents.ITEM_NAME, net.minecraft.network.chat.Component.literal(buff.displayName).withColor(0xFFAA00))
                    set(DataComponents.TOOLTIP_DISPLAY, TooltipDisplay(false,linkedSetOf(DataComponents.JUKEBOX_PLAYABLE)))
                })
            }
        }

        override fun onClick(packet: ServerboundContainerClickPacket) {
            mc.execute {
                if (!isOpened) return@execute

                val spell = spells.getOrNull(packet.slotNum.toInt()) ?: return@execute
                selectedSpell[player.bukkitEntity] = spell

                close()
            }
        }
    }
}