package gecko10000.spawnermarathoner

import gecko10000.geckolib.extensions.parseMM
import gecko10000.spawnermarathoner.di.MyKoinComponent
import io.papermc.paper.plugin.lifecycle.event.handler.LifecycleEventHandler
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents
import net.strokkur.commands.annotations.Command
import net.strokkur.commands.annotations.Executes
import org.bukkit.command.CommandSender
import org.koin.core.component.inject

@Command("spawnermarathoner")
class CommandHandler : MyKoinComponent {

    private val plugin: SpawnerMarathoner by inject()

    fun register() {
        plugin.lifecycleManager
            .registerEventHandler(LifecycleEvents.COMMANDS.newHandler(LifecycleEventHandler { event ->
                CommandHandlerBrigadier.register(
                    event.registrar()
                )
            }))
    }

    @Executes("reload")
    fun reload(sender: CommandSender) {
        plugin.reloadConfigs()
        sender.sendMessage(parseMM("<green>Config reloaded.", true))
    }
}
