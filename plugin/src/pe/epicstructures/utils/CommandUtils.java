package pe.epicstructures.utils;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

public class CommandUtils {
	public static int parseIntFromString(CommandSender sender, String str) throws Exception {
		int value = 0;

		try{
			value = Integer.parseInt(str);
		} catch (NumberFormatException e) {
			if (sender != null) {
				sender.sendMessage(ChatColor.RED + "Invalid parameter " + str +
				". Must be whole number value between " + Integer.MIN_VALUE + " and " + Integer.MAX_VALUE);
			}
			throw new Exception(e);
		}

		return value;
	}
}
