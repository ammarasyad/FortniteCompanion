package com.tb24.fn.util;

public enum EFortRarity {
	COMMON("Common"), UNCOMMON("Uncommon"), RARE("Rare"), EPIC("Epic"), LEGENDARY("Legendary"), MYTHIC("Mythic");
//	HANDMADE("Common"), UNCOMMON("Uncommon"), STURDY("Rare"), QUALITY("Epic"), FINE("Legendary"), ELEGANT("Mythic"), MASTERWORK("Transcendent"), LEGENDARY("Impossible");

	public final String name;

	EFortRarity(String name) {
		this.name = name;
	}

	public static EFortRarity from(String rarity) {
		if (rarity == null) {
			return UNCOMMON;
		}

		String check = rarity.substring(rarity.indexOf("::") + "::".length()).toUpperCase();

		for (EFortRarity val : values()) {
			if (check.equals(val.toString())) {
				return val;
			}
		}

		return UNCOMMON;
	}
}
