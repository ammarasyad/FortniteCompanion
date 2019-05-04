package com.tb24.fn.activity;

import android.content.Context;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v14.preference.PreferenceFragment;
import android.support.v7.preference.DropDownPreference;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceManager;

import com.tb24.fn.R;
import com.tb24.fn.util.EnumRegion;
import com.tb24.fn.util.Utils;

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
		@Override
		public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
			setPreferencesFromResource(R.xml.pref_general, rootKey);
			DropDownPreference prefRegion = (DropDownPreference) findPreference("matchmaking_region");
			int len = EnumRegion.values().length;
			String[] values = new String[len], names = new String[len];
			EnumRegion[] regions = EnumRegion.values();

			for (int i = 0; i < regions.length; i++) {
				EnumRegion region = regions[i];
				values[i] = region.toString();
				names[i] = region.name;
			}

			prefRegion.setEntryValues(values);
			prefRegion.setEntries(names);
			bindPreferenceSummaryToValue(prefRegion);
		}

		@Override
		public boolean onPreferenceChange(Preference preference, Object value) {
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

		private void bindPreferenceSummaryToValue(Preference preference) {
			preference.setOnPreferenceChangeListener(this);
			onPreferenceChange(preference, PreferenceManager.getDefaultSharedPreferences(preference.getContext()).getString(preference.getKey(), ""));
		}
	}
}
