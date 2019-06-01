package com.tb24.fn.activity;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RectShape;
import android.media.AudioAttributes;
import android.media.SoundPool;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AnimationUtils;
import android.view.animation.Interpolator;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Px;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.common.base.Joiner;
import com.google.common.base.Supplier;
import com.google.common.collect.ComparisonChain;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.tb24.fn.R;
import com.tb24.fn.event.CalendarDataLoadedEvent;
import com.tb24.fn.event.ProfileUpdatedEvent;
import com.tb24.fn.model.Affiliate;
import com.tb24.fn.model.CalendarTimelineResponse;
import com.tb24.fn.model.CommonCoreProfileAttributes;
import com.tb24.fn.model.EpicError;
import com.tb24.fn.model.FortCatalogResponse;
import com.tb24.fn.model.FortItemStack;
import com.tb24.fn.model.FortMcpResponse;
import com.tb24.fn.model.assetdata.FortItemDefinition;
import com.tb24.fn.model.command.PurchaseCatalogEntry;
import com.tb24.fn.model.command.SetAffiliateName;
import com.tb24.fn.util.EFortRarity;
import com.tb24.fn.util.ItemUtils;
import com.tb24.fn.util.JsonUtils;
import com.tb24.fn.util.LoadingViewController;
import com.tb24.fn.util.Utils;
import com.tb24.fn.view.UpdateEverySecondTextView;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import retrofit2.Call;
import retrofit2.Response;

