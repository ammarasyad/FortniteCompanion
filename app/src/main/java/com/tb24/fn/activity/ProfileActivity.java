package com.tb24.fn.activity;

import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.tb24.fn.R;
import com.tb24.fn.model.AthenaProfileAttributes;
import com.tb24.fn.model.CommonCoreProfileAttributes;
import com.tb24.fn.model.FortMcpProfile;
import com.tb24.fn.model.WorldInfoResponse;
import com.tb24.fn.model.XGameProfile;
import com.tb24.fn.util.LoadingViewController;
import com.tb24.fn.util.Utils;

import java.util.Arrays;

import retrofit2.Call;

public class ProfileActivity extends BaseActivity {
	private LoadingViewController lc;
	private Call<WorldInfoResponse> callWorldInfo;
	private WorldInfoResponse data;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.common_loadable_framed);
		setupActionBar();
		ViewGroup frame = findViewById(R.id.main_content);
		getLayoutInflater().inflate(R.layout.activity_profile, frame);
		lc = new LoadingViewController(this, frame, "");
		refreshUi();
		lc.content();
	}

	private void refreshUi() {
		XGameProfile currentLoggedIn = getThisApplication().currentLoggedIn;

		if (currentLoggedIn != null) {
			((TextView) findViewById(R.id.profile_name)).setText(currentLoggedIn.name + ' ' + currentLoggedIn.lastName);
			((TextView) findViewById(R.id.profile_email)).setText(currentLoggedIn.email);
			((TextView) findViewById(R.id.profile_account_id)).setText(currentLoggedIn.getDisplayName() + " \u2022 " + currentLoggedIn.getId());
			((TextView) findViewById(R.id.profile_country)).setText("Country: " + currentLoggedIn.country);
			((TextView) findViewById(R.id.profile_display_name_changes)).setText(String.valueOf(currentLoggedIn.numberOfDisplayNameChanges));
			((TextView) findViewById(R.id.profile_display_name_last_change)).setText(Utils.formatDateSimple(currentLoggedIn.lastDisplayNameChange));
			((TextView) findViewById(R.id.profile_2fa)).setText(bool(currentLoggedIn.tfaEnabled));
		}

		if (getThisApplication().profileManager.profileData.containsKey("common_core")) {
			FortMcpProfile common_core = getThisApplication().profileManager.profileData.get("common_core");
			CommonCoreProfileAttributes attributes = (CommonCoreProfileAttributes) common_core.stats.attributesObj;
			((TextView) findViewById(R.id.profile_created_on)).setText(Utils.formatDateSimple(common_core.created));
			((TextView) findViewById(R.id.profile_sac)).setText(attributes.mtx_affiliate);
			((TextView) findViewById(R.id.profile_sac_set_time)).setText(Utils.formatDateSimple(attributes.mtx_affiliate_set_time));
			((TextView) findViewById(R.id.profile_gift_send)).setText(bool(attributes.allowed_to_send_gifts));
			((TextView) findViewById(R.id.profile_gift_receive)).setText(bool(attributes.allowed_to_receive_gifts));
		}

		if (getThisApplication().profileManager.profileData.containsKey("athena")) {
			AthenaProfileAttributes attributes = (AthenaProfileAttributes) getThisApplication().profileManager.profileData.get("athena").stats.attributesObj;
			((TextView) findViewById(R.id.profile_br_account_level)).setText(String.valueOf(attributes.accountLevel));
			((TextView) findViewById(R.id.profile_br_xp_boost)).setText(String.valueOf(attributes.season_match_boost) + '%');
			((TextView) findViewById(R.id.profile_br_xp_boost_friend)).setText(String.valueOf(attributes.season_friend_match_boost) + '%');
			((TextView) findViewById(R.id.profile_br_past_season_data)).setText(Joiner.on('\n').join(Lists.transform(Arrays.asList(attributes.past_seasons), new Function<AthenaProfileAttributes.AthenaPastSeasonData, String>() {
				@Override
				public String apply(AthenaProfileAttributes.AthenaPastSeasonData input) {
					return String.format("Season %d:\n Lvl %d @ %d XP, %s Tier %d; %d Wins", input.seasonNumber, input.seasonLevel, input.seasonXp, input.purchasedVIP ? "Battle Pass" : "Free Pass", input.bookLevel, input.numWins);
				}
			})));
		}
	}

	private static String bool(boolean b) {
		return b ? "Yes" : "No";
	}
}
