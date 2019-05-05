package com.tb24.fn.util;

public enum ERegion {
	NAE("NA-East"), NAW("NA-West"), OCE("Oceania"), ASIA("Asia"), EU("Europe"), BR("Brazil");

	public final String name;

	ERegion(String name) {
		this.name = name;
	}

	public static ERegion from(String s) {
		for (ERegion region : values()) {
			if (region.toString().equals(s)) {
				return region;
			}
		}

		throw new IllegalArgumentException(s);
	}
}
