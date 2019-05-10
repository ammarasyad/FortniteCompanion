package com.tb24.fn.model.assetdata;

public class FloatColor {
	public Float r;
	public Float g;
	public Float b;
	public Float a;

	public int toInt() {
		return ((int) (a * 255.0f + 0.5f) << 24) | ((int) (r * 255.0f + 0.5f) << 16) | ((int) (g * 255.0f + 0.5f) << 8) | (int) (b * 255.0f + 0.5f);
	}
}
