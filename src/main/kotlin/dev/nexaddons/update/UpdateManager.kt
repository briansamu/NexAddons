package dev.nexaddons.update

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
import dev.nexaddons.NexAddons
import dev.nexaddons.config.ConfigManager
import dev.nexaddons.config.gui.AboutConfig
import dev.nexaddons.text.NexAddonsText
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.client.MinecraftClient
import java.io.IOException
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.time.Duration
import java.util.concurrent.CompletableFuture
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.io.path.name

object UpdateManager {
    private const val OWNER = "briansamu"
    private const val REPOSITORY = "NexAddons"
    private const val RELEASES_API = "https://api.github.com/repos/$OWNER/$REPOSITORY/releases"
    private const val RELEASES_PAGE = "https://github.com/$OWNER/$REPOSITORY/releases"

    private val gson = Gson()
    private val releasesType = object : TypeToken<List<GithubRelease>>() {}.type
    private val checkedOnStartup = AtomicBoolean(false)
    private val versionRegex = Regex("""^v?(\d+(?:\.\d+)*)(?:[-+](.+))?$""")

    @Volatile
    var updateState: UpdateState = UpdateState.IDLE
        private set

    @Volatile
    private var availableUpdate: AvailableUpdate? = null

    private val httpClient: HttpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(15))
        .followRedirects(HttpClient.Redirect.NORMAL)
        .build()

    fun register() {
        ClientTickEvents.END_CLIENT_TICK.register { client ->
            if (checkedOnStartup.get()) return@register
            if (client.player == null) return@register

            checkedOnStartup.set(true)
            if (ConfigManager.config.checkForUpdatesOnStartup) {
                checkForUpdates(force = false, updateStream = ConfigManager.config.updateStream)
            }
        }
    }

    fun checkForUpdates(
        force: Boolean = true,
        updateStream: AboutConfig.UpdateStream = ConfigManager.config.updateStream,
    ) {
        if (!begin(UpdateState.CHECKING, force)) return

        CompletableFuture.supplyAsync {
            findLatestUpdate(updateStream)
        }.whenComplete { update, throwable ->
            MinecraftClient.getInstance().execute {
                if (throwable != null) {
                    updateState = UpdateState.FAILED
                    if (force) {
                        sendError("Could not check for updates: ${throwable.userMessage()}")
                    }
                    NexAddons.LOGGER.warn("Could not check for NexAddons updates.", throwable)
                    return@execute
                }

                if (update == null) {
                    availableUpdate = null
                    updateState = UpdateState.UP_TO_DATE
                    if (force) {
                        sendSuccess("NexAddons is up to date.")
                    }
                    return@execute
                }

                availableUpdate = update
                updateState = UpdateState.AVAILABLE
                sendSuccess("NexAddons ${update.version} is available. Use the About screen's Download Update button to install it.")
            }
        }
    }

    fun downloadLatestUpdate(updateStream: AboutConfig.UpdateStream = ConfigManager.config.updateStream) {
        if (!begin(UpdateState.DOWNLOADING, force = true)) return

        CompletableFuture.supplyAsync {
            val update = findLatestUpdate(updateStream)
            update?.let { installUpdate(it) }
        }.whenComplete { install, throwable ->
            MinecraftClient.getInstance().execute {
                if (throwable != null) {
                    updateState = UpdateState.FAILED
                    sendError("Could not download update: ${throwable.userMessage()}")
                    NexAddons.LOGGER.warn("Could not download NexAddons update.", throwable)
                    return@execute
                }

                if (install == null) {
                    availableUpdate = null
                    updateState = UpdateState.UP_TO_DATE
                    sendSuccess("NexAddons is up to date.")
                    return@execute
                }

                availableUpdate = install.update
                updateState = UpdateState.INSTALLED
                sendSuccess("Downloaded NexAddons ${install.update.version}. Restart Minecraft to finish updating.")
                sendInfo("Installed jar: ${install.installedJar.fileName}.")
                install.disabledOldJar?.let {
                    sendInfo("Disabled the old jar: ${it.fileName}.")
                }
            }
        }
    }

    fun openReleasesPage() {
        net.minecraft.util.Util.getOperatingSystem().open(URI(RELEASES_PAGE))
    }

    fun openAvailableReleasePage() {
        net.minecraft.util.Util.getOperatingSystem().open(URI(availableUpdate?.releasePage ?: RELEASES_PAGE))
    }

    fun getNextVersion(): String? = availableUpdate?.version

    fun currentVersion(): String = currentModVersion()

    private fun begin(nextState: UpdateState, force: Boolean): Boolean {
        return when (updateState) {
            UpdateState.CHECKING -> {
                if (force) sendInfo("Already checking for updates.")
                false
            }

            UpdateState.DOWNLOADING -> {
                if (force) sendInfo("An update download is already running.")
                false
            }

            else -> {
                updateState = nextState
                true
            }
        }
    }

    private fun findLatestUpdate(updateStream: AboutConfig.UpdateStream): AvailableUpdate? {
        val currentVersion = currentModVersion()
        val current = parseVersion(currentVersion)
        if (current == null) {
            NexAddons.LOGGER.info("Skipping update check for non-release NexAddons version: {}", currentVersion)
            return null
        }

        val releases = fetchReleases()
        return releases.asSequence()
            .filter { !it.draft }
            .filter { updateStream == AboutConfig.UpdateStream.BETA || !it.prerelease }
            .mapNotNull { release ->
                val version = parseVersion(release.tagName) ?: return@mapNotNull null
                val asset = findJarAsset(release.assets) ?: return@mapNotNull null
                if (version <= current) return@mapNotNull null

                AvailableUpdate(
                    version = release.tagName.removePrefix("v"),
                    comparableVersion = version,
                    releasePage = release.htmlUrl ?: RELEASES_PAGE,
                    asset = asset,
                )
            }
            .maxByOrNull { it.comparableVersion }
    }

    private fun fetchReleases(): List<GithubRelease> {
        val request = HttpRequest.newBuilder(URI(RELEASES_API))
            .timeout(Duration.ofSeconds(20))
            .header("Accept", "application/vnd.github+json")
            .header("User-Agent", "NexAddons/${currentModVersion()}")
            .GET()
            .build()

        val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
        if (response.statusCode() !in 200..299) {
            throw IOException("GitHub returned HTTP ${response.statusCode()}")
        }

        return gson.fromJson(response.body(), releasesType)
    }

    private fun installUpdate(update: AvailableUpdate): InstallResult {
        val modsDir = FabricLoader.getInstance().gameDir.resolve("mods")
        Files.createDirectories(modsDir)

        val fileName = sanitizeFileName(update.asset.name)
        val target = modsDir.resolve(fileName)
        val tempFile = Files.createTempFile(modsDir, "$fileName.", ".download")

        try {
            downloadAsset(update.asset.downloadUrl, tempFile)
            val disabledOldJar = disableCurrentJarIfNeeded(target)
            moveReplacing(tempFile, target)
            return InstallResult(update, target, disabledOldJar)
        } catch (error: Throwable) {
            Files.deleteIfExists(tempFile)
            throw error
        }
    }

    private fun downloadAsset(url: String, destination: Path) {
        val request = HttpRequest.newBuilder(URI(url))
            .timeout(Duration.ofMinutes(2))
            .header("Accept", "application/octet-stream")
            .header("User-Agent", "NexAddons/${currentModVersion()}")
            .GET()
            .build()

        val response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream())
        if (response.statusCode() !in 200..299) {
            throw IOException("GitHub returned HTTP ${response.statusCode()} for the jar")
        }

        response.body().use { stream ->
            Files.copy(stream, destination, StandardCopyOption.REPLACE_EXISTING)
        }
    }

    private fun disableCurrentJarIfNeeded(newJar: Path): Path? {
        val currentJar = currentModJar() ?: return null
        if (samePath(currentJar, newJar)) return null
        if (!samePath(currentJar.parent, newJar.parent)) return null

        val disabledPath = uniqueSibling(currentJar, ".disabled")
        return try {
            Files.move(currentJar, disabledPath, StandardCopyOption.REPLACE_EXISTING)
            disabledPath
        } catch (error: IOException) {
            throw IOException(
                "downloaded the update, but could not disable the current jar. Close Minecraft and update manually from $RELEASES_PAGE",
                error,
            )
        }
    }

    private fun moveReplacing(source: Path, target: Path) {
        try {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
        } catch (_: IOException) {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING)
        }
    }

    private fun currentModJar(): Path? {
        val container = FabricLoader.getInstance().getModContainer(NexAddons.MOD_ID).orElse(null) ?: return null
        return container.origin.paths.firstOrNull { path ->
            Files.isRegularFile(path) && path.name.endsWith(".jar")
        }
    }

    private fun currentModVersion(): String {
        return FabricLoader.getInstance()
            .getModContainer(NexAddons.MOD_ID)
            .map { it.metadata.version.friendlyString }
            .orElse("dev")
    }

    private fun minecraftVersion(): String {
        return FabricLoader.getInstance()
            .getModContainer("minecraft")
            .map { it.metadata.version.friendlyString }
            .orElse("")
    }

    private fun findJarAsset(assets: List<GithubAsset>): GithubAsset? {
        val jarAssets = assets.filter { asset ->
            asset.name.endsWith(".jar", ignoreCase = true) &&
                !asset.name.contains("sources", ignoreCase = true) &&
                !asset.name.contains("-dev", ignoreCase = true) &&
                asset.downloadUrl.isNotBlank()
        }

        val minecraftVersion = minecraftVersion()
        return jarAssets.firstOrNull { minecraftVersion.isNotBlank() && it.name.contains(minecraftVersion) }
            ?: jarAssets.firstOrNull()
    }

    private fun parseVersion(value: String): ComparableVersion? {
        val match = versionRegex.matchEntire(value.trim()) ?: return null
        val numbers = match.groupValues[1].split(".").map { it.toIntOrNull() ?: return null }
        val qualifier = match.groupValues.getOrNull(2)?.takeIf { it.isNotBlank() }
        return ComparableVersion(numbers, qualifier)
    }

    private fun sanitizeFileName(value: String): String {
        val fileName = Path.of(value).fileName.toString()
        return fileName.replace(Regex("""[^A-Za-z0-9._-]"""), "_")
    }

    private fun uniqueSibling(path: Path, suffix: String): Path {
        val base = path.resolveSibling("${path.fileName}$suffix")
        if (Files.notExists(base)) return base

        var counter = 1
        while (true) {
            val candidate = path.resolveSibling("${path.fileName}.$counter$suffix")
            if (Files.notExists(candidate)) return candidate
            counter += 1
        }
    }

    private fun samePath(first: Path, second: Path): Boolean {
        return try {
            Files.exists(first) && Files.exists(second) && Files.isSameFile(first, second)
        } catch (_: IOException) {
            first.toAbsolutePath().normalize() == second.toAbsolutePath().normalize()
        }
    }

    private fun Throwable.userMessage(): String {
        return message ?: javaClass.simpleName
    }

    private fun sendInfo(message: String) {
        MinecraftClient.getInstance().player?.sendMessage(NexAddonsText.info(message), false)
    }

    private fun sendSuccess(message: String) {
        MinecraftClient.getInstance().player?.sendMessage(NexAddonsText.success(message), false)
    }

    private fun sendError(message: String) {
        MinecraftClient.getInstance().player?.sendMessage(NexAddonsText.error(message), false)
    }

    private data class GithubRelease(
        @SerializedName("tag_name")
        val tagName: String,
        @SerializedName("html_url")
        val htmlUrl: String?,
        val draft: Boolean,
        val prerelease: Boolean,
        val assets: List<GithubAsset>,
    )

    private data class GithubAsset(
        val name: String,
        @SerializedName("browser_download_url")
        val downloadUrl: String,
    )

    private data class AvailableUpdate(
        val version: String,
        val comparableVersion: ComparableVersion,
        val releasePage: String,
        val asset: GithubAsset,
    )

    private data class InstallResult(
        val update: AvailableUpdate,
        val installedJar: Path,
        val disabledOldJar: Path?,
    )

    private data class ComparableVersion(
        val numbers: List<Int>,
        val qualifier: String?,
    ) : Comparable<ComparableVersion> {
        override fun compareTo(other: ComparableVersion): Int {
            val maxLength = maxOf(numbers.size, other.numbers.size)
            for (index in 0 until maxLength) {
                val left = numbers.getOrElse(index) { 0 }
                val right = other.numbers.getOrElse(index) { 0 }
                if (left != right) return left.compareTo(right)
            }

            if (qualifier == null && other.qualifier != null) return 1
            if (qualifier != null && other.qualifier == null) return -1
            return qualifier.orEmpty().compareTo(other.qualifier.orEmpty(), ignoreCase = true)
        }
    }

    enum class UpdateState {
        IDLE,
        CHECKING,
        AVAILABLE,
        UP_TO_DATE,
        DOWNLOADING,
        INSTALLED,
        FAILED,
    }
}
