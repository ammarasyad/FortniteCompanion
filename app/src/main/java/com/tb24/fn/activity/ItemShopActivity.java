package com.tb24.fn.activity;

import android.app.AlertDialog;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.tb24.fn.R;
import com.tb24.fn.model.CommonCoreProfileAttributes;
import com.tb24.fn.model.EpicError;
import com.tb24.fn.model.FortCatalogResponse;
import com.tb24.fn.model.FortItemStack;
import com.tb24.fn.util.LoadingViewController;
import com.tb24.fn.util.Utils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Response;

public class ItemShopActivity extends BaseActivity {
	private RecyclerView list;
	private ItemShopAdapter adapter;
	private LoadingViewController lc;
	private GridLayoutManager layout;

	public static String shortDescription(FortItemStack itemStack, JsonObject jsonObject) {
		String s;

		if (jsonObject.has("ShortDescription")) {
			s = jsonObject.get("ShortDescription").getAsString();
		} else {
			s = itemStack.getIdCategory().equals("AthenaItemWrap") ? "Wrap" : "";
		}

		return s;
	}

	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.common_loadable_recycler_view);
		setupActionBar();
		list = findViewById(R.id.main_recycler_view);
		int p = (int) Utils.dp(getResources(), 4);
		list.setPadding(p, p, p, p);
		list.setClipToPadding(false);
		list.post(new Runnable() {
			@Override
			public void run() {
				layout = new GridLayoutManager(ItemShopActivity.this, (int) (list.getWidth() / Utils.dp(getResources(), 200)));
				list.setLayoutManager(layout);
			}
		});
		lc = new LoadingViewController(this, list, "");
		load();
	}

	private void load() {
		final Call<FortCatalogResponse> call = getThisApplication().fortnitePublicService.catalog();
		lc.loading();
		new Thread(new Runnable() {
			@Override
			public void run() {
				String errorText = "";
				try {
					final Response<FortCatalogResponse> response = call.execute();

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

	public void display(FortCatalogResponse data) {
		Toast.makeText(this, "Expiration: " + Utils.formatDateSimple(data.expiration), Toast.LENGTH_LONG).show();
		List<FortCatalogResponse.CatalogEntry> entries = new ArrayList<>();

		for (FortCatalogResponse.Storefront storefront : data.storefronts) {
			if (storefront.name.equals("BRWeeklyStorefront") || storefront.name.equals("BRDailyStorefront")) {
				entries.addAll(Arrays.asList(storefront.catalogEntries));
			}
		}

		list.setAdapter(adapter = new ItemShopAdapter(this, entries));
		lc.content();
	}

	private static class ItemShopAdapter extends RecyclerView.Adapter<ItemShopAdapter.ItemShopViewHolder> {
		private final ItemShopActivity activity;
		private final List<FortCatalogResponse.CatalogEntry> data;
		private final Map<String, Bitmap> bitmapCache = new HashMap<>();
//		private final Map<String, JsonElement> extraData2 = new HashMap<>();

		public ItemShopAdapter(ItemShopActivity activity, List<FortCatalogResponse.CatalogEntry> data) {
			this.activity = activity;
			this.data = data;
		}

		@NonNull
		@Override
		public ItemShopViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
			ViewGroup itemView = (ViewGroup) LayoutInflater.from(parent.getContext()).inflate(R.layout.item_shop_entry, parent, false);
			ViewGroup.LayoutParams layoutParams = itemView.getLayoutParams();
//			layoutParams.width = (int) Utils.dp(activity.getResources(), (int) ((float) activity.list.getWidth() / activity.layout.getSpanCount()));
			return new ItemShopViewHolder(itemView);
		}

		@Override
		public void onBindViewHolder(@NonNull final ItemShopViewHolder holder, int position) {
			final FortCatalogResponse.CatalogEntry item = data.get(position);
			holder.backgroundable.setBackgroundResource(R.drawable.bg_common);
			holder.shortDescription.setText(null);
			Bitmap bitmap = null;
			JsonElement jsonFirst = null;

			if (item.itemGrants != null) {
//				Log.d("ItemShopActivity", "devName >> " + item.devName);
				String[] fromDevName = item.devName.substring("[VIRTUAL]".length(), item.devName.lastIndexOf(" for ")).replaceAll("1 x ", "").split(", ");
				List<String> compiledNames = new ArrayList<>();
				FortItemStack[] itemGrants = item.itemGrants;

				for (int i = 0; i < itemGrants.length; i++) {
					FortItemStack itemStack = itemGrants[i];
					JsonElement json = activity.getThisApplication().itemRegistry.get(itemStack.templateId);

					if (json == null) {
						compiledNames.add('*' + fromDevName[i] + '*');
						continue;
					}

					JsonObject jsonObject = json.getAsJsonArray().get(0).getAsJsonObject();

					if (i == 0) {
						jsonFirst = json;

						if (bitmapCache.containsKey(itemStack.templateId)) {
							bitmap = bitmapCache.get(itemStack.templateId);
						} else {
							bitmapCache.put(itemStack.templateId, bitmap = getBitmapImageFromItemStackData(activity, itemStack, jsonObject));
						}

						try {
							holder.shortDescription.setText(shortDescription(itemStack, jsonObject));
							holder.backgroundable.setBackgroundResource(rarityBackground(jsonObject));
						} catch (NullPointerException e) {
							Log.w("ItemShopActivity", "Failed setting short description or rarity background for " + itemStack.templateId, e);
						}
					}

					compiledNames.add(jsonObject.get("DisplayName").getAsString());
				}

//				holder.itemName.setText(Joiner.on("; ").join(compiledNames));
				holder.itemName.setText(compiledNames.get(0));
			}

			holder.displayImage.setImageBitmap(bitmap);
			boolean owned = false;

			if (activity.getThisApplication().dataAthena != null) {
				for (FortCatalogResponse.Requirement requirement : item.requirements) {
					if (requirement.requirementType.equals("DenyOnItemOwnership")) {
						for (Map.Entry<String, FortItemStack> inventoryItem : activity.getThisApplication().dataAthena.profileChanges.get(0).profile.items.entrySet()) {
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

			if (item.meta.has("BannerOverride")) {
				banner = item.meta.get("BannerOverride").getAsString();
			}

			if (banner != null) {
				if (banner.equals("CollectTheSet")) {
					banner = "Collect the set!";
				} else if (banner.equals("New")) {
					banner = "New!";
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

			holder.itemView.setOnClickListener(new View.OnClickListener() {
				int index = 0;

				@Override
				public void onClick(View v) {
					ViewGroup view = (ViewGroup) LayoutInflater.from(activity).inflate(R.layout.item_shop_panel_detail, null);
					makeView(view);
					new AlertDialog.Builder(activity).setView(view).show().getWindow().setBackgroundDrawableResource(android.R.color.transparent);
				}

				private void makeView(ViewGroup view) {
					TextView priceTv = view.findViewById(R.id.item_price);
					TextView itemSale = view.findViewById(R.id.item_sale_from);
					ViewGroup sacRoot = view.findViewById(R.id.sac_root);
					TextView sacName = view.findViewById(R.id.sac_name);
					final Button btnPurchase = view.findViewById(R.id.btn_item_shop_purchase);
					final Button btnGift = view.findViewById(R.id.btn_item_shop_gift);
					FortItemStack itemStack = item.itemGrants[index];
					CommonCoreProfileAttributes attributes = activity.getThisApplication().gson.fromJson(activity.getThisApplication().dataCommonCore.profileChanges.get(0).profile.stats.attributes, CommonCoreProfileAttributes.class);
					JsonElement json = activity.getThisApplication().itemRegistry.get(itemStack.templateId);

					if (json != null) {
						LockerActivity.decorateItemDetailBox(view, itemStack, json);
					}

					priceTv.setText(String.format("%,d", price.basePrice));

					if (price.saleType != null) {
						itemSale.setVisibility(View.VISIBLE);
						itemSale.setText(String.format("%,d", price.regularPrice));
					} else {
						itemSale.setVisibility(View.GONE);
					}

					View.OnClickListener cl = new View.OnClickListener() {
						@Override
						public void onClick(View v) {
							if (v == btnPurchase) {
								// TODO THE MOMENT OF GLORY
							} else if (v == btnGift) {

							}
						}
					};
					btnPurchase.setOnClickListener(cl);
					btnGift.setOnClickListener(cl);
					btnGift.setVisibility(attributes.allowed_to_send_gifts && item.giftInfo != null && item.giftInfo.bIsEnabled ? View.VISIBLE : View.GONE);
					view.findViewById(R.id.no_refund).setVisibility(item.refundable ? View.GONE : View.VISIBLE);

					if (attributes.mtx_affiliate != null) {
						sacRoot.setVisibility(View.VISIBLE);
						sacName.setText(attributes.mtx_affiliate);
					} else {
						sacRoot.setVisibility(View.GONE);
					}
				}
			});
		}

		@Override
		public int getItemCount() {
			return data.size();
		}

		static class ItemShopViewHolder extends RecyclerView.ViewHolder {
			ImageView displayImage;//, blur;
			TextView itemName, itemPrice, itemSale, shortDescription, banner;
			ViewGroup backgroundable, owned, priceGroup;

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
				backgroundable = itemView.findViewById(R.id.backgroundable);
			}
		}
	}
}