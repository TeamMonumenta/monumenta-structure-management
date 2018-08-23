package pe.epicstructures.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import pe.epicstructures.Plugin;

public class RemoveRespawningStructure implements CommandExecutor {
	Plugin mPlugin;

	public RemoveRespawningStructure(Plugin plugin) {
		mPlugin = plugin;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String arg2, String[] arg3) {
		if (arg3.length == 1) {
			sender.sendMessage(ChatColor.RED + "Invalid number of parameters - expected 1");
			return false;
		}

		String configLabel = arg3[0];

		try {
			mPlugin.mRespawnManager.removeStructure(configLabel);
		} catch (Exception e) {
			sender.sendMessage(ChatColor.RED + "Failed to remove structure: " + e.getMessage());
			return false;
		}

		sender.sendMessage("Structure added successfully");

		return true;
	}
}
