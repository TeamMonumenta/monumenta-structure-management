package com.playmonumenta.structures.utils;

import java.io.File;
import java.nio.file.Paths;
import java.util.regex.Pattern;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ProxiedCommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;

import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.exceptions.WrapperCommandSyntaxException;

public class CommandUtils {
	private static final String BASE_FOLDER_NAME = "structures";

	public static String getSchematicPath(Plugin plugin, String baseName) {
		return Paths.get(plugin.getDataFolder().toString(), BASE_FOLDER_NAME, baseName + ".schematic").toString();
	}

	public static File getAndValidateSchematicPath(Plugin plugin, String baseName, boolean failIfNotExist) throws WrapperCommandSyntaxException {
		final Pattern invalidPathPattern = Pattern.compile("[^-/_a-zA-Z0-9]");

		if (baseName == null || baseName.isEmpty()) {
			CommandAPI.fail("Path is null or empty");
		}

		if (invalidPathPattern.matcher(baseName).find()) {
			CommandAPI.fail("Path contains illegal characters");
		}

		if (baseName.contains("..")) {
			CommandAPI.fail("Path cannot contain '..'");
		}

		final String fileName = getSchematicPath(plugin, baseName);
		File file = new File(fileName);
		if (failIfNotExist && !file.exists()) {
			CommandAPI.fail("Path '" + baseName + "' does not exist (full path '" + fileName + "')");
		}
		return file;
	}

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

	public static CommandSender getProxiedSender(CommandSender sender, boolean callee) throws Exception {
		if (sender == null) {
			throw new Exception("sender is null!");
		}

		if (sender instanceof Entity) {
			return sender;
		} else if (sender instanceof BlockCommandSender) {
			return sender;
		} else if (sender instanceof ProxiedCommandSender) {
			if (callee) {
				return getProxiedSender(((ProxiedCommandSender)sender).getCallee(), callee);
			} else {
				return getProxiedSender(((ProxiedCommandSender)sender).getCaller(), callee);
			}
		}

		throw new Exception("Unknown sender type!");
	}

	public static Location getLocation(CommandSender sender) throws Exception {
		if (sender == null) {
			throw new Exception("sender is null!");
		}
		Location senderLoc = null;

		if (sender instanceof Entity) {
			senderLoc = ((Entity)sender).getLocation();
		} else if (sender instanceof BlockCommandSender) {
			senderLoc = ((BlockCommandSender)sender).getBlock().getLocation();
		} else if (sender instanceof ProxiedCommandSender) {
			senderLoc = getLocation(((ProxiedCommandSender)sender).getCallee());
		}

		if (senderLoc == null) {
			throw new Exception("Failed to get command sender coordinates");
		}

		return senderLoc;
	}

	public static Location parseLocationFromString(CommandSender sender, World world, String x,
	                                               String y, String z) throws Exception {
		Location senderLoc;

		if (x.contains("~") || y.contains("~") || z.contains("~")) {
			senderLoc = getLocation(sender);
		} else {
			senderLoc = new Location(world, 0, 0, 0);
		}

		if (!x.equals("~")) {
			if (x.startsWith("~")) {
				senderLoc.add(Float.parseFloat(x.substring(1)), 0, 0);
			} else {
				senderLoc.setX(Float.parseFloat(x));
			}
		}

		if (!y.equals("~")) {
			if (y.startsWith("~")) {
				senderLoc.add(0, Float.parseFloat(y.substring(1)), 0);
			} else {
				senderLoc.setY(Float.parseFloat(y));
			}
		}

		if (!z.equals("~")) {
			if (z.startsWith("~")) {
				senderLoc.add(0, 0, Float.parseFloat(z.substring(1)));
			} else {
				senderLoc.setZ(Float.parseFloat(z));
			}
		}

		return senderLoc;
	}
}
