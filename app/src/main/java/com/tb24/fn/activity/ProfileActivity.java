package com.tb24.fn.activity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.tb24.fn.FortniteCompanionApp;
import com.tb24.fn.R;
import com.tb24.fn.event.ProfileUpdatedEvent;
import com.tb24.fn.model.AthenaProfileAttributes;
import com.tb24.fn.model.CommonCoreProfileAttributes;
import com.tb24.fn.model.CommonPublicProfileAttributes;
import com.tb24.fn.model.EpicError;
import com.tb24.fn.model.ExchangeResponse;
import com.tb24.fn.model.ExternalAuth;
import com.tb24.fn.model.FortMcpProfile;
import com.tb24.fn.model.FortMcpResponse;
import com.tb24.fn.model.XGameProfile;
import com.tb24.fn.model.command.SetHomebaseName;
import com.tb24.fn.util.Utils;

import org.checkerframework.checker.nullness.compatqual.NullableDecl;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.IOException;
import java.util.Arrays;

import retrofit2.Call;
import retrofit2.Response;

public class ProfileActivity extends BaseActivity implements View.OnClickListener {
	private static final Joiner NEWLINE = Joiner.on('\n');
	private Call<ExternalAuth[]> externalAuthCall;
	private Call<FortMcpResponse> callSetHombaseName;
	private Call<ExchangeResponse> callExchange;

	private static String bool(boolean b) {
		return b ? "Yes" : "No";
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_profile);
		setupActionBar();
		findViewById(R.id.profile_btn_manage_account).setOnClickListener(this);
		refreshUi();
		getThisApplication().eventBus.register(this);
		externalAuthCall = getThisApplication().accountPublicService.accountExternalAuths(PreferenceManager.getDefaultSharedPreferences(ProfileActivity.this).getString("epic_account_id", ""));
		new Thread("External Auths Worker") {
			@Override
			public void run() {
				try {
					final Response<ExternalAuth[]> response = externalAuthCall.execute();

					if (response.isSuccessful()) {
						runOnUiThread(new Runnable() {
							@Override
							public void run() {
								((TextView) findViewById(R.id.profile_external_auths)).setText(NEWLINE.join(Lists.transform(Arrays.asList(response.body()), new Function<ExternalAuth, String>() {
									@Override
									public String apply(@NullableDecl ExternalAuth input) {
										return input == null ? "" : input.type + ":\n\u2022 Name: " + input.externalDisplayName + "\n\u2022 Added: " + (input.dateAdded == null ? "N/A" : Utils.formatDateSimple(input.dateAdded)) + "\n\u2022 Last login: " + (input.lastLogin == null ? "N/A" : Utils.formatDateSimple(input.lastLogin));
									}
								})));
							}
						});
					} else {
						Utils.dialogError(ProfileActivity.this, EpicError.parse(response).getDisplayText());
					}
				} catch (IOException e) {
					Utils.throwableDialog(ProfileActivity.this, e);
				}
			}
		}.start();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		getThisApplication().eventBus.unregister(this);

		if (externalAuthCall != null) {
			externalAuthCall.cancel();
		}

		if (callSetHombaseName != null) {
			callSetHombaseName.cancel();
		}

