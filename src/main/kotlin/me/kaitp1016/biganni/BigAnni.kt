package me.kaitp1016.biganni

import me.kaitp1016.biganni.anniclass.AnniClassHandler
import me.kaitp1016.biganni.events.EventManager
import me.kaitp1016.biganni.game.Game
import me.kaitp1016.biganni.modifiers.*
import me.kaitp1016.biganni.packetgui.PacketGuiManager
import me.kaitp1016.biganni.utils.Scheduler
import net.minecraft.server.MinecraftServer
import org.bukkit.plugin.java.JavaPlugin

class BigAnni : JavaPlugin() {
    override fun onEnable() {
        plugin = this

        listOf(
            DamageModifier,
            EnchantModifier,
            FastCraft,
            ItemSign,
            RequiredTool,
            LauncherPad,
            GoldenAppleModifier,
            BrewingStandModifier,
            TaggedItem,
            RespawnBlocks,
            CooldownFix,
            EnderFurnace,
            AnniClassHandler,
            PacketGuiManager,
            EventManager,
            Game,
            Scheduler,
        ).forEach { plugin.server.pluginManager.registerEvents(it,plugin) }
    }

    override fun onDisable() {
    }
}

const val PLUGIN_ID = "biganni"

lateinit var plugin: JavaPlugin
    private set

val mc = MinecraftServer.getServer()