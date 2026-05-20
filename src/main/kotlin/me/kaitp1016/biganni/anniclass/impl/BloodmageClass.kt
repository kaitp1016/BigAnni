package me.kaitp1016.biganni.anniclass.impl

import com.destroystokyo.paper.event.server.ServerTickStartEvent
import io.papermc.paper.datacomponent.DataComponentTypes
import io.papermc.paper.datacomponent.item.UseCooldown
import me.kaitp1016.biganni.PLUGIN_ID
import me.kaitp1016.biganni.anniclass.AnniClass
import me.kaitp1016.biganni.plugin
import me.kaitp1016.biganni.utils.ItemUtils.getAnniId
import me.kaitp1016.biganni.utils.ItemUtils.setAnniItem
import me.kaitp1016.biganni.utils.LevelBlockPos
import me.kaitp1016.biganni.utils.MCUtils.toMC
import me.kaitp1016.biganni.utils.Scheduler
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.minecraft.core.BlockPos
import net.minecraft.world.item.Items
import net.minecraft.world.level.block.state.BlockState
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.Sound
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeModifier
import org.bukkit.damage.DamageType
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockDropItemEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType

object BloodmageClass: AnniClass(), Listener {
    override val icon = Items.FERMENTED_SPIDER_EYE
    override val name = "Bloodmage"
    override val shortName = "BMG"
    override val description = arrayOf(
        "攻撃をしたときに毒の効果を与える確率がある。",
        "Corruptを使用すると周囲の敵の最大体力を減らし、ウィザーの効果を与える。",
        "Bloodcursed Terraformを使用すると周囲のブロックを変え、その範囲にいる敵にデバフを与える。",
    )

    const val CORRUPT_ITEM_ID = "bloodmage_corrupt"
    const val CORRUPT_COOLDOWN = 1200
    val CORRUPT_COOLDOWN_GROUP = Key.key(PLUGIN_ID,"bloodmage_corrupt")

    const val BLOODCURSED_TERRAFORM_ITEM_ID = "bloodmage_bloodcursed_terraform"
    const val BLOODCURSED_TERRAFORM_COOLDOWN = 2400
    val BLOODCURSED_TERRAFORM_COOLDOWN_GROUP = Key.key(PLUGIN_ID,"bloodmage_bloodcursed_terraform")

    const val TERRAFORM_DISTANCE = 16
    const val TERRAFORM_EFFECT_TICK = 600

    const val CURRUPT_TIME = 200
    const val CURSE_COOLDOWN = 200

    val terraformBlocks = mapOf(
        Material.DIRT to Material.NETHERRACK,
        Material.GRASS_BLOCK to Material.CRIMSON_NYLIUM,
        Material.SHORT_GRASS to Material.CRIMSON_ROOTS,
        Material.OAK_LOG to Material.CRIMSON_STEM,
        Material.STRIPPED_OAK_LOG to Material.STRIPPED_CRIMSON_STEM,
        Material.OAK_PLANKS to Material.CRIMSON_PLANKS,
        Material.OAK_LEAVES to Material.BLACKSTONE,
        Material.SEA_LANTERN to Material.GLOWSTONE,
        Material.LANTERN to Material.SOUL_LANTERN,
        Material.SAND to Material.SOUL_SAND,
        Material.DANDELION to Material.CRIMSON_FUNGUS,
        Material.OXEYE_DAISY to Material.CRIMSON_FUNGUS,
        Material.POPPY to Material.CRIMSON_FUNGUS,
        Material.CORNFLOWER to Material.CRIMSON_FUNGUS,
    )

    override fun getDefaultItems(player: Player): MutableList<ItemStack> {
        return super.getDefaultItems(player).also {
            it.removeIf { it.type == Material.WOODEN_SWORD }
            it.add(ItemStack(Material.STONE_SWORD).uniqueClassItem().soulbound())

            it.add(ItemStack(Material.SPIDER_EYE).apply {
                uniqueClassItem()
                soulbound()
                setAnniItem(CORRUPT_ITEM_ID)

                setData(DataComponentTypes.USE_COOLDOWN, UseCooldown.useCooldown(CORRUPT_COOLDOWN / 20f).cooldownGroup(CORRUPT_COOLDOWN_GROUP).build())

                editMeta {
                    it.itemName(Component.text("Corrupt").color(NamedTextColor.GOLD))
                }
            })

            it.add(ItemStack(Material.SOUL_SAND).apply {
                uniqueClassItem()
                soulbound()
                setAnniItem(BLOODCURSED_TERRAFORM_ITEM_ID)

                setData(DataComponentTypes.USE_COOLDOWN, UseCooldown.useCooldown(BLOODCURSED_TERRAFORM_COOLDOWN / 20f).cooldownGroup(BLOODCURSED_TERRAFORM_COOLDOWN_GROUP).build())

                editMeta {
                    it.itemName(Component.text("Bloodcursed Terraform").color(NamedTextColor.GOLD))
                }
            })
        }
    }

    data class CurseCooldown(val player: Player,var tick: Int = 200)

    val terraforms = mutableListOf<Terraform>()
    val curseCooldown = mutableListOf<CurseCooldown>()

    data class TerraformedBlock(val pos: LevelBlockPos, val original: BlockState,val current: Material)

    data class Terraform(val pos: LevelBlockPos,val player: Player) {
        val terraforms = mutableListOf<TerraformedBlock>()
        var tick = TERRAFORM_EFFECT_TICK

