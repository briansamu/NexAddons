package dev.nexaddons.text

import net.minecraft.text.Text
import net.minecraft.util.Formatting

object NexAddonsText {
    fun info(message: String): Text = prefixed(message, Formatting.WHITE)

    fun success(message: String): Text = prefixed(message, Formatting.GREEN)

    private fun prefixed(message: String, formatting: Formatting): Text {
        return Text.literal("[NexAddons] ")
            .formatted(Formatting.AQUA)
            .append(Text.literal(message).formatted(formatting))
    }
}
