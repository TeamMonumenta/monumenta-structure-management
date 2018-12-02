package com.playmonumenta.epicstructures.commands;

import com.playmonumenta.epicstructures.Plugin;

import io.github.jorelali.commandapi.api.arguments.Argument;
import io.github.jorelali.commandapi.api.arguments.DynamicSuggestedStringArgument;
import io.github.jorelali.commandapi.api.arguments.IntegerArgument;
import io.github.jorelali.commandapi.api.CommandAPI;
import io.github.jorelali.commandapi.api.CommandPermission;

import java.util.LinkedHashMap;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

public class SetRespawnTimer {
	public static void register(Plugin plugin) {
		/* First one of these includes coordinate arguments */
		LinkedHashMap<String, Argument> arguments = new LinkedHashMap<>();

		arguments.put("label", new DynamicSuggestedStringArgument(() -> {return plugin.mRespawnManager.listStructures();}));
		arguments.put("ticks", new IntegerArgument(0));
		CommandAPI.getInstance().register("setrespawntimer",
		                                  CommandPermission.fromString("epicstructures"),
		                                  arguments,
		                                  (sender, args) -> {
		                                      setTimer(sender, plugin, (String)args[0], (Integer)args[1]);
		                                  }
		);
	}

	private static void setTimer(CommandSender sender, Plugin plugin, String label, int ticks) {
		if (plugin.mRespawnManager == null) {
			return;
		}
		try {
			plugin.mRespawnManager.setTimerPeriod(label, ticks);
		} catch (Exception e) {
			sender.sendMessage(ChatColor.RED + "Got error while attempting to set post respawn command: " + e.getMessage());
			return;
		}

		sender.sendMessage("Successfully set respawn timer to '" + Integer.toString(ticks) + "'");
	}
}
