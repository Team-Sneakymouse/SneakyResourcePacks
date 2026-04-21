package com.danidipp.sneakyresourcepacks

import net.sneakycharactermanager.paper.handlers.character.LoadCharacterEvent
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.event.EventHandler
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerLoginEvent
import org.bukkit.event.player.PlayerResourcePackStatusEvent
import org.bukkit.scheduler.BukkitTask

class ResourcepackEvents : Listener {
    companion object {
        val joinedPlayers = mutableMapOf<Player,BukkitTask>()
        fun scmEventListener(): Listener {
            return object : Listener {
                @EventHandler
                fun onCharacterLoadEvent(event: LoadCharacterEvent) {
                    val player = event.player
                    if (joinedPlayers.contains(player)) {
                        joinedPlayers[player]?.cancel()

                        SneakyResourcePack.applyAll(player)
                    }
                }
            }
        }
    }
//    @EventHandler
//    fun onResourcepackStatus(event: PlayerResourcePackStatusEvent) {
//        event.player.sendMessage("Resourcepack status: ${event.status}")
//    }
    @EventHandler
    fun onPlayerJoinEvent(event: PlayerJoinEvent) {
        joinedPlayers[event.player] = Bukkit.getScheduler().runTaskLater(SneakyResourcePacks.getInstance(), Runnable {
            SneakyResourcePack.applyAll(event.player)
        }, 20 * 5)
    }

    @EventHandler
    fun onResourcepacksLoaded(event: ResourcepacksLoadedEvent) {
        SneakyResourcePacks.getInstance().logger.info("${event.resourcepacks.size} resourcepacks loaded for ${event.player.name}")
        if (joinedPlayers.contains(event.player)) {
            joinedPlayers.remove(event.player)
            val command = "ms cast as ${event.player.name} holiday-xmas-login"
            Bukkit.dispatchCommand(event.player.server.consoleSender, command)
        }
    }
}

class ResourcepacksLoadedEvent(val player: Player, val resourcepacks: List<SneakyResourcePack>) : Event() {
    override fun getHandlers(): HandlerList {
        return HANDLERS
    }

    companion object {
        private val HANDLERS = HandlerList()

        @JvmStatic
        fun getHandlerList(): HandlerList {
            return HANDLERS
        }
    }
}