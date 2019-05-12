package com.tb24.fn.util;

import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.text.TextUtils;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.tb24.fn.R;
import com.tb24.fn.activity.BaseActivity;
import com.tb24.fn.model.FortItemStack;

public class ItemUtils {
	public static JsonObject setData;
	public static JsonObject userFacingTagsData;

	public ItemUtils() {
	}

	private static Bitmap getBitmapImageFromItemStackDataInternal(BaseActivity activity, FortItemStack item, JsonObject jsonObject) {
		try {
			if (jsonObject.has("SmallPreviewImage")) {
				String path = jsonObject.get("SmallPreviewImage").getAsJsonObject().get("asset_path_name").getAsString();
				return Utils.bitmapFromTga(activity, path);
			} else if (jsonObject.has("HeroDefinition")) {
				JsonElement json1 = activity.getThisApplication().itemRegistry.get("AthenaHero_:" + jsonObject.get("HeroDefinition").getAsString());

				if (json1 != null) {
					return getBitmapImageFromItemStackDataInternal(activity, item, json1.getAsJsonArray().get(0).getAsJsonObject());
				}
			} else if (jsonObject.has("WeaponDefinition")) {
				JsonElement json1 = activity.getThisApplication().itemRegistry.get("AthenaWeapon_:" + jsonObject.get("WeaponDefinition").getAsString());

				if (json1 != null) {
					return getBitmapImageFromItemStackDataInternal(activity, item, json1.getAsJsonArray().get(0).getAsJsonObject());
				}
			}
		} catch (Throwable e) {
			Log.w("ItemUtils", "Failed getting image for item " + item.templateId + '\n' + e.toString());
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
		EFortRarity rarity = EFortRarity.fromObject(jsonObject);
		return rarity == EFortRarity.COMMON ? R.drawable.bg_common : rarity == EFortRarity.UNCOMMON ? R.drawable.bg_uncommon : rarity == EFortRarity.RARE ? R.drawable.bg_rare : rarity == EFortRarity.EPIC ? R.drawable.bg_epic : rarity == EFortRarity.LEGENDARY ? R.drawable.bg_legendary : R.drawable.bg_common;
	}

	public static int rarityBackground2(JsonObject jsonObject) {
		EFortRarity rarity = EFortRarity.fromObject(jsonObject);
		return rarity == EFortRarity.COMMON ? R.drawable.bg_common2 : rarity == EFortRarity.UNCOMMON ? R.drawable.bg_uncommon2 : rarity == EFortRarity.RARE ? R.drawable.bg_rare2 : rarity == EFortRarity.EPIC ? R.drawable.bg_epic2 : rarity == EFortRarity.LEGENDARY ? R.drawable.bg_legendary2 : R.drawable.bg_common2;
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

	public static String shortDescriptionFromCtg(String idCategory) {
		switch (idCategory) {
			case "AthenaCharacter":
				return "Outfit";
			case "AthenaDance":
				return "Emote";
			case "AthenaGlider":
				return "Glider";
			case "AthenaItemWrap":
				return "Wrap";
			case "AthenaPickaxe":
				return "Harvesting Tool";
			default:
				return "Unknown";
		}
	}

	public static void populateItemDetailBox(ViewGroup viewGroup, FortItemStack item, JsonElement json) {
		JsonObject jsonObject = json.getAsJsonArray().get(0).getAsJsonObject();
		EFortRarity rarity = EFortRarity.fromObject(jsonObject);
		View viewById = viewGroup.findViewById(R.id.to_set_background);
		viewById.setBackgroundResource(rarityBackground2(jsonObject));
		int twelve = (int) Utils.dp(viewGroup.getResources(), 12);
		int eight = (int) Utils.dp(viewGroup.getResources(), 8);
		viewById.setPadding(twelve, eight, twelve, eight);
		((TextView) viewGroup.findViewById(R.id.item_text1)).setText(rarity.name + " | " + shortDescription(item, jsonObject));
		((TextView) viewGroup.findViewById(R.id.item_text2)).setText((item.attributes != null && item.attributes.has("DUMMY") ? "[Dummy] " : "") + JsonUtils.getStringOr("DisplayName", jsonObject, "??"));
		CharSequence concat = JsonUtils.getStringOr("Description", jsonObject, "");

		if (jsonObject.has("GameplayTags")) {
			for (JsonElement s : jsonObject.get("GameplayTags").getAsJsonObject().get("gameplay_tags").getAsJsonArray()) {
				if (s.getAsString().startsWith("Cosmetics.Set.")) {
					concat = TextUtils.concat(concat, '\n' + "Part of the ", Utils.span(setData.get(s.getAsString()).getAsJsonObject().get("DisplayName").getAsString(), new StyleSpan(Typeface.BOLD)), " set.");
				} else if (s.getAsString().startsWith("Cosmetics.UserFacingFlags.")) {
					concat = TextUtils.concat(concat, "\n[", Utils.span(userFacingTagsData.get(s.getAsString()).getAsJsonObject().get("DisplayName").getAsString(), new StyleSpan(Typeface.ITALIC)), "]");
				}
			}
		}

		TextView textItemDescription = viewGroup.findViewById(R.id.item_text3);

		if (concat.length() == 0) {
			textItemDescription.setVisibility(View.GONE);
		} else {
			textItemDescription.setVisibility(View.VISIBLE);
			textItemDescription.setText(concat);
		}
	}
}
