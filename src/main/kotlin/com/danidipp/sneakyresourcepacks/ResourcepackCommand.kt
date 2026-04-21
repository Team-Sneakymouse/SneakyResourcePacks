package com.danidipp.sneakyresourcepacks

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.event.HoverEvent
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import java.util.UUID
import java.util.stream.Collector

class ResourcepackCommand : Command("resourcepack") {
    init {
        description = "Manage resourcepacks"
        usageMessage = "/resourcepack [list|enable|disable|add|remove|clear|reload] [name]"
//        permission = "sneakyresourcepacks.resourcepack"
        aliases = listOf("rp")
    }

    /**
     * /resourcepack -> reload all resourcepacks
     * /resourcepack list -> list all resourcepacks
     * /resourcepack enable <name|id> -> enable specified resourcepack
     * /resourcepack disable <name|id> -> disable specified resourcepack
     * /resourcepack add <name|id> -> load specified resourcepack
     * /resourcepack remove <name|id> -> remove specified resourcepack
     * /resourcepack clear -> remove all resourcepacks
     * /resourcepack reload -> reload config
     * /resourcepack update <name|id> <hash> -> update resourcepack hash
     */
    override fun execute(sender: CommandSender, commandLabel: String, args: Array<out String>): Boolean {
        if (!SneakyResourcePacks.getInstance().isEnabled) {
            sender.sendMessage("Plugin is disabled")
            return true
        }

        // /resourcepack
        if (args.isEmpty() || Bukkit.getServer().getPlayer(args.last()) != null) {
            if (sender !is Player) {
                sender.sendMessage("You must be a player to use this command")
                return true
            }
            SneakyResourcePack.applyAll(sender)
            return true
        }

        if (!sender.hasPermission("sneakyresourcepacks.admin")) {
            sender.sendMessage("You do not have permission to use this command")
            return true
        }
        val target =
            if (args[args.size - 1] == "all") Bukkit.getServer()
            else Bukkit.getPlayer(args[args.size - 1]) ?: sender


        // /resourcepack <user>
        if (args.size == 1 && target != null && (target != sender || args[0].lowercase() == sender.name.lowercase())) {
            SneakyResourcePack.applyAll(target)
            return true
        }

        when (args[0]) {
            "list" -> {
                if (SneakyResourcePacks.resourcePacks.isEmpty()) {
                    sender.sendMessage("No resourcepacks found")
                    return true
                }
                val text = SneakyResourcePacks.resourcePacks.values.stream()
                    .sorted(compareByDescending  { it.priority })
                    .map {
                        Component.text(it.name, if (it.enabled) NamedTextColor.DARK_GREEN else NamedTextColor.RED)
                            .hoverEvent(HoverEvent.showText(Component.text(it.hash)))
                            .clickEvent(ClickEvent.copyToClipboard(it.hash))
                    }
                    .reduce(Component.text("")) { a, b -> a.append(Component.text(if (a.children().isNotEmpty()) ", " else "", NamedTextColor.WHITE)).append(b) }
                sender.sendMessage(text)
            }
            "enable" -> {
                val resourcepack = parseResourcepackArg(args)
                if (resourcepack == null) {
                    sender.sendMessage("Resourcepack ${args[1]} not found")
                    return true
                }
                resourcepack.enabled = true
                SneakyResourcePacks.getInstance().updateConfig()
                sender.sendMessage("${resourcepack.name} enabled")
            }
            "disable" -> {
                val resourcepack = parseResourcepackArg(args)
                if (resourcepack == null) {
                    sender.sendMessage("Resourcepack ${args[1]} not found")
                    return true
                }
                resourcepack.enabled = false
                SneakyResourcePacks.getInstance().updateConfig()
                sender.sendMessage("${resourcepack.name} disabled")
            }

            "add" -> {
                if (target == null) {
                    sender.sendMessage("Player not found")
                    return true
                }
                val resourcepack = parseResourcepackArg(args)
                if (resourcepack == null) {
                    sender.sendMessage("Resourcepack ${args[1]} not found")
                    return true
                }
                resourcepack.sendTo(target)
            }
            "remove" -> {
                if (target == null) {
                    sender.sendMessage("Player not found")
                    return true
                }
                val resourcepack = parseResourcepackArg(args)
                if (resourcepack == null) {
                    sender.sendMessage("Resourcepack ${args[1]} not found")
                    return true
                }
                resourcepack.removeFrom(target)
            }
            "clear" -> {
                if (target == null) {
                    sender.sendMessage("Player not found")
                    return true
                }
                SneakyResourcePack.clearAll(target)
            }
            "reload" -> {
                SneakyResourcePacks.getInstance().reloadConfig()
                SneakyResourcePacks.getInstance().loadConfig()
                sender.sendMessage("Resourcepacks reloaded")
            }
            "update" -> {
                if (args.size < 3) {
                    sender.sendMessage("Usage: /resourcepack update <name|id> <hash>")
                    return true
                }
                val resourcepack = parseResourcepackArg(args)
                if (resourcepack == null) {
                    sender.sendMessage("Resourcepack ${args[1]} not found")
                    return true
                }
                val oldHash = resourcepack.hash
                resourcepack.hash = args[2]
                SneakyResourcePacks.getInstance().updateConfig()
                sender.sendMessage("${resourcepack.name} updated from $oldHash to ${resourcepack.hash}")
            }
            else -> {
                sender.sendMessage("Invalid subcommand")
            }
        }
        return true
    }

    private fun parseResourcepackArg(args: Array<out String>): SneakyResourcePack? {
        if (args.size < 2) {
            return null
        }
        val uuid = runCatching { UUID.fromString(args[1]) }.getOrNull()
        return if (uuid != null) {
            SneakyResourcePacks.resourcePacks[uuid]
        } else {
            SneakyResourcePacks.resourcePacks.values.find { it.name.equals(args[1], true) }
        }
    }

    override fun tabComplete(sender: CommandSender, alias: String, args: Array<out String>): MutableList<String> {
        if (!sender.hasPermission("sneakyresourcepacks.admin")) {
            return mutableListOf()
        }
        return when (args.size) {
            0, 1 -> listOf("list", "enable", "disable", "add", "remove", "clear", "reload", "update")
                .filter { args.isEmpty() || it.startsWith(args[0], ignoreCase = true) }
                .toMutableList()
            2 -> when (args[0]) {
                "enable" -> SneakyResourcePacks.resourcePacks.values
                    .filter { !it.enabled }
                    .map { it.name }
                    .filter { it.startsWith(args[1], ignoreCase = true) }
                    .toMutableList()

                "disable" -> SneakyResourcePacks.resourcePacks.values
                    .filter { it.enabled }
                    .map { it.name }
                    .filter { it.startsWith(args[1], ignoreCase = true) }
                    .toMutableList()

                "add" -> SneakyResourcePacks.resourcePacks.values
                    .filter { !it.players.contains(sender) }
                    .map { it.name }
                    .filter { it.startsWith(args[1], ignoreCase = true) }
                    .toMutableList()

                "remove" -> SneakyResourcePacks.resourcePacks.values
                    .filter { it.players.contains(sender) }
                    .map { it.name }
                    .filter { it.startsWith(args[1], ignoreCase = true) }
                    .toMutableList()

                "update" -> SneakyResourcePacks.resourcePacks.values
                    .map { it.name }
                    .filter { it.startsWith(args[1], ignoreCase = true) }
                    .toMutableList()

                else -> mutableListOf()
            }
            else -> mutableListOf()
        }
    }
}