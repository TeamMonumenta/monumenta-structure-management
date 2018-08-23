package pe.epicstructures.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.util.Vector;
import org.bukkit.World;

import pe.epicstructures.Plugin;
import pe.epicstructures.utils.CommandUtils;

public class AddRespawningStructure implements CommandExecutor {
	Plugin mPlugin;
	World mWorld;

	public AddRespawningStructure(Plugin plugin, World world) {
		mPlugin = plugin;
		mWorld = world;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String arg2, String[] arg3) {
		if (arg3.length >= 8) {
			sender.sendMessage(ChatColor.RED + "Invalid number of parameters! Need at least 8");
			return false;
		}

		String configLabel = arg3[0];
		String path = arg3[1];

		// Parse the integer values
		Vector loadPos;
		try {
			loadPos = CommandUtils.parseLocationFromString(sender, mWorld, arg3[2], arg3[3], arg3[4]).toVector();
		} catch (Exception e) {
			sender.sendMessage(ChatColor.RED + "Failed to parse coordinates");
			return false;
		}

		int extraRadius;
		try {
			extraRadius = CommandUtils.parseIntFromString(sender, arg3[5]);
		} catch (Exception e) {
			sender.sendMessage(ChatColor.RED + "Failed to parse extra detection radius");
			return false;
		}

		int respawnTime;
		try {
			respawnTime = CommandUtils.parseIntFromString(sender, arg3[6]);
		} catch (Exception e) {
			sender.sendMessage(ChatColor.RED + "Failed to parse respawn time");
			return false;
		}

		// Accumulate the name of the POI from remaining arguments
		String name = "";
		for (int i = 7; i < arg3.length; i++) {
			if (i > 7) {
				name += " ";
			}
			name += arg3[i].replaceAll("\"", "");
		}

		try {
			mPlugin.mRespawnManager.addStructure(extraRadius, configLabel, name, path, loadPos, respawnTime);
		} catch (Exception e) {
			sender.sendMessage(ChatColor.RED + "Failed to add structure: " + e.getMessage());
			return false;
		}

		sender.sendMessage("Structure added successfully");

		return true;
	}
}
