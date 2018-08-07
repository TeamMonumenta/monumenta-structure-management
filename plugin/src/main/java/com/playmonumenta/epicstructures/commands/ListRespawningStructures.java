package com.playmonumenta.epicstructures.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import com.playmonumenta.epicstructures.Plugin;

public class ListRespawningStructures implements CommandExecutor {
	Plugin mPlugin;

	public ListRespawningStructures(Plugin plugin) {
		mPlugin = plugin;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String arg2, String[] arg3) {
		if (arg3.length != 0) {
			sender.sendMessage(ChatColor.RED + "No parameters are needed for this function");
			return false;
		}

		mPlugin.mRespawnManager.listStructures(sender);

		return true;
	}
}