		if (callExchange != null) {
			callExchange.cancel();
		}
	}

	@Subscribe(threadMode = ThreadMode.MAIN)
	public void onProfileUpdated(ProfileUpdatedEvent event) {
		refreshUi();
	}

	@Override
	public void onClick(View v) {
		if (v.getId() != R.id.profile_btn_manage_account) {
			return;
		}

		callExchange = getThisApplication().accountPublicService.oauthExchange();
		new Thread("Exchange Worker") {
			@Override
			public void run() {
				try {
					Response<ExchangeResponse> response = callExchange.execute();

					if (response.isSuccessful()) {
						startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(String.format("https://accounts.epicgames.com/exchange?exchangeCode=%s&redirectUrl=%s", response.body().code, Uri.encode("https://epicgames.com/site/account")))));
					} else {
						Utils.dialogError(ProfileActivity.this, EpicError.parse(response).getDisplayText());
					}
				} catch (IOException e) {
					Utils.throwableDialog(ProfileActivity.this, e);
				}
			}
		}.start();
	}

	private void refreshUi() {
		XGameProfile currentLoggedIn = getThisApplication().currentLoggedIn;

		if (currentLoggedIn != null) {
			((TextView) findViewById(R.id.profile_name)).setText(currentLoggedIn.name + ' ' + currentLoggedIn.lastName);
			((TextView) findViewById(R.id.profile_email)).setText(currentLoggedIn.email);
			((TextView) findViewById(R.id.profile_account_id)).setText(currentLoggedIn.getDisplayName() + " \u2022 " + currentLoggedIn.getId());
			((TextView) findViewById(R.id.profile_country)).setText(currentLoggedIn.country);
			((TextView) findViewById(R.id.profile_display_name_changes)).setText(String.valueOf(currentLoggedIn.numberOfDisplayNameChanges));
			((TextView) findViewById(R.id.profile_display_name_last_change)).setText(currentLoggedIn.lastDisplayNameChange == null ? "Never" : Utils.formatDateSimple(currentLoggedIn.lastDisplayNameChange));
			((TextView) findViewById(R.id.profile_2fa)).setText(bool(currentLoggedIn.tfaEnabled));
		}

		if (getThisApplication().profileManager.hasProfileData("common_core")) {
			FortMcpProfile common_core = getThisApplication().profileManager.getProfileData("common_core");
			CommonCoreProfileAttributes attributes = (CommonCoreProfileAttributes) common_core.stats.attributesObj;
			((TextView) findViewById(R.id.profile_created_on)).setText(Utils.formatDateSimple(common_core.created));
			((TextView) findViewById(R.id.profile_sac)).setText(attributes.mtx_affiliate.isEmpty() ? "None" : attributes.mtx_affiliate);
			((TextView) findViewById(R.id.profile_sac_set_time)).setText(attributes.mtx_affiliate_set_time == null ? "Never" : Utils.formatDateSimple(attributes.mtx_affiliate_set_time));
			((TextView) findViewById(R.id.profile_gift_send)).setText(bool(attributes.allowed_to_send_gifts));
			((TextView) findViewById(R.id.profile_gift_receive)).setText(bool(attributes.allowed_to_receive_gifts));
		}

		if (getThisApplication().profileManager.hasProfileData("common_public")) {
			FortMcpProfile common_public = getThisApplication().profileManager.getProfileData("common_public");
			final CommonPublicProfileAttributes attributes = (CommonPublicProfileAttributes) common_public.stats.attributesObj;
			((ImageView) findViewById(R.id.p_banner)).setImageBitmap(LockerActivity.makeBannerIcon(this, attributes.banner_icon, attributes.banner_color));
			TextView homebaseNameValTxt = findViewById(R.id.profile_homebase_name);

			if (!attributes.homebase_name.isEmpty()) {
				homebaseNameValTxt.setVisibility(View.VISIBLE);
				homebaseNameValTxt.setMovementMethod(LinkMovementMethod.getInstance());
				homebaseNameValTxt.setText(Utils.span(attributes.homebase_name, new ClickableSpan() {
					@Override
					public void onClick(@NonNull View widget) {
						AlertDialog editTextDialog = Utils.createEditTextDialog(ProfileActivity.this, "Change Homebase name", "Save", new Utils.EditTextDialogCallback() {
							@Override
							public void onResult(String s) {
								executeSetHomebaseName(s);
							}
						});
						editTextDialog.show();
						EditText editText = editTextDialog.findViewById(R.id.dialog_edit_text_field);
						editText.setHint("New Homebase name");
						editText.setText(attributes.homebase_name);
					}
				}));
			} else {
				homebaseNameValTxt.setVisibility(View.GONE);
			}
		}

		if (getThisApplication().profileManager.hasProfileData("athena")) {
			AthenaProfileAttributes attributes = (AthenaProfileAttributes) getThisApplication().profileManager.getProfileData("athena").stats.attributesObj;
			((TextView) findViewById(R.id.profile_br_account_level)).setText(String.valueOf(attributes.accountLevel));
			((TextView) findViewById(R.id.profile_br_xp_boost)).setText(String.valueOf(attributes.season_match_boost) + '%');
			((TextView) findViewById(R.id.profile_br_xp_boost_friend)).setText(String.valueOf(attributes.season_friend_match_boost) + '%');
			// TODO make a real layout for this
			((TextView) findViewById(R.id.profile_br_past_season_data)).setText(NEWLINE.join(Lists.transform(Arrays.asList(attributes.past_seasons), new Function<AthenaProfileAttributes.AthenaPastSeasonData, String>() {
				@Override
				public String apply(AthenaProfileAttributes.AthenaPastSeasonData input) {
					// TODO max xp prior to season 8 is different
					return String.format("Season %,d:\n Lvl %,d @ %s, %s Tier %,d; %,d Wins", input.seasonNumber, input.seasonLevel, input.seasonLevel == 100 ? "MAX" : String.format("%,d / %,d XP", input.seasonXp, FortniteCompanionApp.MAX_XPS_S8[input.seasonLevel - 1]), input.purchasedVIP ? "Battle Pass" : "Free Pass", input.bookLevel, input.numWins);
				}
			})));
		}
	}

	private void executeSetHomebaseName(String newName) {
		SetHomebaseName payload = new SetHomebaseName();
		payload.homebaseName = newName;
		callSetHombaseName = getThisApplication().fortnitePublicService.mcp(
				"SetHomebaseName",
				PreferenceManager.getDefaultSharedPreferences(this).getString("epic_account_id", ""),
				"common_public",
				getThisApplication().profileManager.getRvn("common_public"),
				payload);
		new Thread("Set Homebase Name Worker") {
			@Override
			public void run() {
				try {
					Response<FortMcpResponse> response = callSetHombaseName.execute();

					if (response.isSuccessful()) {
						getThisApplication().profileManager.executeProfileChanges(response.body());
					} else {
						Utils.dialogError(ProfileActivity.this, EpicError.parse(response).getDisplayText());
					}
				} catch (IOException e) {
					Utils.throwableDialog(ProfileActivity.this, e);
				}
			}
		}.start();
	}
}
