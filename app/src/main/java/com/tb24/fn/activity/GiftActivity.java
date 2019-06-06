package com.tb24.fn.activity;

import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Lists;
import com.google.gson.JsonObject;
import com.tb24.fn.GameProfileRepository;
import com.tb24.fn.R;
import com.tb24.fn.model.EpicError;
import com.tb24.fn.model.FortCatalogResponse;
import com.tb24.fn.model.FortItemStack;
import com.tb24.fn.model.FortMcpResponse;
import com.tb24.fn.model.Friend;
import com.tb24.fn.model.GameProfile;
import com.tb24.fn.model.command.GiftCatalogEntry;
import com.tb24.fn.util.LoadingViewController;
import com.tb24.fn.util.ProfileLookupCallback;
import com.tb24.fn.util.Utils;

import org.checkerframework.checker.nullness.compatqual.NullableDecl;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import retrofit2.Call;
import retrofit2.Response;

public class GiftActivity extends BaseActivity implements View.OnClickListener {
	private static final int MAX_RECIPIENTS = 4;
	private static final Joiner COMMA_JOINER = Joiner.on(", ");
	private RecyclerView recipientList;
	private ViewGroup mainFrame;
	private ViewGroup giftLayout;
	private EditText giftMessage;
	private TextView recipientSummary;
	private TextView vbucksTotal;
	private Button buttonPrev;
	private Button buttonNext;
	private LoadingViewController lc;
	private FriendsAdapter adapter;
	private FortCatalogResponse.CatalogEntry catalogEntry;
	private List<Friend> friendsData;
	private Map<String, String> displayNameMap = new HashMap<>();
	private boolean isStep2;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if (!getIntent().hasExtra("a")) {
			finish();
			return;
		}

		setContentView(R.layout.activity_gift);
		setupActionBar();
		catalogEntry = getThisApplication().gson.fromJson(getIntent().getStringExtra("a"), FortCatalogResponse.CatalogEntry.class);
		mainFrame = findViewById(R.id.main_content);
		recipientList = (RecyclerView) getLayoutInflater().inflate(R.layout.vertical_rv, mainFrame, false);
		recipientList.setLayoutManager(new LinearLayoutManager(this));
		recipientList.setVisibility(View.GONE);
		giftLayout = (ViewGroup) getLayoutInflater().inflate(R.layout.step_gift, mainFrame, false);
		giftMessage = giftLayout.findViewById(R.id.gift_message);
		giftLayout.setVisibility(View.GONE);
		mainFrame.addView(recipientList);
		mainFrame.addView(giftLayout);
		ViewGroup footer = findViewById(R.id.pinned_footer);
		ViewGroup footerView = (ViewGroup) getLayoutInflater().inflate(R.layout.activity_gift_footer, footer);
		((TextView) footerView.findViewById(R.id.gift_summary_item)).setText(COMMA_JOINER.join(Lists.transform(Arrays.asList(catalogEntry.itemGrants), new Function<FortItemStack, String>() {
			@Override
			public String apply(FortItemStack input) {
				return input.getDefData() == null ? input.templateId : input.getDefData().DisplayName;
			}
		})));
		recipientSummary = footerView.findViewById(R.id.gift_summary_recipients);
		vbucksTotal = footerView.findViewById(R.id.gift_vbucks_total);
		buttonPrev = footerView.findViewById(android.R.id.button1);
		buttonPrev.setText("Back");
		buttonPrev.setOnClickListener(this);
		buttonNext = footerView.findViewById(android.R.id.button2);

