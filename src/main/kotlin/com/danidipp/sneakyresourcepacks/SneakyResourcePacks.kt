package com.danidipp.sneakyresourcepacks

import org.bukkit.plugin.java.JavaPlugin
import java.net.URI
import java.util.*

/**
 * Requirements
 * - configure resource packs in config
 * - make sure players have resourcepacks
 * - command to reload resourcepack
 * - option to force-reload players
 */
class SneakyResourcePacks : JavaPlugin() {

    override fun onLoad() {
        instance = this
    }
    override fun onEnable() {
        saveDefaultConfig()
        reloadConfig()
        this.loadConfig()

        server.commandMap.registerAll(IDENTIFIER, listOf(
            ResourcepackCommand()
        ))

        server.pluginManager.registerEvents(ResourcepackEvents(), this)

    }

    fun loadConfig() {
        val configPacks = config.getList("resourcepacks")
        val resourcePackMap = mutableMapOf<UUID, SneakyResourcePack>()
        if (configPacks != null) {
            for (configPack in configPacks) {
                if (configPack is Map<*, *>) {
                    val configId = configPack["id"] as String
                    val name = configPack["name"] as String
                    val url = configPack["url"] as String
                    val hash = configPack["hash"] as String
                    val enabled = configPack["enabled"] as Boolean
                    val priority = configPack["priority"] as Int

                    val id = runCatching { UUID.fromString(configId) }.getOrNull()
                    if (id == null) {
                        logger.warning("Invalid UUID for resourcepack $name: $configId")
                        continue
                    }
                    val uri = runCatching { URI(url) }.getOrNull()
                    if (uri == null) {
                        logger.warning("Invalid URL for resourcepack $name: $url")
                        continue
                    }

                    resourcePackMap[id] = SneakyResourcePack(id, name, uri, hash, enabled, priority)
                }
            }
        }
        val removedPacks = resourcePacks.keys - resourcePackMap.keys
        resourcePacks = resourcePackMap
    }

    fun updateConfig() {
        val configPacks = resourcePacks.values.map {
            mapOf(
                "id" to it.id.toString(),
                "name" to it.name,
                "url" to it.uri.toString(),
                "hash" to it.hash,
                "enabled" to it.enabled,
                "priority" to it.priority
            )
        }
        config.set("resourcepacks", configPacks)
        saveConfig()
    }

    companion object {
        const val IDENTIFIER = "sneakyresourcepacks"
        const val AUTHORS = "Team Sneakymouse"
        const val VERSION = "1.0"
        private lateinit var instance: SneakyResourcePacks

        var resourcePacks = mapOf<UUID, SneakyResourcePack>()
        fun getInstance(): SneakyResourcePacks {
            return instance
        }
    }
}
