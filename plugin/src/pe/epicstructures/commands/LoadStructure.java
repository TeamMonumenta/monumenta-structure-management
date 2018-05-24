package pe.epicstructures.commands;

import com.boydti.fawe.object.schematic.Schematic;

import com.sk89q.worldedit.Vector;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.Location;

import pe.epicstructures.Plugin;
import pe.epicstructures.utils.CommandUtils;
import pe.epicstructures.utils.MessagingUtils;
import pe.epicstructures.utils.StructureUtils;

public class LoadStructure implements CommandExecutor {
	Plugin mPlugin;
	org.bukkit.World mWorld;

	public LoadStructure(Plugin plugin, org.bukkit.World world) {
		mPlugin = plugin;
		mWorld = world;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String arg2, String[] arg3) {
		if (arg3.length != 4) {
			sender.sendMessage(ChatColor.RED + "This command requires exactly four arguments");
			return false;
		}

		if (arg3[0].contains("..")) {
			sender.sendMessage(ChatColor.RED + "Paths containing '..' are not allowed");
			return false;
		}

		// Parse the coordinates to load the structure
		Vector loadPos;
		try {
			Location loadLoc = CommandUtils.parseLocationFromString(sender, mWorld, arg3[1], arg3[2], arg3[3]);

			loadPos = new Vector(loadLoc.getBlockX(), loadLoc.getBlockY(), loadLoc.getBlockZ());
		} catch (Exception e) {
			sender.sendMessage(ChatColor.RED + "Failed to parse coordinates");
			MessagingUtils.sendStackTrace(sender, e);
			return false;
		}

		Schematic schem;
		try {
			schem = mPlugin.mStructureManager.loadSchematic("structures", arg3[0]);
		} catch (Exception e) {
			mPlugin.getLogger().severe("Caught exception: " + e);
			e.printStackTrace();

			if (sender != null) {
				sender.sendMessage(ChatColor.RED + "Failed to load structure");
				MessagingUtils.sendStackTrace(sender, e);
			}
			return false;
		}

		StructureUtils.paste(schem.getClipboard(), mWorld, loadPos);

		sender.sendMessage("Loaded structure '" + arg3[0] + "' at " + loadPos);

		return true;
	}
}
