package com.tb24.fn.activity;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.os.Bundle;
import android.support.v14.preference.PreferenceFragment;
import android.support.v7.preference.DropDownPreference;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceManager;
import android.support.v7.preference.TwoStatePreference;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.tb24.fn.FortniteCompanionApp;
import com.tb24.fn.R;
import com.tb24.fn.model.AccountPrivacyResponse;
import com.tb24.fn.model.CommonCoreProfileAttributes;
import com.tb24.fn.model.EpicError;
import com.tb24.fn.model.FortMcpProfile;
import com.tb24.fn.model.FortMcpResponse;
import com.tb24.fn.model.command.SetMtxPlatform;
import com.tb24.fn.util.EMtxPlatform;
import com.tb24.fn.util.ERegion;
import com.tb24.fn.util.Utils;
import com.tb24.fn.view.LayoutPreference;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Response;

public class SettingsActivity extends BaseActivity {
//	private static boolean isXLargeTablet(Context context) {
//		return (context.getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_XLARGE;
//	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setupActionBar();
		getFragmentManager().beginTransaction().replace(android.R.id.content, new GeneralPreferenceFragment()).commit();
	}

	public static class GeneralPreferenceFragment extends PreferenceFragment implements Preference.OnPreferenceChangeListener {
		private DropDownPreference prefRegion;
		private TwoStatePreference prefPrivacy;
		private DropDownPreference prefMtxPlatform;
		private Preference prefViewLoginData;
		private LayoutPreference prefLogOut;
		private AccountPrivacyResponse privacyData;

		@Override
		public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
			setPreferencesFromResource(R.xml.pref_general, rootKey);

			prefRegion = (DropDownPreference) findPreference("matchmaking_region");
			int len = ERegion.values().length;
			String[] values = new String[len];
			String[] names = new String[len];
			ERegion[] enumValues = ERegion.values();

			for (int i = 0; i < enumValues.length; i++) {
				ERegion value = enumValues[i];
				values[i] = value.toString();
				names[i] = value.name;
			}

			prefRegion.setEntryValues(values);
			prefRegion.setEntries(names);
			bindPreferenceSummaryToValue(prefRegion);

			Preference ctgAccount = findPreference("account_ctg");
			ctgAccount.setVisible(getPreferenceManager().getSharedPreferences().getBoolean("is_logged_in", false));

			if (ctgAccount.isVisible()) {
				prefPrivacy = (TwoStatePreference) findPreference("leaderboard_privacy");
				prefPrivacy.setOnPreferenceChangeListener(this);
				prefPrivacy.setEnabled(false);
				final Call<AccountPrivacyResponse> callPrivacy = getApplication_().fortnitePublicService.getAccountPrivacy(getPreferenceManager().getSharedPreferences().getString("epic_account_id", ""));
				new Thread("Privacy Worker") {
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

				prefMtxPlatform = (DropDownPreference) findPreference("mtx_platform");
				int len1 = EMtxPlatform.values().length;
				String[] values1 = new String[len1];
				String[] names1 = new String[len1];
				EMtxPlatform[] enumValues1 = EMtxPlatform.values();

				for (int i = 0; i < enumValues1.length; i++) {
					EMtxPlatform value = enumValues1[i];
					values1[i] = value.toString();
					names1[i] = value.toString();
				}

				prefMtxPlatform.setEntryValues(values1);
				prefMtxPlatform.setEntries(names1);
				prefMtxPlatform.setOnPreferenceChangeListener(this);
				updateMtxPlatformPreference(null);
			}

			prefViewLoginData = findPreference("view_login_data");
			prefViewLoginData.setVisible(getPreferenceManager().getSharedPreferences().getBoolean("is_logged_in", false));

			prefLogOut = (LayoutPreference) findPreference("log_out");
			prefLogOut.setVisible(getPreferenceManager().getSharedPreferences().getBoolean("is_logged_in", false));
			prefLogOut.findViewById(R.id.log_out_button).setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					new AlertDialog.Builder(getActivity())
							.setTitle("Log out?")
							.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog, int which) {
									new Thread("Logout Worker") {
										@Override
										public void run() {
											Call<Void> call = getApplication_().accountPublicService.oauthSessionsKillAccessToken(getPreferenceManager().getSharedPreferences().getString("epic_account_access_token", null));

											try {
												call.execute();
												getApplication_().clearLoginData();
												getApplication_().eventBus.post(new LoggedOutEvent());
												getActivity().finish();
											} catch (IOException e) {
												Utils.throwableDialog(getActivity(), e);
											}
										}
									}.start();
								}
							})
							.setNegativeButton("No", null)
							.show();
				}
			});
		}

		private void updatePrivacyPreference() {
			if (getActivity() != null) {
				getActivity().runOnUiThread(new Runnable() {
					@Override
					public void run() {
						Preference.OnPreferenceChangeListener listener = prefPrivacy.getOnPreferenceChangeListener();
						prefPrivacy.setOnPreferenceChangeListener(null);
						prefPrivacy.setChecked(privacyData.optOutOfPublicLeaderboards);
						prefPrivacy.setEnabled(true);
						prefPrivacy.setOnPreferenceChangeListener(listener);
					}
				});
			}
		}

		private void updateMtxPlatformPreference(final String changeBackTo) {
			if (getActivity() != null) {
				getActivity().runOnUiThread(new Runnable() {
					@Override
					public void run() {
						Map<String, FortMcpProfile> profileData = getApplication_().profileManager.profileData;

						if (profileData.containsKey("common_core")) {
							prefMtxPlatform.setEnabled(true);
							String newValue = changeBackTo != null ? changeBackTo : ((CommonCoreProfileAttributes) profileData.get("common_core").stats.attributesObj).current_mtx_platform.toString();
							prefMtxPlatform.setValue(newValue);
							prefMtxPlatform.setSummary(newValue);
						} else {
							prefMtxPlatform.setEnabled(false);
						}
					}
				});
			}
		}

		@Override
		public boolean onPreferenceChange(Preference preference, Object value) {
			if (preference == prefPrivacy) {
				preference.setEnabled(false);
				final boolean old = privacyData.optOutOfPublicLeaderboards;
				privacyData.optOutOfPublicLeaderboards = (Boolean) value;
				final Call<AccountPrivacyResponse> callSetPrivacy = getApplication_().fortnitePublicService.setAccountPrivacy(getPreferenceManager().getSharedPreferences().getString("epic_account_id", ""), privacyData);
				new Thread() {
					@Override
					public void run() {
						try {
							Response<AccountPrivacyResponse> response = callSetPrivacy.execute();

							if (response.isSuccessful()) {
								privacyData = response.body();
								updatePrivacyPreference();
							} else {
								Utils.dialogError(getActivity(), EpicError.parse(response).getDisplayText());
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
			} else if (preference == prefMtxPlatform) {
				if (getApplication_().profileManager.profileData.containsKey("common_core") && !value.equals(((CommonCoreProfileAttributes) getApplication_().profileManager.profileData.get("common_core").stats.attributesObj).current_mtx_platform.toString())) {
					preference.setEnabled(false);
					final String old = prefMtxPlatform.getValue();
					prefMtxPlatform.setSummary(value.toString());
					SetMtxPlatform payload = new SetMtxPlatform();
					payload.newPlatform = EMtxPlatform.valueOf(value.toString());
					final Call<FortMcpResponse> callSetMtxPlatform = getApplication_().fortnitePublicService.mcp("SetMtxPlatform", getPreferenceManager().getSharedPreferences().getString("epic_account_id", ""), "common_core", -1, true, payload);
					new Thread("Set MTX Platform Worker") {
						@Override
						public void run() {
							try {
								Response<FortMcpResponse> response = callSetMtxPlatform.execute();

								if (response.isSuccessful()) {
									getApplication_().profileManager.executeProfileChanges(response.body());
									updateMtxPlatformPreference(null);
								} else {
									Utils.dialogError(getActivity(), EpicError.parse(response).getDisplayText());
									updateMtxPlatformPreference(old);
								}
							} catch (IOException e) {
								Utils.dialogError(getActivity(), Utils.userFriendlyNetError(e));
								updateMtxPlatformPreference(old);
							}
						}
					}.start();
				}

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

		private FortniteCompanionApp getApplication_() {
			return (FortniteCompanionApp) getActivity().getApplication();
		}

		@Override
		public boolean onPreferenceTreeClick(Preference preference) {
			if (preference.getKey() != null) {
				if (preference.getKey().equals("view_login_data")) {
					SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
					String s = "Account ID: " + prefs.getString("epic_account_id", null)
							+ '\n' + "Access Token: " + prefs.getString("epic_account_token_type", null) + " " + prefs.getString("epic_account_access_token", null)
							+ '\n' + "Refresh Token: " + prefs.getString("epic_account_refresh_token", null);
					Utils.dialogOkNonMain(getActivity(), null, s);
					Log.d("LoginDump", s);
				} else if (preference.getKey().equals("test_just_for_debugging")) {
					AssetManager assets = getActivity().getAssets();
					List<String> notFound = new ArrayList<>();
					Collection<JsonElement> all = getApplication_().itemRegistry.getAll();

					for (JsonElement entry : all) {
						JsonObject jsonObject = entry.getAsJsonArray().get(0).getAsJsonObject();

						if (jsonObject.has("SmallPreviewImage")) {
							String path = jsonObject.get("SmallPreviewImage").getAsJsonObject().get("asset_path_name").getAsString();

							try {
								InputStream is = assets.open(Utils.parseUPath(path) + ".tga");
								is.close();
							} catch (IOException e) {
								notFound.add(path);
							}
						}

					}

					for (String s : new HashSet<>(notFound)) {
						Log.d("NotFoundItems", s);
					}

					Toast.makeText(getActivity(), "Database verification completed.\nResult: " + (all.size() - notFound.size()) + '/' + all.size() + " items has textures.", Toast.LENGTH_LONG).show();
				}
			}

			return super.onPreferenceTreeClick(preference);
		}

		private void bindPreferenceSummaryToValue(Preference preference) {
			preference.setOnPreferenceChangeListener(this);
			onPreferenceChange(preference, PreferenceManager.getDefaultSharedPreferences(preference.getContext()).getString(preference.getKey(), ""));
		}
	}
}
