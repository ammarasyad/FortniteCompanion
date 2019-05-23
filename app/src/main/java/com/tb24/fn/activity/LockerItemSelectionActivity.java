package com.tb24.fn.activity;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v7.util.DiffUtil;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.common.base.Predicate;
import com.google.common.collect.ComparisonChain;
import com.google.common.collect.FluentIterable;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.tb24.fn.R;
import com.tb24.fn.Registry;
import com.tb24.fn.event.ProfileUpdatedEvent;
import com.tb24.fn.model.AthenaProfileAttributes;
import com.tb24.fn.model.FortItemStack;
import com.tb24.fn.model.FortMcpProfile;
import com.tb24.fn.model.FortMcpResponse;
import com.tb24.fn.model.assetdata.AthenaPetCarrierItemDefinition;
import com.tb24.fn.model.assetdata.FortItemDefinition;
import com.tb24.fn.model.command.MarkItemSeen;
import com.tb24.fn.model.command.SetItemFavoriteStatusBatch;
import com.tb24.fn.util.EFortRarity;
import com.tb24.fn.util.ItemUtils;
import com.tb24.fn.util.JsonUtils;
import com.tb24.fn.util.LoadingViewController;
import com.tb24.fn.util.Utils;
import com.tb24.fn.view.ForcedMarqueeTextView;

import org.checkerframework.checker.nullness.compatqual.NullableDecl;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import retrofit2.Call;
import retrofit2.Response;

public class LockerItemSelectionActivity extends BaseActivity implements AdapterView.OnItemSelectedListener {
	protected static final String ARG_FILTER_INDEX = "filter_index";
	private static final ItemFilter ALL_FILTER = new ItemFilter(new Predicate<FortItemStack>() {
		@Override
		public boolean apply(@NullableDecl FortItemStack input) {
			return true;
		}
	}, R.string.locker_filter_all, null);
	private static final FortItemStack EMPTY_ITEM = new FortItemStack("", 1);

	private ViewGroup mPinnedHeaderFrameLayout;
	private ViewGroup mPinnedFooterFrameLayout;
	private ViewGroup mSpinnerHeader;
	private Spinner mFilterSpinner;
	private ArrayAdapter<String> mFilterAdapter;
	private RecyclerView list;
	private LockerAdapter adapter;
	private LoadingViewController lc;
	private GridLayoutManager layout;

	private String itemTypeFilter;
	private List<ItemFilter> itemFilters;
	private int mFilterIndex;
	private String selectedItem;

	private FortMcpProfile profileData;
	private FortItemStack[] referenceData;
	private Map<String, Boolean> favoriteChangeMap = new HashMap<>();
	private Set<String> seenChangeSet = new HashSet<>();

	public static String getItemCategoryFilterById(int id) {
		switch (id) {
			case R.id.locker_slot_character:
				return "AthenaCharacter";
			case R.id.locker_slot_backpack:
				return "AthenaBackpack";
			case R.id.locker_slot_pickaxe:
				return "AthenaPickaxe";
			case R.id.locker_slot_glider:
				return "AthenaGlider";
			case R.id.locker_slot_skydivecontrail:
				return "AthenaSkyDiveContrail";
			case R.id.locker_slot_emote1:
			case R.id.locker_slot_emote2:
			case R.id.locker_slot_emote3:
			case R.id.locker_slot_emote4:
			case R.id.locker_slot_emote5:
			case R.id.locker_slot_emote6:
				return "AthenaDance";
			case R.id.locker_slot_wrap1:
			case R.id.locker_slot_wrap2:
			case R.id.locker_slot_wrap3:
			case R.id.locker_slot_wrap4:
			case R.id.locker_slot_wrap5:
			case R.id.locker_slot_wrap6:
			case R.id.locker_slot_wrap7:
				return "AthenaItemWrap";
			case R.id.locker_slot_musicpack:
				return "AthenaMusicPack";
			case R.id.locker_slot_loadingscreen:
				return "AthenaLoadingScreen";
			default:
				return null;
		}
	}

