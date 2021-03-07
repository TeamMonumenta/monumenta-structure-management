package com.playmonumenta.structures.commands;

import com.playmonumenta.structures.Plugin;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.IntegerArgument;
import dev.jorel.commandapi.arguments.StringArgument;

public class SetRespawnTimer {
	public static void register(Plugin plugin) {
		new CommandAPICommand("setrespawntimer")
			.withPermission(CommandPermission.fromString("monumenta.structures"))
			.withArguments(new StringArgument("label").overrideSuggestions((sender) -> {return plugin.mRespawnManager.listStructures();}))
			.withArguments(new IntegerArgument("ticks", 0))
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
