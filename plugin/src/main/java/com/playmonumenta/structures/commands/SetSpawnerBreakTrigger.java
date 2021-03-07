package com.playmonumenta.structures.commands;

import com.playmonumenta.structures.Plugin;
import com.playmonumenta.structures.managers.SpawnerBreakTrigger;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.GreedyStringArgument;
import dev.jorel.commandapi.arguments.IntegerArgument;
import dev.jorel.commandapi.arguments.StringArgument;

public class SetSpawnerBreakTrigger {
	public static void register(Plugin plugin) {
		final String command = "setspawnerbreaktrigger";
		final CommandPermission perms = CommandPermission.fromString("monumenta.structures");

		new CommandAPICommand(command)
			.withPermission(perms)
			.withArguments(new StringArgument("label").overrideSuggestions((sender) -> {return plugin.mRespawnManager.listStructures();}))
			.executes((sender, args) -> {
				setTrigger(sender, plugin, (String)args[0], 0, null);
			})
			.register();

		new CommandAPICommand(command)
			.withPermission(perms)
			.withArguments(new StringArgument("label").overrideSuggestions((sender) -> {return plugin.mRespawnManager.listStructures();}))
			.withArguments(new IntegerArgument("spawner_count", 0))
			.withArguments(new GreedyStringArgument("quest_component"))
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
