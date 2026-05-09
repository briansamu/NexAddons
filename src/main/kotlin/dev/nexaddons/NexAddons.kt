package dev.nexaddons

import dev.nexaddons.command.NexAddonsCommands
import dev.nexaddons.config.ConfigManager
import dev.nexaddons.skyblock.SkyBlockContext
import net.fabricmc.api.ClientModInitializer
import org.slf4j.Logger
import org.slf4j.LoggerFactory

object NexAddons : ClientModInitializer {
    const val MOD_ID: String = "nexaddons"
    val LOGGER: Logger = LoggerFactory.getLogger("NexAddons")

    private val skyBlockContext = SkyBlockContext()

    override fun onInitializeClient() {
        ConfigManager.config
        NexAddonsCommands.register()
        skyBlockContext.register()

        LOGGER.info("NexAddons initialized")
    }
}
