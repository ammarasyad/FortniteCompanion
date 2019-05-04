package com.tb24.fn.activity;

import android.app.ActionBar;
import android.app.Activity;
import android.graphics.Bitmap;
import android.util.Log;
import android.view.MenuItem;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.tb24.fn.EFortRarity;
import com.tb24.fn.FortniteCompanionApp;
import com.tb24.fn.R;
import com.tb24.fn.model.FortItemStack;
import com.tb24.fn.util.Utils;

public abstract class BaseActivity extends Activity {
	public static Bitmap getBitmapImageFromItemStackData(BaseActivity activity, FortItemStack input, JsonObject jsonObject) {
		try {
			if (jsonObject.has("SmallPreviewImage")) {
				String path = jsonObject.get("SmallPreviewImage").getAsJsonObject().get("asset_path_name").getAsString();
				return Utils.bitmapFromTga(activity, path);
			} else if (jsonObject.has("HeroDefinition")) {
				JsonElement json1 = activity.getThisApplication().itemRegistry.get("AthenaHero_:" + jsonObject.get("HeroDefinition").getAsString());

				if (json1 != null) {
					JsonObject jsonObject1 = json1.getAsJsonArray().get(0).getAsJsonObject();

					if (jsonObject1.has("SmallPreviewImage")) {
						String path = jsonObject1.get("SmallPreviewImage").getAsJsonObject().get("asset_path_name").getAsString();
						return Utils.bitmapFromTga(activity, path);
					}
				}
			} else if (jsonObject.has("WeaponDefinition")) {
				JsonElement json1 = activity.getThisApplication().itemRegistry.get("AthenaWeapon_:" + jsonObject.get("WeaponDefinition").getAsString());

				if (json1 != null) {
					JsonObject jsonObject1 = json1.getAsJsonArray().get(0).getAsJsonObject();

					if (jsonObject1.has("SmallPreviewImage")) {
						String path = jsonObject1.get("SmallPreviewImage").getAsJsonObject().get("asset_path_name").getAsString();
						return Utils.bitmapFromTga(activity, path);
					}
				}
			}
		} catch (NullPointerException e) {
			Log.w("ItemShopActivity", "Failed getting image for item " + input.templateId + '\n' + e.toString());
		}

		return null;
	}

	public static int rarityBackground(JsonObject jsonObject) {
		if (jsonObject.has("Rarity")) {
			EFortRarity rarity = EFortRarity.from(jsonObject.get("Rarity").getAsString());
			return rarity == EFortRarity.HANDMADE ? R.drawable.bg_common : rarity == EFortRarity.UNCOMMON ? R.drawable.bg_uncommon : rarity == EFortRarity.STURDY ? R.drawable.bg_rare : rarity == EFortRarity.QUALITY ? R.drawable.bg_epic : rarity == EFortRarity.FINE ? R.drawable.bg_legendary : R.drawable.bg_common;
		} else {
			return R.drawable.bg_uncommon;
		}
	}

	public static int rarityBackground2(JsonObject jsonObject) {
		if (jsonObject.has("Rarity")) {
			EFortRarity rarity = EFortRarity.from(jsonObject.get("Rarity").getAsString());
			return rarity == EFortRarity.HANDMADE ? R.drawable.bg_common2 : rarity == EFortRarity.UNCOMMON ? R.drawable.bg_uncommon2 : rarity == EFortRarity.STURDY ? R.drawable.bg_rare2 : rarity == EFortRarity.QUALITY ? R.drawable.bg_epic2 : rarity == EFortRarity.FINE ? R.drawable.bg_legendary2 : R.drawable.bg_common2;
		} else {
			return R.drawable.bg_uncommon2;
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == android.R.id.home) finish();
		return super.onOptionsItemSelected(item);
	}

	protected FortniteCompanionApp getThisApplication() {
		return (FortniteCompanionApp) getApplication();
	}

	protected void setupActionBar() {
		ActionBar actionBar = getActionBar();

		if (actionBar != null) {
			actionBar.setDisplayHomeAsUpEnabled(true);
		}
	}
}
