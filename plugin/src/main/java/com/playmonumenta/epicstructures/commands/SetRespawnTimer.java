package com.playmonumenta.epicstructures.commands;

import java.util.LinkedHashMap;

import com.playmonumenta.epicstructures.Plugin;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.IntegerArgument;
import dev.jorel.commandapi.arguments.StringArgument;

public class SetRespawnTimer {
	public static void register(Plugin plugin) {
		/* First one of these includes coordinate arguments */
		LinkedHashMap<String, Argument> arguments = new LinkedHashMap<>();

		arguments.put("label", new StringArgument().overrideSuggestions((sender) -> {return plugin.mRespawnManager.listStructures();}));
		arguments.put("ticks", new IntegerArgument(0));
		new CommandAPICommand("setrespawntimer")
			.withPermission(CommandPermission.fromString("epicstructures"))
			.withArguments(arguments)
			.executes((sender, args) -> {
				setTimer(sender, plugin, (String)args[0], (Integer)args[1]);
			})
			.register();
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
