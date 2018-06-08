package pe.epicstructures.managers;

import com.boydti.fawe.object.schematic.Schematic;

import com.sk89q.worldedit.Vector;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.Location;

import pe.epicstructures.Plugin;
import pe.epicstructures.utils.MessagingUtils;
import pe.epicstructures.utils.StructureUtils;

public class UserStructure {
	public enum StructureRotation {
		NONE,
		CLOCKWISE_90,
		CLOCKWISE_180,
		COUNTERCLOCKWISE_90;

		public static StructureRotation fromString(String str) throws IllegalArgumentException {
			switch (str) {
			case "NONE":
				return NONE;
			case "CLOCKWISE_180":
			case "CLOCKWISE_90":
			case "COUNTERCLOCKWISE_90":
				throw new IllegalArgumentException("Rotation '" + str + "' is not supported yet");
			default:
				throw new IllegalArgumentException("Rotation '" + str + "' is not one of ( NONE | CLOCKWISE_90 | CLOCKWISE_180 | COUNTERCLOCKWISE_90 )");
			}
		}
	}

	Plugin mPlugin;
	Player mPlayer;
	Vector mMinPos;
	Vector mMaxPos;
	String mType;
	StructureRotation mRotation;
	Schematic emptySchem;

	public UserStructure(Plugin plugin, Player player, Vector minpos, Vector maxpos,
	                     Location teleLoc, String type, StructureRotation rotation) throws Exception {
		mPlugin = plugin;
		mPlayer = player;
		mMinPos = minpos;
		mMaxPos = maxpos;
		mType = type;
		mRotation = rotation;

		/* TODO: Fail if this location overlaps an existing loaded location */

		/* Fail if there are any players within the structure coordinates */
		for (Player testPlayer : Bukkit.getOnlinePlayers()) {
			if (_playerInStructure(testPlayer)) {
				player.sendMessage(ChatColor.RED + "Can not load on top of a player!");
				throw new Exception("Can not load on top of a player!");
			}
		}

		/* Attempt to load the player's structure */
		Schematic schem;
		try {
			schem = mPlugin.mStructureManager.loadSchematic("user_structures", player.getUniqueId().toString() + "_" + type);
		} catch (Exception e) {
			/* Failed to load the player's structure - try the template */
			try {
				schem = mPlugin.mStructureManager.loadSchematic("templates", type);
				// TODO: Remove this debug statement
				player.sendMessage(ChatColor.RED + "Loading default!");
			} catch (Exception ex) {
				throw new Exception("Failed to load both user's structure AND template structure");
			}
		}

		/* TODO: Fail if the structure is the wrong size */

		/* Load the empty structure for use when player leaves later */
		try {
			emptySchem = mPlugin.mStructureManager.loadSchematic("templates", type + "_empty");
		} catch (Exception ex) {
			throw new Exception("Failed to load empty finalize structure");
		}

		// TODO: Remove this debug
		player.sendMessage(ChatColor.RED + "Loading structure!");

		/* TODO: Load the structure and paste it */
		StructureUtils.paste(schem.getClipboard(), player.getWorld(), minpos);

		/* Teleport player into the loaded area */
		teleLoc.setYaw(player.getLocation().getYaw());
		teleLoc.setPitch(player.getLocation().getPitch());
		teleLoc.add(0.5, 0, 0.5);
		player.teleport(teleLoc);
	}

	private boolean _playerInStructure(Player player) {
		//TODO: There must be a bug here? Structure clears prematurely...
		Location loc = player.getLocation();

		if (loc.getBlockX() >= mMinPos.getBlockX() && loc.getBlockX() <= mMaxPos.getBlockX() &&
		    loc.getBlockY() >= mMinPos.getBlockY() && loc.getBlockY() <= mMaxPos.getBlockY() &&
		    loc.getBlockZ() >= mMinPos.getBlockZ() && loc.getBlockZ() <= mMaxPos.getBlockZ()) {
			return true;
		}
		return false;
	}

	public boolean playerInStructure() {
		return _playerInStructure(mPlayer);
	}

	public void finalize() {
		// TODO: Remove this debug
		mPlayer.sendMessage("Finalizing!");

		/* Save the player's schematic */
		try {
			mPlugin.mStructureManager.saveSchematic("user_structures", mPlayer.getUniqueId().toString() + "_" + mType, mMinPos, mMaxPos);
		} catch (Exception e) {
			mPlayer.sendMessage(ChatColor.RED + "Failed to save your room! This is a serious problem. Please hover your mouse over the " +
			                   "exception below, take a screenshot, and report this to a moderator.");
			mPlugin.getLogger().severe("Caught exception: " + e);
			e.printStackTrace();

			MessagingUtils.sendStackTrace(mPlayer, e);

			/* Better to not overwrite? No good option here */
			return;
		}

		// TODO: Need a callabck from saving the schematic to invoke the paste overwrite that removes this from the list.

		/* Load the empty schematic now that the player has left */
		StructureUtils.paste(emptySchem.getClipboard(), mPlayer.getWorld(), mMinPos);
	}
}
