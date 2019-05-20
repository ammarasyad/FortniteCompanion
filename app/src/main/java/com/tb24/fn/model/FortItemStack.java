package com.tb24.fn.model;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.tb24.fn.Registry;
import com.tb24.fn.model.assetdata.FortItemDefinition;

public class FortItemStack {
	private static Gson gson = new GsonBuilder().registerTypeAdapter(FortItemDefinition.class, new FortItemDefinition.Serializer()).create();
	public String templateId;
	public JsonObject attributes;
	public Integer quantity;
	private FortItemDefinition defData;

	public FortItemStack() {
	}

	public FortItemStack(String idCategory, String idName, int quantity) {
		templateId = idCategory + ':' + idName;
		this.quantity = quantity;
	}

	public FortItemStack(String templateId, int quantity) {
		this.templateId = templateId;
		this.quantity = quantity;
	}

	public String getIdCategory() {
		return templateId.split(":")[0];
	}

	public String getIdName() {
		return templateId.split(":")[1];
	}

	public FortItemDefinition setAndGetDefData(Registry registry) {
		if (defData == null) {
			JsonElement jsonElement = registry.get(templateId);

			if (jsonElement == null) {
				return null;
			}

			defData = gson.fromJson(jsonElement.getAsJsonArray().get(0), FortItemDefinition.class);
		}

		return defData;
	}

	@Override
	public String toString() {
		return quantity + " x " + templateId;
	}
}
