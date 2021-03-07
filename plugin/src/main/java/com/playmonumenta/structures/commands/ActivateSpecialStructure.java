package com.playmonumenta.structures.commands;

import com.playmonumenta.structures.Plugin;
import com.playmonumenta.structures.utils.CommandUtils;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.StringArgument;
import dev.jorel.commandapi.arguments.TextArgument;
import dev.jorel.commandapi.exceptions.WrapperCommandSyntaxException;

public class ActivateSpecialStructure {
	public static void register(Plugin plugin) {
		final String command = "activatespecialstructure";
		final CommandPermission perms = CommandPermission.fromString("monumenta.structures");

		new CommandAPICommand(command)
			.withPermission(perms)
			.withArguments(new StringArgument("label").overrideSuggestions((sender) -> {return plugin.mRespawnManager.listStructures();}))
			.executes((sender, args) -> {
				activate(sender, plugin, (String)args[0], null);
			})
			.register();

		new CommandAPICommand(command)
			.withPermission(perms)
			.withArguments(new StringArgument("label").overrideSuggestions((sender) -> {return plugin.mRespawnManager.listStructures();}))
			.withArguments(new TextArgument("special_structure_path"))
			.executes((sender, args) -> {
				activate(sender, plugin, (String)args[0], (String)args[1]);
			})
			.register();
	}

	private static void activate(CommandSender sender, Plugin plugin, String label, String path) throws WrapperCommandSyntaxException {
		CommandUtils.getAndValidateSchematicPath(plugin, path, true);

		if (plugin.mRespawnManager == null) {
			return;
		}

		try {
			plugin.mRespawnManager.activateSpecialStructure(label, path);
		} catch (Exception e) {
			sender.sendMessage(ChatColor.RED + "Got error while attempting to activate special structure: " + e.getMessage());
		}

		if (path != null) {
			sender.sendMessage("Successfully activated special structure");
		} else {
			sender.sendMessage("Successfully deactivated special structure");
		}
	}
}
