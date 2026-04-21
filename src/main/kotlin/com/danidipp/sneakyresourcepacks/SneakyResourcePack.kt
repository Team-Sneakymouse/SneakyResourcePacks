package com.danidipp.sneakyresourcepacks

import net.kyori.adventure.audience.Audience
import net.kyori.adventure.resource.ResourcePackCallback
import net.kyori.adventure.resource.ResourcePackInfo
import net.kyori.adventure.resource.ResourcePackInfoLike
import net.kyori.adventure.resource.ResourcePackRequest
import net.kyori.adventure.resource.ResourcePackStatus
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import java.net.URI
import java.util.*

data class SneakyResourcePack(
    val id: UUID,
    val name: String,
    val uri: URI,
    var hash: String,
    var enabled: Boolean = true,
    val priority: Int = 0
) : ResourcePackInfo {
    val players = mutableSetOf<Player>()

    override fun id(): UUID {
        return id
    }

    override fun uri(): URI {
        return uri
    }

    override fun hash(): String {
        return hash
    }

    fun sendTo(target: Audience) {
        target.forEachAudience {
            if (it !is Player) {
                SneakyResourcePacks.getInstance().logger.warning("Tried to send resource pack to non-player ${it.javaClass.simpleName}")
                return@forEachAudience
            }
            val request = ResourcePackRequest.resourcePackRequest()
                .packs(this)
                .required(!it.hasPermission("sneakyresourcepacks.bypass"))
                .prompt(prompt)
                .callback(callback)
                .build()
            it.sendResourcePacks(request)
        }
    }

    fun removeFrom(target: Audience) {
        target.forEachAudience {
            if (it !is Player) {
                SneakyResourcePacks.getInstance().logger.warning("Tried to remove resource pack from non-player ${it.javaClass.simpleName}")
                return@forEachAudience
            }
            val request = ResourcePackRequest.resourcePackRequest()
                .packs(this)
                .required(!it.hasPermission("sneakyresourcepacks.bypass"))
                .prompt(prompt)
                .callback(callback)
                .build()
            it.removeResourcePacks(request)
        }
    }

    companion object {
        private val prompt = Component.text("The server resourcepacks are required for custom models, textures, and sounds.")
        private val callback = ResourcePackCallback { uuid, status, audience ->
            val resourcePack = SneakyResourcePacks.resourcePacks[uuid] ?: return@ResourcePackCallback
            if (uuid != resourcePack.id) return@ResourcePackCallback
            if (audience !is Player) return@ResourcePackCallback

            when (status) {
                ResourcePackStatus.SUCCESSFULLY_LOADED -> {
                    SneakyResourcePacks.getInstance().logger.info("Resource pack ${resourcePack.name} loaded for ${audience.name}")
                    resourcePack.players.add(audience)
                    if(SneakyResourcePacks.resourcePacks.values.filter { it.enabled }.all { it.players.contains(audience) }) {
                        val packs = SneakyResourcePacks.resourcePacks.values
                            .filter { it.players.contains(audience) }
                        Bukkit.getServer().pluginManager.callEvent(ResourcepacksLoadedEvent(audience, packs))
                    }
                }
                ResourcePackStatus.FAILED_DOWNLOAD -> {
                    SneakyResourcePacks.getInstance().logger.severe("Resource pack ${resourcePack.name} failed to download for ${audience.name}")
                    audience.sendMessage(Component.text("Failed to download resource pack ${resourcePack.name}", NamedTextColor.RED))
                }
                ResourcePackStatus.FAILED_RELOAD -> {
                    SneakyResourcePacks.getInstance().logger.severe("Resource pack ${resourcePack.name} failed to reload for ${audience.name}")
                    audience.sendMessage(Component.text("Failed to apply resource pack ${resourcePack.name}", NamedTextColor.RED))
                }
                ResourcePackStatus.DISCARDED -> {
                    SneakyResourcePacks.getInstance().logger.info("Resource pack ${resourcePack.name} discarded for ${audience.name}")
                    resourcePack.players.remove(audience)
                }
                else -> {
                    SneakyResourcePacks.getInstance().logger.warning("Unhandled resource pack ${resourcePack.name} status for ${audience.name}: $status")
                }
            }
        }

        fun applyAll(target: Audience) {
            val packs = SneakyResourcePacks.resourcePacks.values.filter { it.enabled }
            target.forEachAudience {
                if (it !is Player) {
                    SneakyResourcePacks.getInstance().logger.warning("Tried to apply resource packs to non-player ${it.javaClass.simpleName}")
                    return@forEachAudience
                }
                val request = ResourcePackRequest.resourcePackRequest()
                    .packs(packs)
                    .replace(true)
                    .required(!it.hasPermission("sneakyresourcepacks.bypass"))
                    .prompt(prompt)
                    .callback(callback)
                    .build()
                it.sendResourcePacks(request)
            }
        }

        fun clearAll(target: Audience) {
            val packs = SneakyResourcePacks.resourcePacks.values
            target.forEachAudience {
                if (it !is Player) {
                    SneakyResourcePacks.getInstance().logger.warning("Tried to remove resource packs from non-player ${it.javaClass.simpleName}")
                    return@forEachAudience
                }
                val request = ResourcePackRequest.resourcePackRequest()
                    .packs(packs)
                    .replace(true)
                    .required(!it.hasPermission("sneakyresourcepacks.bypass"))
                    .prompt(prompt)
                    .callback(callback)
                    .build()
                it.removeResourcePacks(request)
            }
        }
    }
}
