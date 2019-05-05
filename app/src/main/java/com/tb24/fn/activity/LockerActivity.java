package com.tb24.fn.activity;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.common.collect.ComparisonChain;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.tb24.fn.R;
import com.tb24.fn.model.FortItemStack;
import com.tb24.fn.util.EFortRarity;
import com.tb24.fn.util.LoadingViewController;
import com.tb24.fn.util.Utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LockerActivity extends BaseActivity {
	private RecyclerView list;
	private LockerAdapter adapter;
	private LoadingViewController lc;
	private GridLayoutManager layout;

	@Override
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
				layout = new GridLayoutManager(LockerActivity.this, (int) (list.getWidth() / Utils.dp(getResources(), 66 + 8)));
				list.setLayoutManager(layout);
			}
		});
		lc = new LoadingViewController(this, list, "");
		displayData();
	}

	private void displayData() {
		ArrayList<FortItemStack> data = new ArrayList<>(getThisApplication().dataAthena.profileChanges.get(0).profile.items.values());
		Collections.sort(data, new Comparator<FortItemStack>() {
			@Override
			public int compare(FortItemStack o1, FortItemStack o2) {
				JsonElement jsonElement = getThisApplication().itemRegistry.get(o1.templateId);
				JsonElement jsonElement1 = getThisApplication().itemRegistry.get(o2.templateId);
				EFortRarity rarity1 = EFortRarity.HANDMADE, rarity2 = EFortRarity.HANDMADE;

				if (jsonElement != null) {
					JsonObject jsonObject = jsonElement.getAsJsonArray().get(0).getAsJsonObject();

					if (jsonObject.has("Rarity")) {
						rarity1 = EFortRarity.from(jsonObject.get("Rarity").getAsString());
					}
				}

				if (jsonElement1 != null) {
					JsonObject jsonObject1 = jsonElement1.getAsJsonArray().get(0).getAsJsonObject();

					if (jsonObject1.has("Rarity")) {
						rarity2 = EFortRarity.from(jsonObject1.get("Rarity").getAsString());
					}
				}

				return ComparisonChain.start().compare(o1.getIdCategory(), o2.getIdCategory()).compare(rarity2, rarity1).compare(o1.getIdName(), o2.getIdName()).result();
			}
		});
		list.setAdapter(new LockerAdapter(this, data));
		lc.content();
	}

	private static class LockerAdapter extends RecyclerView.Adapter<LockerAdapter.LockerViewHolder> {
		private final LockerActivity activity;
		private final List<FortItemStack> data;
		private final Map<String, Bitmap> extraData = new HashMap<>();

		public LockerAdapter(LockerActivity activity, List<FortItemStack> data) {
			this.activity = activity;
			this.data = data;
		}

		@NonNull
		@Override
		public LockerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
			return new LockerViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.slot_view_encased, parent, false));
		}

		@Override
		public void onBindViewHolder(@NonNull LockerViewHolder holder, int position) {
			final FortItemStack item = data.get(position);
			holder.rarityBackground.setBackgroundResource(R.drawable.bg_common);
			final JsonElement json = activity.getThisApplication().itemRegistry.get(item.templateId);
			Bitmap bitmap = null;

			if (json != null) {
				JsonObject jsonObject = json.getAsJsonArray().get(0).getAsJsonObject();

				if (extraData.containsKey(item.templateId)) {
					bitmap = extraData.get(item.templateId);
				} else {
					extraData.put(item.templateId, bitmap = getBitmapImageFromItemStackData(activity, item, jsonObject));
				}

				holder.rarityBackground.setBackgroundResource(rarityBackground(jsonObject));
			}

			holder.displayImage.setImageBitmap(bitmap);
			holder.itemName.setText(bitmap == null ? item.templateId : null);
			holder.quantity.setVisibility(item.quantity > 1 ? View.VISIBLE : View.GONE);
			holder.quantity.setText(String.valueOf(item.quantity));
			holder.itemView.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					if (json == null) {
						return;
					}

					ViewGroup viewGroup = (ViewGroup) LayoutInflater.from(activity).inflate(R.layout.fort_item_detail_box, null);
					decorateItemDetailBox(viewGroup, item, json);
					Toast toast = new Toast(activity);
					toast.setView(viewGroup);
					toast.setDuration(Toast.LENGTH_LONG);
					toast.show();
				}
			});
		}

		@Override
		public int getItemCount() {
			return data.size();
		}

		static class LockerViewHolder extends RecyclerView.ViewHolder {
			ImageView displayImage, favorite;
			TextView itemName, quantity;
			View rarityBackground;

			LockerViewHolder(View itemView) {
				super(itemView);
				displayImage = itemView.findViewById(R.id.item_img);
				itemName = itemView.findViewById(R.id.item_slot_dbg_text);
				quantity = itemView.findViewById(R.id.item_slot_quantity);
				favorite = itemView.findViewById(R.id.item_owned);
				rarityBackground = itemView.findViewById(R.id.to_set_background);
			}
		}
	}

}
