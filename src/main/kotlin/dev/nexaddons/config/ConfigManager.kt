package dev.nexaddons.config

import com.google.gson.GsonBuilder
import com.google.gson.JsonParseException
import dev.nexaddons.NexAddons
import net.fabricmc.loader.api.FabricLoader
import org.slf4j.LoggerFactory
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path

object ConfigManager {
    private val logger = LoggerFactory.getLogger("NexAddons/Config")
    private val gson = GsonBuilder().setPrettyPrinting().create()
    private val configPath: Path = FabricLoader.getInstance().configDir.resolve("${NexAddons.MOD_ID}.json")

    var config: NexAddonsConfig = readOrCreate()
        private set

    fun reload() {
        config = readOrCreate()
    }

    fun save() {
        write(config)
    }

    private fun readOrCreate(): NexAddonsConfig {
        if (Files.notExists(configPath)) {
            return NexAddonsConfig().also(::write)
        }

        return try {
            Files.newBufferedReader(configPath).use { reader ->
                gson.fromJson(reader, NexAddonsConfig::class.java) ?: NexAddonsConfig()
            }
        } catch (error: IOException) {
            logger.warn("Could not read NexAddons config. Falling back to defaults.", error)
            NexAddonsConfig()
        } catch (error: JsonParseException) {
            logger.warn("Could not parse NexAddons config. Falling back to defaults.", error)
            NexAddonsConfig()
        }
    }

    private fun write(value: NexAddonsConfig) {
        try {
            Files.createDirectories(configPath.parent)
            Files.newBufferedWriter(configPath).use { writer ->
                gson.toJson(value, writer)
            }
        } catch (error: IOException) {
            logger.warn("Could not write NexAddons config.", error)
        }
    }
}