		// TODO figure out tinting for lollipop or drop support
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			buttonNext.setCompoundDrawableTintList(buttonNext.getTextColors());
		}

		buttonNext.setEnabled(false);
		buttonNext.setOnClickListener(this);
		footer.setVisibility(View.VISIBLE);
		lc = new LoadingViewController(this, mainFrame, "");
		loadFriends();
	}

	@Override
	public void onBackPressed() {
		if (isStep2) {
			showRecipientList();
		} else {
			super.onBackPressed();
		}
	}

	@Override
	public void onClick(View v) {
		if (v == buttonPrev) {
			onBackPressed();
		} else if (v == buttonNext) {
			if (isStep2) {
				if (adapter == null) {
					return;
				}

				GiftCatalogEntry payload = new GiftCatalogEntry();
				payload.offerId = catalogEntry.offerId;
				payload.currency = catalogEntry.prices[0].currencyType;
				payload.currencySubType = catalogEntry.prices[0].currencySubType;
				payload.expectedPrice = catalogEntry.prices[0].basePrice;
				payload.receiverAccountIds = adapter.checkedSet.toArray(new String[0]);
				payload.giftWrapTemplateId = "";
				payload.personalMessage = giftMessage.getText().toString();
				final Call<FortMcpResponse> call = getThisApplication().fortnitePublicService.mcp(
						"ClientQuestLogin",
						PreferenceManager.getDefaultSharedPreferences(this).getString("epic_account_id", ""),
						"athena",
						getThisApplication().profileManager.getRvn("athena"),
						new JsonObject());
				/* TODO work on this quick plz
				new Thread("Client Quest Login Worker") {
					@Override
					public void run() {
						try {
							Response<FortMcpResponse> response = call.execute();

							if (response.isSuccessful()) {
								getThisApplication().profileManager.executeProfileChanges(response.body());
							} else {
								Utils.dialogError(GiftActivity.this, EpicError.parse(response).getDisplayText());
							}
						} catch (IOException e) {
							Utils.throwableDialog(GiftActivity.this, e);
						}
					}
				}.start();*/
			} else {
				showGiftLayout();
			}
		}
	}

	private void loadFriends() {
		lc.loading();
		final Call<Friend[]> callFriends = getThisApplication().friendsPublicService.friends(PreferenceManager.getDefaultSharedPreferences(this).getString("epic_account_id", ""), false);
		new Thread("Query Friends Worker") {
			@Override
			public void run() {
				try {
					final Response<Friend[]> response = callFriends.execute();

					if (response.isSuccessful()) {
						friendsData = Arrays.asList(response.body());
						Set<String> criteria = new HashSet<>();

						for (Friend friend : friendsData) {
							criteria.add(friend.accountId);
						}

						GameProfileRepository.findProfileDataByIds(GiftActivity.this, criteria, new ProfileLookupCallback() {
							@Override
							public void onProfileLookupSucceeded(GameProfile profile) {
								String displayName = profile.getDisplayName();

								if (displayName == null && profile.getExternalAuths() != null && !profile.getExternalAuths().isEmpty()) {
									displayName = profile.getExternalAuths().entrySet().iterator().next().getValue().externalDisplayName;
								}

								if (displayName == null) {
									displayName = "???";
								}

								displayNameMap.put(profile.getId(), displayName);
							}

							@Override
							public void onProfileLookupFailed(GameProfile profile, Exception exception) {
								displayNameMap.put(profile.getId(), profile.getId());
							}
						});
						Collections.sort(friendsData, new Comparator<Friend>() {
							@Override
							public int compare(Friend o1, Friend o2) {
								return displayNameMap.get(o1.accountId).compareToIgnoreCase(displayNameMap.get(o2.accountId));
							}
						});
						runOnUiThread(new Runnable() {
							@Override
							public void run() {
								showRecipientList();
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

	private void showRecipientList() {
		isStep2 = false;

		if (adapter == null) {
			recipientList.setAdapter(adapter = new FriendsAdapter(this));
		}

		recipientList.setVisibility(View.VISIBLE);
		giftLayout.setVisibility(View.GONE);
		setTitle("Select Recipients");
		recipientSelectionUpdated();
		buttonNext.setText("Continue");
		lc.content();
	}

	private void showGiftLayout() {
		isStep2 = true;
		giftMessage.setHorizontallyScrolling(false);
		giftMessage.setMaxLines(Integer.MAX_VALUE);
		recipientList.setVisibility(View.GONE);
		giftLayout.setVisibility(View.VISIBLE);
		setTitle("Personalize the Gift");
		getSupportActionBar().setSubtitle(null);
		buttonNext.setText("Send");
	}

	private void recipientSelectionUpdated() {
		if (adapter != null) {
			getSupportActionBar().setSubtitle(String.format("%,d of %,d Selected", adapter.checkedSet.size(), MAX_RECIPIENTS));
			recipientSummary.setText(adapter.checkedSet.isEmpty() ? "Please select at least one recipient" : COMMA_JOINER.join(FluentIterable.from(adapter.checkedSet).transform(new Function<String, String>() {
				@Override
				public String apply(String input) {
					String displayName = displayNameMap.get(input);
					return displayName == null ? "??" : displayName;
				}
			}).toSortedList(new Comparator<String>() {
				@Override
				public int compare(String o1, String o2) {
					return o1.compareToIgnoreCase(o2);
				}
			})));
			vbucksTotal.setText(String.format("%,d", adapter.checkedSet.size() * catalogEntry.prices[0].basePrice));
			buttonNext.setEnabled(!adapter.checkedSet.isEmpty());
		}
	}

	private static class FriendsAdapter extends RecyclerView.Adapter<FriendsAdapter.FriendsViewHolder> {
		private final GiftActivity activity;
		private Set<String> checkedSet = new HashSet<>();

		public FriendsAdapter(GiftActivity activity) {
			this.activity = activity;
		}

		@NonNull
		@Override
		public FriendsAdapter.FriendsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
			return new FriendsViewHolder(activity.getLayoutInflater().inflate(R.layout.gift_friend_entry, parent, false));
		}

		@Override
		public void onBindViewHolder(@NonNull final FriendsAdapter.FriendsViewHolder holder, int position) {
			final String friendId = activity.friendsData.get(position).accountId;
			holder.checkbox.setOnCheckedChangeListener(null);
			holder.checkbox.setChecked(checkedSet.contains(friendId));
			holder.text1.setText(activity.displayNameMap.containsKey(friendId) ? activity.displayNameMap.get(friendId) : friendId);
			holder.itemView.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					boolean newState = !holder.checkbox.isChecked();

					if (newState && checkedSet.size() < MAX_RECIPIENTS) {
						holder.checkbox.setChecked(true);
					} else {
						holder.checkbox.setChecked(false);
					}
				}
			});
			holder.checkbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
				@Override
				public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
					if (isChecked) {
						if (checkedSet.size() < MAX_RECIPIENTS) {
							checkedSet.add(friendId);
						} else {
							holder.checkbox.setChecked(false);
						}
					} else {
						checkedSet.remove(friendId);
					}

					activity.recipientSelectionUpdated();
				}
			});
		}

		@Override
		public int getItemCount() {
			return activity.friendsData.size();
		}

		private static class FriendsViewHolder extends RecyclerView.ViewHolder {
			CompoundButton checkbox;
			TextView text1;

			FriendsViewHolder(View itemView) {
				super(itemView);
				checkbox = itemView.findViewById(android.R.id.checkbox);
				text1 = itemView.findViewById(android.R.id.text1);
			}
		}
	}
}
