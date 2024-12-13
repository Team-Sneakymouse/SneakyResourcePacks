package com.danidipp.sneakyresourcepacks

import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerLoginEvent
import org.bukkit.event.player.PlayerResourcePackStatusEvent

class ResourcepackEvents : Listener {
//    @EventHandler
//    fun onResourcepackStatus(event: PlayerResourcePackStatusEvent) {
//        event.player.sendMessage("Resourcepack status: ${event.status}")
//    }
    @EventHandler
    fun onPlayerJoinEvent(event: PlayerJoinEvent) {
        Bukkit.getScheduler().runTaskLater(SneakyResourcePacks.getInstance(), Runnable {
            SneakyResourcePack.applyAll(event.player)
        }, 20 * 3)
    }
}