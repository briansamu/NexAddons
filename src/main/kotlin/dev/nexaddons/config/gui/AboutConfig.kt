package dev.nexaddons.config.gui

import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.Accordion
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorButton
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDropdown
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorInfoText
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import net.minecraft.util.Util
import java.net.URI

class AboutConfig {
    @ConfigOption(name = "Current Version", desc = "This is the NexAddons version you are currently running.")
    @ConfigEditorInfoText(infoTitle = "NexAddons")
    @Transient
    var currentVersionLabel: String = "dev"

    @Expose
    @ConfigOption(name = "Update Stream", desc = "How frequently you want updates for NexAddons.")
    @ConfigEditorDropdown
    var updateStream: UpdateStream = UpdateStream.RELEASES

    @Expose
    @ConfigOption(name = "Used Software", desc = "Information about used software and licenses.")
    @Accordion
    val licenses: Licenses = Licenses()

    enum class UpdateStream(private val label: String) {
        BETA("Beta"),
        RELEASES("Full"),
        ;

        override fun toString(): String = label
    }

    class Licenses {
        @ConfigOption(name = "MoulConfig", desc = "MoulConfig is available under the LGPL 3.0 License or later version.")
        @ConfigEditorButton(buttonText = "Source")
        val moulConfig: Runnable = Runnable { openBrowser("https://github.com/NotEnoughUpdates/MoulConfig") }

        @ConfigOption(name = "Fabric", desc = "Fabric Loader and Fabric API power the client mod environment.")
        @ConfigEditorButton(buttonText = "Source")
        val fabric: Runnable = Runnable { openBrowser("https://github.com/FabricMC") }

        @ConfigOption(name = "NexAddons", desc = "NexAddons source repository.")
        @ConfigEditorButton(buttonText = "Source")
        val nexAddons: Runnable = Runnable { openBrowser("https://github.com/briansamu/NexAddons") }
    }
}

private fun openBrowser(url: String) {
    Util.getOperatingSystem().open(URI(url))
}
