package dev.nexaddons.command

import com.mojang.brigadier.Command
import com.mojang.brigadier.CommandDispatcher
import dev.nexaddons.config.ConfigManager
import dev.nexaddons.text.NexAddonsText
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource

object NexAddonsCommands {
    fun register() {
        ClientCommandRegistrationCallback.EVENT.register { dispatcher, _ ->
            registerRoot(dispatcher)
        }
    }

    private fun registerRoot(dispatcher: CommandDispatcher<FabricClientCommandSource>) {
        val root = dispatcher.register(
            literal("nexaddons")
                .executes { context ->
                    sendStatus(context.source)
                }
                .then(
                    literal("toggle")
                        .executes { context ->
                            val config = ConfigManager.config
                            config.enabled = !config.enabled
                            ConfigManager.save()

                            context.source.sendFeedback(
                                NexAddonsText.success("Features are now ${if (config.enabled) "enabled" else "disabled"}."),
                            )

                            Command.SINGLE_SUCCESS
                        },
                )
                .then(
                    literal("reload")
                        .executes { context ->
                            ConfigManager.reload()
                            context.source.sendFeedback(NexAddonsText.success("Config reloaded."))
                            Command.SINGLE_SUCCESS
                        },
                ),
        )

        dispatcher.register(literal("na").redirect(root))
    }

    private fun sendStatus(source: FabricClientCommandSource): Int {
        val config = ConfigManager.config
        source.sendFeedback(
            NexAddonsText.info(
                "NexAddons ${if (config.enabled) "enabled" else "disabled"}; join message ${
                    if (config.showJoinMessage) "enabled" else "disabled"
                }.",
            ),
        )

        return Command.SINGLE_SUCCESS
    }
}
