package com.tb24.fn.util;

public enum EnumRegion {
	NAE("NA-East"), NAW("NA-West"), OCE("Oceania"), ASIA("Asia"), EU("Europe"), BR("Brazil");

	public final String name;

	EnumRegion(String name) {
		this.name = name;
	}

	public static EnumRegion from(String s) {
		for (EnumRegion region : values()) {
			if (region.toString().equals(s)) {
				return region;
			}
		}

		throw new IllegalArgumentException(s);
	}
}
