package com.tb24.fn.model.assetdata;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.tb24.fn.util.JsonUtils;
import com.tb24.fn.util.Utils;

import java.lang.reflect.Type;

public class FortItemDefinition {
	public String export_type;
	public String[] ItemVariants;
	public String Rarity;
	public String DisplayName;
	public String ShortDescription;
	public String Description;
	public GameplayTags GameplayTags;
	public AssetReference SmallPreviewImage;
	public AssetReference LargePreviewImage;

	public static class Serializer implements JsonDeserializer<FortItemDefinition> {
		@Override
		public FortItemDefinition deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
			Type type = FortItemDefinition.class;

			switch (JsonUtils.getStringOr("export_type", json.getAsJsonObject(), "")) {
				case "AthenaCharacterItemDefinition":
					type = AthenaCharacterItemDefinition.class;
					break;
				case "AthenaDailyQuestDefinition":
					type = FortQuestItemDefinition.class;
					break;
				case "AthenaPetCarrierItemDefinition":
					type = AthenaPetCarrierItemDefinition.class;
					break;
				case "FortChallengeBundleItemDefinition":
					type = FortChallengeBundleItemDefinition.class;
					break;
				case "FortQuestItemDefinition":
					type = FortQuestItemDefinition.class;
					break;
			}

			return Utils.GSON.fromJson(json, type);
		}
	}
}