        fun start() {
            val level = pos.level

            repeat(TERRAFORM_DISTANCE * TERRAFORM_DISTANCE * TERRAFORM_DISTANCE) { index ->
                val dx = index % TERRAFORM_DISTANCE - TERRAFORM_DISTANCE / 2
                val dy = index / TERRAFORM_DISTANCE % TERRAFORM_DISTANCE - TERRAFORM_DISTANCE / 2
                val dz = index / TERRAFORM_DISTANCE / TERRAFORM_DISTANCE - TERRAFORM_DISTANCE / 2
                val pos = BlockPos(dx + pos.x,dy + pos.y,dz + pos.z )
                val block = level.getBlockState(pos)
                val material = block.bukkitMaterial

                val replacement = terraformBlocks[material] ?: return@repeat
                val terraform = TerraformedBlock(LevelBlockPos(level,pos.x,pos.y,pos.z),block,replacement)
                level.setBlock(pos,replacement.createBlockData().toMC(),818)

                terraforms.add(terraform)
            }
        }

        fun tick(): Boolean {
            tick--

            if (tick < 1) {
                terraforms.forEach { terraform ->
                    val pos = terraform.pos
                    val level = pos.level
                    val blockPos = BlockPos(pos.x,pos.y,pos.z)
                    if (level.getBlockState(blockPos).bukkitMaterial == terraform.current) level.setBlock(blockPos,terraform.original,818)
                }

                return true
            }

            val level = pos.level
            val world = level.world
            val location = Location(world,pos.x + 0.5,pos.y + 0.5,pos.z + 0.5)

            val team = player.toMC().teamColor
            world.getNearbyPlayers(location,TERRAFORM_DISTANCE / 2.0).forEach { player ->
                if (player.toMC().teamColor == team || curseCooldown.any { it.player == player }) return@forEach

                player.addPotionEffect(PotionEffect(PotionEffectType.WITHER,100,1))
                player.addPotionEffect(PotionEffect(PotionEffectType.BLINDNESS,200,0))
                player.addPotionEffect(PotionEffect(PotionEffectType.HUNGER,200,2))

                curseCooldown.add(CurseCooldown(player,CURSE_COOLDOWN))
            }

            return false
        }
    }

    val CORRUPT_ATTRIBUTE_KEY = NamespacedKey(plugin,"bloodmage_corrupt_max_health_reduce")

    @EventHandler
    fun onInteract(event: PlayerInteractEvent) {
        val player = event.player
        if (!isSelected(player) || event.isCancelled) return

        val item = event.item ?: return
        if (player.hasCooldown(item)) return

        val anniId = item.getAnniId()

        if (anniId == CORRUPT_ITEM_ID) {
            event.isCancelled = true
            val team = player.toMC().teamColor

            val targets = player.world.getNearbyPlayers(player.location, 2.5).filter { it.toMC().teamColor != team }
            if (targets.isEmpty()) return

            targets.forEach { target ->
                val attribute = target.getAttribute(Attribute.MAX_HEALTH)
                if (attribute == null || attribute.getModifier(CORRUPT_ATTRIBUTE_KEY) != null) return@forEach

                attribute.addTransientModifier(AttributeModifier(CORRUPT_ATTRIBUTE_KEY, -4.0, AttributeModifier.Operation.ADD_NUMBER))

                Scheduler.scheduleTask(CURRUPT_TIME) {
                    attribute.removeModifier(CORRUPT_ATTRIBUTE_KEY)
                }

                target.playSound(target, Sound.ENTITY_WITHER_DEATH,1f,1f)
            }

            player.setCooldown(CORRUPT_COOLDOWN_GROUP, CORRUPT_COOLDOWN)
            player.world.playSound(player.location, Sound.ENTITY_WITHER_DEATH,1f,1f)
        }

        if (anniId == BLOODCURSED_TERRAFORM_ITEM_ID) {
            event.isCancelled = true

            val mcPlayer = player.toMC()
            val pos = LevelBlockPos(mcPlayer.level(),mcPlayer.x.toInt(),mcPlayer.y.toInt(),mcPlayer.z.toInt())
            val terraform = Terraform(pos,player)
            terraform.start()

            terraforms.add(terraform)
            player.setCooldown(item,BLOODCURSED_TERRAFORM_COOLDOWN)
        }
    }

    @EventHandler
    fun onDamage(event: EntityDamageEvent) {
        val source = event.damageSource
        val entity = event.entity
        if (entity !is Player) return

        val attacker = source.causingEntity
        if (attacker is Player && source.damageType == DamageType.PLAYER_ATTACK && isSelected(attacker)) {
            entity.addPotionEffect(PotionEffect(PotionEffectType.WITHER,40,0))
        }
    }

    @EventHandler
    fun onBlockDropItem(event: BlockDropItemEvent) {
        val block = event.block
        val level = block.world.toMC()
        val pos = LevelBlockPos(level,block.x,block.y,block.z)

        terraforms.forEach { terraform ->
            terraform.terraforms.forEach {
                if (it.pos == pos) {
                    event.items.clear()
                }
            }
        }
    }

    @EventHandler
    fun onTick(event: ServerTickStartEvent) {
        if (!terraforms.isEmpty()) {
            terraforms.removeAll {
                it.tick()
                return@removeAll it.tick < 1
            }
        }

        if (!curseCooldown.isEmpty()) {
            curseCooldown.removeAll {
                it.tick--
                return@removeAll it.tick < 1
            }
        }
    }
}