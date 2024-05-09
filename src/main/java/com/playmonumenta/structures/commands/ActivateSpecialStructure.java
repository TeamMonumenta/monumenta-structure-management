package com.playmonumenta.structures.commands;

import com.playmonumenta.structures.managers.RespawnManager;
import com.playmonumenta.structures.utils.CommandUtils;
import com.playmonumenta.structures.utils.MessagingUtils;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.StringArgument;
import dev.jorel.commandapi.arguments.TextArgument;
import javax.annotation.Nullable;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;

public class ActivateSpecialStructure {
	public static void register(Plugin plugin) {
		final String command = "activatespecialstructure";
		final CommandPermission perms = CommandPermission.fromString("monumenta.structures");

		Argument<String> labelArg = new StringArgument("label").replaceSuggestions(RespawnManager.SUGGESTIONS_STRUCTURES);
		TextArgument pathArg = new TextArgument("special_structure_path");

		new CommandAPICommand(command)
			.withPermission(perms)
			.withArguments(labelArg)
			.withArguments(pathArg)
			.executes((sender, args) -> {
				activate(sender, plugin, args.getByArgument(labelArg), args.getByArgument(pathArg));
			})
			.register();
	}

	private static void activate(CommandSender sender, Plugin plugin, String label, String path) {
		try {
			CommandUtils.getAndValidateSchematicPath(plugin, path, true);
			RespawnManager.getInstance().activateSpecialStructure(label, path);
			sender.sendMessage("Successfully activated special structure");
		} catch (Exception e) {
			sender.sendMessage(Component.text("Got error while attempting to activate special structure: " + e.getMessage(), NamedTextColor.RED));
			MessagingUtils.sendStackTrace(sender, e);
		}
	}
}
