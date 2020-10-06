package com.playmonumenta.epicstructures.commands;

import java.util.LinkedHashMap;

import com.playmonumenta.epicstructures.Plugin;
import com.playmonumenta.epicstructures.managers.SpawnerBreakTrigger;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.GreedyStringArgument;
import dev.jorel.commandapi.arguments.IntegerArgument;
import dev.jorel.commandapi.arguments.StringArgument;

public class SetSpawnerBreakTrigger {
	public static void register(Plugin plugin) {
		final String command = "setspawnerbreaktrigger";
		final CommandPermission perms = CommandPermission.fromString("epicstructures");

		/* First one of these includes coordinate arguments */
		LinkedHashMap<String, Argument> arguments = new LinkedHashMap<>();

		arguments.put("label", new StringArgument().overrideSuggestions((sender) -> {return plugin.mRespawnManager.listStructures();}));
		new CommandAPICommand(command)
			.withPermission(perms)
			.withArguments(arguments)
			.executes((sender, args) -> {
				setTrigger(sender, plugin, (String)args[0], 0, null);
			})
			.register();

		arguments.put("spawner_count", new IntegerArgument(0));
		arguments.put("quest_component", new GreedyStringArgument());
		new CommandAPICommand(command)
			.withPermission(perms)
			.withArguments(arguments)
			.executes((sender, args) -> {
				setTrigger(sender, plugin, (String)args[0], (Integer)args[1], (String)args[2]);
			})
			.register();
	}

	private static void setTrigger(CommandSender sender, Plugin plugin, String label, int spawnerCount, String questComponentStr) {
		if (plugin.mRespawnManager == null) {
			return;
		}
		try {
			SpawnerBreakTrigger trigger = null;
			if (questComponentStr != null) {
				trigger = new SpawnerBreakTrigger(plugin, spawnerCount, spawnerCount, questComponentStr);
			}
			plugin.mRespawnManager.setSpawnerBreakTrigger(label, trigger);
		} catch (Exception e) {
			sender.sendMessage(ChatColor.RED + "Got error while attempting to set spawner break trigger: " + e.getMessage());
			return;
		}

		if (questComponentStr != null) {
			sender.sendMessage("Successfully set spawner break trigger");
		} else {
			sender.sendMessage("Successfully unset spawner break trigger");
		}
	}
}
