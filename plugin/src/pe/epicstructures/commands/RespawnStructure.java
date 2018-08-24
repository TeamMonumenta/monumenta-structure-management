package pe.epicstructures.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import pe.epicstructures.Plugin;
import pe.epicstructures.utils.CommandUtils;

public class RespawnStructure implements CommandExecutor {
	Plugin mPlugin;

	public RespawnStructure(Plugin plugin) {
		mPlugin = plugin;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String arg2, String[] arg3) {
		if (arg3.length < 1 || arg3.length > 2) {
			sender.sendMessage(ChatColor.RED + "This command requires one or two arguments");
			return false;
		}

		String label = arg3[0];
		int ticksUntilRespawn = 600; // Default 30s

		if (arg3.length == 2) {
			try {
				ticksUntilRespawn = CommandUtils.parseIntFromString(sender, arg3[1]);
			} catch (Exception e) {
				return false;
			}
		}

		try {
			mPlugin.mRespawnManager.setTimer(label, ticksUntilRespawn);
		} catch (Exception e) {
			sender.sendMessage(ChatColor.RED + "Got error while attempting to respawn structure: " + e.getMessage());
			return false;
		}

		return true;
	}
}
