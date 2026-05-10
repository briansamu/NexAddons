package dev.nexaddons.config.gui

import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class GeneralConfig {
    @Expose
    @ConfigOption(name = "Enabled", desc = "Master toggle for NexAddons features.")
    @ConfigEditorBoolean
    var enabled: Boolean = true

    @Expose
    @ConfigOption(name = "Join Message", desc = "Show the NexAddons message when joining Hypixel SkyBlock.")
    @ConfigEditorBoolean
    var showJoinMessage: Boolean = true
}
