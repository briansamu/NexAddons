package dev.nexaddons.command

import com.mojang.brigadier.Command
import com.mojang.brigadier.CommandDispatcher
import dev.nexaddons.config.ConfigGuiManager
import dev.nexaddons.config.ConfigManager
import dev.nexaddons.text.NexAddonsText
import dev.nexaddons.update.UpdateManager
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
                .executes {
                    openConfig()
                }
                .then(
                    literal("config")
                        .executes {
                            openConfig()
                        },
                )
                .then(
                    literal("gui")
                        .executes {
                            openConfig()
                        },
                )
                .then(
                    literal("status")
                        .executes { context ->
                            sendStatus(context.source)
                        },
                )
                .then(
                    literal("update")
                        .executes {
                            UpdateManager.checkForUpdates(force = true)
                            Command.SINGLE_SUCCESS
                        }
                        .then(
                            literal("check")
                                .executes {
                                    UpdateManager.checkForUpdates(force = true)
                                    Command.SINGLE_SUCCESS
                                },
                        )
                        .then(
                            literal("download")
                                .executes {
                                    UpdateManager.downloadLatestUpdate()
                                    Command.SINGLE_SUCCESS
                                },
                        ),
                )
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
        dispatcher.register(
            literal("naupdate")
                .executes {
                    UpdateManager.checkForUpdates(force = true)
                    Command.SINGLE_SUCCESS
                }
                .then(
                    literal("download")
                        .executes {
                            UpdateManager.downloadLatestUpdate()
                            Command.SINGLE_SUCCESS
                        },
                ),
        )
    }

    private fun openConfig(): Int {
        ConfigGuiManager.openConfigGui()
        return Command.SINGLE_SUCCESS
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
