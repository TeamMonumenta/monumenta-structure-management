package pe.epicstructures.managers;

import com.boydti.fawe.object.schematic.Schematic;

import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.regions.Region;

import java.util.LinkedHashMap;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.Location;
import org.bukkit.util.Vector;
import org.bukkit.World;

import pe.epicstructures.Plugin;
import pe.epicstructures.utils.MessagingUtils;
import pe.epicstructures.utils.StructureUtils;

public class RespawningStructure implements Comparable<RespawningStructure> {
	public class StructureBounds {
		public Vector mLowerCorner;
		public Vector mUpperCorner;

		public StructureBounds(Vector lowerCorner, Vector upperCorner) {
			mLowerCorner = Vector.getMinimum(lowerCorner, upperCorner);
			mUpperCorner = Vector.getMaximum(lowerCorner, upperCorner);
		}

		public boolean within(Vector vec) {
			return vec.getX() >= mLowerCorner.getX() && vec.getX() <= mUpperCorner.getX() &&
			       vec.getY() >= mLowerCorner.getY() && vec.getY() <= mUpperCorner.getY() &&
			       vec.getZ() >= mLowerCorner.getZ() && vec.getZ() <= mUpperCorner.getZ();
		}
	}

	private Plugin mPlugin;
	private World mWorld;
	private Clipboard mClipboard;         // The structure itself
	protected String mConfigLabel;        // The label used to modify this structure via commands
	private String mName;                 // What the pretty name of the structure is
	private String mPath;                 // Path where the structure should load from
	private Vector mLoadPos;              // Where it will be loaded
	private StructureBounds mInnerBounds; // The bounding box for the structure itself
	private StructureBounds mOuterBounds; // The bounding box for the nearby area around the structure
	private int mExtraRadius;             // Radius around the structure that still gets messages
	private int mTicksLeft;               // How many ticks remaining until respawn
	private int mRespawnTime;             // How many ticks between respawns
	private String mPostRespawnCommand;   // Command run via the console after respawning structure

	@Override
	public int compareTo(RespawningStructure other) {
		return mConfigLabel.compareTo(other.mConfigLabel);
	}

	public static RespawningStructure fromConfig(Plugin plugin, World world, String configLabel,
	        ConfigurationSection config) throws Exception {
		if (!config.isString("name")) {
			throw new Exception("Invalid name");
		} else if (!config.isString("path")) {
			throw new Exception("Invalid path");
		} else if (!config.isInt("x")) {
			throw new Exception("Invalid x value");
		} else if (!config.isInt("y")) {
			throw new Exception("Invalid y value");
		} else if (!config.isInt("z")) {
			throw new Exception("Invalid z value");
		} else if (!config.isInt("respawn_period")) {
			throw new Exception("Invalid respawn_period value");
		} else if (!config.isInt("ticks_until_respawn")) {
			throw new Exception("Invalid ticks_until_respawn value");
		} else if (!config.isInt("extra_detection_radius")) {
			throw new Exception("Invalid extra_detection_radius value");
		}

		String postRespawnCommand = null;
		if (config.isString("post_respawn_command")) {
			postRespawnCommand = config.getString("post_respawn_command");
		}

		// TODO: Command to run after respawning
		// TODO: Alternate regular variant
		// TODO: Alternate selectable variant

		return new RespawningStructure(plugin, world, config.getInt("extra_detection_radius"), configLabel,
		                               config.getString("name"), config.getString("path"),
		                               new Vector(config.getInt("x"), config.getInt("y"), config.getInt("z")),
		                               config.getInt("respawn_period"), config.getInt("ticks_until_respawn"),
									   postRespawnCommand);
	}

