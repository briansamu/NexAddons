package dev.nexaddons.update

import dev.nexaddons.config.ConfigManager
import dev.nexaddons.config.gui.NexAddonsMoulConfig
import io.github.notenoughupdates.moulconfig.common.RenderContext
import io.github.notenoughupdates.moulconfig.common.text.StructuredText
import io.github.notenoughupdates.moulconfig.gui.GuiOptionEditor
import io.github.notenoughupdates.moulconfig.gui.MouseEvent
import io.github.notenoughupdates.moulconfig.processor.ProcessedOption
import kotlin.math.max

class GuiOptionEditorUpdateCheck(option: ProcessedOption) : GuiOptionEditor(option) {
    override fun render(context: RenderContext, x: Int, y: Int, width: Int) {
        val font = context.minecraft.defaultFontRenderer
        val adjustedX = x + 10
        val adjustedWidth = width - 20
        val primaryLabel = primaryButtonLabel()
        val secondaryLabel = "Open Release"
        val primaryWidth = buttonWidth(context, primaryLabel)
        val secondaryWidth = buttonWidth(context, secondaryLabel)
        val showSecondary = shouldShowSecondaryButton()
        val buttonColumnWidth = if (showSecondary) max(primaryWidth, secondaryWidth) else primaryWidth
        val versionWidth = adjustedWidth - buttonColumnWidth - 10
        val primaryButtonY = if (showSecondary) y + 10 else y + (height - BUTTON_HEIGHT) / 2
        val nextVersion = UpdateManager.getNextVersion()
        val versionText = UpdateManager.currentVersion() +
            if (nextVersion != null && !nextVersion.equals(UpdateManager.currentVersion(), ignoreCase = true)) {
                " -> $nextVersion"
            } else {
                ""
            }
        val versionColor = when (UpdateManager.updateState) {
            UpdateManager.UpdateState.AVAILABLE -> 0xFFFF5555.toInt()
            UpdateManager.UpdateState.FAILED -> 0xFFFFAA00.toInt()
            else -> 0xFF55FF55.toInt()
        }

        context.drawDarkRect(x, y, width, height, true)
        renderButton(
            context = context,
            x = adjustedX + adjustedWidth - primaryWidth,
            y = primaryButtonY,
            width = primaryWidth,
            text = primaryLabel,
            enabled = isPrimaryButtonEnabled(),
        )

        if (showSecondary) {
            renderButton(
                context = context,
                x = adjustedX + adjustedWidth - secondaryWidth,
                y = y + 30,
                width = secondaryWidth,
                text = secondaryLabel,
                enabled = true,
            )
        }

        context.pushMatrix()
        context.translate(adjustedX.toFloat(), y.toFloat())
        context.scale(2F, 2F)
        context.drawStringCenteredScaledMaxWidth(
            StructuredText.of(versionText),
            font,
            versionWidth / 4F,
            height / 4F,
            true,
            versionWidth / 2,
            versionColor,
        )
        context.popMatrix()
    }

    override fun getHeight(): Int = 55

    override fun mouseInput(
        x: Int,
        y: Int,
        width: Int,
        mouseX: Int,
        mouseY: Int,
        mouseEvent: MouseEvent,
    ): Boolean {
        if (mouseEvent !is MouseEvent.Click || !mouseEvent.mouseState || mouseEvent.mouseButton != 0) return false

        val adjustedX = x + 10
        val adjustedWidth = width - 20
        val primaryLabel = primaryButtonLabel()
        val primaryWidth = buttonWidth(null, primaryLabel)
        val primaryX = adjustedX + adjustedWidth - primaryWidth
        val primaryY = if (shouldShowSecondaryButton()) y + 10 else y + (height - BUTTON_HEIGHT) / 2

        if (isInside(mouseX, mouseY, primaryX, primaryY, primaryWidth, BUTTON_HEIGHT)) {
            runPrimaryAction()
            return true
        }

        if (!shouldShowSecondaryButton()) return false

        val secondaryWidth = buttonWidth(null, "Open Release")
        val secondaryX = adjustedX + adjustedWidth - secondaryWidth
        if (isInside(mouseX, mouseY, secondaryX, y + 30, secondaryWidth, BUTTON_HEIGHT)) {
            UpdateManager.openAvailableReleasePage()
            return true
        }

        return false
    }

