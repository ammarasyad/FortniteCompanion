package com.tb24.fn.activity;

import android.app.ActionBar;
import android.app.Activity;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.tb24.fn.FortniteCompanionApp;
import com.tb24.fn.model.FortItemStack;

import java.util.Map;

public abstract class BaseActivity extends Activity {
	public static int countAndSetVbucks(BaseActivity activity, ViewGroup vBucksView) {
		if (!activity.getThisApplication().profileManager.profileData.containsKey("common_core")) {
			vBucksView.setVisibility(View.GONE);
			return 0;
		}

		int vBucksQty = 0;

		for (Map.Entry<String, FortItemStack> entry : activity.getThisApplication().profileManager.profileData.get("common_core").items.entrySet()) {
			String templateId = entry.getValue().templateId;

			if (templateId.equals("Currency:MtxComplimentary") || templateId.equals("Currency:MtxGiveaway") || templateId.equals("Currency:MtxPurchaseBonus")) {
				vBucksQty += entry.getValue().quantity;
			}
		}

		vBucksView.setVisibility(View.VISIBLE);
		((TextView) vBucksView.getChildAt(1)).setText(String.format("%,d", vBucksQty));
		return vBucksQty;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == android.R.id.home) {
			finish();
		}

		return super.onOptionsItemSelected(item);
	}

	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_ESCAPE && !event.isCanceled()) {
			onBackPressed();
			return true;
		}

		return super.onKeyUp(keyCode, event);
	}

	public FortniteCompanionApp getThisApplication() {
		return (FortniteCompanionApp) getApplication();
	}

	protected void setupActionBar() {
		ActionBar actionBar = getActionBar();

		if (actionBar != null) {
			actionBar.setDisplayHomeAsUpEnabled(true);
		}
	}
}
