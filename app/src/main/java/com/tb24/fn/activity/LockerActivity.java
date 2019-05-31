package com.tb24.fn.activity;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.Region;
import android.graphics.Shader;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RectShape;
import android.os.Bundle;
import android.util.SparseArray;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewPropertyAnimator;
import android.view.animation.AccelerateInterpolator;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.tb24.fn.R;
import com.tb24.fn.event.ProfileUpdatedEvent;
import com.tb24.fn.model.AthenaProfileAttributes;
import com.tb24.fn.model.FortItemStack;
import com.tb24.fn.model.FortMcpProfile;
import com.tb24.fn.model.assetdata.BannerColor;
import com.tb24.fn.model.assetdata.BannerIcon;
import com.tb24.fn.model.assetdata.FortHomebaseBannerColorMap;
import com.tb24.fn.util.ItemUtils;
import com.tb24.fn.util.JsonUtils;
import com.tb24.fn.util.LoadingViewController;
import com.tb24.fn.util.Utils;
import com.tb24.fn.view.SlotCustomImageView;

import org.checkerframework.checker.nullness.compatqual.NullableDecl;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

public class LockerActivity extends BaseActivity implements View.OnClickListener, View.OnHoverListener, View.OnTouchListener, View.OnFocusChangeListener {
	private TextView hoverText;
	private TextView hoverText2;
	private ViewGroup toastView;
	private View characterSlot;
	private View backpackSlot;
	private View pickaxeSlot;
	private View gliderSlot;
	private View contrailSlot;
	private ViewGroup emoteSlotGroup;
	private ViewGroup wrapSlotGroup;
	private View musicPackSlot;
	private View loadingScreenSlot;
	private View bannerSlot;
	private View selected;
	private Toast toast;
	private FortMcpProfile profileData;
	private SparseArray<FortItemStack> itemMap = new SparseArray<>();
	private LoadingViewController lc;
	private ViewPropertyAnimator animateHover1;
	private ViewPropertyAnimator animateHover2;

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
				path = "/Game/UI/Foundation/Textures/Icons/Locker/T_Icon_Wrap_128.T_Icon_Wrap_128";
				break;
			case R.id.locker_slot_wrap7:
				path = "/Game/UI/Foundation/Textures/Icons/Locker/T-Icon-Wrap-Misc-128.T-Icon-Wrap-Misc-128";
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

	public static Bitmap makeBannerIcon(BaseActivity activity, String bannerIcon, String bannerColor) {
		BannerIcon bannerIconDef = activity.getThisApplication().bannerIcons.get(bannerIcon == null || bannerIcon.isEmpty() ? "standardbanner31" : bannerIcon);

		if (bannerIconDef != null) {
			Bitmap bitmap = Utils.loadTga(activity, bannerIconDef.SmallImage.asset_path_name).copy(Bitmap.Config.ARGB_8888, true);

			if (bannerColor != null && !bannerColor.isEmpty()) {
				int color1 = 0xFF000000;
				int color2 = 0xFF000000;
				BannerColor bannerColorDef = activity.getThisApplication().bannerColors.get(bannerColor);

				if (bannerColorDef != null) {
					FortHomebaseBannerColorMap.ColorEntry colorEntry = activity.getThisApplication().bannerColorMap.ColorMap.get(bannerColorDef.ColorKeyName);

					if (colorEntry != null) {
						color1 = colorEntry.PrimaryColor.toPackedARGB();
						color2 = colorEntry.SecondaryColor.toPackedARGB();
					}
				}

				Canvas canvas = new Canvas(bitmap);
				Paint paint = new Paint();
				paint.setShader(new LinearGradient(0.0F, 0.0F, 0.0F, bitmap.getHeight(), color1, color2, Shader.TileMode.CLAMP));
				paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SCREEN));
				canvas.drawPaint(paint);
			}

