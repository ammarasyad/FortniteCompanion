package com.tb24.fn.activity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.SparseArray;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.tb24.fn.R;
import com.tb24.fn.event.ProfileUpdatedEvent;
import com.tb24.fn.model.AthenaProfileAttributes;
import com.tb24.fn.model.FortItemStack;
import com.tb24.fn.model.FortMcpProfile;
import com.tb24.fn.util.ItemUtils;
import com.tb24.fn.util.JsonUtils;
import com.tb24.fn.util.Utils;

import org.checkerframework.checker.nullness.compatqual.NullableDecl;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

public class LockerActivity extends BaseActivity implements View.OnClickListener, View.OnHoverListener, View.OnTouchListener, View.OnFocusChangeListener {
	private View selected;
	private TextView hoverText;
	private TextView hoverText2;
	private FortMcpProfile profileData;
	private SparseArray<FortItemStack> itemMap = new SparseArray<>();
	private Toast toast;

	private static Bitmap getEmptyIcon(BaseActivity activity, int id) {
		String path = null;

		switch (id) {
			case R.id.locker_slot_character:
				path = "/Game/UI/Foundation/Textures/Icons/Locker/T_Icon_Outfit_128.T_Icon_Outfit_128";
				break;
			case R.id.locker_slot_backpack:
				path = "/Game/UI/Foundation/Textures/Icons/Locker/T_Icon_BackBling_128.T_Icon_BackBling_128";
				break;
			case R.id.locker_slot_pickaxe:
				path = "/Game/UI/Foundation/Textures/Icons/Locker/T_Icon_HarvestingTool_128.T_Icon_HarvestingTool_128";
				break;
			case R.id.locker_slot_glider:
				path = "/Game/UI/Foundation/Textures/Icons/Locker/T_Icon_Glider_128.T_Icon_Glider_128";
				break;
			case R.id.locker_slot_skydivecontrail:
				path = "/Game/UI/Foundation/Textures/Icons/Locker/T_Icon_Contrail_128.T_Icon_Contrail_128";
				break;
			case R.id.locker_slot_emote1:
			case R.id.locker_slot_emote2:
			case R.id.locker_slot_emote3:
			case R.id.locker_slot_emote4:
			case R.id.locker_slot_emote5:
			case R.id.locker_slot_emote6:
				path = "/Game/UI/Foundation/Textures/Icons/Locker/T_Icon_Dance_128.T_Icon_Dance_128";
				break;
			case R.id.locker_slot_wrap1:
			case R.id.locker_slot_wrap2:
			case R.id.locker_slot_wrap3:
			case R.id.locker_slot_wrap4:
			case R.id.locker_slot_wrap5:
			case R.id.locker_slot_wrap6:
			case R.id.locker_slot_wrap7:
				path = "/Game/UI/Foundation/Textures/Icons/Locker/T_Icon_Wrap_128.T_Icon_Wrap_128";
				break;
			case R.id.locker_slot_musicpack:
				path = "/Game/UI/Foundation/Textures/Icons/Locker/T_Icon_MusicTrack_128.T_Icon_MusicTrack_128";
				break;
			case R.id.locker_slot_loadingscreen:
				path = "/Game/UI/Foundation/Textures/Icons/Locker/T_Icon_LoadingScreen_128.T_Icon_LoadingScreen_128";
				break;
		}

		// TODO blend the red and green colors
		return path == null ? null : Utils.loadTga(activity, path);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_locker);
		setupActionBar();
		hoverText = findViewById(R.id.locker_hover_text);
		hoverText2 = findViewById(R.id.locker_hover_text_2);
		getThisApplication().eventBus.register(this);

		if (getThisApplication().profileManager.profileData.containsKey("athena")) {
			refreshUi(getThisApplication().profileManager.profileData.get("athena"));
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		getThisApplication().eventBus.unregister(this);
	}

	@Subscribe(threadMode = ThreadMode.MAIN)
	public void onProfileUpdated(ProfileUpdatedEvent event) {
		if (event.profileId.equals("athena")) {
			refreshUi(event.profileObj);
		}
	}

