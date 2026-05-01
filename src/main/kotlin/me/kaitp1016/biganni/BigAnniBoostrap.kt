package me.kaitp1016.biganni

import io.papermc.paper.plugin.bootstrap.BootstrapContext
import io.papermc.paper.plugin.bootstrap.PluginBootstrap
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents
import me.kaitp1016.biganni.commands.AnniCommand


class BigAnniBoostrap: PluginBootstrap {
    override fun bootstrap(context: BootstrapContext) {
        context.lifecycleManager.registerEventHandler(LifecycleEvents.COMMANDS, { commands ->
            commands.registrar().register(AnniCommand.register().build())
        })
    }
}