public class ItemShopActivity extends BaseActivity {
	private static final String CONFIRM_PHRASE = "CONFIRM";
	private boolean bypassVbucks;
	private boolean fakePurchases;
	private SoundPool soundPool;
	private int[] sounds;
	private RecyclerView list;
	private ItemShopAdapter adapter;
	private LoadingViewController lc;
	private GridLayoutManager layout;
	private ViewGroup vBucksView;
	private int vBucksQty;
	private List<FortCatalogResponse.CatalogEntry> featuredItems;
	private List<FortCatalogResponse.CatalogEntry> dailyItems;
	private Call<FortCatalogResponse> catalogCall;
	private Handler handler = new Handler();
	private Runnable scheduleRunnable;
	private AlertDialog currentlyShowingItemDialog;
	private ProgressDialog affiliateProgressDialog;
	private Affiliate affiliateResult;

	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.common_loadable_recycler_view);
		setupActionBar();
		bypassVbucks = PreferenceManager.getDefaultSharedPreferences(this).getBoolean("mtx_check_bypass", false);
		fakePurchases = PreferenceManager.getDefaultSharedPreferences(this).getBoolean("fake_purchases", false);
		List<String> warnings = new ArrayList<>();

		if (bypassVbucks) {
			warnings.add("V-Bucks check bypassed");
		}

		if (fakePurchases) {
			warnings.add("Purchases simulated");
		}

		if (!warnings.isEmpty()) {
			TextView tv = new AppCompatTextView(this);
			tv.setGravity(Gravity.CENTER_HORIZONTAL);
			tv.setTextColor(0xFFFFFF00);
			tv.setText(Joiner.on(", ").join(warnings));
			FrameLayout pinnedHeader = findViewById(R.id.pinned_header);
			pinnedHeader.setVisibility(View.VISIBLE);
			pinnedHeader.addView(tv, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		}

		soundPool = new SoundPool.Builder().setMaxStreams(4).setAudioAttributes(new AudioAttributes.Builder().setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build()).build();
		int purchasedSound1 = soundPool.load(this, R.raw.store_purchaseitem_athena_01, 1);
		int purchasedSound2 = soundPool.load(this, R.raw.store_purchaseitem_athena_02, 1);
		sounds = new int[]{purchasedSound1, purchasedSound2};
		affiliateProgressDialog = new ProgressDialog(this);
		affiliateProgressDialog.setCancelable(false);
		list = findViewById(R.id.main_recycler_view);
		int p = (int) Utils.dp(getResources(), 4);
		list.setPadding(p, p, p, p);
		list.setClipToPadding(false);
		list.setMotionEventSplittingEnabled(false);
		list.post(new Runnable() {
			@Override
			public void run() {
				layout = new GridLayoutManager(ItemShopActivity.this, (int) (list.getWidth() / Utils.dp(getResources(), 180)));
				layout.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
					@Override
					public int getSpanSize(int i) {
						return adapter.getItemViewType(i) == 1 ? layout.getSpanCount() : 1;
					}
				});
				list.setLayoutManager(layout);
			}
		});
		lc = new LoadingViewController(this, list, "");
		load();
		getThisApplication().eventBus.register(this);
	}

	private void load() {
		catalogCall = getThisApplication().fortnitePublicService.storefrontCatalog();
		lc.loading();
		new Thread(new Runnable() {
			@Override
			public void run() {
				CharSequence errorText = "";

				try {
					final Response<FortCatalogResponse> response = catalogCall.execute();

					if (response.isSuccessful()) {
						runOnUiThread(new Runnable() {
							@Override
							public void run() {
								display(response.body());
							}
						});
					} else {
						errorText = EpicError.parse(response).getDisplayText();
					}
				} catch (IOException e) {
					errorText = Utils.userFriendlyNetError(e);
				}

				final CharSequence finalText = errorText;
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						if (finalText.length() > 0) {
							lc.error(finalText);
						}
					}
				});
			}
		}).start();

		if (getThisApplication().calendarData != null) {
			scheduleRefresh();
		}
	}

	private void scheduleRefresh() {
		long delta = getThisApplication().calendarData.dailyStoreEnd.getTime() - System.currentTimeMillis();

		if (delta >= 0) {
			if (scheduleRunnable != null) {
				handler.removeCallbacks(scheduleRunnable);
			}

			handler.postDelayed(scheduleRunnable = new Runnable() {
				@Override
				public void run() {
					getThisApplication().calendarData = null;
					load();
				}
			}, delta);
		}
	}

	public void display(FortCatalogResponse data) {
		for (FortCatalogResponse.Storefront storefront : data.storefronts) {
			List<FortCatalogResponse.CatalogEntry> catalogEntries = Arrays.asList(storefront.catalogEntries);

			if (storefront.name.equals("BRWeeklyStorefront")) {
				Collections.sort(catalogEntries, new Comparator<FortCatalogResponse.CatalogEntry>() {
					@Override
					public int compare(FortCatalogResponse.CatalogEntry o1, FortCatalogResponse.CatalogEntry o2) {
						return ComparisonChain.start().compare(o1.categories[0], o2.categories[0]).compare(o2.sortPriority, o1.sortPriority).result();
					}
				});
				featuredItems = catalogEntries;
			} else if (storefront.name.equals("BRDailyStorefront")) {
				Collections.sort(catalogEntries, new Comparator<FortCatalogResponse.CatalogEntry>() {
					@Override
					public int compare(FortCatalogResponse.CatalogEntry o1, FortCatalogResponse.CatalogEntry o2) {
						FortItemDefinition defData1 = o1.itemGrants[0].getDefData();
						FortItemDefinition defData2 = o2.itemGrants[0].getDefData();
						EFortRarity rarity1 = EFortRarity.COMMON;
						EFortRarity rarity2 = EFortRarity.COMMON;

						if (defData1 != null) {
							rarity1 = EFortRarity.from(defData1.Rarity);
						}

						if (defData2 != null) {
							rarity2 = EFortRarity.from(defData2.Rarity);
						}

						return ComparisonChain.start().compare(rarity2, rarity1).compare(o2.prices[0].basePrice, o1.prices[0].basePrice).compare(o1.itemGrants[0].getIdName(), o2.itemGrants[0].getIdName()).result();
					}
				});
				dailyItems = catalogEntries;
			}
		}

		if (featuredItems == null) {
			featuredItems = new ArrayList<>();
		}

		if (dailyItems == null) {
			dailyItems = new ArrayList<>();
		}

		if (adapter == null) {
			list.setAdapter(adapter = new ItemShopAdapter(this));
		} else {
			adapter.notifyDataSetChanged();
		}

		lc.content();
	}

	private void updateVbucks() {
		vBucksQty = BaseActivity.countAndSetVbucks(this, vBucksView);
	}

	@Subscribe(threadMode = ThreadMode.MAIN)
	public void onProfileUpdated(ProfileUpdatedEvent event) {
		if (event.profileId.equals("common_core")) {
			updateVbucks();

			if (currentlyShowingItemDialog != null) {
				// TODO update sac code and/or even owned status in dialogs
			}
		} else if (event.profileId.equals("athena")) {
			if (adapter != null) {
				adapter.notifyDataSetChanged();
			}
		}
	}

	@Subscribe(threadMode = ThreadMode.MAIN)
	public void onCalendarDataLoaded(CalendarDataLoadedEvent event) {
		scheduleRefresh();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add("V-Bucks").setActionView(vBucksView = (ViewGroup) getLayoutInflater().inflate(R.layout.vbucks, null)).setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_ALWAYS);
		updateVbucks();
		menu.add(0, 1, 0, "Support a Creator");
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == 1) {
			View view = getLayoutInflater().inflate(R.layout.dialog_edit_text, null);
			final EditText editText = view.findViewById(R.id.dialog_edit_text_field);
			DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					if (which == DialogInterface.BUTTON_POSITIVE) {
						String s = editText.getText().toString();

						if (!s.equals(getCoreAttributes().mtx_affiliate)) {
							affiliateProgressDialog.show();
							executeSetAffiliate(s);
						}
					} else if (which == DialogInterface.BUTTON_NEUTRAL) {
						startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.fortnite.com/creator-list")));
					}
				}
			};
			final AlertDialog ad = new AlertDialog.Builder(this)
					.setTitle("Support a Creator")
					.setMessage("Declare your support for a Creator! Your in-game purchases will help support this Creator.")
					.setView(view)
					.setPositiveButton("Accept", listener)
					.setNegativeButton("Close", listener)
					.setNeutralButton("View Approved Creators", listener)
					.show();
			editText.setText(getCoreAttributes().mtx_affiliate);
			editText.requestFocus();
			final Button button = ad.getButton(DialogInterface.BUTTON_POSITIVE);
			editText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
				@Override
				public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
					if (i == EditorInfo.IME_ACTION_DONE) {
						if (button.isEnabled()) {
							button.callOnClick();
						}

						return true;
					}

					return false;
				}
			});
		}

		return super.onOptionsItemSelected(item);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		soundPool.release();
		handler.removeCallbacks(scheduleRunnable);
		getThisApplication().eventBus.unregister(this);

		if (catalogCall != null) {
			catalogCall.cancel();
		}
	}

	// TODO login with Epic Games launcher token to be able to check SAC codes, this time anything inputted are set regardless of being existent
