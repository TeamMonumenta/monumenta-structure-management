package com.playmonumenta.epicstructures.commands;

import com.playmonumenta.epicstructures.managers.SpawnerBreakTrigger;
import com.playmonumenta.epicstructures.Plugin;

import io.github.jorelali.commandapi.api.arguments.Argument;
import io.github.jorelali.commandapi.api.arguments.DynamicSuggestedStringArgument;
import io.github.jorelali.commandapi.api.arguments.GreedyStringArgument;
import io.github.jorelali.commandapi.api.arguments.IntegerArgument;
import io.github.jorelali.commandapi.api.CommandAPI;
import io.github.jorelali.commandapi.api.CommandPermission;

import java.util.LinkedHashMap;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

public class SetSpawnerBreakTrigger {
	public static void register(Plugin plugin) {
		/* First one of these includes coordinate arguments */
		LinkedHashMap<String, Argument> arguments = new LinkedHashMap<>();

		arguments.put("label", new DynamicSuggestedStringArgument(() -> {return plugin.mRespawnManager.listStructures();}));
		CommandAPI.getInstance().register("setspawnerbreaktrigger",
		                                  new CommandPermission("epicstructures"),
		                                  arguments,
		                                  (sender, args) -> {
		                                      setTrigger(sender, plugin, (String)args[0], 0, null);
		                                  }
		);

		arguments.put("spawner_count", new IntegerArgument(0));
		arguments.put("quest_component", new GreedyStringArgument());
		CommandAPI.getInstance().register("setspawnerbreaktrigger",
		                                  new CommandPermission("epicstructures"),
		                                  arguments,
		                                  (sender, args) -> {
		                                      setTrigger(sender, plugin, (String)args[0], (Integer)args[1], (String)args[2]);
		                                  }
		);
	}

	private static void setTrigger(CommandSender sender, Plugin plugin, String label, int spawnerCount, String questComponentStr) {
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
