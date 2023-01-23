package com.playmonumenta.structures;

import com.playmonumenta.structures.commands.ActivateSpecialStructure;
import com.playmonumenta.structures.commands.AddRespawningStructure;
import com.playmonumenta.structures.commands.ChangeLogLevel;
import com.playmonumenta.structures.commands.CompassRespawn;
import com.playmonumenta.structures.commands.ForceConquerRespawn;
import com.playmonumenta.structures.commands.ForceloadLazy;
import com.playmonumenta.structures.commands.ListRespawningStructures;
import com.playmonumenta.structures.commands.LoadStructure;
import com.playmonumenta.structures.commands.ReloadStructures;
import com.playmonumenta.structures.commands.RemoveRespawningStructure;
import com.playmonumenta.structures.commands.RespawnStructure;
import com.playmonumenta.structures.commands.SaveStructure;
import com.playmonumenta.structures.commands.SetPostRespawnCommand;
import com.playmonumenta.structures.commands.SetRespawnTimer;
import com.playmonumenta.structures.commands.SetSpawnerBreakTrigger;
import com.playmonumenta.structures.managers.EventListener;
import com.playmonumenta.structures.managers.RespawnManager;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nullable;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public class StructuresPlugin extends JavaPlugin {
	public @Nullable RespawnManager mRespawnManager = null;

	private static @Nullable StructuresPlugin INSTANCE = null;

	private @Nullable File mConfigFile = null;
	private @Nullable File mTimeLeftFile = null;
	private @Nullable YamlConfiguration mConfig = null;
	private @Nullable YamlConfiguration mTimeLeftConfig = null;
	private @Nullable CustomLogger mLogger = null;

	@Override
	public void onLoad() {
		if (mLogger == null) {
			mLogger = new CustomLogger(super.getLogger(), Level.INFO);
		}

		ChangeLogLevel.register();
		ActivateSpecialStructure.register(this);
		AddRespawningStructure.register(this);
		CompassRespawn.register();
		ForceConquerRespawn.register();
		ForceloadLazy.register();
		ListRespawningStructures.register();
		LoadStructure.register();
		ReloadStructures.register(this);
		RemoveRespawningStructure.register();
		RespawnStructure.register();
		SaveStructure.register();
		SetPostRespawnCommand.register();
		SetRespawnTimer.register();
		SetSpawnerBreakTrigger.register();
	}

	@Override
	public void onEnable() {
		INSTANCE = this;

		//TODO: Command to add an alternate generic structure

		PluginManager manager = getServer().getPluginManager();
		manager.registerEvents(new EventListener(), this);

		reloadConfig();
	}

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

		INSTANCE = null;
	}

	@Override
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
				if (!mConfigFile.getParentFile().mkdirs()) {
					throw new IOException();
				}

				// Create the file if it does not exist
				if (!mConfigFile.createNewFile()) {
					throw new IOException();
				}
			} catch (IOException ex) {
				getLogger().log(Level.SEVERE, "Failed to create non-existent configuration file");
			}

			// TODO: Put sample config file in here also
		}

		mConfig = YamlConfiguration.loadConfiguration(mConfigFile);

		if (mTimeLeftFile == null) {
			mTimeLeftFile = new File(getDataFolder(), "ticks_until_respawn.yml");
		}

		boolean respawnFileExisted = mTimeLeftFile.exists();
		if (respawnFileExisted) {
			mTimeLeftConfig = YamlConfiguration.loadConfiguration(mTimeLeftFile);
		} else {
			mTimeLeftConfig = new YamlConfiguration();
		}

		mRespawnManager = new RespawnManager(this, mConfig, mTimeLeftConfig);

		// Delete respawn times from main config if present
		if (!respawnFileExisted) {
			try {
				mConfig = mRespawnManager.getConfig();
				if (mConfigFile == null) {
					throw new RuntimeException("No, IntelliJ, mConfigFile CANNOT be null at this point.");
				}
				mConfig.save(mConfigFile);
			} catch (Exception ex) {
				getLogger().log(Level.SEVERE, "Could not save config to " + mConfigFile, ex);
			}
		}
	}

	@Override
	public void saveConfig() {
		if (mRespawnManager != null) {
			try {
				mConfig = mRespawnManager.getConfig();
				if (mConfigFile == null) {
					throw new RuntimeException("mConfigFile is not set");
				}
				mConfig.save(mConfigFile);
			} catch (Exception ex) {
				getLogger().log(Level.SEVERE, "Could not save config to " + mConfigFile, ex);
			}

			try {
				mTimeLeftConfig = mRespawnManager.getTimeLeftConfig();
				if (mTimeLeftFile == null) {
					throw new RuntimeException("mTimeLeftFile is not set");
				}
				mTimeLeftConfig.save(mTimeLeftFile);
			} catch (Exception ex) {
				getLogger().log(Level.SEVERE, "Could not save time left to " + mTimeLeftFile, ex);
			}
		}
	}

	@Override
	public Logger getLogger() {
		if (mLogger == null) {
			mLogger = new CustomLogger(super.getLogger(), Level.INFO);
		}
		return mLogger;
	}

	public static StructuresPlugin getInstance() {
		if (INSTANCE == null) {
			throw new RuntimeException("Attempted to get StructurePlugin before it finished loading");
		}
		return INSTANCE;
	}

	public static RespawnManager getRespawnManager() {
		StructuresPlugin plugin = getInstance();
		if (plugin.mRespawnManager == null) {
			throw new RuntimeException("Attempted to get StructurePlugin before it finished loading");
		}
		return plugin.mRespawnManager;
	}
}