	public RespawningStructure(Plugin plugin, World world, int extraRadius,
	                           String configLabel, String name, String path,
	                           Vector loadPos, int respawnTime, int ticksLeft,
							   String postRespawnCommand) throws Exception {
		mPlugin = plugin;
		mWorld = world;
		mConfigLabel = configLabel;
		mName = name;
		mPath = path;
		mLoadPos = loadPos;
		mExtraRadius = extraRadius;
		mRespawnTime = respawnTime;
		mTicksLeft = ticksLeft;
		mPostRespawnCommand = postRespawnCommand;

		if (mRespawnTime < 200) {
			throw new Exception("Minimum respawn_period value is 200 ticks");
		}

		// Load the structure
		mClipboard = mPlugin.mStructureManager.loadSchematic("structures", path).getClipboard();

		// Determine structure size
		Region clipboardRegion = mClipboard.getRegion().clone();
		com.sk89q.worldedit.Vector structureSize =
		    clipboardRegion.getMaximumPoint().subtract(clipboardRegion.getMinimumPoint());

		// Create a bounding box for the structure itself, plus a slightly larger box to notify nearby players
		mInnerBounds = new StructureBounds(mLoadPos, mLoadPos.clone().add(new Vector(structureSize.getX(),
		                                                                             structureSize.getY(),
		                                                                             structureSize.getZ())));
		Vector extraRadiusVec = new Vector(extraRadius, extraRadius, extraRadius);
		mOuterBounds = new StructureBounds(mInnerBounds.mLowerCorner.clone().subtract(extraRadiusVec),
		                                   mInnerBounds.mUpperCorner.clone().add(extraRadiusVec));
	}

	public String getInfoString() {
		return "name='" + mName + "' pos=(" + Integer.toString((int)mLoadPos.getX()) + " " +
		       Integer.toString((int)mLoadPos.getY()) + " " + Integer.toString((int)mLoadPos.getZ()) +
			   ") path=" + mPath + " period=" + Integer.toString(mRespawnTime) + " ticksleft=" +
			   Integer.toString(mTicksLeft) +
			   (mPostRespawnCommand == null ? "" : " respawnCmd='" + mPostRespawnCommand + "'");
	}

	public void respawn() {
		StructureUtils.paste(mClipboard, mWorld,
		                     new com.sk89q.worldedit.Vector(mLoadPos.getX(), mLoadPos.getY(), mLoadPos.getZ()));
		if (mPostRespawnCommand != null) {
			Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), mPostRespawnCommand);
		}
		mTicksLeft = mRespawnTime;
	}

	public void tellRespawnTime(Player player) {
		int minutes = mTicksLeft / (60 * 20);
		int seconds = (mTicksLeft / 20) % 60;
		String message = mName + " is respawning in ";
		String color = ChatColor.GREEN + "" + ChatColor.BOLD;

		if (mTicksLeft <= 2400) {
			color = ChatColor.RED + "" + ChatColor.BOLD;
		}

		if (minutes > 1) {
			message += Integer.toString(minutes) + " minutes";
		} else if (minutes == 1) {
			message += "1 minute";
		}

		if (minutes > 0 && seconds > 0) {
			message += " and ";
		}

		if (seconds > 1) {
			message += Integer.toString(seconds) + " seconds";
		} else if (seconds == 1) {
			message += "1 second";
		}
		player.sendMessage(color + message);
	}

	public void tellRespawnTimeIfNearby(Player player) {
		if (mOuterBounds.within(player.getLocation().toVector())) {
			tellRespawnTime(player);
		}
	}

	public void setRespawnTimer(int ticksUntilRespawn) {
		if (ticksUntilRespawn < 0) {
			respawn();
		} else {
			mTicksLeft = ticksUntilRespawn;
		}
	}

	public void setPostRespawnCommand(String postRespawnCommand) {
		mPostRespawnCommand = postRespawnCommand;
	}

	public Map<String, Object> getConfig() {
		Map<String, Object> configMap = new LinkedHashMap<String, Object>();

		configMap.put("name", mName);
		configMap.put("path", mPath);
		configMap.put("x", (int)mLoadPos.getX());
		configMap.put("y", (int)mLoadPos.getY());
		configMap.put("z", (int)mLoadPos.getZ());
		configMap.put("extra_detection_radius", mExtraRadius);
		configMap.put("respawn_period", mRespawnTime);
		configMap.put("ticks_until_respawn", mTicksLeft);
		if (mPostRespawnCommand != null) {
			configMap.put("post_respawn_command", mPostRespawnCommand);
		}

		return configMap;
	}

	public void tick(int ticks) {
		if (((mTicksLeft >= 2400) && ((mTicksLeft - ticks) < 2400)) ||
		    ((mTicksLeft >= 600) && ((mTicksLeft - ticks) < 600))) {
			for (Player player : Bukkit.getOnlinePlayers()) {
				if (mOuterBounds.within(player.getLocation().toVector())) {
					tellRespawnTime(player);
				}
			}
		}

		mTicksLeft -= ticks;

		if (mTicksLeft < 0) {
			for (Player player : Bukkit.getOnlinePlayers()) {
				if (mOuterBounds.within(player.getLocation().toVector())) {
					respawn();
					break;
				}
			}
		}
	}
}
