package com.tb24.fn;

import android.content.Context;
import android.util.Log;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.tb24.fn.util.Utils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Registry {
	public static final Gson GSON = new Gson();
	private Table<String, String, JsonElement> itemRegistry = HashBasedTable.create();
	private Map<String, List<String>> paths = new HashMap<>();

	public Registry(Context ctx) {
		registerPath("AthenaBackpack", "Game/Athena/Items/Cosmetics/Backpacks", "Game/Athena/Items/Cosmetics/PetCarriers");
		registerPath("AthenaCharacter", "Game/Athena/Items/Cosmetics/Characters");
		registerPath("AthenaDance", "Game/Athena/Items/Cosmetics/Dances", "Game/Athena/Items/Cosmetics/Dances/Emoji", "Game/Athena/Items/Cosmetics/Sprays", "Game/Athena/Items/Cosmetics/Toys");
		registerPath("AthenaGlider", "Game/Athena/Items/Cosmetics/Gliders");
		registerPath("AthenaItemWrap", "Game/Athena/Items/Cosmetics/ItemWraps");
		registerPath("AthenaLoadingScreen", "Game/Athena/Items/Cosmetics/LoadingScreens");
		registerPath("AthenaMusicPack", "Game/Athena/Items/Cosmetics/MusicPacks");
		registerPath("AthenaPickaxe", "Game/Athena/Items/Cosmetics/Pickaxes");
		registerPath("AthenaSkyDiveContrail", "Game/Athena/Items/Cosmetics/Contrails");
		registerPath("Token", "Game/Items/Tokens", "Game/Items/Tokens/Fake_LS_Tokens");
		registerPath("AthenaHero_", "Game/Athena/Heroes");
		registerPath("AthenaWeapon_", "Game/Athena/Items/Weapons");
		registerPath("DisplayAsset_", "Game/Catalog/DisplayAssets");

		for (Map.Entry<String, List<String>> entry : paths.entrySet()) {
			for (String path : entry.getValue()) {
				String dbg = null;
				try {
					for (String file : ctx.getAssets().list(path)) {
						int dotIndex = file.lastIndexOf('.');

						if (dotIndex < 0) {
							continue;
						}

						dbg = file;
						register(entry.getKey(), file.substring(0, dotIndex).toLowerCase(), GSON.fromJson(Utils.getStringFromAssets(ctx.getAssets(), path + '/' + file), JsonElement.class));
					}
				} catch (Throwable e) {
					Log.w("Registry", "Failed registering " + dbg, e);
				}
			}
		}
	}

	private void registerPath(String namespace, String... path) {
		paths.put(namespace, Arrays.asList(path));
	}

	private void register(String itemNamespace, String itemName, JsonElement json) {
		itemRegistry.put(itemNamespace, itemName, json);
	}

	public JsonElement get(String namespacedString) {
		String[] split = namespacedString.split(":");
		return itemRegistry.get(split[0], split[1].toLowerCase());
	}
}