//	private void executeAffiliateCheck(final String s) {
//		runOnUiThread(new Runnable() {
//			@Override
//			public void run() {
//				affiliateProgressDialog.setMessage("Checking code");
//			}
//		});
//		final Call<Affiliate> call = getThisApplication().affiliatePublicService.affiliate(s);
//		new Thread("Check Affiliate Worker") {
//			@Override
//			public void run() {
//				try {
//					Response<Affiliate> response = call.execute();
//
//					if (response.isSuccessful()) {
//						affiliateResult = response.body();
//						executeSetAffiliate(s);
//					} else {
//						Utils.dialogError(ItemShopActivity.this, EpicError.parse(response).getDisplayText());
//						dismissAffiliateProgressDialog();
//					}
//				} catch (IOException e) {
//					Utils.throwableDialog(ItemShopActivity.this, e);
//					dismissAffiliateProgressDialog();
//				}
//			}
//		}.start();
//	}

	private void executeSetAffiliate(String s) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				affiliateProgressDialog.setMessage("Setting code");
			}
		});
		SetAffiliateName payload = new SetAffiliateName();
		payload.affiliateName = s;
		final Call<FortMcpResponse> call = getThisApplication().fortnitePublicService.mcp(
				"SetAffiliateName",
				PreferenceManager.getDefaultSharedPreferences(ItemShopActivity.this).getString("epic_account_id", ""),
				"common_core",
				getThisApplication().profileManager.getRvn("common_core"),
				payload);
		new Thread("Set Affiliate Worker") {
			@Override
			public void run() {
				try {
					Response<FortMcpResponse> response = call.execute();

					if (response.isSuccessful()) {
						getThisApplication().profileManager.executeProfileChanges(response.body());
//						runOnUiThread(new Runnable() {
//							@Override
//							public void run() {
//								Toast.makeText(ItemShopActivity.this, "Support a Creator code set to:\n" + affiliateResult.slug + " (" + affiliateResult.displayName + ")", Toast.LENGTH_LONG).show();
//							}
//						});
					} else {
						Utils.dialogError(ItemShopActivity.this, EpicError.parse(response).getDisplayText());
					}
				} catch (IOException e) {
					Utils.throwableDialog(ItemShopActivity.this, e);
				} finally {
					dismissAffiliateProgressDialog();
				}
			}
		}.start();
	}

	private void dismissAffiliateProgressDialog() {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				affiliateProgressDialog.dismiss();
			}
		});
	}

	private CommonCoreProfileAttributes getCoreAttributes() {
		return (CommonCoreProfileAttributes) getThisApplication().profileManager.getProfileData("common_core").stats.attributesObj;
	}

	private static class ItemShopAdapter extends RecyclerView.Adapter<ItemShopAdapter.ItemShopViewHolder> {
		private final ItemShopActivity activity;

		public ItemShopAdapter(ItemShopActivity activity) {
			this.activity = activity;
		}

		@NonNull
		@Override
		public ItemShopViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
			if (viewType == 1) {
				ViewGroup inflate = (ViewGroup) LayoutInflater.from(parent.getContext()).inflate(R.layout.item_shop_entry_header, parent, false);
				ShapeDrawable shapeDrawable = new ShapeDrawable(new HeaderBackgroundShape((int) Utils.dp(activity.getResources(), 12)));
				shapeDrawable.getPaint().setColor(0x807AA4D3);
				inflate.getChildAt(0).setBackground(shapeDrawable);
				return new ItemShopViewHolder(inflate);
			} else {
				ViewGroup itemView = (ViewGroup) LayoutInflater.from(parent.getContext()).inflate(R.layout.item_shop_entry, parent, false);
//				ViewGroup.LayoutParams layoutParams = itemView.getLayoutParams();
//				layoutParams.width = (int) Utils.dp(activity.getResources(), (int) ((float) activity.list.getWidth() / activity.layout.getSpanCount()));
				return new ItemShopViewHolder(itemView);
			}
		}

		@Override
		public void onBindViewHolder(@NonNull final ItemShopViewHolder holder, final int positionWithHeader) {
			final boolean isDaily = positionWithHeader - 1 >= activity.featuredItems.size();

			if (getItemViewType(positionWithHeader) == 1) {
				holder.itemName.setText(isDaily ? "Daily Items" : "Featured Items");
				((UpdateEverySecondTextView) holder.itemPrice).setTextSupplier(new Supplier<CharSequence>() {
					@Override
					public CharSequence get() {
						CalendarTimelineResponse.ClientEventState calendarData = activity.getThisApplication().calendarData;
						return calendarData == null ? "" : Utils.formatElapsedTime(activity, (isDaily ? calendarData.dailyStoreEnd : calendarData.weeklyStoreEnd).getTime() - System.currentTimeMillis(), true);
					}
				});
				return;
			}

			final FortCatalogResponse.CatalogEntry item;

			if (isDaily) {
				item = activity.dailyItems.get(positionWithHeader - 2 - activity.featuredItems.size());
			} else {
				item = activity.featuredItems.get(positionWithHeader - 1);
			}

			holder.backgroundable.setBackgroundResource(R.drawable.bg_common);
			holder.shortDescription.setText(null);
			Bitmap bitmap = null;
			String[] fromDevName = item.devName.substring("[VIRTUAL]".length(), item.devName.lastIndexOf(" for ")).replaceAll("1 x ", "").split(", ");
			final List<String> compiledNames = new ArrayList<>();
			final List<JsonElement> jsons = new ArrayList<>();

			for (int i = 0; i < item.itemGrants.length; i++) {
				FortItemStack itemStack = item.itemGrants[i];
				FortItemDefinition defData = itemStack.getDefData();
				JsonElement json = activity.getThisApplication().itemRegistry.get(itemStack.templateId);
				jsons.add(json);

				if (json == null) {
					// item data not found from assets, item is encrypted or new
					compiledNames.add(fromDevName[i]);

					if (i == 0) {
						holder.itemName.setText(fromDevName[i]);
						holder.shortDescription.setText(ItemUtils.shortDescriptionFromCtg(itemStack.getIdCategory()));
					}

					continue;
				}

				JsonObject jsonObject = json.getAsJsonArray().get(0).getAsJsonObject();
				String displayName = defData.DisplayName;
				compiledNames.add(displayName);

				if (i == 0) {
					bitmap = ItemUtils.getBitmapImageFromItemStackData(activity, itemStack, jsonObject);

					try {
						holder.itemName.setText(displayName);
						holder.shortDescription.setText(ItemUtils.getShortDescription(itemStack));
						holder.backgroundable.setBackground(ItemUtils.rarityBgSlot(activity, EFortRarity.from(defData.Rarity)));
					} catch (NullPointerException e) {
						Log.w("ItemShopActivity", "Failed setting short description or rarity background for " + itemStack.templateId, e);
					}
				}
			}

			holder.displayImage.setImageBitmap(bitmap);
			boolean owned = false;

			if (activity.getThisApplication().profileManager.hasProfileData("athena")) {
				for (FortCatalogResponse.Requirement requirement : item.requirements) {
					if (requirement.requirementType.equals("DenyOnItemOwnership")) {
						for (Map.Entry<String, FortItemStack> inventoryItem : activity.getThisApplication().profileManager.getProfileData("athena").items.entrySet()) {
							if (inventoryItem.getValue().templateId.equals(requirement.requiredId) && inventoryItem.getValue().quantity >= requirement.minQuantity) {
								owned = true;
								break;
							}
						}

						break;
					}
				}
			}

			holder.priceGroup.setVisibility(owned ? View.GONE : View.VISIBLE);
			holder.owned.setVisibility(owned ? View.VISIBLE : View.GONE);
			final FortCatalogResponse.Price price = item.prices[0];
			String banner = null;

			if (price.saleType != null) {
				banner = "On sale!";
				holder.itemSale.setVisibility(View.VISIBLE);
				holder.itemSale.setText(String.format("%,d", price.regularPrice));
			} else {
				holder.itemSale.setVisibility(View.GONE);
			}

			banner = JsonUtils.getStringOr("BannerOverride", item.meta, banner);

			if (banner != null) {
				try {
					banner = activity.getString(activity.getResources().getIdentifier("itemshop_banner_" + banner, "string", activity.getPackageName()));
				} catch (Resources.NotFoundException ignored) {
				}
			}

			holder.banner.setText(banner);
			holder.banner.setVisibility(banner == null ? View.INVISIBLE : View.VISIBLE);
			holder.itemPrice.setText(String.format("%,d", price.basePrice));

//			TODO bg blur for the blacked thing
//			if (bitmap != null) {
//				Blurry.with(activity).radius(10).capture(holder.itemView).into(holder.blur);
//			} else {
//				holder.blur.setImageDrawable(null);
//			}

			final boolean finalOwned = owned;
			holder.itemView.setOnClickListener(new View.OnClickListener() {
				private ViewGroup view;
				private ViewGroup group;
				private ViewGroup owned;
				private Button btnPurchase, btnGift;
				private View.OnClickListener buttonsClickListener;
				private boolean purchasePending, purchaseSuccess;
				private int previewingIndex = -1;

				@Override
				public void onClick(View v) {
					view = (ViewGroup) activity.getLayoutInflater().inflate(R.layout.item_shop_panel_detail, null);
					populateView();
					AlertDialog alertDialog = new AlertDialog.Builder(activity).setView(view).create();
					alertDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
					alertDialog.show();
				}

				private void populateView() {
					TextView priceTv = view.findViewById(R.id.item_price);
					TextView itemSale = view.findViewById(R.id.item_sale_from);
					owned = view.findViewById(R.id.item_owned);
					btnPurchase = view.findViewById(R.id.btn_item_shop_purchase);
					btnGift = view.findViewById(R.id.btn_item_shop_gift);
					ViewGroup sacRoot = view.findViewById(R.id.sac_root);
					TextView sacName = view.findViewById(R.id.sac_name);
					group = view.findViewById(R.id.item_shop_all_item_grants);

					if (item.itemGrants.length > 1) {
						for (int i = 0; i < item.itemGrants.length; i++) {
							FortItemStack itemStackLoop = item.itemGrants[i];
							View slotView = activity.getLayoutInflater().inflate(R.layout.slot_view, null);
							ItemUtils.populateSlotView(activity, slotView, itemStackLoop, jsons.get(i));
							final int finalI = i;
							slotView.setClickable(true);
							slotView.setFocusableInTouchMode(true);
							slotView.setOnFocusChangeListener(new View.OnFocusChangeListener() {
								@Override
								public void onFocusChange(View v, boolean hasFocus) {
									if (hasFocus) {
										updateItemInfo(finalI);
									}
								}
							});

							LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams((int) Utils.dp(activity.getResources(), 66), (int) Utils.dp(activity.getResources(), 110));
							int m = (int) Utils.dp(activity.getResources(), 4);
							lp.setMargins(m, m, m, m);
							group.addView(slotView, lp);
						}
					} else {
						// TODO big item slot icon
					}

					updateItemInfo(0);
					priceTv.setText(String.format("%,d", price.basePrice));

					if (price.saleType != null) {
						itemSale.setVisibility(View.VISIBLE);
						itemSale.setText(String.format("%,d", price.regularPrice));
					} else {
						itemSale.setVisibility(View.GONE);
					}

					buttonsClickListener = new View.OnClickListener() {
						@Override
						public void onClick(View v) {
							if (v == btnPurchase) {
								if (activity.fakePurchases) {
									Toast.makeText(activity, "This purchase is simulated!", Toast.LENGTH_SHORT).show();
									doPurchase(item);
								} else {
									AlertDialog dialog = Utils.createEditTextDialog(activity, "Purchase " + compiledNames.get(0), activity.getString(android.R.string.ok), new Utils.EditTextDialogCallback() {
										@Override
										public void onResult(String s) {
											if (s.equals(CONFIRM_PHRASE)) {
												doPurchase(item);
											}
										}
									});
									dialog.setMessage("Type \"" + CONFIRM_PHRASE + "\" to proceed");
									dialog.show();
									((EditText) dialog.findViewById(R.id.dialog_edit_text_field)).setHint(CONFIRM_PHRASE);
								}
							} else if (v == btnGift) {
								// TODO gifting: firstly you have to add friends endpoint
								Toast.makeText(activity, "Gifting not available yet", Toast.LENGTH_SHORT).show();
							}
						}
					};
					btnPurchase.setOnClickListener(buttonsClickListener);
					btnGift.setOnClickListener(buttonsClickListener);
					updateButtons();
					view.findViewById(R.id.no_refund).setVisibility(item.refundable ? View.GONE : View.VISIBLE);

					if (!activity.getCoreAttributes().mtx_affiliate.isEmpty()) {
						sacRoot.setVisibility(View.VISIBLE);
						sacName.setText(activity.getCoreAttributes().mtx_affiliate);
					} else {
						sacRoot.setVisibility(View.GONE);
					}
				}

				private void updateItemInfo(int to) {
					if (item.itemGrants.length > 1 && previewingIndex >= 0) {
						group.getChildAt(previewingIndex).setSelected(false);
					}

					previewingIndex = to;

					if (item.itemGrants.length > 1) {
						group.getChildAt(previewingIndex).setSelected(true);
					}

					FortItemStack itemStack = item.itemGrants[previewingIndex];
					JsonElement json = jsons.get(previewingIndex);

					if (json != null) {
						ItemUtils.populateItemDetailBox(view, itemStack);
					} else {
						((TextView) view.findViewById(R.id.item_text1)).setText("Unknown | " + ItemUtils.shortDescriptionFromCtg(itemStack.getIdCategory()));
						((TextView) view.findViewById(R.id.item_text2)).setText(compiledNames.get(previewingIndex));
						view.findViewById(R.id.item_text3).setVisibility(View.GONE);
					}
				}

				private void updateButtons() {
					boolean finallyOwned = purchaseSuccess || finalOwned;
					boolean notEnough = !activity.bypassVbucks && activity.vBucksQty < price.basePrice;
					owned.setVisibility(finallyOwned ? View.VISIBLE : View.GONE);
					btnPurchase.setText(item.itemGrants.length == 1 ? "Purchase" : "Purchase Items");
					btnPurchase.setEnabled(!purchasePending);
					btnPurchase.setVisibility(finallyOwned ? View.GONE : View.VISIBLE);
					btnGift.setVisibility(activity.getCoreAttributes().allowed_to_send_gifts && item.giftInfo != null && item.giftInfo.bIsEnabled && (finallyOwned || !notEnough) ? View.VISIBLE : View.GONE);

					if (notEnough) {
						btnPurchase.setEnabled(false);
						// TODO "Get V-Bucks"
						btnPurchase.setText("Not enough V-Bucks");

						if (btnGift.getVisibility() == View.VISIBLE) {
							btnGift.setEnabled(false);
							btnGift.setText("Gift: Not enough V-Bucks");
						}
					} else {
						btnGift.setEnabled(true);
						btnGift.setText("Buy As A Gift");
					}
				}

				private void doPurchase(final FortCatalogResponse.CatalogEntry item) {
					PurchaseCatalogEntry payload = new PurchaseCatalogEntry();
					payload.currency = item.prices[0].currencyType;
					payload.currencySubType = item.prices[0].currencySubType;
					payload.expectedPrice = item.prices[0].basePrice;
					payload.offerId = item.offerId;
					final Call<FortMcpResponse> call = activity.getThisApplication().fortnitePublicService.mcp(
							"PurchaseCatalogEntry",
							PreferenceManager.getDefaultSharedPreferences(activity).getString("epic_account_id", ""),
							"common_core",
							activity.getThisApplication().profileManager.getRvn("common_core"),
							payload);
					purchasePending = true;
					updateButtons();
					new Thread("Purchase Worker") {
						@Override
						public void run() {
							try {
								if (activity.fakePurchases) {
									// fake it
									Thread.sleep(2000L);
									activity.getThisApplication().profileManager.executeProfileChanges(activity.getThisApplication().gson.fromJson("{\"multiUpdate\":[{\"profileRevision\":-1,\"profileId\":\"athena\",\"profileChangesBaseRevision\":7043,\"profileChanges\":[{\"changeType\":\"itemAdded\",\"itemId\":\"" + UUID.randomUUID() + "\",\"item\":{\"templateId\":\"" + item.itemGrants[0].templateId + "\",\"attributes\":{\"max_level_bonus\":0,\"level\":1,\"item_seen\":false,\"xp\":0,\"variants\":[],\"favorite\":false,\"DUMMY\":true},\"quantity\":1}}],\"profileCommandRevision\":6412}]}", FortMcpResponse.class));
									purchaseSuccess();
								} else {
									// here we're going for real
									Response<FortMcpResponse> mcpResponse = call.execute();

									if (mcpResponse.isSuccessful()) {
										activity.getThisApplication().profileManager.executeProfileChanges(mcpResponse.body());
										// hooray
										purchaseSuccess();
									} else {
										Utils.dialogError(activity, EpicError.parse(mcpResponse).getDisplayText());
									}
								}
							} catch (IOException e) {
								Utils.throwableDialog(activity, e);
							} catch (InterruptedException e) {
								e.printStackTrace();
							} finally {
								activity.runOnUiThread(new Runnable() {
									@Override
									public void run() {
										purchasePending = false;
										updateButtons();
									}
								});
							}
						}
					}.start();
				}

				private void purchaseSuccess() {
					activity.runOnUiThread(new Runnable() {
						@Override
						public void run() {
							purchaseSuccess = true;
							FortItemStack purchasedItem = item.itemGrants[0];
							View purchasedDialogView = activity.getLayoutInflater().inflate(R.layout.dialog_purchased, null);
							View purchasedText = purchasedDialogView.findViewById(R.id.item_shop_purchased_text);
							View purchasedCheck = purchasedDialogView.findViewById(R.id.item_shop_purchased_check);
							TextView purchasedItemTitle = purchasedDialogView.findViewById(R.id.item_shop_purchased_item_title);
							View slotView = purchasedDialogView.findViewById(R.id.to_set_background);
							purchasedItemTitle.setText(compiledNames.get(0));
							ItemUtils.populateSlotView(activity, purchasedDialogView, purchasedItem, jsons.get(0));
							slotView.setFocusable(false);
							final Dialog dialog = new Dialog(activity);
							dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

							if (dialog.getWindow() != null) {
								dialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
								dialog.getWindow().getDecorView().setSystemUiVisibility(0);
								dialog.getWindow().setStatusBarColor(0);
								dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
							}

							dialog.setCancelable(false);
							dialog.setContentView(purchasedDialogView);
							dialog.show();
							dialog.getWindow().setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT);
							long duration = 250L;
							Interpolator samsungBounceInterpolator = AnimationUtils.loadInterpolator(activity, R.anim.elastic_50_menu_popup);
							slotView.setScaleX(2.5F);
							slotView.setScaleY(2.5F);
							slotView.setAlpha(0.0F);
							slotView.animate().scaleX(1.0F).scaleY(1.0F).alpha(1.0F).setInterpolator(samsungBounceInterpolator).setDuration(duration);
							purchasedItemTitle.setTranslationX(400.0F);
							purchasedItemTitle.setAlpha(0.0F);
							purchasedItemTitle.animate().translationX(0.0F).alpha(1.0F).setInterpolator(samsungBounceInterpolator).setDuration(duration).setStartDelay(duration);
							purchasedText.setTranslationY(100.0F);
							purchasedText.setScaleX(0.0F);
							purchasedText.animate().translationY(0.0F).scaleX(1.0F).setInterpolator(samsungBounceInterpolator).setDuration(duration).setStartDelay(duration + duration);
							purchasedCheck.setRotation(540.0F);
							purchasedCheck.setScaleX(2.0F);
							purchasedCheck.setScaleY(2.0F);
							purchasedCheck.setAlpha(0.0F);
							purchasedCheck.animate().rotation(0.0F).scaleX(1.0F).scaleY(1.0F).alpha(1.0F).setInterpolator(samsungBounceInterpolator).setDuration(375L).setStartDelay(duration + duration + duration);
							activity.soundPool.play(activity.sounds[new Random().nextInt(activity.sounds.length)], 1.0F, 1.0F, 0, 0, 1.0F);
							purchasedDialogView.postDelayed(new Runnable() {
								@Override
								public void run() {
									dialog.dismiss();
								}
							}, 4000L);
						}
					});
				}
			});
		}

		@Override
		public int getItemViewType(int position) {
			return position == 0 || position - 1 == activity.featuredItems.size() ? 1 : 0;
		}

		@Override
		public int getItemCount() {
			return activity.featuredItems.size() + activity.dailyItems.size() + 2;
		}

		static class ItemShopViewHolder extends RecyclerView.ViewHolder {
			ImageView displayImage;//, blur;
			TextView itemName;
			TextView itemPrice;
			TextView itemSale;
			TextView shortDescription;
			TextView banner;
			ViewGroup backgroundable;
			ViewGroup owned;
			ViewGroup priceGroup;

			ItemShopViewHolder(View itemView) {
				super(itemView);
				displayImage = itemView.findViewById(R.id.item_img);
//				blur = itemView.findViewById(R.id.bg_blur);
				itemName = itemView.findViewById(R.id.item_text1);
				itemPrice = itemView.findViewById(R.id.item_text2);
				itemSale = itemView.findViewById(R.id.item_sale_from);
				shortDescription = itemView.findViewById(R.id.item_text3);
				banner = itemView.findViewById(R.id.news_entry_adspace);
				priceGroup = itemView.findViewById(R.id.item_price_group);
				owned = itemView.findViewById(R.id.item_owned);
				backgroundable = itemView.findViewById(R.id.to_set_background);
			}
		}
	}

	private static class HeaderBackgroundShape extends RectShape {
		private final int indent;
		private final Path path = new Path();

		public HeaderBackgroundShape(@Px int indent) {
			this.indent = indent;
		}

		@Override
		public void draw(Canvas canvas, Paint paint) {
			path.reset();
			path.moveTo(getWidth() - indent, 0.0F);
			path.lineTo(0.0F, 0.0F);
			path.lineTo(0.0F, getHeight());
			path.lineTo(getWidth(), getHeight());
			path.close();
			canvas.drawPath(path, paint);
		}
	}
}