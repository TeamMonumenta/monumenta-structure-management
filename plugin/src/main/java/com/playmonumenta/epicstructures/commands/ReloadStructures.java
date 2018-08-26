package com.playmonumenta.epicstructures.commands;

import com.playmonumenta.epicstructures.Plugin;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class ReloadStructures implements CommandExecutor {
	Plugin mPlugin;

	public ReloadStructures(Plugin plugin) {
		mPlugin = plugin;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String arg2, String[] arg3) {
		if (arg3.length != 0) {
			sender.sendMessage(ChatColor.RED + "No parameters are needed for this function");
			return false;
		}

		mPlugin.reloadConfig();

		return true;
	}
}