	private static List<ItemFilter> getItemFilterListByItemCategory(final Registry registry, String filterId) {
		List<ItemFilter> out = new ArrayList<>();
		out.add(ALL_FILTER);
		out.add(new ItemFilter(new Predicate<FortItemStack>() {
			@Override
			public boolean apply(@NullableDecl FortItemStack input) {
				if (input == null) {
					return false;
				}

				return input.attributes != null && !JsonUtils.getBooleanOr("item_seen", input.attributes, true);
			}
		}, R.string.locker_filter_new, null));
		out.add(new ItemFilter(new Predicate<FortItemStack>() {
			@Override
			public boolean apply(@NullableDecl FortItemStack input) {
				if (input == null) {
					return false;
				}

				return input.attributes != null && JsonUtils.getBooleanOr("favorite", input.attributes, false);
			}
		}, R.string.locker_filter_favorite, null));
		ItemFilter filterStyles = new ItemFilter(new Predicate<FortItemStack>() {
			@Override
			public boolean apply(@NullableDecl FortItemStack input) {
				if (input == null) {
					return false;
				}

				FortItemDefinition defData = input.getDefData();
				return defData != null && defData.ItemVariants != null;
			}
		}, R.string.locker_filter_styles, null);
		ItemFilter filterReactive = new ItemFilter(new Predicate<FortItemStack>() {
			@Override
			public boolean apply(@NullableDecl FortItemStack input) {
				if (input == null) {
					return false;
				}

				FortItemDefinition defData = input.getDefData();
				return defData != null && defData.GameplayTags != null && Arrays.binarySearch(defData.GameplayTags.gameplay_tags, "Cosmetics.UserFacingFlags.Reactive") >= 0;
			}
		}, R.string.locker_filter_reactive, null);

		switch (filterId) {
			case "AthenaCharacter":
				out.add(filterStyles);
				out.add(filterReactive);
				break;
			case "AthenaBackpack":
				out.add(filterStyles);
				out.add(filterReactive);
				out.add(new ItemFilter(new Predicate<FortItemStack>() {
					@Override
					public boolean apply(@NullableDecl FortItemStack input) {
						if (input == null) {
							return false;
						}

						return input.getDefData() instanceof AthenaPetCarrierItemDefinition;
					}
				}, R.string.locker_filter_pets, null));
				break;
			case "AthenaPickaxe":
				out.add(filterStyles);
				out.add(filterReactive);
				break;
			case "AthenaGlider":
				out.add(filterStyles);
				out.add(filterReactive);
				break;
			case "AthenaSkyDiveContrail":
				break;
			case "AthenaDance":
				out.add(new ItemFilter(new Predicate<FortItemStack>() {
					@Override
					public boolean apply(@NullableDecl FortItemStack input) {
						if (input == null) {
							return false;
						}

						FortItemDefinition defData = input.getDefData();
						return defData != null && defData.GameplayTags != null && Arrays.binarySearch(defData.GameplayTags.gameplay_tags, "Cosmetics.EmoteType.Dance") >= 0;
					}
				}, R.string.locker_filter_dances, null));
				out.add(new ItemFilter(new Predicate<FortItemStack>() {
					@Override
					public boolean apply(@NullableDecl FortItemStack input) {
						if (input == null) {
							return false;
						}

						FortItemDefinition defData = input.getDefData();
						return defData != null && defData.GameplayTags != null && Arrays.binarySearch(defData.GameplayTags.gameplay_tags, "Cosmetics.EmoteType.Emoji") >= 0;
					}
				}, R.string.locker_filter_emoticons, null));
				out.add(new ItemFilter(new Predicate<FortItemStack>() {
					@Override
					public boolean apply(@NullableDecl FortItemStack input) {
						if (input == null) {
							return false;
						}

						FortItemDefinition defData = input.getDefData();
						return defData != null && defData.GameplayTags != null && Arrays.binarySearch(defData.GameplayTags.gameplay_tags, "Cosmetics.EmoteType.Spray") >= 0;
					}
				}, R.string.locker_filter_sprays, null));
				out.add(new ItemFilter(new Predicate<FortItemStack>() {
					@Override
					public boolean apply(@NullableDecl FortItemStack input) {
						if (input == null) {
							return false;
						}

						FortItemDefinition defData = input.getDefData();
						return defData != null && defData.GameplayTags != null && Arrays.binarySearch(defData.GameplayTags.gameplay_tags, "Cosmetics.EmoteType.Toy") >= 0;
					}
				}, R.string.locker_filter_toys, null));
				break;
			case "AthenaItemWrap":
				out.add(new ItemFilter(new Predicate<FortItemStack>() {
					@Override
					public boolean apply(@NullableDecl FortItemStack input) {
						if (input == null) {
							return false;
						}

						FortItemDefinition defData = input.getDefData();
						return defData != null && defData.GameplayTags != null && Arrays.binarySearch(defData.GameplayTags.gameplay_tags, "Cosmetics.UserFacingFlags.Wrap.Animated") >= 0;
					}
				}, R.string.locker_filter_animated, null));
				break;
			case "AthenaMusicPack":
				break;
			case "AthenaLoadingScreen":
				out.add(new ItemFilter(new Predicate<FortItemStack>() {
					@Override
					public boolean apply(@NullableDecl FortItemStack input) {
						if (input == null) {
							return false;
						}

						FortItemDefinition defData = input.getDefData();
						return defData != null && defData.GameplayTags != null && Arrays.binarySearch(defData.GameplayTags.gameplay_tags, "Cosmetics.UserFacingFlags.LoadingScreen.Animated") >= 0;
					}
				}, R.string.locker_filter_animated, null));
				break;
		}

		return out;
	}

