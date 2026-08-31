package me.kaitp1016.biganni

import com.google.gson.GsonBuilder
import me.kaitp1016.biganni.anniclass.AnniClassHandler
import me.kaitp1016.biganni.events.EventManager
import me.kaitp1016.biganni.features.*
import me.kaitp1016.biganni.game.Game
import me.kaitp1016.biganni.game.MapProtector
import me.kaitp1016.biganni.game.StartCountdown
import me.kaitp1016.biganni.game.boss.BossBuffItems
import me.kaitp1016.biganni.game.boss.BossManager
import me.kaitp1016.biganni.game.boss.FinalBossFight
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
            GoldenAppleModifier,
            BrewingStandModifier,
            InvisibleModifier,
            WeaknessModifier,
            FarmlandModifier,
            KnockbackModifier,
            HealModifier,
            FastCraft,
            ItemSign,
            RequiredTool,
            LauncherPad,
            TaggedItem,
            RespawnBlocks,
            CooldownFix,
            EnderFurnace,
            DelayingBlock,
            TeamDoor,
            PrivateStand,
            ServerLink,
            CombatTag,
            FallDamageResistance,
            FullyInvisible,
            CooldownVisualizer,
            AnniClassHandler,
            PacketGuiManager,
            EventManager,
            Game,
            MapProtector,
            BossManager,
            FinalBossFight,
            StartCountdown,
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