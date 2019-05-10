package com.tb24.fn.util;

import com.google.gson.JsonObject;

public enum EFortRarity {
	COMMON("Common"), UNCOMMON("Uncommon"), RARE("Rare"), EPIC("Epic"), LEGENDARY("Legendary"), MYTHIC("Mythic");
//	8.51 >> HANDMADE("Common"), UNCOMMON("Uncommon"), STURDY("Rare"), QUALITY("Epic"), FINE("Legendary"), ELEGANT("Mythic"), MASTERWORK("T"), LEGENDARY("I");

	public final String name;

	EFortRarity(String name) {
		this.name = name;
	}

	public static EFortRarity from(String rarity) {
		String check = rarity.substring(rarity.indexOf("::") + "::".length());

		for (EFortRarity val : values()) {
			if (check.toUpperCase().equals(val.toString())) {
				return val;
			}
		}

		return UNCOMMON;
	}

	public static EFortRarity fromObject(JsonObject jsonObject) {
		EFortRarity rarity = EFortRarity.UNCOMMON;

		if (jsonObject.has("Rarity")) {
			rarity = EFortRarity.from(jsonObject.get("Rarity").getAsString());
		}

		return rarity;
	}
}
