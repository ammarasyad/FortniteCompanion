package com.tb24.fn.model.assetdata;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.HashMap;
import java.util.Map;

public class FortHomebaseBannerColorMap {
	public Map<String, ColorEntry> ColorMap = new HashMap<>();

	public static FortHomebaseBannerColorMap parse(JsonElement json, Gson gson) {
		// TODO dynamic types
		FortHomebaseBannerColorMap fortHomebaseBannerColorMap = new FortHomebaseBannerColorMap();

		for (JsonElement e : json.getAsJsonArray().get(0).getAsJsonObject().get("ColorMap").getAsJsonArray()) {
			JsonObject jsonObject = e.getAsJsonObject();
			fortHomebaseBannerColorMap.ColorMap.put(jsonObject.get("key").getAsString(), gson.fromJson(jsonObject.get("value"), ColorEntry.class));
		}

		return fortHomebaseBannerColorMap;
	}

	public static class ColorEntry {
		public FColor PrimaryColor;
		public FColor SecondaryColor;
	}
}
