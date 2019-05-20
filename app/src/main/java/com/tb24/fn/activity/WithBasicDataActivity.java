package com.tb24.fn.activity;

import com.tb24.fn.model.EpicError;
import com.tb24.fn.model.FortBasicDataResponse;
import com.tb24.fn.util.LoadingViewController;

import java.io.IOException;

import retrofit2.Call;
import retrofit2.Response;

public abstract class WithBasicDataActivity extends BaseActivity {
	protected void load(final LoadingViewController lc) {
		if (getThisApplication().basicData != null) {
			refreshUi(getThisApplication().basicData);
			return;
		}

		final Call<FortBasicDataResponse> call = getThisApplication().fortniteContentWebsiteService.getBasicData();
		lc.loading();
		new Thread(new Runnable() {
			@Override
			public void run() {
				String errorText = "";
				try {
					final Response<FortBasicDataResponse> response = call.execute();

					if (response.isSuccessful()) {
						getThisApplication().basicData = response.body();
						runOnUiThread(new Runnable() {
							@Override
							public void run() {
								refreshUi(getThisApplication().basicData);
							}
						});
					} else {
						errorText = EpicError.parse(response).getDisplayText();
					}
				} catch (IOException e) {
					errorText = e.toString();
				}

				final String finalText = errorText;
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						if (!finalText.isEmpty()) {
							lc.error(finalText);
						}
					}
				});
			}
		}).start();
	}

	protected abstract void refreshUi(FortBasicDataResponse data);
}
