package dev.nexaddons.skyblock

import dev.nexaddons.NexAddons
import dev.nexaddons.config.ConfigManager
import dev.nexaddons.text.NexAddonsText
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.minecraft.client.MinecraftClient

class SkyBlockContext {
    private var wasOnHypixel = false
    private var announcedForServer = false

    fun register() {
        ClientTickEvents.END_CLIENT_TICK.register { client ->
            onClientTick(client)
        }
    }

    private fun onClientTick(client: MinecraftClient) {
        if (client.world == null || client.player == null) {
            reset()
            return
        }

        if (!ConfigManager.config.enabled) {
            return
        }

        val onHypixel = HypixelServerDetector.matches(client.currentServerEntry?.address)
        if (onHypixel != wasOnHypixel) {
            wasOnHypixel = onHypixel
            NexAddons.LOGGER.info("Hypixel connection state changed: {}", onHypixel)
        }

        if (onHypixel && !announcedForServer && ConfigManager.config.showJoinMessage) {
            announcedForServer = true
            client.player?.sendMessage(
                NexAddonsText.info("Connected to Hypixel. SkyBlock feature hooks are ready."),
                false,
            )
        }
    }

    private fun reset() {
        wasOnHypixel = false
        announcedForServer = false
    }
}
