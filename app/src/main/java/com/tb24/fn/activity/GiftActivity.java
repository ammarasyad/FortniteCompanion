package com.tb24.fn.activity;

import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.tb24.fn.R;
import com.tb24.fn.model.EpicError;
import com.tb24.fn.model.FortCatalogResponse;
import com.tb24.fn.model.Friend;
import com.tb24.fn.util.LoadingViewController;
import com.tb24.fn.util.Utils;

import java.io.IOException;

import retrofit2.Call;
import retrofit2.Response;

public class GiftActivity extends BaseActivity {
	private FortCatalogResponse.CatalogEntry catalogEntry;
	private RecyclerView list;
	private Friend[] friendsData;
	private LoadingViewController lc;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if (!getIntent().hasExtra("a")) {
			finish();
			return;
		}

		setContentView(R.layout.common_loadable_recycler_view);
		setupActionBar();
		list = findViewById(R.id.main_recycler_view);
		list.setLayoutManager(new LinearLayoutManager(this));
		lc = new LoadingViewController(this, list, "");
		catalogEntry = getThisApplication().gson.fromJson(getIntent().getStringExtra("a"), FortCatalogResponse.CatalogEntry.class);
		loadFriends();
	}

	private void loadFriends() {
		lc.loading();
		final Call<Friend[]> callFriends = getThisApplication().friendsPublicService.friends(PreferenceManager.getDefaultSharedPreferences(this).getString("epic_account_id", ""), false);
		new Thread("Query Friends Worker") {
			@Override
			public void run() {
				try {
					Response<Friend[]> response = callFriends.execute();

					if (response.isSuccessful()) {
						friendsData = response.body();
						runOnUiThread(new Runnable() {
							@Override
							public void run() {
								showFriends();
							}
						});
					} else {
						Utils.dialogError(GiftActivity.this, EpicError.parse(response).getDisplayText());
					}
				} catch (IOException e) {
					Utils.throwableDialog(GiftActivity.this, e);
				}
			}
		}.start();
	}

	private void showFriends() {
		lc.content();
		list.setAdapter(new FriendsAdapter(this));
	}

	private static class FriendsAdapter extends RecyclerView.Adapter<FriendsAdapter.FriendsViewHolder> {
		private final GiftActivity activity;

		public FriendsAdapter(GiftActivity activity) {
			this.activity = activity;
		}

		@NonNull
		@Override
		public FriendsAdapter.FriendsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
			return new FriendsViewHolder(activity.getLayoutInflater().inflate(android.R.layout.simple_list_item_1, parent, false));
		}

		@Override
		public void onBindViewHolder(@NonNull FriendsAdapter.FriendsViewHolder holder, int position) {
			holder.text.setText(activity.friendsData[position].accountId);
		}

		@Override
		public int getItemCount() {
			return activity.friendsData.length;
		}

		private static class FriendsViewHolder extends RecyclerView.ViewHolder {
			TextView text;

			FriendsViewHolder(View itemView) {
				super(itemView);
				text = itemView.findViewById(android.R.id.text1);
			}
		}
	}
}