	private static FortItemStack getRandomItemByCategory(String filterId) {
		String name;

		switch (filterId) {
			case "AthenaCharacter":
				name = "CID_Random";
				break;
			case "AthenaBackpack":
				name = "BID_Random";
				break;
			case "AthenaPickaxe":
				name = "Pickaxe_Random";
				break;
			case "AthenaGlider":
				name = "Glider_Random";
				break;
			case "AthenaSkyDiveContrail":
				name = "Trails_Random";
				break;
			case "AthenaItemWrap":
				name = "Wrap_Random";
				break;
			case "AthenaMusicPack":
				name = "MusicPack_Random";
				break;
			case "AthenaLoadingScreen":
				name = "LSID_Random";
				break;
			default:
				return null;
		}

		return new FortItemStack(filterId, name.toLowerCase(), 1);
	}

	private static boolean slotCanBeBlank(String filterId) {
		if (filterId == null) {
			return true;
		}

		switch (filterId) {
			case "AthenaCharacter":
				return true;
			case "AthenaBackpack":
				return true;
			case "AthenaPickaxe":
				return false;
			case "AthenaGlider":
				return false;
			case "AthenaSkyDiveContrail":
				return true;
			case "AthenaItemWrap":
				return true;
			case "AthenaMusicPack":
				return true;
			case "AthenaLoadingScreen":
				return true;
			default:
				return true;
		}
	}

	public static String getTitleTextById(int id) {
		switch (id) {
			case R.id.locker_slot_character:
				return "Outfit";
			case R.id.locker_slot_backpack:
				return "Back Bling";
			case R.id.locker_slot_pickaxe:
				return "Harvesting Tool";
			case R.id.locker_slot_glider:
				return "Glider";
			case R.id.locker_slot_skydivecontrail:
				return "Contrail";
			case R.id.locker_slot_emote1:
				return "Emote 1";
			case R.id.locker_slot_emote2:
				return "Emote 2";
			case R.id.locker_slot_emote3:
				return "Emote 3";
			case R.id.locker_slot_emote4:
				return "Emote 4";
			case R.id.locker_slot_emote5:
				return "Emote 5";
			case R.id.locker_slot_emote6:
				return "Emote 6";
			case R.id.locker_slot_wrap1:
				return "Vehicle Wrap";
			case R.id.locker_slot_wrap2:
				return "Assault Rifle Wrap";
			case R.id.locker_slot_wrap3:
				return "Shotgun Wrap";
			case R.id.locker_slot_wrap4:
				return "SMG Wrap";
			case R.id.locker_slot_wrap5:
				return "Sniper Wrap";
			case R.id.locker_slot_wrap6:
				return "Pistol Wrap";
			case R.id.locker_slot_wrap7:
				return "Misc Wrap";
			case R.id.locker_slot_banner:
				return "Banner";
			case R.id.locker_slot_musicpack:
				return "Music";
			case R.id.locker_slot_loadingscreen:
				return "Loading Screen";
			default:
				return "??";
		}
	}

	public static String getRowTitleTextById(int id) {
		switch (id) {
			case R.id.locker_slot_character:
			case R.id.locker_slot_backpack:
			case R.id.locker_slot_pickaxe:
			case R.id.locker_slot_glider:
			case R.id.locker_slot_skydivecontrail:
				return "Gear";
			case R.id.locker_slot_emote1:
			case R.id.locker_slot_emote2:
			case R.id.locker_slot_emote3:
			case R.id.locker_slot_emote4:
			case R.id.locker_slot_emote5:
			case R.id.locker_slot_emote6:
				return "Emote";
			case R.id.locker_slot_wrap1:
			case R.id.locker_slot_wrap2:
			case R.id.locker_slot_wrap3:
			case R.id.locker_slot_wrap4:
			case R.id.locker_slot_wrap5:
			case R.id.locker_slot_wrap6:
			case R.id.locker_slot_wrap7:
				return "Wrap";
			case R.id.locker_slot_banner:
			case R.id.locker_slot_musicpack:
			case R.id.locker_slot_loadingscreen:
				return "Account";
			default:
				return "??";
		}
	}