	private void refreshUi(FortMcpProfile profile) {
		profileData = profile;
		itemMap.clear();
		AthenaProfileAttributes attributes = (AthenaProfileAttributes) profile.stats.attributesObj;
		View characterSlot = findViewById(R.id.locker_slot_character);
		apply(characterSlot, attributes.favorite_character);
		select(characterSlot);
		apply(findViewById(R.id.locker_slot_backpack), attributes.favorite_backpack);
		apply(findViewById(R.id.locker_slot_pickaxe), attributes.favorite_pickaxe);
		apply(findViewById(R.id.locker_slot_glider), attributes.favorite_glider);
		apply(findViewById(R.id.locker_slot_skydivecontrail), attributes.favorite_skydivecontrail);

		ViewGroup group = findViewById(R.id.locker_emote_slots);

		for (int i = 0; i < group.getChildCount(); ++i) {
			apply(group.getChildAt(i), i > attributes.favorite_dance.length - 1 ? "" : attributes.favorite_dance[i]);
		}

		group = findViewById(R.id.locker_wrap_slots);

		for (int i = 0; i < group.getChildCount(); ++i) {
			apply(group.getChildAt(i), i > attributes.favorite_itemwraps.length - 1 ? "" : attributes.favorite_itemwraps[i]);
		}

//		apply(findViewById(R.id.locker_slot_banner), attributes.banner_icon);
		apply(findViewById(R.id.locker_slot_musicpack), attributes.favorite_musicpack);
		apply(findViewById(R.id.locker_slot_loadingscreen), attributes.favorite_loadingscreen);
	}

	private void apply(final View slot, String itemGuid) {
		slot.setOnClickListener(this);
		slot.setOnHoverListener(this);
		slot.setOnFocusChangeListener(this);
		slot.setOnTouchListener(this);
		final String filter = LockerItemSelectionActivity.getItemCategoryFilterById(slot.getId());
		TextView newText = slot.findViewById(R.id.item_new);
		int newItemsCount = Collections2.filter(profileData.items.values(), new Predicate<FortItemStack>() {
			@Override
			public boolean apply(@NullableDecl FortItemStack input) {
				return input != null && input.getIdCategory().equals(filter) && input.attributes != null && !JsonUtils.getBooleanOr("item_seen", input.attributes, true);
			}
		}).size();
		newText.setText(String.valueOf(newItemsCount));
		newText.setVisibility(newItemsCount > 0 ? View.VISIBLE : View.GONE);

		if (itemGuid.isEmpty()) {
			slot.setBackground(null);
			((ImageView) slot.findViewById(R.id.item_img)).setImageBitmap(getEmptyIcon(this, slot.getId()));
			return;
		}

		FortItemStack item = itemGuid.contains(":") ? new FortItemStack(itemGuid, 1) : profileData.items.get(itemGuid);

		if (item == null) {
			return;
		}

		itemMap.put(slot.getId(), item);
		ItemUtils.populateSlotView(this, slot, item, getThisApplication().itemRegistry.get(item.templateId));
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(0, 0, 0, "All Items");
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == 0) {
			startActivity(new Intent(this, LockerItemSelectionActivity.class));
		}

		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onClick(View v) {
		Intent intent = new Intent(LockerActivity.this, LockerItemSelectionActivity.class);
		intent.putExtra("a", v.getId());
		startActivity(intent);
	}

	@Override
	public boolean onHover(View v, MotionEvent event) {
		select(v);
		return true;
	}

	@Override
	public void onFocusChange(View v, boolean hasFocus) {
		if (v.hasFocus()) {
			select(v);
		}
	}

	@Override
	public boolean onTouch(View v, MotionEvent event) {
		if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
			select(v);
		}

		return false;
	}

	private void select(View v) {
		if (selected != null) {
			selected.setSelected(false);
		}

		selected = v;
		v.setSelected(true);
		hoverText.setText(LockerItemSelectionActivity.getRowTitleTextById(v.getId()));
		hoverText2.setText(LockerItemSelectionActivity.getTitleTextById(v.getId()));
		FortItemStack item = itemMap.get(v.getId());

		if (toast != null) {
			toast.cancel();
		}

		if (item == null) {
			return;
		}

		ViewGroup viewGroup = (ViewGroup) getLayoutInflater().inflate(R.layout.fort_item_detail_box, null);
		ItemUtils.populateItemDetailBox(viewGroup, item);
		toast = new Toast(this);
		toast.setGravity(Gravity.FILL_HORIZONTAL | Gravity.BOTTOM, 0, 0);
		toast.setView(viewGroup);
		toast.setDuration(Toast.LENGTH_LONG);
		toast.show();
	}
}
