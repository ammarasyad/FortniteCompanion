package com.tb24.fn;

import android.util.Log;

import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import com.tb24.fn.activity.BaseActivity;
import com.tb24.fn.model.EpicError;
import com.tb24.fn.model.GameProfile;
import com.tb24.fn.util.ProfileLookupCallback;
import com.tb24.fn.util.ProfileNotFoundException;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import retrofit2.Response;

public class GameProfileRepository {
	private static final String TAG = GameProfileRepository.class.getSimpleName();
	private static final int ENTRIES_PER_PAGE = 100;
	private static final int MAX_FAIL_COUNT = 3;
	private static final int DELAY_BETWEEN_PAGES = 100;
	private static final int DELAY_BETWEEN_FAILURES = 750;

	private GameProfileRepository() {
	}

	public static void findProfileDataByIds(BaseActivity activity, Set<String> criteriaIds, ProfileLookupCallback callback) {
		for (final List<String> request : Iterables.partition(criteriaIds, ENTRIES_PER_PAGE)) {
			int failCount = 0;
			boolean failed;

			do {
				failed = false;

				try {
					final Response<GameProfile[]> response = activity.getThisApplication().accountPublicService.accountMultiple(request).execute();

					if (response.isSuccessful()) {
						failCount = 0;
						GameProfile[] body = response.body();
						final Set<String> missing = Sets.newHashSet(request);

						for (final GameProfile profile : body) {
//							Log.d(TAG, "Successfully looked up profile " + profile);
							missing.remove(profile.getId().toLowerCase());
							callback.onProfileLookupSucceeded(profile);
						}

						for (final String id : missing) {
							Log.d(TAG, "Couldn't find profile " + id);
							callback.onProfileLookupFailed(new GameProfile(id, null), new ProfileNotFoundException("Server did not find the requested profile"));
						}

						try {
							Thread.sleep(DELAY_BETWEEN_PAGES);
						} catch (final InterruptedException ignored) {
						}
					} else {
						EpicError.parse(response);
					}
				} catch (final IOException e) {
					failCount++;

					if (failCount == MAX_FAIL_COUNT) {
						for (final String id : request) {
//							Log.d(TAG, "Couldn't find profile " + id + " because of a server error");
							callback.onProfileLookupFailed(new GameProfile(null, id), e);
						}
					} else {
						try {
							Thread.sleep(DELAY_BETWEEN_FAILURES);
						} catch (final InterruptedException ignored) {
						}
						failed = true;
					}
				}
			} while (failed);
		}
	}
}
