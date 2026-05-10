package dev.nexaddons.config

import dev.nexaddons.config.gui.NexAddonsMoulConfig
import io.github.notenoughupdates.moulconfig.gui.GuiContext
import io.github.notenoughupdates.moulconfig.gui.GuiElementComponent
import io.github.notenoughupdates.moulconfig.gui.MoulConfigEditor
import io.github.notenoughupdates.moulconfig.platform.MoulConfigScreenComponent
import io.github.notenoughupdates.moulconfig.processor.ConfigProcessorDriver
import io.github.notenoughupdates.moulconfig.processor.MoulConfigProcessor
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.screen.Screen
import net.minecraft.text.Text

object ConfigGuiManager {
    private val moulConfig = NexAddonsMoulConfig()
    private var editor: MoulConfigEditor<NexAddonsMoulConfig>? = null

    fun openConfigGui(parent: Screen? = null) {
        val client = MinecraftClient.getInstance()
        client.execute {
            moulConfig.loadFromStoredConfig()
            editor = null

            client.setScreen(
                MoulConfigScreenComponent(
                    Text.empty(),
                    GuiContext(GuiElementComponent(getEditorInstance())),
                    parent,
                ),
            )
        }
    }

    private fun getEditorInstance(): MoulConfigEditor<NexAddonsMoulConfig> {
        return editor ?: createEditor().also { editor = it }
    }

    private fun createEditor(): MoulConfigEditor<NexAddonsMoulConfig> {
        val processor = MoulConfigProcessor.withDefaults(moulConfig)
        ConfigProcessorDriver(processor).apply {
            warnForPrivateFields = false
            processConfig(moulConfig)
        }

        return MoulConfigEditor(processor)
    }
}
