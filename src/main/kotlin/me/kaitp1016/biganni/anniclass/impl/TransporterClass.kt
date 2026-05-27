package me.kaitp1016.biganni.anniclass.impl

import com.destroystokyo.paper.event.server.ServerTickStartEvent
import me.kaitp1016.biganni.anniclass.AnniClass
import me.kaitp1016.biganni.utils.ItemUtils.getAnniId
import me.kaitp1016.biganni.utils.ItemUtils.setAnniItem
import me.kaitp1016.biganni.utils.MCUtils.toMC
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.minecraft.core.BlockPos
import net.minecraft.world.item.Items
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.World
import org.bukkit.block.BlockState
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.ItemStack
import java.util.UUID
import kotlin.random.Random

object TransporterClass: AnniClass(), Listener {
    override val name = "Transporter"
    override val shortName = "TRA"
    override val icon = Items.QUARTZ
    override val description = arrayOf(
        "アビリティを始点と終点で使用すると、ポータルを設置できる。",
        "ポータルはスニークすることで通過することができ、味方も通過できる。"
    )

    const val PORTAL_MAKER_ID = "transporter_portal_maker"

    override fun getDefaultItems(player: Player): MutableList<ItemStack> {
        return super.getDefaultItems(player).also {
            it.add(ItemStack(Material.QUARTZ).apply {
                uniqueClassItem()
                soulbound()
                setAnniItem(PORTAL_MAKER_ID)

                editMeta {
                    it.itemName(Component.text("Portal Maker").color(NamedTextColor.GOLD))
                }
            })
        }
    }

    override fun onUnselect(player: Player) {
        super.onUnselect(player)

        val uuid = player.uniqueId
        val portal = portals.find { it.owner == uuid } ?: return

        destroy(portal)
        portals.remove(portal)
    }

    data class PortalBlock(val world: World, val pos: BlockPos, val originalBlock: BlockState) {
        val location = Location(world,pos.x.toDouble(),pos.y.toDouble(),pos.z.toDouble())
        var portal: Portal? = null
        var otherSide: PortalBlock? = null

        fun destroy() {
            world.setBlockData(pos.x,pos.y,pos.z, originalBlock.blockData)
            world.playSound(location,world.getBlockAt(pos.x,pos.y,pos.z).blockSoundGroup.breakSound,1f,1f)
        }

        fun tick() {
            spawnParticle()

            if (otherSide == null || !canUse(world,pos)) return

            world.getNearbyPlayers(location.clone().add(0.5,1.0,0.5),0.3,0.1).forEach { player ->
                val ownerUUID = portal?.owner ?: return@forEach
                val owner = Bukkit.getPlayer(ownerUUID) ?: return@forEach
                val ownerName = owner.teamDisplayName()
                player.sendActionBar(ownerName.append(Component.text("'s Portal").color(NamedTextColor.GRAY)))

                if (owner.toMC().teamColor !=  player.toMC().teamColor || !player.isSneaking || teleportPlayers.contains(player) || !canUse(otherSide!!.world,otherSide!!.pos)) return@forEach

                player.teleport(otherSide!!.location.clone().apply {
                    add(0.5,1.0,0.5)
                    yaw = player.yaw
                    pitch = player.pitch
                })

                teleportPlayers.add(player)
                player.playSound(player, Sound.ENTITY_ENDERMAN_TELEPORT,1f,1f)

                owner.sendMessage("${player.name} used your portal.")
            }
        }

        fun spawnParticle() {
            if (portal != null) {
                Particle.SPIT.builder()
                    .location(location.clone().add(Random.nextDouble(0.3,0.7),Random.nextDouble(2.2,2.5),Random.nextDouble(0.3,0.7)))
                    .offset(0.0,-0.2,0.0)
                    .count(0)
                    .receivers(16,true)
                    .spawn()
            }
            else {
                Particle.SMOKE.builder()
                    .location(location.clone().add(Random.nextDouble(0.0,1.0),Random.nextDouble(1.7,2.2),Random.nextDouble(0.0,1.0)))
                    .count(0)
                    .offset(0.0,0.05,0.0)
                    .receivers(16,true)
                    .spawn()
            }
        }
    }

    data class Portal(val owner: UUID, val first: PortalBlock, var secound: PortalBlock?)

