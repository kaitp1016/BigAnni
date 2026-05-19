package me.kaitp1016.biganni

import com.google.gson.GsonBuilder
import me.kaitp1016.biganni.anniclass.AnniClassHandler
import me.kaitp1016.biganni.events.EventManager
import me.kaitp1016.biganni.features.*
import me.kaitp1016.biganni.game.boss.BossManager
import me.kaitp1016.biganni.game.Game
import me.kaitp1016.biganni.game.boss.BossBuffItems
import me.kaitp1016.biganni.modifiers.*
import me.kaitp1016.biganni.packetgui.PacketGuiManager
import me.kaitp1016.biganni.utils.FallDamageResistance
import me.kaitp1016.biganni.utils.FullyInvisible
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
            InvisibleModifier,
            DelayingBlock,
            WeaknessModifier,
            FarmlandModifier,
            KnockbackModifier,
            FallDamageResistance,
            FullyInvisible,
            AnniClassHandler,
            PacketGuiManager,
            EventManager,
            Game,
            BossManager,
            BossBuffItems,
            Scheduler,
        ).forEach { plugin.server.pluginManager.registerEvents(it,plugin) }

        BossBuffItems.register()
    }

    override fun onDisable() {
    }
}

const val PLUGIN_ID = "biganni"

lateinit var plugin: JavaPlugin
    private set

val mc = MinecraftServer.getServer()
val gson = GsonBuilder().apply {
    setPrettyPrinting()
}.create()!!