package me.kaitp1016.biganni.anniclass.impl

import com.destroystokyo.paper.event.server.ServerTickStartEvent
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
import net.minecraft.tags.ItemTags
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.item.Items
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.SoundCategory
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeModifier
import org.bukkit.damage.DamageType
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.EquipmentSlotGroup
import org.bukkit.inventory.ItemStack

object LumberjackClass: AnniClass(), Listener {
    override val icon = Items.STONE_AXE
    override val name = "Lumberjack"
    override val shortName = "LUM"
    override val description = arrayOf(
        "斧の近接ダメージが常に増える。",
        "原木を掘った時に追加で原木を入手できる。",
        "アビリティの効果中に斧で殴ることで敵の防具の耐久値を減らすことができる。"
    )

    const val BRUTE_FORCE_ITEM_ID = "lumberjack_brute_force"
    const val BRUTE_FORCE_COOLDOWN = 900
    val BRUTE_FORCE_COOLDOWN_GROUP = Key.key(PLUGIN_ID, "lumberjack_brute_force")

    const val BRUTE_FORCE_TIME = 300

    override fun getDefaultItems(player: Player): MutableList<ItemStack> {
        return super.getDefaultItems(player).also {
            it.removeIf { it.type == Material.WOODEN_AXE }
            it.add(ItemStack(Material.STONE_AXE).uniqueClassItem().soulbound().apply {
                addEnchantment(Enchantment.EFFICIENCY, 1)

                editMeta {
                    it.addAttributeModifier(Attribute.ATTACK_DAMAGE, AttributeModifier(AXE_ATTRIBUTE_MODIFIER_KEY,3.5, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.MAINHAND))
                }
            })

            it.add(ItemStack(Material.BRICKS).apply {
                uniqueClassItem()
                soulbound()
                setAnniItem(BRUTE_FORCE_ITEM_ID)

                setData(DataComponentTypes.USE_COOLDOWN, UseCooldown.useCooldown(BRUTE_FORCE_COOLDOWN / 20f).cooldownGroup(BRUTE_FORCE_COOLDOWN_GROUP).build())

                editMeta {
                    it.itemName(Component.text("Brute Force").color(NamedTextColor.GOLD))
                }
            })
        }
    }

    data class BruteForceAbility(val player: Player, var time: Int)

    val bruteForces = mutableListOf<BruteForceAbility>()

    val armorSlots = arrayOf(EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET)

    @EventHandler
    fun onInteract(event: PlayerInteractEvent) {
        val player = event.player
        if (event.isCancelled || !isSelected(player)) return

        val item = event.item ?: return
        if (item.getAnniId() != BRUTE_FORCE_ITEM_ID || player.hasCooldown(item)) return

        bruteForces.add(BruteForceAbility(player, BRUTE_FORCE_TIME))
        player.playSound(player, Sound.ENTITY_ZOMBIE_ATTACK_IRON_DOOR, SoundCategory.MASTER,1f,0f,6L)
        player.setCooldown(BRUTE_FORCE_COOLDOWN_GROUP, BRUTE_FORCE_COOLDOWN)

        event.isCancelled = true
    }

    @EventHandler
    fun onTick(event: ServerTickStartEvent) {
        if (bruteForces.isEmpty()) return

        bruteForces.removeAll {
            it.time--
            return@removeAll it.time <= 0
        }
    }

    @EventHandler
    fun onDamage(event: EntityDamageEvent) {
        val source = event.damageSource
        val entity = event.entity as? Player ?: return

        val attacker = source.causingEntity
        if (attacker !is Player || !isSelected(attacker) || bruteForces.none { it.player == attacker } || !attacker.toMC().mainHandItem.`is`(ItemTags.AXES) || source.damageType != DamageType.PLAYER_ATTACK) return

        val target = entity.toMC()
        val damage = getDamage(attacker.inventory.itemInMainHand.type) ?: return

        armorSlots.forEach {
            target.getItemBySlot(it).hurtAndBreak(damage, target, it)
        }

        event.damage += 1.0
        entity.world.playSound(entity.location, Sound.ENTITY_ZOMBIE_ATTACK_IRON_DOOR,1f,1f)
    }

    fun getMultiply(player: Player): Int {
        if (!isSelected(player)) return 1
        return 2
    }

    fun getDamage(item: Material): Int? {
        if (item == Material.WOODEN_AXE) return 3
        if (item == Material.GOLDEN_AXE) return 3
        if (item == Material.STONE_AXE) return 6
        if (item == Material.IRON_AXE) return 9
        if (item == Material.DIAMOND_AXE) return 12
        if (item == Material.NETHERITE_AXE) return 12
        return null
    }
}