	private static String getSelectedItemFromProfileById(AthenaProfileAttributes attributes, int id) {
		switch (id) {
			case R.id.locker_slot_character:
				return attributes.favorite_character;
			case R.id.locker_slot_backpack:
				return attributes.favorite_backpack;
			case R.id.locker_slot_pickaxe:
				return attributes.favorite_pickaxe;
			case R.id.locker_slot_glider:
				return attributes.favorite_glider;
			case R.id.locker_slot_skydivecontrail:
				return attributes.favorite_skydivecontrail;
			case R.id.locker_slot_emote1:
				return 0 > attributes.favorite_dance.length - 1 ? "" : attributes.favorite_dance[0];
			case R.id.locker_slot_emote2:
				return 1 > attributes.favorite_dance.length - 1 ? "" : attributes.favorite_dance[1];
			case R.id.locker_slot_emote3:
				return 2 > attributes.favorite_dance.length - 1 ? "" : attributes.favorite_dance[2];
			case R.id.locker_slot_emote4:
				return 3 > attributes.favorite_dance.length - 1 ? "" : attributes.favorite_dance[3];
			case R.id.locker_slot_emote5:
				return 4 > attributes.favorite_dance.length - 1 ? "" : attributes.favorite_dance[4];
			case R.id.locker_slot_emote6:
				return 5 > attributes.favorite_dance.length - 1 ? "" : attributes.favorite_dance[5];
			case R.id.locker_slot_wrap1:
				return 0 > attributes.favorite_itemwraps.length - 1 ? "" : attributes.favorite_itemwraps[0];
			case R.id.locker_slot_wrap2:
				return 1 > attributes.favorite_itemwraps.length - 1 ? "" : attributes.favorite_itemwraps[1];
			case R.id.locker_slot_wrap3:
				return 2 > attributes.favorite_itemwraps.length - 1 ? "" : attributes.favorite_itemwraps[2];
			case R.id.locker_slot_wrap4:
				return 3 > attributes.favorite_itemwraps.length - 1 ? "" : attributes.favorite_itemwraps[3];
			case R.id.locker_slot_wrap5:
				return 4 > attributes.favorite_itemwraps.length - 1 ? "" : attributes.favorite_itemwraps[4];
			case R.id.locker_slot_wrap6:
				return 5 > attributes.favorite_itemwraps.length - 1 ? "" : attributes.favorite_itemwraps[5];
			case R.id.locker_slot_wrap7:
				return 6 > attributes.favorite_itemwraps.length - 1 ? "" : attributes.favorite_itemwraps[6];
			case R.id.locker_slot_musicpack:
				return attributes.favorite_musicpack;
			case R.id.locker_slot_loadingscreen:
				return attributes.favorite_loadingscreen;
			default:
				return null;
		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.common_loadable_recycler_view);
		setupActionBar();
		mPinnedHeaderFrameLayout = findViewById(R.id.pinned_header);
		mPinnedFooterFrameLayout = findViewById(R.id.pinned_footer);

		if (getIntent().hasExtra("a")) {
			int id = getIntent().getIntExtra("a", 0);
			itemTypeFilter = getItemCategoryFilterById(id);
			itemFilters = getItemFilterListByItemCategory(getThisApplication().itemRegistry, itemTypeFilter);

			if (getActionBar() != null) {
//				getActionBar().setTitle("Selecting");
				getActionBar().setTitle("Viewing");
				getActionBar().setSubtitle(getTitleTextById(id));
			}

			mSpinnerHeader = (ViewGroup) setPinnedHeaderView(R.layout.apps_filter_spinner);
			mFilterSpinner = mSpinnerHeader.findViewById(R.id.filter_spinner);
			mFilterAdapter = new ArrayAdapter<>(mFilterSpinner.getContext(), R.layout.filter_spinner_item);
			mFilterAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

			for (ItemFilter filter : itemFilters) {
				mFilterAdapter.add(getString(filter.name));
			}

			mFilterSpinner.setAdapter(mFilterAdapter);
			mFilterSpinner.setSelection(mFilterIndex = savedInstanceState != null ? savedInstanceState.getInt(ARG_FILTER_INDEX) : 0);
			mFilterSpinner.setOnItemSelectedListener(this);

			if (getResources().getConfiguration().keyboard == Configuration.KEYBOARD_QWERTY) {
				TextView footer = new ForcedMarqueeTextView(this);
				footer.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
				footer.setEllipsize(TextUtils.TruncateAt.MARQUEE);
				footer.setGravity(Gravity.CENTER);
				footer.setMarqueeRepeatLimit(-1);
				footer.setSingleLine();
				footer.setText("(F) Toggle Favorite \u2022 ([) Previous Filter \u2022 (]) Next Filter");
				setPinnedFooterView(footer);
			}
		}

		list = findViewById(R.id.main_recycler_view);
		int p = (int) Utils.dp(getResources(), 4);
		list.setPadding(p, p, p, p);
		list.setClipToPadding(false);

		if (!PreferenceManager.getDefaultSharedPreferences(this).getBoolean("locker_item_animations", true)) {
			list.setItemAnimator(null);
		}

		list.post(new Runnable() {
			@Override
			public void run() {
				layout = new GridLayoutManager(LockerItemSelectionActivity.this, (int) (list.getWidth() / Utils.dp(getResources(), 66 + 8)));
				list.setLayoutManager(layout);
			}
		});
		lc = new LoadingViewController(this, list, "") {
			@Override
			public boolean shouldShowEmpty() {
				return adapter.data.isEmpty();
			}
		};
		profileData = getThisApplication().profileManager.profileData.get("athena");
		refreshUi();
		getThisApplication().eventBus.register(this);
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putInt(ARG_FILTER_INDEX, mFilterIndex);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		getThisApplication().eventBus.unregister(this);

		if (!favoriteChangeMap.isEmpty()) {
			SetItemFavoriteStatusBatch payload = new SetItemFavoriteStatusBatch();
			payload.itemIds = new String[favoriteChangeMap.size()];
			payload.itemFavStatus = new Boolean[favoriteChangeMap.size()];
			int i = 0;

			for (Map.Entry<String, Boolean> entry : favoriteChangeMap.entrySet()) {
				payload.itemIds[i] = entry.getKey();
				payload.itemFavStatus[i] = entry.getValue();
				++i;
			}

			final Call<FortMcpResponse> call = getThisApplication().fortnitePublicService.mcp("SetItemFavoriteStatusBatch", PreferenceManager.getDefaultSharedPreferences(this).getString("epic_account_id", ""), "athena", -1, payload);
			new Thread("Set Favorite Worker") {
				@Override
				public void run() {
					try {
						Response<FortMcpResponse> response = call.execute();

						if (response.isSuccessful()) {
							getThisApplication().profileManager.executeProfileChanges(response.body());
						}
					} catch (IOException ignored) {
					}
				}
			}.start();
		}

		if (!seenChangeSet.isEmpty()) {
			MarkItemSeen payload = new MarkItemSeen();
			payload.itemIds = seenChangeSet.toArray(new String[]{});
			final Call<FortMcpResponse> call = getThisApplication().fortnitePublicService.mcp("MarkItemSeen", PreferenceManager.getDefaultSharedPreferences(this).getString("epic_account_id", ""), "athena", -1, payload);
			new Thread("Mark Item Seen Worker") {
				@Override
				public void run() {
					try {
						Response<FortMcpResponse> response = call.execute();

						if (response.isSuccessful()) {
							getThisApplication().profileManager.executeProfileChanges(response.body());
						}
					} catch (IOException ignored) {
					}
				}
			}.start();
		}
	}

	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_F || keyCode == KeyEvent.KEYCODE_BUTTON_THUMBR) {
			if (adapter.focusedItem != null) {
				adapter.toggleFavoriteItem(adapter.focusedItem);
			}

			return true;
		} else if (itemTypeFilter != null) {
			if (keyCode == KeyEvent.KEYCODE_LEFT_BRACKET || keyCode == KeyEvent.KEYCODE_BUTTON_L2) {
				mFilterSpinner.setSelection(Math.max(0, mFilterIndex - 1));
				return true;
			} else if (keyCode == KeyEvent.KEYCODE_RIGHT_BRACKET || keyCode == KeyEvent.KEYCODE_BUTTON_R2) {
				mFilterSpinner.setSelection(Math.min(mFilterIndex + 1, itemFilters.size() - 1));
				return true;
			}
		}

		return super.onKeyUp(keyCode, event);
	}

	@Override
	public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
		mFilterIndex = position;
		refreshUi();
	}

	@Override
	public void onNothingSelected(AdapterView<?> parent) {
		// Select something.
		mFilterSpinner.setSelection(0);
	}

	@Subscribe(threadMode = ThreadMode.MAIN)
	public void onProfileUpdated(ProfileUpdatedEvent event) {
		if (event.profileId.equals("athena")) {
			profileData = event.profileObj;
			refreshUi();
		}
	}

	private void refreshUi() {
		if (profileData == null) {
			lc.loading();
			return;
		}

		FluentIterable<FortItemStack> chain = FluentIterable.from(profileData.items.values());
		FortItemStack randomItem = null;

		if (getIntent().hasExtra("a")) {
			int id = getIntent().getIntExtra("a", 0);
			String selected = getSelectedItemFromProfileById((AthenaProfileAttributes) profileData.stats.attributesObj, id);
			selectedItem = selected == null ? null : selected.contains("-") ? profileData.items.get(selected).templateId : selected;

			if (itemTypeFilter != null) {
				ItemFilter itemFilter = itemFilters.get(mFilterIndex);
				chain = chain.filter(new Predicate<FortItemStack>() {
					@Override
					public boolean apply(@NullableDecl FortItemStack input) {
						return input != null && input.getIdCategory().equals(itemTypeFilter);
					}
				}).filter(itemFilter.predicate);

				if (mFilterIndex == 0) {
					randomItem = getRandomItemByCategory(itemTypeFilter);
				}

				lc.mEmptyView.setText(String.format("You have no %s items.", getString(itemFilter.name).toUpperCase()));
			}
		}

		List<FortItemStack> data = new ArrayList<>();

		if (itemTypeFilter != null && mFilterIndex == 0 && slotCanBeBlank(itemTypeFilter)) {
			data.add(EMPTY_ITEM);
		}

		data.addAll(chain.toSortedList(new Comparator<FortItemStack>() {
			@Override
			public int compare(FortItemStack o1, FortItemStack o2) {
				FortItemDefinition defData = o1.getDefData();
				FortItemDefinition defData1 = o2.getDefData();
				EFortRarity rarity1 = EFortRarity.COMMON;
				EFortRarity rarity2 = EFortRarity.COMMON;

				if (defData != null) {
					rarity1 = EFortRarity.from(defData.Rarity);
				}

				if (defData1 != null) {
					rarity2 = EFortRarity.from(defData1.Rarity);
				}

				return ComparisonChain.start().compareTrueFirst(JsonUtils.getBooleanOr("favorite", o1.attributes, false), JsonUtils.getBooleanOr("favorite", o2.attributes, false)).compare(o1.getIdCategory(), o2.getIdCategory()).compare(rarity2, rarity1).compare(o1.getIdName(), o2.getIdName()).result();
			}
		}));

		if (randomItem != null) {
			data.add(randomItem);
		}

		if (adapter == null) {
			list.setAdapter(adapter = new LockerAdapter(this, data));
		} else {
			adapter.update(data);
		}

		referenceData = Utils.cloneObjectUsingJson(data.toArray(new FortItemStack[0]), FortItemStack[].class);
		lc.content();
	}

	public View setPinnedHeaderView(int layoutResId) {
		final LayoutInflater inflater = getLayoutInflater();
		final View pinnedHeader = inflater.inflate(layoutResId, mPinnedHeaderFrameLayout, false);
		setPinnedHeaderView(pinnedHeader);
		return pinnedHeader;
	}

	public void setPinnedHeaderView(View pinnedHeader) {
		mPinnedHeaderFrameLayout.addView(pinnedHeader);
		mPinnedHeaderFrameLayout.setVisibility(View.VISIBLE);
	}

	public View setPinnedFooterView(int layoutResId) {
		final LayoutInflater inflater = getLayoutInflater();
		final View pinnedFooter = inflater.inflate(layoutResId, mPinnedFooterFrameLayout, false);
		setPinnedFooterView(pinnedFooter);
		return pinnedFooter;
	}

	public void setPinnedFooterView(View pinnedFooter) {
		mPinnedFooterFrameLayout.addView(pinnedFooter);
		mPinnedFooterFrameLayout.setVisibility(View.VISIBLE);
	}

	private String findItemId(String templateId) {
		for (Map.Entry<String, FortItemStack> entry : profileData.items.entrySet()) {
			if (entry.getValue().templateId.equals(templateId)) {
				return entry.getKey();
			}
		}

		return null;
	}

	private static class LockerAdapter extends RecyclerView.Adapter<LockerAdapter.LockerViewHolder> {
		private final LockerItemSelectionActivity activity;
		private List<FortItemStack> data;
		private Toast toast;
		private ViewGroup toastView;
		private FortItemStack focusedItem;

		public LockerAdapter(LockerItemSelectionActivity activity, List<FortItemStack> data) {
			this.activity = activity;
			this.data = data;
		}

		@NonNull
		@Override
		public LockerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
			LockerViewHolder holder = new LockerViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.slot_view_encased, parent, false));
			holder.favorite.setImageBitmap(Utils.loadTga(activity, "/Game/UI/Foundation/Textures/Icons/Locker/T_Icon_FavoriteTab_64.T_Icon_FavoriteTab_64"));
			return holder;
		}

		@Override
		public void onBindViewHolder(@NonNull LockerViewHolder holder, int position) {
			final FortItemStack item = data.get(position);
			holder.itemView.setSelected(item.templateId.equals(activity.selectedItem));

			if (item.attributes != null) {
				holder.newIcon.setVisibility(JsonUtils.getBooleanOr("item_seen", item.attributes, false) ? View.INVISIBLE : View.VISIBLE);
				holder.favorite.setVisibility(JsonUtils.getBooleanOr("favorite", item.attributes, false) ? View.VISIBLE : View.INVISIBLE);
			} else {
				holder.newIcon.setVisibility(View.GONE);
				holder.favorite.setVisibility(View.GONE);
			}

			if (item == EMPTY_ITEM) {
				holder.innerSlotView.setBackground(null);
				holder.displayImage.setImageResource(R.drawable.ic_close_black_24dp);
				holder.itemName.setText(null);
			} else {
				final JsonElement json = activity.getThisApplication().itemRegistry.get(item.templateId);
				Bitmap bitmap = null;

				if (json != null) {
					JsonObject jsonObject = json.getAsJsonArray().get(0).getAsJsonObject();
					bitmap = ItemUtils.getBitmapImageFromItemStackData(activity, item, jsonObject);
					holder.innerSlotView.setBackground(ItemUtils.rarityBgSlot(activity, ItemUtils.getRarity(jsonObject)));
				} else {
					holder.innerSlotView.setBackgroundResource(R.drawable.bg_common);
				}

				holder.displayImage.setImageBitmap(bitmap);
				holder.itemName.setText(bitmap == null ? item.templateId : null);
			}

			holder.quantity.setVisibility(item.quantity > 1 ? View.VISIBLE : View.GONE);
			holder.quantity.setText(String.valueOf(item.quantity));
			holder.innerSlotView.setFocusableInTouchMode(true);
			holder.innerSlotView.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					if (activity.itemTypeFilter == null) {
						return;
					}

					// TODO apply item
					Toast.makeText(activity, "Applying locker item not available yet", Toast.LENGTH_SHORT).show();
//					EquipBattleRoyaleCustomization payload = new EquipBattleRoyaleCustomization();
//					payload.slotName = EquipBattleRoyaleCustomization.ECustomizationSlot.Dance;
//					payload.WHATISTHIS = activity.findItemId(item.templateId);
//					Call<FortMcpResponse> call = ...;
				}
			});
			holder.innerSlotView.setOnLongClickListener(item == EMPTY_ITEM ? null : new View.OnLongClickListener() {
				@Override
				public boolean onLongClick(View v) {
					ViewGroup viewGroup = null;

					if (item.getDefData() != null) {
						viewGroup = (ViewGroup) activity.getLayoutInflater().inflate(R.layout.fort_item_detail_box, null);
						ItemUtils.populateItemDetailBox(viewGroup, item);
					}

					AlertDialog.Builder builder = new AlertDialog.Builder(activity)
							.setCustomTitle(viewGroup)
							.setPositiveButton("Close", null);

					if (item.attributes != null && activity.itemTypeFilter != null) {
						String favorite = JsonUtils.getBooleanOr("favorite", item.attributes, false) ? "Unfavorite" : "Favorite";
						CharSequence[] items = new CharSequence[]{favorite};

						if (!JsonUtils.getBooleanOr("item_seen", item.attributes, true)) {
							items = new CharSequence[]{favorite, "Mark as seen"};
						}

						builder.setItems(items, new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								if (which == 0) {
									toggleFavoriteItem(item);
								} else if (which == 1) {
									if (item.attributes.has("item_seen")) {
										item.attributes.addProperty("item_seen", true);
										activity.seenChangeSet.add(activity.findItemId(item.templateId));
										activity.refreshUi();
									}
								}
							}
						});
					}

					if (item.getDefData() == null) {
						builder.setTitle(item.templateId);
					}

					if (item.getDefData() != null && item.getIdCategory().equals("Quest")) {
						View inflate = activity.getLayoutInflater().inflate(R.layout.quest_entry, null);
						ChallengeBundleActivity.populateQuestView(activity, inflate, item);
						builder.setView(inflate);
					} else if (item.getDefData() != null && item.getIdCategory().equals("ChallengeBundle")) {
						builder.setNeutralButton("More Details", new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								Intent intent = new Intent(activity, ChallengeBundleActivity.class);
								intent.putExtra("a", item.templateId);
								activity.startActivity(intent);
							}
						});
					}

					builder.show();
					return true;
				}
			});
			holder.innerSlotView.setOnFocusChangeListener(new View.OnFocusChangeListener() {
				@Override
				public void onFocusChange(View v, boolean hasFocus) {
					if (hasFocus) {
						focusedItem = item;
						showItemToast(item);
					}
				}
			});
		}

		private void toggleFavoriteItem(FortItemStack item) {
			if (activity.itemTypeFilter != null && item != EMPTY_ITEM && item.attributes != null && item.attributes.has("favorite")) {
				boolean newFavValue = !item.attributes.get("favorite").getAsBoolean();
				item.attributes.addProperty("favorite", newFavValue);
				String itemId = activity.findItemId(item.templateId);

				if (itemId != null) {
					activity.favoriteChangeMap.put(itemId, newFavValue);
				}

				activity.refreshUi();
			}
		}

		private void showItemToast(FortItemStack item) {
			if (toast != null) {
				toast.cancel();
			}

			if (item == EMPTY_ITEM || item.getDefData() == null) {
				return;
			}

			if (toastView == null) {
				toastView = (ViewGroup) activity.getLayoutInflater().inflate(R.layout.fort_item_detail_box, null);
			}

			ItemUtils.populateItemDetailBox(toastView, item);
			toast = new Toast(activity);
			int off = 0;

			if (activity.mPinnedFooterFrameLayout != null) {
				off = activity.mPinnedFooterFrameLayout.getHeight();
			}

			// TODO this can annoyingly fill the width of the screen in landscape
			toast.setGravity(Gravity.FILL_HORIZONTAL | Gravity.BOTTOM, 0, off);
			toast.setView(toastView);
			toast.setDuration(Toast.LENGTH_LONG);
			toast.show();
		}

		@Override
		public int getItemCount() {
			return data.size();
		}

		public void update(final List<FortItemStack> newData) {
			DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new DiffUtil.Callback() {
				@Override
				public int getOldListSize() {
					return activity.referenceData.length;
				}

				@Override
				public int getNewListSize() {
					return newData.size();
				}

				@Override
				public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
					return data.get(oldItemPosition).templateId.equals(newData.get(newItemPosition).templateId);
				}

				@Override
				public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
					JsonObject oldAttr = activity.referenceData[oldItemPosition].attributes;
					JsonObject newAttr = newData.get(newItemPosition).attributes;
					return (oldAttr == null || newAttr == null) || JsonUtils.getBooleanOr("favorite", oldAttr, false) == JsonUtils.getBooleanOr("favorite", newAttr, false) && JsonUtils.getBooleanOr("item_seen", oldAttr, true) == JsonUtils.getBooleanOr("item_seen", newAttr, true);
				}
			});
			diffResult.dispatchUpdatesTo(this);
			data = newData;
		}

		static class LockerViewHolder extends RecyclerView.ViewHolder {
			ImageView displayImage;
			TextView itemName;
			TextView quantity;
			ImageView favorite;
			TextView newIcon;
			View innerSlotView;

			LockerViewHolder(View itemView) {
				super(itemView);
				displayImage = itemView.findViewById(R.id.item_img);
				itemName = itemView.findViewById(R.id.item_slot_dbg_text);
				quantity = itemView.findViewById(R.id.item_slot_quantity);
				favorite = itemView.findViewById(R.id.item_favorite);
				newIcon = itemView.findViewById(R.id.item_new);
				innerSlotView = itemView.findViewById(R.id.to_set_background);
			}
		}
	}

	private static class ItemFilter {
		public Predicate<FortItemStack> predicate;
		public int name;
		public String icon;

		public ItemFilter(Predicate<FortItemStack> predicate, int name, String icon) {
			this.predicate = predicate;
			this.name = name;
			this.icon = icon;
		}
	}
}
