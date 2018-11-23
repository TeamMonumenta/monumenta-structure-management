package com.playmonumenta.epicstructures;

import com.playmonumenta.epicstructures.commands.ActivateSpecialStructure;
import com.playmonumenta.epicstructures.commands.AddRespawningStructure;
import com.playmonumenta.epicstructures.commands.ListRespawningStructures;
import com.playmonumenta.epicstructures.commands.LoadStructure;
import com.playmonumenta.epicstructures.commands.ReloadStructures;
import com.playmonumenta.epicstructures.commands.RemoveRespawningStructure;
import com.playmonumenta.epicstructures.commands.RespawnStructure;
import com.playmonumenta.epicstructures.commands.SaveStructure;
import com.playmonumenta.epicstructures.commands.SetPostRespawnCommand;
import com.playmonumenta.epicstructures.commands.SetSpawnerBreakTrigger;
import com.playmonumenta.epicstructures.managers.EventListener;
import com.playmonumenta.epicstructures.managers.RespawnManager;
import com.playmonumenta.epicstructures.managers.StructureManager;

import java.io.File;
import java.io.IOException;

import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.World;

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

		LoadStructure.register(this, mWorld);
		ActivateSpecialStructure.register(this);

		RespawnStructure.register(this);
		ListRespawningStructures.register(this);
		SetPostRespawnCommand.register(this);
		SetSpawnerBreakTrigger.register(this);
		RemoveRespawningStructure.register(this);

		getCommand("SaveStructure").setExecutor(new SaveStructure(this, mWorld));
		getCommand("AddRespawningStructure").setExecutor(new AddRespawningStructure(this, mWorld));
		getCommand("ReloadStructures").setExecutor(new ReloadStructures(this));
		//TODO: Command to add an alternate generic structure
		//TODO: Command to set all timers to max ?

		PluginManager manager = getServer().getPluginManager();
		manager.registerEvents(new EventListener(this), this);

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

		if (!mConfigFile.exists()) {
			try {
				// Create parent directories if they do not exist
				mConfigFile.getParentFile().mkdirs();

				// Create the file if it does not exist
				mConfigFile.createNewFile();
			} catch (IOException ex) {
				getLogger().log(Level.SEVERE, "Failed to create non-existent configuration file");
			}

			// TODO: Put sample config file in here also
		}

		mConfig = YamlConfiguration.loadConfiguration(mConfigFile);

		mRespawnManager = new RespawnManager(this, mWorld, mConfig);
	}

	public void saveConfig() {
		if (mRespawnManager != null) {
			try {
				mConfig = mRespawnManager.getConfig();
				mConfig.save(mConfigFile);
			} catch (Exception ex) {
				getLogger().log(Level.SEVERE, "Could not save config to " + mConfigFile, ex);
			}
		}
	}

	public void asyncLog(Level level, String message) {
		new BukkitRunnable() {
			@Override
			public void run()
			{
				getLogger().log(level, message);
			}
		}.runTask(this);
	}

	public void asyncLog(Level level, String message, Exception ex) {
		new BukkitRunnable() {
			@Override
			public void run()
			{
				getLogger().log(level, message, ex);
			}
		}.runTask(this);
	}
}
