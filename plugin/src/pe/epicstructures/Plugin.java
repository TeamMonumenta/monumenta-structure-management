package pe.epicstructures;

import java.io.File;
import java.io.IOException;

import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.World;

import pe.epicstructures.commands.AddRespawningStructure;
import pe.epicstructures.commands.ListRespawningStructures;
import pe.epicstructures.commands.LoadStructure;
import pe.epicstructures.commands.RemoveRespawningStructure;
import pe.epicstructures.commands.RespawnStructure;
import pe.epicstructures.commands.SaveStructure;
import pe.epicstructures.managers.RespawnManager;
import pe.epicstructures.managers.StructureManager;

public class Plugin extends JavaPlugin {
	public World mWorld;
	public StructureManager mStructureManager;

	public RespawnManager mRespawnManager = null;

	private File mConfigFile;
	private YamlConfiguration mConfig;

	// Logic that is performed upon enabling the plugin.
	@Override
	public void onEnable() {
		mWorld = Bukkit.getWorlds().get(0);

		mStructureManager = new StructureManager(this, mWorld);

		getCommand("LoadStructure").setExecutor(new LoadStructure(this, mWorld));
		getCommand("SaveStructure").setExecutor(new SaveStructure(this, mWorld));
		getCommand("RespawnStructure").setExecutor(new RespawnStructure(this));
		getCommand("AddRespawningStructure").setExecutor(new AddRespawningStructure(this, mWorld));
		getCommand("RemoveRespawningStructure").setExecutor(new RemoveRespawningStructure(this));
		getCommand("ListRespawningStructures").setExecutor(new ListRespawningStructures(this));
		//TODO: Command to reload structures from config file
		//TODO: Compass listener for telling players how long is left

		reloadConfig();
	}

	// Logic that is performed upon disabling the plugin.
	@Override
	public void onDisable() {
		// Save current structure respawn times
		saveConfig();

		// Cancel structure respawning and clear list
		if (mRespawnManager != null) {
			mRespawnManager.cleanup();
			mRespawnManager = null;
		}

		// Cancel any remaining tasks
		getServer().getScheduler().cancelTasks(this);
	}

	public void reloadConfig() {
		// Do not save first
		if (mRespawnManager != null) {
			mRespawnManager.cleanup();
			mRespawnManager = null;
		}

		if (mConfigFile == null) {
			mConfigFile = new File(getDataFolder(), "config.yml");
		}

		// TODO: Check file exists, if not create default one

		mConfig = YamlConfiguration.loadConfiguration(mConfigFile);

		mRespawnManager = new RespawnManager(this, mWorld, mConfig);
	}

	public void saveConfig() {
		if (mRespawnManager != null) {
			try {
				mConfig = mRespawnManager.getConfig();
				mConfig.save(mConfigFile);
			} catch (IOException ex) {
				getLogger().log(Level.SEVERE, "Could not save config to " + mConfigFile, ex);
			}
		}
	}
}
