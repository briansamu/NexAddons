package dev.nexaddons.config.gui

import com.google.gson.annotations.Expose
import dev.nexaddons.NexAddons
import dev.nexaddons.config.ConfigManager
import io.github.notenoughupdates.moulconfig.Config
import io.github.notenoughupdates.moulconfig.annotations.Category
import io.github.notenoughupdates.moulconfig.common.text.StructuredText
import net.fabricmc.loader.api.FabricLoader

class NexAddonsMoulConfig : Config() {
    @Expose
    @Category(name = "About", desc = "Information about NexAddons and updates.")
    val about: AboutConfig = AboutConfig()

    @Expose
    @Category(name = "General", desc = "General NexAddons settings.")
    val general: GeneralConfig = GeneralConfig()

    fun loadFromStoredConfig() {
        val storedConfig = ConfigManager.config
        general.enabled = storedConfig.enabled
        general.showJoinMessage = storedConfig.showJoinMessage
        about.updateStream = storedConfig.updateStream
        about.checkForUpdatesOnStartup = storedConfig.checkForUpdatesOnStartup
    }

    override fun saveNow() {
        val storedConfig = ConfigManager.config
        storedConfig.enabled = general.enabled
        storedConfig.showJoinMessage = general.showJoinMessage
        storedConfig.updateStream = about.updateStream
        storedConfig.checkForUpdatesOnStartup = about.checkForUpdatesOnStartup
        ConfigManager.save()
    }

    override fun shouldAutoFocusSearchbar(): Boolean = false

    override fun getTitle(): StructuredText {
        return StructuredText.of(
            "NexAddons $version by §cSunexMC§r, config by §5MoulConfig",
        )
    }

    private val version: String
        get() = FabricLoader.getInstance()
            .getModContainer(NexAddons.MOD_ID)
            .map { it.metadata.version.friendlyString }
            .orElse("dev")
}
