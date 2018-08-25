package pe.epicstructures.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import pe.epicstructures.Plugin;

public class ActivateSpecialStructure implements CommandExecutor {
	Plugin mPlugin;

	public ActivateSpecialStructure(Plugin plugin) {
		mPlugin = plugin;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String arg2, String[] arg3) {
		if (arg3.length != 1 && arg3.length != 2) {
			sender.sendMessage(ChatColor.RED + "This command requires one or two arguments");
			return false;
		}

		String label = arg3[0];
		String nextRespawnPath = null;
		if (arg3.length == 2) {
			nextRespawnPath = arg3[1];
		}

		try {
			mPlugin.mRespawnManager.activateSpecialStructure(label, nextRespawnPath);
		} catch (Exception e) {
			sender.sendMessage(ChatColor.RED + "Got error while attempting to activate special structure: " + e.getMessage());
			return false;
		}

		if (arg3.length == 2) {
			sender.sendMessage("Successfully activated special structure");
		} else {
			sender.sendMessage("Successfully deactivated special structure");
		}

		return true;
	}
}
