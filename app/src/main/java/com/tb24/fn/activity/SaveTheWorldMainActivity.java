package com.tb24.fn.activity;

import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.LinearLayout;

import androidx.appcompat.app.AlertDialog;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.tb24.fn.R;
import com.tb24.fn.model.EpicError;
import com.tb24.fn.model.FortItemStack;
import com.tb24.fn.model.FortMcpResponse;
import com.tb24.fn.util.ItemUtils;
import com.tb24.fn.util.JsonUtils;
import com.tb24.fn.util.Utils;

import java.io.IOException;

import retrofit2.Call;
import retrofit2.Response;

public class SaveTheWorldMainActivity extends BaseActivity implements View.OnClickListener {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_save_the_world_main);
		setupActionBar();
		findViewById(R.id.stw_main_btn_daily_rewards).setOnClickListener(this);
		findViewById(R.id.stw_main_btn_missions_info).setOnClickListener(this);
		findViewById(R.id.stw_main_btn_research).setOnClickListener(this);
		findViewById(R.id.main_screen_btn_news).setOnClickListener(this);

		if (!getThisApplication().profileManager.hasProfileData("campaign")) {
			getThisApplication().profileManager.requestProfileUpdate("campaign");
		}
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
			case R.id.stw_main_btn_daily_rewards:
				final Call<FortMcpResponse> call = getThisApplication().fortnitePublicService.mcp(
						"ClaimLoginReward",
						PreferenceManager.getDefaultSharedPreferences(this).getString("epic_account_id", ""),
						"campaign",
						getThisApplication().profileManager.getRvn("campaign"),
						new JsonObject());
				new Thread("Claim Login Reward Worker") {
					@Override
					public void run() {
						try {
							Response<FortMcpResponse> response = call.execute();

							if (response.isSuccessful()) {
								final FortMcpResponse mcpResponse = response.body();
								getThisApplication().profileManager.executeProfileChanges(mcpResponse);
								runOnUiThread(new Runnable() {
									@Override
									public void run() {
										JsonObject notificationObj = mcpResponse.notifications[0];
										JsonArray itemsArr = notificationObj.getAsJsonArray("items");
										AlertDialog.Builder builder = new AlertDialog.Builder(SaveTheWorldMainActivity.this)
												.setTitle("Daily Rewards")
												.setMessage(String.format("%,d days logged in", JsonUtils.getIntOr("daysLoggedIn", notificationObj, 0)) + (itemsArr.size() == 0 ? '\n' + "Reward for today has already been claimed" : ""))
												.setPositiveButton(android.R.string.ok, null);
										LinearLayout layout = new LinearLayout(SaveTheWorldMainActivity.this);

										if (itemsArr.size() > 0) {
											for (JsonElement item : itemsArr) {
												JsonObject itemObj = item.getAsJsonObject();
												View slot = getLayoutInflater().inflate(R.layout.slot_view, layout);
												FortItemStack itemStack = new FortItemStack(JsonUtils.getStringOr("itemType", itemObj, "???:Invalid"), JsonUtils.getIntOr("quantity", itemObj, 1));
												ItemUtils.populateSlotView(SaveTheWorldMainActivity.this, slot, itemStack, getThisApplication().itemRegistry.get(itemStack.templateId));
											}

											builder.setView(layout);
										}

										builder.show();
									}
								});
							} else {
								Utils.dialogError(SaveTheWorldMainActivity.this, EpicError.parse(response).getDisplayText());
							}
						} catch (IOException e) {
							Utils.throwableDialog(SaveTheWorldMainActivity.this, e);
						}
					}
				}.start();
				break;
			case R.id.stw_main_btn_missions_info:
				startActivity(new Intent(this, StwWorldInfoActivity.class));
				break;
			case R.id.stw_main_btn_research:
				break;
			case R.id.main_screen_btn_news:
				startActivity(new Intent(this, NewsActivity.class));
				break;
		}
	}
}