			return bitmap;
		} else {
			return null;
		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.common_loadable_framed);
		setupActionBar();
		ViewGroup frame = findViewById(R.id.main_content);
		getLayoutInflater().inflate(R.layout.activity_locker, frame);
		hoverText = frame.findViewById(R.id.locker_hover_text);
		hoverText2 = frame.findViewById(R.id.locker_hover_text_2);
		characterSlot = frame.findViewById(R.id.locker_slot_character);
		backpackSlot = frame.findViewById(R.id.locker_slot_backpack);
		pickaxeSlot = frame.findViewById(R.id.locker_slot_pickaxe);
		gliderSlot = frame.findViewById(R.id.locker_slot_glider);
		contrailSlot = frame.findViewById(R.id.locker_slot_skydivecontrail);
		emoteSlotGroup = frame.findViewById(R.id.locker_emote_slots);
		wrapSlotGroup = frame.findViewById(R.id.locker_wrap_slots);
		bannerSlot = frame.findViewById(R.id.locker_slot_banner);
		musicPackSlot = frame.findViewById(R.id.locker_slot_musicpack);
		loadingScreenSlot = frame.findViewById(R.id.locker_slot_loadingscreen);
		ShapeDrawable shapeDrawable1 = new ShapeDrawable(new HeaderBackgroundShape(getResources().getDisplayMetrics().density));
		shapeDrawable1.getPaint().setColor(0xFF0044CB);
		hoverText.setBackground(shapeDrawable1);
		ShapeDrawable shapeDrawable2 = new ShapeDrawable(new HeaderBackgroundShape2(getResources().getDisplayMetrics().density));
		shapeDrawable2.getPaint().setColor(0xFF00AAFF);
		hoverText2.setBackground(shapeDrawable2);
		lc = new LoadingViewController(this, frame, "");
		refreshUi();
		getThisApplication().eventBus.register(this);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		getThisApplication().eventBus.unregister(this);
	}

	@Override
	protected void onResume() {
		super.onResume();
		refreshUi();
		resetAnimatedViews();
	}

	private void resetAnimatedViews() {
		if (animateHover1 != null) {
			animateHover1.cancel();
			hoverText.setTranslationX(0.0F);
			hoverText.setAlpha(1.0F);
		}

		if (animateHover2 != null) {
			animateHover2.cancel();
			hoverText2.setScaleX(1.0F);
			hoverText2.setScaleY(1.0F);
			hoverText2.setAlpha(1.0F);
		}
	}

	@Subscribe(threadMode = ThreadMode.MAIN)
	public void onProfileUpdated(ProfileUpdatedEvent event) {
		if (event.profileId.equals("athena")) {
			refreshUi();
		}
	}

	private void refreshUi() {
		profileData = getThisApplication().profileManager.getProfileData("athena");

		if (profileData == null) {
			lc.loading();
			return;
		} else {
			lc.content();
		}

		itemMap.clear();
		AthenaProfileAttributes attributes = (AthenaProfileAttributes) profileData.stats.attributesObj;
		apply(characterSlot, attributes.favorite_character);
		apply(backpackSlot, attributes.favorite_backpack);
		apply(pickaxeSlot, attributes.favorite_pickaxe);
		apply(gliderSlot, attributes.favorite_glider);
		apply(contrailSlot, attributes.favorite_skydivecontrail);

		for (int i = 0; i < emoteSlotGroup.getChildCount(); ++i) {
			apply(emoteSlotGroup.getChildAt(i), i > attributes.favorite_dance.length - 1 ? "" : attributes.favorite_dance[i]);
		}


		for (int i = 0; i < wrapSlotGroup.getChildCount(); ++i) {
			apply(wrapSlotGroup.getChildAt(i), i > attributes.favorite_itemwraps.length - 1 ? "" : attributes.favorite_itemwraps[i]);
		}

//		apply(bannerSlot, attributes.banner_icon);
		apply(musicPackSlot, attributes.favorite_musicpack);
		apply(loadingScreenSlot, attributes.favorite_loadingscreen);

		if (selected == null) {
			select(characterSlot);
		}

		// TODO if you wanna make this app RIP-ped old phones even more, go ahead add the slot enter/exit staggering animations
	}

	private void apply(final View slot, String itemGuid) {
		slot.setOnClickListener(this);
		slot.setOnHoverListener(this);
		slot.setOnFocusChangeListener(this);
		slot.setOnTouchListener(this);
		final String filter = LockerItemSelectionActivity.getItemCategoryFilterById(slot.getId());
		TextView newText = slot.findViewById(R.id.item_new);
		newText.setPadding(0, 0, 0, 0);
		newText.setMinWidth((int) Utils.dp(getResources(), 22));
		ShapeDrawable newTextBackground = new ShapeDrawable(new EventWindowLeaderboardActivity.ParallelogramShape((int) Utils.dp(getResources(), 4)));
		newTextBackground.getPaint().setColor(0xFFFFF448);
		newText.setBackground(newTextBackground);
		int newItemsCount = Collections2.filter(profileData.items.values(), new Predicate<FortItemStack>() {
			@Override
			public boolean apply(@NullableDecl FortItemStack input) {
				return input != null && input.getIdCategory().equals(filter) && input.attributes != null && !JsonUtils.getBooleanOr("item_seen", input.attributes, true);
			}
		}).size();
		newText.setText(String.valueOf(newItemsCount));
		newText.setVisibility(newItemsCount > 0 ? View.VISIBLE : View.GONE);

		if (itemGuid.isEmpty()) {
			SlotCustomImageView displayImage = slot.findViewById(R.id.item_img);
			slot.setBackground(new EmptySlotBackgroundDrawable(this));
			displayImage.setFancyBackgroundEnabled(false);
			displayImage.setImageDrawable(new BitmapDrawable(getResources(), getEmptyIcon(this, slot.getId())));
			return;
		}

		FortItemStack item = itemGuid.contains(":") ? new FortItemStack(itemGuid, 1) : profileData.items.get(itemGuid);
		itemMap.put(slot.getId(), item);

		if (item == null) {
			return;
		}

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
	public void onClick(final View v) {
		resetAnimatedViews();
		animateHover1 = hoverText.animate().translationX(hoverText.getWidth()).alpha(0.0F).setDuration(125L).setInterpolator(new AccelerateInterpolator());
		animateHover2 = hoverText2.animate().scaleX(2.0F).scaleY(2.0F).alpha(0.0F).setDuration(125L).setStartDelay(63L).setInterpolator(new AccelerateInterpolator());
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
		if (v == selected) {
			return;
		}

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

		if (toastView == null) {
			toastView = (ViewGroup) getLayoutInflater().inflate(R.layout.fort_item_detail_box, null);
		}

		ItemUtils.populateItemDetailBox(toastView, item);
		toast = new Toast(this);
		toast.setGravity(Gravity.FILL_HORIZONTAL | Gravity.BOTTOM, 0, 0);
		toast.setView(toastView);
		toast.setDuration(Toast.LENGTH_LONG);
		toast.show();
	}

	private static class HeaderBackgroundShape extends RectShape {
		private final float indent;
		private final Path path = new Path();

		public HeaderBackgroundShape(float density) {
			this.indent = 8.0F * density;
		}

		@Override
		public void draw(Canvas canvas, Paint paint) {
			path.reset();
			path.moveTo(getWidth(), 0.0F);
			path.lineTo(indent, 0.0F);
			path.lineTo(0.0F, getHeight());
			path.lineTo(getWidth(), getHeight());
			path.close();
			canvas.drawPath(path, paint);
		}
	}

	private static class HeaderBackgroundShape2 extends RectShape {
		private final float indent;
		private final Path path = new Path();

		public HeaderBackgroundShape2(float density) {
			this.indent = 4.0F * density;
		}

		@Override
		public void draw(Canvas canvas, Paint paint) {
			path.reset();
			path.moveTo(getWidth(), 0.0F);
			path.lineTo(0.0F, 0.0F);
			path.lineTo(indent, getHeight());
			path.lineTo(getWidth() - indent, getHeight());
			path.close();
			canvas.drawPath(path, paint);
		}
	}

	public static class EmptySlotBackgroundDrawable extends Drawable {
		private final Paint paint = new Paint();
		private final Paint paint2 = new Paint();
		private final Path path = new Path();
//		private final float density;

		public EmptySlotBackgroundDrawable(Context ctx) {
//			density = ctx.getResources().getDisplayMetrics().density;
		}

		@Override
		public void draw(@NonNull Canvas canvas) {
			// TODO jaggies and as always, RIP old phones
			Rect rect = getBounds();
			float offMax = rect.width() / 10.0F;
			float offMin = rect.width() / 20.0F;
			path.reset();
			path.moveTo(rect.width() - offMin, offMin);
			path.lineTo(offMax, offMax);
			path.lineTo(offMin, rect.height() - offMin);
			path.lineTo(rect.width() - offMax, rect.height() - offMax);
			path.close();
			canvas.save();
			canvas.clipPath(path, Region.Op.DIFFERENCE);
			paint.setColor(0x60FFFFFF);
			canvas.drawRect(rect, paint);
			canvas.restore();
			paint2.setShader(new LinearGradient(0.0F, 0.0F, 0.0F, rect.height(), 0, 0x40003468, Shader.TileMode.CLAMP));
			canvas.drawRect(rect, paint2);
		}

		@Override
		public void setAlpha(int alpha) {
			paint.setAlpha(alpha);
		}

		@Override
		public void setColorFilter(@Nullable ColorFilter colorFilter) {
			paint.setColorFilter(colorFilter);
		}

		@Override
		public int getOpacity() {
			return PixelFormat.OPAQUE;
		}
	}
}
