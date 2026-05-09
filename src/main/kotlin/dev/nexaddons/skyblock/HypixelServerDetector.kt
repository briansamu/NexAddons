package dev.nexaddons.skyblock

object HypixelServerDetector {
    fun matches(address: String?): Boolean {
        val host = normalizedHost(address) ?: return false
        return host == "hypixel.net" || host.endsWith(".hypixel.net")
    }

    internal fun normalizedHost(address: String?): String? {
        val trimmed = address
            ?.trim()
            ?.lowercase()
            ?.takeIf { it.isNotBlank() }
            ?: return null

        val withoutScheme = trimmed.substringAfter("://", trimmed)
        val withoutPath = withoutScheme.substringBefore("/")
        val host = if (withoutPath.startsWith("[")) {
            withoutPath.substringAfter("[").substringBefore("]")
        } else {
            withoutPath.substringBefore(":")
        }

        return host.trim().trimEnd('.').takeIf { it.isNotBlank() }
    }
}
