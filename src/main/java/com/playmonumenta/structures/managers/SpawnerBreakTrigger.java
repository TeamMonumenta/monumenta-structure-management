package com.playmonumenta.structures.managers;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.playmonumenta.scriptedquests.quests.QuestContext;
import com.playmonumenta.scriptedquests.quests.components.QuestComponent;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.annotation.Nullable;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

public class SpawnerBreakTrigger {
	// Respawning structure label
	String mStructureLabel;
	// Number of spawners remaining when the POI is fresh
	int mSpawnerCount;
	boolean mRespawnsStructure;

	@Nullable QuestComponent mQuestComponent;
	@Nullable String mQuestComponentStr;
	@Nullable JsonObject mQuestComponentObj;
	com.playmonumenta.scriptedquests.Plugin mScriptedQuestsPlugin;

	// TODO:
	// Want to be able to specify different triggers for special structures or alternate variants.
	// This probably means having a "default" spawner break object for each structure, but letting
	// sub-structures override that default. This would require a bit of restructuring...
	//
	// For now, all structures of the same label share this information
	public static SpawnerBreakTrigger fromConfig(String structureLabel, @Nullable ConfigurationSection config) throws Exception {
		if (config == null) {
			throw new NullPointerException("Got null config");
		}
		if (!config.isInt("spawner_count")) {
			throw new Exception("Invalid spawner_count");
		} else if (!config.isString("scripted_quests_component")) {
			throw new Exception("Invalid scripted_quests_component");
		}

		return new SpawnerBreakTrigger(structureLabel, config.getInt("spawner_count"),
		                               config.getString("scripted_quests_component"));

	}

	public SpawnerBreakTrigger(String structureLabel, int spawnerCount,
	                           @Nullable String questComponentStr) throws Exception {
		this(structureLabel, spawnerCount, true, questComponentStr);
	}

	public SpawnerBreakTrigger(String structureLabel, int spawnerCount, boolean respawnsStructure,
	                           @Nullable String questComponentStr) throws Exception {
		mScriptedQuestsPlugin = (com.playmonumenta.scriptedquests.Plugin)Bukkit.getPluginManager().getPlugin("ScriptedQuests");
		if (mScriptedQuestsPlugin == null) {
			throw new Exception("ScriptedQuests is not present!");
		}

		mStructureLabel = structureLabel;
		mSpawnerCount = spawnerCount;
		mRespawnsStructure = respawnsStructure;

		Gson gson = new Gson();
		if (questComponentStr == null) {
			mQuestComponentStr = null;
			mQuestComponentObj = null;
			mQuestComponent = null;
		} else {
			mQuestComponentObj = gson.fromJson(questComponentStr, JsonObject.class);
			if (mQuestComponentObj == null) {
				throw new Exception("Failed to parse scripted_quests_component as JSON object");
			}
			mQuestComponent = new QuestComponent("REPORT THIS BUG", "REPORT THIS BUG", EntityType.UNKNOWN, mQuestComponentObj);
		}
	}

	public SpawnerBreakTrigger(String structureLabel, JsonObject object) throws Exception {
		mScriptedQuestsPlugin = (com.playmonumenta.scriptedquests.Plugin)Bukkit.getPluginManager().getPlugin("ScriptedQuests");
		if (mScriptedQuestsPlugin == null) {
			throw new Exception("ScriptedQuests is not present!");
		}

		mStructureLabel = structureLabel;
		mSpawnerCount = object.get("spawner_count").getAsInt();

		JsonPrimitive respawnsStructurePrimitive = object.getAsJsonPrimitive("respawns_structure");
		if (respawnsStructurePrimitive == null || !respawnsStructurePrimitive.isBoolean()) {
			mRespawnsStructure = false;
		} else {
			mRespawnsStructure = respawnsStructurePrimitive.getAsBoolean();
		}

		mQuestComponentObj = object.getAsJsonObject("mQuestComponentObj");
		if (mQuestComponentObj == null) {
			mQuestComponentStr = null;
			mQuestComponent = null;
		} else {
			mQuestComponentStr = mQuestComponentObj.toString();
			mQuestComponent = new QuestComponent("REPORT THIS BUG", "REPORT THIS BUG", EntityType.UNKNOWN, mQuestComponentObj);
		}
	}

	public @Nullable RespawningStructure getStructure() {
		try {
			return RespawnManager.getInstance().getStructure(mStructureLabel);
		} catch (Exception e) {
			return null;
		}
	}



	// Called only when a spawner is broken in this structure
	public void spawnerBreakEvent(RespawningStructure structure) {
		if (structure.spawnersBroken() >= mSpawnerCount) {
			if (mQuestComponent != null) {
				for (Player player : structure.getWorld().getPlayers()) {
					if (structure.isWithin(player)) {
						mQuestComponent.doActionsIfPrereqsMet(new QuestContext(mScriptedQuestsPlugin, player, null));
					}
				}
			}
			if (mRespawnsStructure && !structure.isConquered()) {
				structure.conquerStructure();
			}
		}
	}

	public int getSpawnerCountRemaining() {
		RespawningStructure structure = getStructure();
		if (structure == null) {
			return mSpawnerCount;
		}
		return mSpawnerCount - structure.spawnersBroken();
	}

	public double getSpawnerRatio() {
		return (double) getSpawnerCountRemaining() / mSpawnerCount;
	}

	@Deprecated
	public void resetCount() {}

	public String getInfoString() {
		return "count=" + mSpawnerCount + " respawnsStructure=" + mRespawnsStructure +
		       " component='" + mQuestComponentStr + "'";
	}

	public Map<String, Object> getConfig() {
		Map<String, Object> configMap = new LinkedHashMap<>();

		configMap.put("spawner_count", mSpawnerCount);
		configMap.put("scripted_quests_component", mQuestComponentStr);

		return configMap;
	}

	public JsonObject toJson() {
		JsonObject result = new JsonObject();

		result.addProperty("spawner_count", mSpawnerCount);
		result.addProperty("respawns_structure", mRespawnsStructure);
		result.add("scripted_quests_component", mQuestComponentObj);

		return result;
	}
}
