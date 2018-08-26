package com.playmonumenta.epicstructures.utils;

import org.bukkit.ChatColor;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ProxiedCommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.Location;
import org.bukkit.World;

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
