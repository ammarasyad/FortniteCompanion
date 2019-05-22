package com.tb24.fn;

import android.content.Context;
import android.content.res.AssetManager;
import android.util.Log;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.tb24.fn.util.Utils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class Registry {
	public static final Gson GSON = new Gson();
	private final Table<String, String, JsonElement> itemRegistry = HashBasedTable.create();
	private final Map<String, List<String>> paths = new HashMap<>();
	private final AssetManager assets;

	public Registry(Context context) {
		assets = context.getAssets();
		registerPath("AthenaBackpack", "Game/Athena/Items/Cosmetics/Backpacks", "Game/Athena/Items/Cosmetics/PetCarriers");
		registerPath("AthenaCharacter", "Game/Athena/Items/Cosmetics/Characters");
		registerPath("AthenaDance", "Game/Athena/Items/Cosmetics/Dances", "Game/Athena/Items/Cosmetics/Dances/Emoji", "Game/Athena/Items/Cosmetics/Sprays", "Game/Athena/Items/Cosmetics/Toys");
		registerPath("AthenaGlider", "Game/Athena/Items/Cosmetics/Gliders");
		registerPath("AthenaItemWrap", "Game/Athena/Items/Cosmetics/ItemWraps");
		registerPath("AthenaLoadingScreen", "Game/Athena/Items/Cosmetics/LoadingScreens");
		registerPath("AthenaMusicPack", "Game/Athena/Items/Cosmetics/MusicPacks");
		registerPath("AthenaPickaxe", "Game/Athena/Items/Cosmetics/Pickaxes");
		registerPath("AthenaSkyDiveContrail", "Game/Athena/Items/Cosmetics/Contrails");
		registerPath("ChallengeBundle", "Game/Athena/Items/ChallengeBundles/*");
		registerPath("ChallengeBundleSchedule", "Game/Athena/Items/ChallengeBundleSchedules/*");
		registerPath("CosmeticVariantToken", "Game/Athena/Items/CosmeticVariantTokens");
		registerPath("Quest", "Game/Athena/Items/Quests/*");
		registerPath("AccountResource", "Game/Items/PersistentResources");
		registerPath("Token", "Game/Items/Tokens/*");
		registerPath("AthenaHero_", "Game/Athena/Heroes");
		registerPath("AthenaWeapon_", "Game/Athena/Items/Weapons");
//		registerPath("DisplayAsset_", "Game/Catalog/DisplayAssets");

		for (Map.Entry<String, List<String>> entry : paths.entrySet()) {
			for (String path : entry.getValue()) {
				String fileName = null;

				try {
					for (String file : Objects.requireNonNull(assets.list(path))) {
						int dotIndex = file.lastIndexOf('.');

						if (dotIndex < 0 || !file.substring(dotIndex).equals(".json")) {
							continue;
						}

						fileName = file;
						itemRegistry.put(entry.getKey(), file.substring(0, dotIndex).toLowerCase(), GSON.fromJson(Utils.getStringFromAssets(assets, path + '/' + file), JsonElement.class));
					}
				} catch (Throwable e) {
					Log.w("Registry", "Failed registering " + fileName, e);
				}
			}
		}
	}

	private void registerPath(String namespace, String... path) {
		List<String> value = null;

		if (path[0].endsWith("/*")) {
			try {
				value = new ArrayList<>();
				String thisPath = path[0].substring(0, path[0].indexOf("/*"));
				value.add(thisPath);
				addAllFolders(thisPath, value);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		if (value == null) {
			value = Arrays.asList(path);
		}

		paths.put(namespace, value);
	}

	private void addAllFolders(String thisPath, List<String> value) throws IOException {
		for (String s : Objects.requireNonNull(assets.list(thisPath))) {
			if (!s.contains(".")) {
				String sub = thisPath + '/' + s;
				value.add(sub);
				addAllFolders(sub, value);
			}
		}
	}

	public JsonElement get(String namespacedString) {
		String[] split = namespacedString.split(":");
		return itemRegistry.get(split[0], split[1].toLowerCase());
	}

	public Collection<JsonElement> getAll() {
		return itemRegistry.values();
	}
}
