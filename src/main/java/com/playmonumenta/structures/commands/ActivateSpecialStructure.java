package com.playmonumenta.structures.commands;

import com.playmonumenta.structures.managers.RespawnManager;
import com.playmonumenta.structures.utils.CommandUtils;
import com.playmonumenta.structures.utils.MessagingUtils;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.StringArgument;
import dev.jorel.commandapi.arguments.TextArgument;
import javax.annotation.Nullable;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;

public class ActivateSpecialStructure {
	public static void register(Plugin plugin) {
		final String command = "activatespecialstructure";
		final CommandPermission perms = CommandPermission.fromString("monumenta.structures");

		new CommandAPICommand(command)
			.withPermission(perms)
			.withArguments(new StringArgument("label").replaceSuggestions(RespawnManager.SUGGESTIONS_STRUCTURES))
			.executes((sender, args) -> {
				activate(sender, plugin, (String)args[0], null);
			})
			.register();

		new CommandAPICommand(command)
			.withPermission(perms)
			.withArguments(new StringArgument("label").replaceSuggestions(RespawnManager.SUGGESTIONS_STRUCTURES))
			.withArguments(new TextArgument("special_structure_path"))
			.executes((sender, args) -> {
				activate(sender, plugin, (String)args[0], (String)args[1]);
			})
			.register();
	}

	private static void activate(CommandSender sender, Plugin plugin, String label, @Nullable String path) {
		try {
			CommandUtils.getAndValidateSchematicPath(plugin, path, true);
			RespawnManager.getInstance().activateSpecialStructure(label, path);
		} catch (Exception e) {
			sender.sendMessage(ChatColor.RED + "Got error while attempting to activate special structure: " + e.getMessage());
			MessagingUtils.sendStackTrace(sender, e);
			return;
		}

		if (path != null) {
			sender.sendMessage("Successfully activated special structure");
		} else {
			sender.sendMessage("Successfully deactivated special structure");
		}
	}
}
