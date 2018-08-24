package pe.epicstructures.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import pe.epicstructures.Plugin;

public class SetPostRespawnCommand implements CommandExecutor {
	Plugin mPlugin;

	public SetPostRespawnCommand(Plugin plugin) {
		mPlugin = plugin;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String arg2, String[] arg3) {
		if (arg3.length < 1) {
			sender.sendMessage(ChatColor.RED + "This command requires at least one argument");
			return false;
		}

		String label = arg3[0];

		String postRespawnCommand = null;
		if (arg3.length > 1) {
			postRespawnCommand = "";

			// Accumulate the post respawn command from remaining arguments
			for (int i = 1; i < arg3.length; i++) {
				if (i > 1) {
					postRespawnCommand += " ";
				}
				postRespawnCommand += arg3[i];
			}
			if (postRespawnCommand.startsWith("/")) {
				postRespawnCommand = postRespawnCommand.substring(1);
			}
		}

		try {
			mPlugin.mRespawnManager.setPostRespawnCommand(label, postRespawnCommand);
		} catch (Exception e) {
			sender.sendMessage(ChatColor.RED + "Got error while attempting to respawn structure: " + e.getMessage());
			return false;
		}

		if (arg3.length > 1) {
			sender.sendMessage("Successfully set post_respawn_command to '" + postRespawnCommand + "'");
		} else {
			sender.sendMessage("Successfully unset post_respawn_command");
		}

		return true;
	}
}
