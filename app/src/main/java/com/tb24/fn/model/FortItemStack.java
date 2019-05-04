package com.tb24.fn.model;

import com.google.gson.JsonObject;

public class FortItemStack {
	public String templateId;
	public JsonObject attributes;
	public int quantity;

	public String getIdCategory() {
		return templateId.split(":")[0];
	}

	public String getIdName() {
		return templateId.split(":")[1];
	}
}
