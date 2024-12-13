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

    fun sendTo(player: Player) {
        val request = makeRequest(!player.hasPermission("sneakyresourcepacks.bypass"), this)
        player.sendResourcePacks(request)
    }

    fun removeFrom(player: Player) {
        val request = makeRequest(!player.hasPermission("sneakyresourcepacks.bypass"), this)
        player.removeResourcePacks(request)
    }

    companion object {
        fun makeRequest(required: Boolean, first: ResourcePackInfoLike, vararg more: ResourcePackInfoLike): ResourcePackRequest {
            return ResourcePackRequest.resourcePackRequest()
                .packs(first, *more)
                .required(required)
                .prompt(prompt)
                .callback(callback)
                .build()
        }
        private val prompt = Component.text("The server resourcepacks are required for custom models, textures, and sounds.")
        private val callback = ResourcePackCallback { uuid, status, audience ->
            val resourcePack = SneakyResourcePacks.resourcePacks[uuid] ?: return@ResourcePackCallback
            if (uuid != resourcePack.id) return@ResourcePackCallback
            if (audience !is Player) return@ResourcePackCallback

            when (status) {
                ResourcePackStatus.SUCCESSFULLY_LOADED -> {
                    SneakyResourcePacks.getInstance().logger.info("Resource pack ${resourcePack.name} loaded for ${audience.name}")
                    resourcePack.players.add(audience)
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

        fun applyAll() {
            applyAll(null)
        }
        fun applyAll(player: Player?) {
            val packs = SneakyResourcePacks.resourcePacks.values
                .filter { it.enabled }
            val first = packs.first()
            val more = packs.drop(1).toTypedArray()
            if (player != null) {
                val request = makeRequest(!player.hasPermission("sneakyresourcepacks.bypass"), first, *more)
                player.sendResourcePacks(request)
                return
            }

            for(p in Bukkit.getServer().onlinePlayers) {
                val request = makeRequest(!p.hasPermission("sneakyresourcepacks.bypass"), first, *more)
                p.sendResourcePacks(request)
            }
        }

        fun clearAll(player: Player?) {
            val packs = SneakyResourcePacks.resourcePacks.values
            val first = packs.first()
            val more = packs.drop(1).toTypedArray()
            if (player != null) {
                val request = makeRequest(!player.hasPermission("sneakyresourcepacks.bypass"), first, *more)
                player.removeResourcePacks(request)
                return
            }

            for(p in Bukkit.getServer().onlinePlayers) {
                val request = makeRequest(!p.hasPermission("sneakyresourcepacks.bypass"), first, *more)
                p.removeResourcePacks(request)
            }
        }
    }
}
