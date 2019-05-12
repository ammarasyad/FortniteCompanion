package com.tb24.fn.activity;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v14.preference.PreferenceFragment;
import android.support.v7.preference.DropDownPreference;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceManager;
import android.support.v7.preference.TwoStatePreference;
import android.util.Log;

import com.tb24.fn.FortniteCompanionApp;
import com.tb24.fn.R;
import com.tb24.fn.model.AccountPrivacyResponse;
import com.tb24.fn.model.EpicError;
import com.tb24.fn.util.ERegion;
import com.tb24.fn.util.Utils;

import java.io.IOException;

import retrofit2.Call;
import retrofit2.Response;

public class SettingsActivity extends BaseActivity {
	private static boolean isXLargeTablet(Context context) {
		return (context.getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_XLARGE;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setupActionBar();
		getFragmentManager().beginTransaction().replace(android.R.id.content, new GeneralPreferenceFragment()).commit();
	}

	public static class GeneralPreferenceFragment extends PreferenceFragment implements Preference.OnPreferenceChangeListener {
		private TwoStatePreference privacySettings;
		private AccountPrivacyResponse privacyData;

		@Override
		public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
			setPreferencesFromResource(R.xml.pref_general, rootKey);

			DropDownPreference prefRegion = (DropDownPreference) findPreference("matchmaking_region");
			int len = ERegion.values().length;
			String[] values = new String[len], names = new String[len];
			ERegion[] regions = ERegion.values();

			for (int i = 0; i < regions.length; i++) {
				ERegion region = regions[i];
				values[i] = region.toString();
				names[i] = region.name;
			}

			prefRegion.setEntryValues(values);
			prefRegion.setEntries(names);
			bindPreferenceSummaryToValue(prefRegion);

			privacySettings = (TwoStatePreference) findPreference("leaderboard_privacy");
			privacySettings.setOnPreferenceChangeListener(this);
			privacySettings.setEnabled(false);
			final Call<AccountPrivacyResponse> callPrivacy = ((FortniteCompanionApp) getActivity().getApplication()).fortnitePublicService.getAccountPrivacy(getPreferenceManager().getSharedPreferences().getString("epic_account_id", ""));

			new Thread() {
				@Override
				public void run() {
					try {
						Response<AccountPrivacyResponse> response = callPrivacy.execute();

						if (response.isSuccessful()) {
							privacyData = response.body();
							updatePrivacyPreference();
						} else {
							Utils.dialogError(getActivity(), EpicError.parse(response).getDisplayText());
						}
					} catch (IOException e) {
						Utils.dialogError(getActivity(), Utils.userFriendlyNetError(e));
					}
				}
			}.start();
		}

		private void updatePrivacyPreference() {
			if (getActivity() != null) {
				getActivity().runOnUiThread(new Runnable() {
					@Override
					public void run() {
						Preference.OnPreferenceChangeListener listener = privacySettings.getOnPreferenceChangeListener();
						privacySettings.setOnPreferenceChangeListener(null);
						privacySettings.setChecked(privacyData.optOutOfPublicLeaderboards);
						privacySettings.setEnabled(true);
						privacySettings.setOnPreferenceChangeListener(listener);
					}
				});
			}
		}

		@Override
		public boolean onPreferenceChange(Preference preference, Object value) {
			if (preference == privacySettings) {
				preference.setEnabled(false);
				final boolean old = privacyData.optOutOfPublicLeaderboards;
				privacyData.optOutOfPublicLeaderboards = (Boolean) value;
				final Call<AccountPrivacyResponse> callSetPrivacy = ((FortniteCompanionApp) getActivity().getApplication()).fortnitePublicService.setAccountPrivacy(getPreferenceManager().getSharedPreferences().getString("epic_account_id", ""), privacyData);
				new Thread() {
					@Override
					public void run() {
						try {
							Response<AccountPrivacyResponse> response = callSetPrivacy.execute();

							if (response.isSuccessful()) {
								privacyData = response.body();
								updatePrivacyPreference();
							} else {
								privacyData.optOutOfPublicLeaderboards = old;
								updatePrivacyPreference();
							}
						} catch (IOException e) {
							Utils.dialogError(getActivity(), Utils.userFriendlyNetError(e));
							privacyData.optOutOfPublicLeaderboards = old;
							updatePrivacyPreference();
						}
					}
				}.start();
				return true;
			}

			String stringValue = value.toString();
			CharSequence summary;

			if (preference instanceof ListPreference) {
				ListPreference listPreference = (ListPreference) preference;
				int index = listPreference.findIndexOfValue(stringValue);

				summary = index >= 0 ? listPreference.getEntries()[index] : null;
			} else {
				summary = stringValue;
			}

			preference.setSummary(Utils.makeItDark(summary, getActivity()));
			return true;
		}

		@Override
		public boolean onPreferenceTreeClick(Preference preference) {
			if (preference.getKey().equals("dump_login_data")) {
				SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
				Log.d("LoginDump", "Access Token: " + prefs.getString("epic_account_token_type", null) + " " + prefs.getString("epic_account_access_token", null));
				Log.d("LoginDump", "Refresh Token: " + prefs.getString("epic_account_refresh_token", null));
			}

			return super.onPreferenceTreeClick(preference);
		}

		private void bindPreferenceSummaryToValue(Preference preference) {
			preference.setOnPreferenceChangeListener(this);
			onPreferenceChange(preference, PreferenceManager.getDefaultSharedPreferences(preference.getContext()).getString(preference.getKey(), ""));
		}
	}
}
