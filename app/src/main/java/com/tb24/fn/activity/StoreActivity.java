package com.tb24.fn.activity;

import android.os.Bundle;

public class StoreActivity extends BaseActivity {
	public static String getOfferPurchaseUrl(String offerId) {
//		return String.format("https://launcher-website-prod07.ol.epicgames.com/purchase?showNavigation=true&namespace=fn&offers=%s", offerId);
		return String.format("https://launcher-website-prod07.ol.epicgames.com/purchase?namespace=fn&offers=%s", offerId);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}
}
