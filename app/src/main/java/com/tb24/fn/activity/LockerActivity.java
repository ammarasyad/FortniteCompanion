package com.tb24.fn.activity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.tb24.fn.R;
import com.tb24.fn.event.ProfileUpdatedEvent;
import com.tb24.fn.model.AthenaProfileAttributes;
import com.tb24.fn.model.FortItemStack;
import com.tb24.fn.model.FortMcpProfile;
import com.tb24.fn.util.ItemUtils;
import com.tb24.fn.util.Utils;

import org.greenrobot.eventbus.Subscribe;

public class LockerActivity extends BaseActivity {
	private FortMcpProfile profileData;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_locker);
		setupActionBar();
		getThisApplication().eventBus.register(this);

		if (getThisApplication().profileManager.profileData.containsKey("athena")) {
			displayData(getThisApplication().profileManager.profileData.get("athena"));
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		getThisApplication().eventBus.unregister(this);
	}

	@Subscribe
	public void onProfileUpdated(ProfileUpdatedEvent event) {
		if (event.profileId.equals("athena")) {
			displayData(event.profileObj);
		}
	}

	private void displayData(FortMcpProfile profile) {
		profileData = profile;
		AthenaProfileAttributes attributes = (AthenaProfileAttributes) profile.stats.attributesObj;
		apply(findViewById(R.id.locker_slot_character), attributes.favorite_character);
		apply(findViewById(R.id.locker_slot_backpack), attributes.favorite_backpack);
		apply(findViewById(R.id.locker_slot_pickaxe), attributes.favorite_pickaxe);
		apply(findViewById(R.id.locker_slot_glider), attributes.favorite_glider);
		apply(findViewById(R.id.locker_slot_skydivecontrail), attributes.favorite_skydivecontrail);

		ViewGroup group = findViewById(R.id.locker_emote_slots);

		for (int i = 0; i < group.getChildCount(); ++i) {
			apply(group.getChildAt(i), attributes.favorite_dance[i]);
		}

		group = findViewById(R.id.locker_wrap_slots);

		for (int i = 0; i < group.getChildCount(); ++i) {
			apply(group.getChildAt(i), attributes.favorite_itemwraps[i]);
		}

		apply(findViewById(R.id.locker_slot_banner), attributes.banner_icon);
		apply(findViewById(R.id.locker_slot_musicpack), attributes.favorite_musicpack);
		apply(findViewById(R.id.locker_slot_loadingscreen), attributes.favorite_loadingscreen);
	}

	private void apply(final View slot, String itemGuid) {
		slot.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(LockerActivity.this, LockerItemSelectionActivity.class);
				intent.putExtra("a", slot.getId());
				startActivity(intent);
			}
		});

		if (itemGuid.isEmpty()) {
			((ImageView) slot.findViewById(R.id.item_img)).setImageBitmap(getEmptyIcon(slot.getId()));
			return;
		}

		FortItemStack item = itemGuid.contains(":") ? new FortItemStack(itemGuid, 1) : profileData.items.get(itemGuid);

		if (item == null) {
			return;
		}

		ItemUtils.populateSlotView(this, slot, item, getThisApplication().itemRegistry.get(item.templateId));
	}

	private Bitmap getEmptyIcon(int id) {
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
				path = "/Game/UI/Foundation/Textures/Icons/Locker/T_Icon_Wrap_128.T_Icon_Wrap_128";
				break;
			case R.id.locker_slot_musicpack:
				path = "/Game/UI/Foundation/Textures/Icons/Locker/T_Icon_MusicTrack_128.T_Icon_MusicTrack_128";
				break;
			case R.id.locker_slot_loadingscreen:
				path = "/Game/UI/Foundation/Textures/Icons/Locker/T_Icon_LoadingScreen_128.T_Icon_LoadingScreen_128";
				break;
		}

		return path == null ? null : Utils.loadTga(this, path);
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
}
