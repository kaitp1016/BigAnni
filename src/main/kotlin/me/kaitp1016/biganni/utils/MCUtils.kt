package me.kaitp1016.biganni.utils

import io.papermc.paper.adventure.PaperAdventure
import net.kyori.adventure.text.Component
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.level.block.state.BlockState
import org.bukkit.World
import org.bukkit.block.Block
import org.bukkit.craftbukkit.CraftWorld
import org.bukkit.craftbukkit.block.CraftBlock
import org.bukkit.craftbukkit.entity.CraftEntity
import org.bukkit.craftbukkit.entity.CraftPlayer
import org.bukkit.craftbukkit.inventory.CraftItemStack
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import net.minecraft.world.item.ItemStack as MCItemStack
import net.minecraft.world.entity.Entity as MCEntity
import net.minecraft.network.chat.Component as MCComponent

object MCUtils {
    fun ItemStack.toMC(): MCItemStack? {
        return (this as CraftItemStack).handle
    }

    fun Player.toMC(): ServerPlayer {
        return (this as CraftPlayer).handle
    }

    fun Entity.toMC(): MCEntity {
        return (this as CraftEntity).handle
    }

    fun Block.toMC(): BlockState {
        return (this as CraftBlock).blockState
    }

    fun World.toMC(): ServerLevel {
        return (this as CraftWorld).handle
    }

    fun Component.toMC(): MCComponent {
        return PaperAdventure.asVanilla(this)
    }

    fun MCComponent.toBukkit(): Component {
        return PaperAdventure.asAdventure(this)
    }
}