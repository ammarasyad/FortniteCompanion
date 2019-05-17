package com.tb24.fn.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.text.TextUtils;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.tb24.fn.FortniteCompanionApp;
import com.tb24.fn.R;
import com.tb24.fn.activity.BaseActivity;
import com.tb24.fn.model.FortItemStack;
import com.tb24.fn.model.RarityData;

public class ItemUtils {
	public static final int OVERLAY_COLOR = 0xFFC0C0C0;
	public static JsonObject setData;
	public static JsonObject userFacingTagsData;

	public ItemUtils() {
	}

	public static Bitmap getBitmapImageFromItemStackData(BaseActivity activity, FortItemStack item, JsonObject jsonObject) {
		try {
			if (jsonObject.has("SmallPreviewImage")) {
				String path = jsonObject.get("SmallPreviewImage").getAsJsonObject().get("asset_path_name").getAsString();
				return Utils.bitmapFromTga(activity, path);
			} else if (jsonObject.has("HeroDefinition")) {
				JsonElement json1 = activity.getThisApplication().itemRegistry.get("AthenaHero_:" + jsonObject.get("HeroDefinition").getAsString());

				if (json1 != null) {
					return getBitmapImageFromItemStackData(activity, item, json1.getAsJsonArray().get(0).getAsJsonObject());
				}
			} else if (jsonObject.has("WeaponDefinition")) {
				JsonElement json1 = activity.getThisApplication().itemRegistry.get("AthenaWeapon_:" + jsonObject.get("WeaponDefinition").getAsString());

				if (json1 != null) {
					return getBitmapImageFromItemStackData(activity, item, json1.getAsJsonArray().get(0).getAsJsonObject());
				}
			}
		} catch (Throwable e) {
			Log.w("ItemUtils", "Failed getting image for item " + item.templateId + '\n' + e.toString());
		}

		return null;
	}

	public static Drawable rarityBgSlot(Context ctx, EFortRarity rarity) {
		if (rarity != EFortRarity.MYTHIC) {
			return ctx.getDrawable(rarity == EFortRarity.COMMON ? R.drawable.bg_common : rarity == EFortRarity.UNCOMMON ? R.drawable.bg_uncommon : rarity == EFortRarity.RARE ? R.drawable.bg_rare : rarity == EFortRarity.EPIC ? R.drawable.bg_epic : rarity == EFortRarity.LEGENDARY ? R.drawable.bg_legendary : R.drawable.bg_common);
		}

//		TODO with the rarity data the colors suck
		RarityData rarityData = FortniteCompanionApp.rarityData[rarity.ordinal()];
		GradientDrawable content = new GradientDrawable() {
			@Override
			public void draw(Canvas canvas) {
				if (getGradientRadius() != getBounds().width()) {
					setGradientRadius(getBounds().width());
				}

				super.draw(canvas);
			}
		};
		content.setGradientType(GradientDrawable.RADIAL_GRADIENT);
		content.setGradientRadius(Utils.dp(ctx.getResources(), 200));
		content.setColors(new int[]{rarityData.Color2.asInt(), rarityData.Color3.asInt()});
		GradientDrawable border = new GradientDrawable();
		border.setOrientation(GradientDrawable.Orientation.BL_TR);
		border.setColors(new int[]{rarityData.Color3.asInt(), rarityData.Color1.asInt()});
		border.setColorFilter(OVERLAY_COLOR, PorterDuff.Mode.OVERLAY);
		LayerDrawable layerDrawable = new LayerDrawable(new Drawable[]{border, content});
		int p = (int) ctx.getResources().getDimension(R.dimen.rarity_bg_square_padding);
		layerDrawable.setLayerInset(1, p, p, p, p);
		layerDrawable.setPadding(p, p, p, p);
		return layerDrawable;
	}

	public static Drawable rarityBgTitle(Context ctx, EFortRarity rarity) {
		if (rarity != EFortRarity.MYTHIC) {
			return ctx.getDrawable(rarity == EFortRarity.COMMON ? R.drawable.bg_common2 : rarity == EFortRarity.UNCOMMON ? R.drawable.bg_uncommon2 : rarity == EFortRarity.RARE ? R.drawable.bg_rare2 : rarity == EFortRarity.EPIC ? R.drawable.bg_epic2 : rarity == EFortRarity.LEGENDARY ? R.drawable.bg_legendary2 : R.drawable.bg_common2);
		}

		RarityData rarityData = FortniteCompanionApp.rarityData[rarity.ordinal()];
		int[] colors = {rarityData.Color3.asInt(), rarityData.Color1.asInt()};
		GradientDrawable content = new GradientDrawable();
		content.setOrientation(GradientDrawable.Orientation.LEFT_RIGHT);
		content.setColors(colors);
		GradientDrawable border = new GradientDrawable();
		border.setOrientation(GradientDrawable.Orientation.LEFT_RIGHT);
		border.setColors(colors);
		border.setColorFilter(OVERLAY_COLOR, PorterDuff.Mode.OVERLAY);
		LayerDrawable layerDrawable = new LayerDrawable(new Drawable[]{content, border});
		layerDrawable.setLayerHeight(1, (int) ctx.getResources().getDimension(R.dimen.rarity_bg_square_padding));
		layerDrawable.setLayerGravity(1, Gravity.BOTTOM);
		return layerDrawable;
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
				return idCategory;
		}
	}

	public static void populateItemDetailBox(ViewGroup viewGroup, FortItemStack item, JsonElement json) {
		JsonObject jsonObject = json.getAsJsonArray().get(0).getAsJsonObject();
		EFortRarity rarity = getRarity(jsonObject);
		View viewById = viewGroup.findViewById(R.id.to_set_background);
		viewById.setBackground(rarityBgTitle(viewGroup.getContext(), rarity));
		int twelve = (int) Utils.dp(viewGroup.getResources(), 12);
		int eight = (int) Utils.dp(viewGroup.getResources(), 8);
		viewById.setPadding(twelve, eight, twelve, eight);
		((TextView) viewGroup.findViewById(R.id.item_text1)).setText(TextUtils.concat(Utils.color(rarity.name, FortniteCompanionApp.rarityData[rarity.ordinal()].Color1.asInt()), " | ", shortDescription(item, jsonObject)));
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

	public static EFortRarity getRarity(JsonObject jsonObject) {
		EFortRarity rarity = EFortRarity.UNCOMMON;

		if (jsonObject.has("Rarity")) {
			rarity = EFortRarity.from(jsonObject.get("Rarity").getAsString());
		}

		return rarity;
	}
}
