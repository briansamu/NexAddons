package dev.nexaddons.config

import dev.nexaddons.config.gui.AboutConfig

data class NexAddonsConfig(
    var enabled: Boolean = true,
    var showJoinMessage: Boolean = true,
    var updateStream: AboutConfig.UpdateStream = AboutConfig.UpdateStream.RELEASES,
    var checkForUpdatesOnStartup: Boolean = true,
)