    val portals = mutableListOf<Portal>()
    val teleportPlayers = mutableListOf<Player>()

    @EventHandler
    fun onInteract(event: PlayerInteractEvent) {
        val player = event.player
        val clickedBlock = event.clickedBlock ?: return
        val pos = BlockPos(clickedBlock.x,clickedBlock.y,clickedBlock.z)

        val destroyNeeded = mutableListOf<Portal>()

        portals.forEach { portal ->
            if (portal.first.pos == pos || portal.secound?.pos == pos) {
                val creator = Bukkit.getPlayer(portal.owner)
                if (creator == null || creator.toMC().teamColor != player.toMC().teamColor) {
                    destroyNeeded.add(portal)
                }
            }
        }

        destroyNeeded.forEach {
            destroy(it)
        }

        if (event.action != Action.RIGHT_CLICK_BLOCK || !isSelected(player)) return

        val hand = event.hand ?: return
        val item = event.player.inventory.getItem(hand)
        if (item.getAnniId() != PORTAL_MAKER_ID) return

        val world = clickedBlock.world

        val uuid = player.uniqueId
        var portal = portals.find { it.owner == uuid }

        if (portal != null) {
            if (portal.secound != null) {
                destroy(portal)
            }
            else {
                if (!canUse(world,pos) || !clickedBlock.blockData.toMC().occlusionShape.`moonrise$isFullBlock`() || clickedBlock.blockData.toMC().hasBlockEntity() || clickedBlock.type == Material .NETHER_QUARTZ_ORE) {
                    player.sendMessage(Component.text("ここにはおけません!").color(NamedTextColor.RED))
                    return
                }

                val originalBlock = world.getBlockState(clickedBlock.location)
                world.setBlockData(pos.x,pos.y,pos.z, Bukkit.createBlockData(Material.NETHER_QUARTZ_ORE))

                portal.secound = PortalBlock(world,pos,originalBlock)
                player.playSound(player.location, Sound.ENTITY_BLAZE_HURT,1f,2f)

                portal.first.portal = portal
                portal.secound!!.portal = portal

                portal.first.otherSide = portal.secound!!
                portal.secound!!.otherSide = portal.first

                return
            }
        }

        if (!canUse(world,pos) || !clickedBlock.blockData.toMC().occlusionShape.`moonrise$isFullBlock`() || clickedBlock.blockData.toMC().hasBlockEntity() || clickedBlock.type == Material .NETHER_QUARTZ_ORE) {
            player.sendMessage(Component.text("ここにはおけません!").color(NamedTextColor.RED))
            return
        }

        val originalBlock = world.getBlockState(clickedBlock.location)
        world.setBlockData(pos.x,pos.y,pos.z, Bukkit.createBlockData(Material.NETHER_QUARTZ_ORE))

        portal = Portal(uuid, PortalBlock(world,pos,originalBlock),null)

        player.playSound(player, Sound.ENTITY_BLAZE_HURT,1f,1f)
        portals.add(portal)
    }

    @EventHandler
    fun onDeath(event: PlayerDeathEvent) {
        val player = event.player
        if (!isSelected(player)) return

        val uuid = player.uniqueId
        val portal = portals.find { it.owner == uuid } ?: return
        if (portal.secound != null) return

        destroy(portal)
        portals.remove(portal)
    }

    @EventHandler
    fun onTick(event: ServerTickStartEvent) {
        if (teleportPlayers.isNotEmpty()) {
            teleportPlayers.removeAll { !it.isOnline || !it.isSneaking }
        }

        portals.forEach { portal ->
            portal.first.tick()
            portal.secound?.tick()
        }
    }

    @EventHandler
    fun onBreak(event: BlockBreakEvent) {
        val block = event.block
        val pos = BlockPos(block.x,block.y,block.z)

        if (portals.any { portal -> portal.first.pos == pos || portal.secound?.pos == pos }) {
            event.isCancelled = true
        }
    }

    private fun canUse(world: World, pos: BlockPos): Boolean {
        return world.getBlockAt(pos.x,pos.y + 1,pos.z).type == Material.AIR && world.getBlockAt(pos.x,pos.y + 2,pos.z).type == Material.AIR
    }

    private fun destroy(portal: Portal) {
        portals.remove(portal)

        portal.first.destroy()
        portal.secound?.destroy()
    }
}