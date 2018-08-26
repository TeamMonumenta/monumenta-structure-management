package com.playmonumenta.epicstructures.commands;

import com.playmonumenta.epicstructures.Plugin;
import com.playmonumenta.epicstructures.utils.CommandUtils;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class ListRespawningStructures implements CommandExecutor {
	Plugin mPlugin;

	public ListRespawningStructures(Plugin plugin) {
		mPlugin = plugin;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String arg2, String[] arg3) {
		if (arg3.length != 0 && arg3.length != 1) {
			sender.sendMessage(ChatColor.RED + "This function takes optionally one argument");
			return false;
		}

		int page = 1;
		if (arg3.length == 1) {
			try {
				page = CommandUtils.parseIntFromString(sender, arg3[0]);
			} catch (Exception e) {
				return false;
			}
		}

		mPlugin.mRespawnManager.listStructures(sender, page);

		return true;
	}
}
