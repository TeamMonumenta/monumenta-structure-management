package pe.epicstructures.commands;

import com.sk89q.worldedit.Vector;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ProxiedCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.Location;

import pe.epicstructures.managers.UserStructure;
import pe.epicstructures.managers.UserStructure.StructureRotation;
import pe.epicstructures.Plugin;
import pe.epicstructures.utils.CommandUtils;
import pe.epicstructures.utils.MessagingUtils;

public class LoadUserStructure implements CommandExecutor {
	Plugin mPlugin;
	org.bukkit.World mWorld;

	public LoadUserStructure(Plugin plugin, org.bukkit.World world) {
		mPlugin = plugin;
		mWorld = world;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String arg2, String[] arg3) {
		if (arg3.length != 11) {
			sender.sendMessage(ChatColor.RED + "This command requires exactly 11 arguments");
			return false;
		}

		if (!(sender instanceof ProxiedCommandSender)) {
			sender.sendMessage(ChatColor.RED + "This command can only be run via /execute");
			return false;
		}

		String type = arg3[0];
		if (type.contains("..")) {
			sender.sendMessage(ChatColor.RED + "Paths containing '..' are not allowed");
			return false;
		}

		/* Identify the callee / caller */
		CommandSender callee;
		CommandSender caller;
		try {
			callee = CommandUtils.getProxiedSender(sender, true);
			caller = CommandUtils.getProxiedSender(sender, false);
		} catch (Exception e) {
			sender.sendMessage(ChatColor.RED + "Failed to identify callee/caller");
			MessagingUtils.sendStackTrace(sender, e);
			return false;
		}
		if (!(callee instanceof Player)) {
			sender.sendMessage(ChatColor.RED + "Execute target must be a player");
			return false;
		}
		Player player = (Player)callee;

		StructureRotation rotation;
		try {
			rotation = StructureRotation.fromString(arg3[1]);
		} catch (IllegalArgumentException e) {
			sender.sendMessage(ChatColor.RED + e.toString());
			return false;
		}

		/* Parse the coordinates */
		Vector minpos;
		Vector maxpos;
		Location teleLoc;
		try {
			Location loc1 = CommandUtils.parseLocationFromString(caller, mWorld, arg3[2], arg3[3], arg3[4]);
			Location loc2 = CommandUtils.parseLocationFromString(caller, mWorld, arg3[5], arg3[6], arg3[7]);
			teleLoc = CommandUtils.parseLocationFromString(caller, mWorld, arg3[8], arg3[9], arg3[10]);

			Vector pos1 = new Vector(loc1.getBlockX(), loc1.getBlockY(), loc1.getBlockZ());
			Vector pos2 = new Vector(loc2.getBlockX(), loc2.getBlockY(), loc2.getBlockZ());

			minpos = Vector.getMinimum(pos1, pos2);
			maxpos = Vector.getMaximum(pos1, pos2);
		} catch (Exception e) {
			sender.sendMessage(ChatColor.RED + "Failed to parse coordinates");
			MessagingUtils.sendStackTrace(sender, e);
			return false;
		}

		/* Create and load the user structure given these parameters */
		UserStructure struct;
		try {
			struct = new UserStructure(mPlugin, player, minpos, maxpos, teleLoc, type, rotation);
		} catch (Exception e) {
			sender.sendMessage(ChatColor.RED + e.toString());
			return false;
		}

		/* Register this structure so it will be tracked and saved/unloaded appropriately */
		mPlugin.mUserStructureManager.add(struct);

		return true;
	}
}

