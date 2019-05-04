package com.tb24.fn;

public enum EFortRarity {
	HANDMADE("Common"), UNCOMMON("Uncommon"), STURDY("Rare"), QUALITY("Epic"), FINE("Legendary"), ELEGANT("Mythic"), MASTERWORK("T"), LEGENDARY("I");

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
}