    override fun fulfillsSearch(word: String): Boolean {
        return super.fulfillsSearch(word) || word in "update download release changelog version"
    }

    private fun primaryButtonLabel(): String {
        return when (UpdateManager.updateState) {
            UpdateManager.UpdateState.IDLE -> "Check for Updates"
            UpdateManager.UpdateState.CHECKING -> "Checking..."
            UpdateManager.UpdateState.AVAILABLE -> "Download Update"
            UpdateManager.UpdateState.UP_TO_DATE -> "Up to date"
            UpdateManager.UpdateState.DOWNLOADING -> "Downloading..."
            UpdateManager.UpdateState.INSTALLED -> "Restart Required"
            UpdateManager.UpdateState.FAILED -> "Check Again"
        }
    }

    private fun isPrimaryButtonEnabled(): Boolean {
        return UpdateManager.updateState !in setOf(
            UpdateManager.UpdateState.CHECKING,
            UpdateManager.UpdateState.DOWNLOADING,
            UpdateManager.UpdateState.INSTALLED,
        )
    }

    private fun runPrimaryAction() {
        when (UpdateManager.updateState) {
            UpdateManager.UpdateState.AVAILABLE -> UpdateManager.downloadLatestUpdate(selectedUpdateStream())
            UpdateManager.UpdateState.IDLE,
            UpdateManager.UpdateState.UP_TO_DATE,
            UpdateManager.UpdateState.FAILED,
            -> UpdateManager.checkForUpdates(force = true, updateStream = selectedUpdateStream())

            UpdateManager.UpdateState.CHECKING,
            UpdateManager.UpdateState.DOWNLOADING,
            UpdateManager.UpdateState.INSTALLED,
            -> Unit
        }
    }

    private fun selectedUpdateStream() =
        (option.config as? NexAddonsMoulConfig)?.about?.updateStream ?: ConfigManager.config.updateStream

    private fun shouldShowSecondaryButton(): Boolean {
        return UpdateManager.updateState in setOf(
            UpdateManager.UpdateState.AVAILABLE,
            UpdateManager.UpdateState.INSTALLED,
        )
    }

    private fun renderButton(
        context: RenderContext,
        x: Int,
        y: Int,
        width: Int,
        text: String,
        enabled: Boolean,
    ) {
        val background = if (enabled) 0xFFB8C7C9.toInt() else 0xFF737D80.toInt()
        val border = if (enabled) 0xFFE7F2F3.toInt() else 0xFFA5ADAF.toInt()
        val textColor = if (enabled) 0xFF202A2D.toInt() else 0xFF30383A.toInt()

        context.drawColoredRect(x.toFloat(), y.toFloat(), (x + width).toFloat(), (y + BUTTON_HEIGHT).toFloat(), border)
        context.drawColoredRect(
            (x + 1).toFloat(),
            (y + 1).toFloat(),
            (x + width - 1).toFloat(),
            (y + BUTTON_HEIGHT - 1).toFloat(),
            background,
        )
        context.drawStringCenteredScaledMaxWidth(
            StructuredText.of(text),
            context.minecraft.defaultFontRenderer,
            x + width / 2F,
            y + BUTTON_HEIGHT / 2F,
            false,
            width - 4,
            textColor,
        )
    }

    private fun buttonWidth(context: RenderContext?, text: String): Int {
        val font = context?.minecraft?.defaultFontRenderer
            ?: io.github.notenoughupdates.moulconfig.common.IMinecraft.INSTANCE.defaultFontRenderer
        return max(76, font.getStringWidth(StructuredText.of(text)) + 12)
    }

    private fun isInside(mouseX: Int, mouseY: Int, x: Int, y: Int, width: Int, height: Int): Boolean {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height
    }

    private companion object {
        const val BUTTON_HEIGHT = 16
    }
}
