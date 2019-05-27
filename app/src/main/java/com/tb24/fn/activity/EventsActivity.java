package com.tb24.fn.activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.squareup.picasso.Picasso;
import com.tb24.fn.R;
import com.tb24.fn.model.EpicError;
import com.tb24.fn.model.EventDownloadResponse;
import com.tb24.fn.model.FortBasicDataResponse;
import com.tb24.fn.util.ERegion;
import com.tb24.fn.util.LoadingViewController;
import com.tb24.fn.util.Utils;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import retrofit2.Call;
import retrofit2.Response;

public class EventsActivity extends WithBasicDataActivity {
	public static final Comparator<EventDownloadResponse.Event> EVENT_DATE_COMPARATOR = new Comparator<EventDownloadResponse.Event>() {
		@Override
		public int compare(EventDownloadResponse.Event o1, EventDownloadResponse.Event o2) {
			return o2.beginTime.compareTo(o1.beginTime);
		}
	};
	private RecyclerView list;
	private EventAdapter adapter;
	private LoadingViewController lc;
	private ERegion region;
	private Call<EventDownloadResponse> downloadCall;

	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.common_loadable_recycler_view);
		setupActionBar();
		region = ERegion.from(PreferenceManager.getDefaultSharedPreferences(this).getString("matchmaking_region", ERegion.NAW.toString()));
		list = findViewById(R.id.main_recycler_view);
		int p = (int) Utils.dp(getResources(), 8);
		list.setPadding(p, p, p, p);
		list.setClipToPadding(false);
		list.post(new Runnable() {
			@Override
			public void run() {
				list.setLayoutManager(new GridLayoutManager(EventsActivity.this, (int) (list.getWidth() / Utils.dp(getResources(), 200))));
			}
		});
		lc = new LoadingViewController(EventsActivity.this, list, "");
		load(lc);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();

		if (downloadCall != null) {
			downloadCall.cancel();
		}
	}

	protected void loadExtraData(final FortBasicDataResponse data) {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

		if (getThisApplication().eventData != null && getThisApplication().eventDataRegion != null && getThisApplication().eventDataRegion == region) {
			displayEventDataLoaded(data);
			return;
		}

		lc.loading();
		String accountId = prefs.getString("epic_account_id", "");
		downloadCall = getThisApplication().eventsPublicServiceLive.download(accountId, region.toString(), "Windows", accountId);
		new Thread() {
			@Override
			public void run() {
				try {
					final Response<EventDownloadResponse> response = downloadCall.execute();

					if (response.isSuccessful()) {
						getThisApplication().eventData = response.body();
						getThisApplication().eventDataRegion = region;
						runOnUiThread(new Runnable() {
							@Override
							public void run() {
								displayEventDataLoaded(data);
							}
						});
					} else {
						runOnUiThread(new Runnable() {
							@Override
							public void run() {
								lc.error(EpicError.parse(response).getDisplayText());
							}
						});
					}
				} catch (final IOException e) {
					runOnUiThread(new Runnable() {
						@Override
						public void run() {
							lc.error(Utils.userFriendlyNetError(e));
						}
					});
				}
			}
		}.start();
	}

	public void refreshUi(FortBasicDataResponse data) {
		lc.content();
		loadExtraData(data);
	}

	private void displayEventDataLoaded(FortBasicDataResponse data) {
		List<EventDownloadResponse.Event> raw = Arrays.asList(getThisApplication().eventData.events);
		Collections.sort(raw, EVENT_DATE_COMPARATOR);
		list.setAdapter(adapter = new EventAdapter(raw, this));
		lc.content();
	}

	private static class EventAdapter extends RecyclerView.Adapter<EventAdapter.EventViewHolder> {
		private final List<EventDownloadResponse.Event> data;
		private final BaseActivity activity;

		public EventAdapter(List<EventDownloadResponse.Event> data, BaseActivity activity) {
			this.data = data;
			this.activity = activity;
		}

		@NonNull
		@Override
		public EventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
			return new EventViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.event_entry, parent, false));
		}

		@Override
		public void onBindViewHolder(@NonNull EventViewHolder holder, int position) {
			final EventDownloadResponse.Event entry = data.get(position);
			FortBasicDataResponse.TournamentDisplayInfo displayInfo = entry.findDisplayInfo(activity.getThisApplication().basicData);

			if (displayInfo == null) {
				throw new RuntimeException("Display info not found for event \'" + entry.eventId + "\', bailing out!");
			}

			//25:36 aspect ratio
			Picasso.get().load(displayInfo.poster_front_image).into(holder.image);
			int color = 0xFFFFFF;

			if (displayInfo.title_color != null) {
				color = Integer.parseInt(displayInfo.title_color, 16);
			}

			holder.titleLine1.setTextColor(0xFF << 24 | color);
			holder.titleLine2.setTextColor(0xFF << 24 | color);
			holder.titleLine1.setText(displayInfo.title_line_1);
			holder.titleLine2.setText(displayInfo.title_line_2);
			holder.scheduleInfo.setText(displayInfo.schedule_info);
			final FortBasicDataResponse.TournamentDisplayInfo finalDisplayInfo = displayInfo;
			holder.itemView.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					Intent intent = new Intent(activity, EventDetailActivity.class);
					intent.putExtra("a", activity.getThisApplication().gson.toJson(entry, EventDownloadResponse.Event.class));
					intent.putExtra("b", activity.getThisApplication().gson.toJson(finalDisplayInfo, FortBasicDataResponse.TournamentDisplayInfo.class));
					activity.startActivity(intent);
				}
			});
		}

		@Override
		public int getItemCount() {
			return data.size();
		}

		class EventViewHolder extends RecyclerView.ViewHolder {
			ImageView image;
			TextView titleLine1;
			TextView titleLine2;
			TextView scheduleInfo;

			EventViewHolder(View itemView) {
				super(itemView);
				image = itemView.findViewById(R.id.item_img);
				titleLine1 = itemView.findViewById(R.id.item_text1);
				titleLine2 = itemView.findViewById(R.id.item_text2);
				scheduleInfo = itemView.findViewById(R.id.item_text3);
			}
		}
	}
}