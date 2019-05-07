package com.tb24.fn.activity;

import android.app.ActionBar;
import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.text.TextUtils;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.tb24.fn.FortniteCompanionApp;
import com.tb24.fn.R;
import com.tb24.fn.model.FortItemStack;
import com.tb24.fn.util.EFortRarity;
import com.tb24.fn.util.JsonUtils;
import com.tb24.fn.util.Utils;

import org.jetbrains.annotations.NotNull;

import java.util.Map;

public abstract class BaseActivity extends Activity {
	private static Bitmap getBitmapImageFromItemStackDataInternal(BaseActivity activity, FortItemStack item, JsonObject jsonObject) {
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
			Log.w("ItemShopActivity", "Failed getting image for item " + item.templateId + '\n' + e.toString());
		}

		return null;
	}

	public static Bitmap getBitmapImageFromItemStackData(BaseActivity activity, FortItemStack item, JsonObject jsonObject) {
		Bitmap bitmap = activity.getThisApplication().bitmapCache.get(item.templateId);

		synchronized (activity.getThisApplication().bitmapCache) {
			if (bitmap == null) {
				bitmap = getBitmapImageFromItemStackDataInternal(activity, item, jsonObject);

				if (bitmap != null) {
					activity.getThisApplication().bitmapCache.put(item.templateId, bitmap);
				}
			}
		}

		return bitmap;
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

	public static String shortDescription(FortItemStack itemStack, JsonObject jsonObject) {
		String s;

		if (jsonObject.has("ShortDescription")) {
			s = jsonObject.get("ShortDescription").getAsString();
		} else {
			s = shortDescriptionFromCtg(itemStack.getIdCategory());
		}

		return s;
	}

	@NotNull
	public static String shortDescriptionFromCtg(String idCategory) {
		return idCategory.equals("AthenaCharacter") ? "Outfit" : idCategory.equals("AthenaPickaxe") ? "Harvesting Tool" : idCategory.equals("AthenaDance") ? "Emote" : idCategory.equals("AthenaItemWrap") ? "Wrap" : "";
	}

	public static void populateItemDetailBox(ViewGroup viewGroup, FortItemStack item, JsonElement json) {
		JsonObject jsonObject = json.getAsJsonArray().get(0).getAsJsonObject();
		EFortRarity rarity = EFortRarity.UNCOMMON;

		if (jsonObject.has("Rarity")) {
			rarity = EFortRarity.from(jsonObject.get("Rarity").getAsString());
		}

		View viewById = viewGroup.findViewById(R.id.to_set_background);
		viewById.setBackgroundResource(rarityBackground2(jsonObject));
		int twelve = (int) Utils.dp(viewGroup.getResources(), 12);
		int eight = (int) Utils.dp(viewGroup.getResources(), 8);
		viewById.setPadding(twelve, eight, twelve, eight);
		((TextView) viewGroup.findViewById(R.id.item_text1)).setText(rarity.name + " | " + BaseActivity.shortDescription(item, jsonObject));
		((TextView) viewGroup.findViewById(R.id.item_text2)).setText((item.attributes != null && item.attributes.has("DUMMY") ? "[Dummy] " : "") + JsonUtils.getStringOr("DisplayName", jsonObject, "??"));
		String description = JsonUtils.getStringOr("Description", jsonObject, "");
		CharSequence setText = "";

		if (jsonObject.has("GameplayTags")) {
			for (JsonElement s : jsonObject.get("GameplayTags").getAsJsonObject().get("gameplay_tags").getAsJsonArray()) {
				if (s.getAsString().startsWith("Cosmetics.Set.")) {
					String setId = s.getAsString().substring("Cosmetics.Set.".length());
					// TODO display set name instead of ID
					setText = TextUtils.concat('\n' + "Part of the ", Utils.span(setId, new StyleSpan(Typeface.BOLD)), " set.");
				}
			}
		}

		CharSequence concat = TextUtils.concat(description, setText);
		TextView textItemDescription = viewGroup.findViewById(R.id.item_text3);

		if (concat.length() == 0) {
			textItemDescription.setVisibility(View.GONE);
		} else {
			textItemDescription.setVisibility(View.VISIBLE);
			textItemDescription.setText(concat);
		}
	}

	public static int countAndSetVbucks(BaseActivity activity, ViewGroup vbxView) {
		if (!activity.getThisApplication().profileData.containsKey("common_core")) {
			vbxView.setVisibility(View.GONE);
			return 0;
		}

		int vBucksQty = 0;

		for (Map.Entry<String, FortItemStack> entry : activity.getThisApplication().profileData.get("common_core").items.entrySet()) {
			if (entry.getValue().templateId.equals("Currency:MtxGiveaway")) {
				vBucksQty += entry.getValue().quantity;
			}
		}

		vbxView.setVisibility(View.VISIBLE);
		((TextView) vbxView.getChildAt(1)).setText(String.format("%,d", vBucksQty));
		return vBucksQty;
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
