package com.tb24.fn.activity;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.tb24.fn.FortniteCompanionApp;
import com.tb24.fn.R;
import com.tb24.fn.event.LoggedOutEvent;
import com.tb24.fn.event.ProfileQueryFailedEvent;
import com.tb24.fn.event.ProfileUpdatedEvent;
import com.tb24.fn.model.AthenaProfileAttributes;
import com.tb24.fn.model.CommonPublicProfileAttributes;
import com.tb24.fn.model.EpicError;
import com.tb24.fn.model.ExchangeResponse;
import com.tb24.fn.model.FortItemStack;
import com.tb24.fn.model.FortMcpProfile;
import com.tb24.fn.model.FortMcpResponse;
import com.tb24.fn.model.GameProfile;
import com.tb24.fn.model.XGameProfile;
import com.tb24.fn.util.ItemUtils;
import com.tb24.fn.util.JsonUtils;
import com.tb24.fn.util.LoadingViewController;
import com.tb24.fn.util.Utils;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.IOException;
import java.util.Arrays;

import retrofit2.Call;
import retrofit2.Response;

public class MainActivity extends BaseActivity implements View.OnClickListener, View.OnLongClickListener {
	private static final int UPDATE_IF_OK_REQ_CODE = 0;
	private SharedPreferences prefs;
	private Button loginBtn;
	private TextView loginText;
	private ViewGroup vBucksView;
	private MenuItem menuVbucks;
	private Call<FortMcpResponse> callMcpCommonPublic;
	private Call<FortMcpResponse> callMcpCommonCore;
	private Call<FortMcpResponse> callMcpAthena;
	private Call<XGameProfile> callSelfName;
	private Call<ExchangeResponse> callExchange;
	private LoadingViewController profileLc;
	private AlertDialog loggedOutDialog;

//	public static boolean checkAuthError(EpicError error) {
//		boolean isForbidden = error.response.code() == HttpURLConnection.HTTP_UNAUTHORIZED;
//		boolean invalidToken = error.errorCode.equals("errors.com.epicgames.common.oauth.invalid_token") || error.numericErrorCode == 1014;
//		boolean tokenVerificationFailed = error.errorCode.equals("errors.com.epicgames.common.authentication.token_verification_failed") || error.numericErrorCode == 1031;
//		boolean authenticationFailed = error.errorCode.equals("errors.com.epicgames.common.oauth.authentication_failed") || error.numericErrorCode == 1032;
//		return isForbidden || invalidToken || tokenVerificationFailed || authenticationFailed;
//		return error.response.code() == HttpURLConnection.HTTP_UNAUTHORIZED;
//	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		findViewById(R.id.main_screen_btn_profile).setOnClickListener(this);
		findViewById(R.id.main_screen_btn_challenges).setOnClickListener(this);
		findViewById(R.id.main_screen_btn_events).setOnClickListener(this);
		findViewById(R.id.main_screen_btn_stats).setOnClickListener(this);
		findViewById(R.id.main_screen_btn_locker).setOnClickListener(this);
		findViewById(R.id.main_screen_btn_item_shop).setOnClickListener(this);
		findViewById(R.id.main_screen_btn_news).setOnClickListener(this);
		findViewById(R.id.main_screen_btn_news).setOnLongClickListener(this);
		findViewById(R.id.main_screen_btn_stw).setOnClickListener(this);
		prefs = PreferenceManager.getDefaultSharedPreferences(this);
		loginBtn = findViewById(R.id.main_screen_btn_login);
		loginBtn.setOnClickListener(this);
		loginText = findViewById(R.id.main_screen_login_username);
		ViewGroup profileFrame = findViewById(R.id.profile_frame);
		DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
		boolean sideBySide = (displayMetrics.widthPixels / displayMetrics.density) >= FortniteCompanionApp.MIN_DP_FOR_TWOCOLUMN;
		((LinearLayout) findViewById(R.id.activity_main_root)).setOrientation(sideBySide ? LinearLayout.HORIZONTAL : LinearLayout.VERTICAL);

		if (sideBySide) {
			profileFrame.getLayoutParams().width = (int) Utils.dp(getResources(), 300);
		}

		profileLc = new LoadingViewController(profileFrame, findViewById(R.id.p_root), "No profile data") {
			@Override
			public boolean shouldShowEmpty() {
				return !getThisApplication().profileManager.hasProfileData("athena");
			}
		};
		loggedOutDialog = new AlertDialog.Builder(this)
				.setTitle("Logout Occured")
				.setMessage("Your login has expired or you logged in elsewhere.\n\nPlease log in again.")
				.setPositiveButton(android.R.string.ok, null)
				.setOnDismissListener(new DialogInterface.OnDismissListener() {
					@Override
					public void onDismiss(DialogInterface dialog) {
						recreate();
					}
				})
				.create();
		loggedOutDialog.setCanceledOnTouchOutside(false);
		checkLogin();
		getThisApplication().eventBus.register(this);
	}

	private void checkLogin() {
		boolean loggedIn = prefs.getBoolean("is_logged_in", false);
		findViewById(R.id.main_screen_btn_profile).setEnabled(loggedIn);
		findViewById(R.id.main_screen_btn_challenges).setEnabled(loggedIn);
		findViewById(R.id.main_screen_btn_events).setEnabled(loggedIn);
		findViewById(R.id.main_screen_btn_stats).setEnabled(loggedIn);
		findViewById(R.id.main_screen_btn_locker).setEnabled(loggedIn);
		findViewById(R.id.main_screen_btn_stw).setEnabled(loggedIn);
		loginBtn.setVisibility(loggedIn ? View.GONE : View.VISIBLE);

		if (loggedIn) {
			updateLoginText("...");
			String accountId = prefs.getString("epic_account_id", "");

			if (!getThisApplication().profileManager.hasProfileData("common_public")) {
				callMcpCommonPublic = getThisApplication().profileManager.requestProfileUpdate("common_public");
			} else {
				commonPublicLoaded();
			}

			if (!getThisApplication().profileManager.hasProfileData("common_core")) {
				callMcpCommonCore = getThisApplication().profileManager.requestProfileUpdate("common_core");
			} else {
				new Thread(new Runnable() {
					@Override
					public void run() {
						try {
							Thread.sleep(100L);
							runOnUiThread(new Runnable() {
								@Override
								public void run() {
									commonCoreLoaded();
								}
							});
						} catch (InterruptedException ignored) {
						}
					}
				}).start();
			}

			if (!getThisApplication().profileManager.hasProfileData("athena")) {
				profileLc.loading();
				callMcpAthena = getThisApplication().profileManager.requestProfileUpdate("athena");
			} else {
				athenaLoaded();
			}

			getThisApplication().loadCalendarData();
			callSelfName = getThisApplication().accountPublicService.account(accountId);
			new Thread() {
				@Override
				public void run() {
					try {
						final Response<XGameProfile> response = callSelfName.execute();
						runOnUiThread(new Runnable() {
							@Override
							public void run() {
								if (response.isSuccessful()) {
									updateLoginText((getThisApplication().currentLoggedIn = response.body()).getDisplayName());
								}
							}
						});
					} catch (IOException ignored) {
					}
				}
			}.start();
		} else {
			updateLoginText(null);
			profileLc.content();
			openLogin();
		}
	}

	@Subscribe(threadMode = ThreadMode.MAIN)
	public void onProfileUpdated(ProfileUpdatedEvent event) {
		switch (event.profileId) {
			case "common_public":
				commonPublicLoaded();
				break;
			case "common_core":
				commonCoreLoaded();
				break;
			case "athena":
				athenaLoaded();
				break;
		}
	}

	@Subscribe(threadMode = ThreadMode.MAIN)
	public void onProfileQueryFailed(ProfileQueryFailedEvent event) {
		if (event.profileId.equals("athena") && !getThisApplication().profileManager.hasProfileData("athena")) {
			profileLc.content();
		}
	}

	@Subscribe(threadMode = ThreadMode.MAIN)
	public void onLoggedOut(LoggedOutEvent event) {
		startActivity(new Intent(this, MainActivity.class).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
		getThisApplication().clearLoginData();

		if (event.bIsLoggedOutByUser) {
			recreate();
		} else {
			loggedOutDialog.show();
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		getThisApplication().eventBus.unregister(this);

		if (callMcpCommonPublic != null) {
			callMcpCommonPublic.cancel();
		}

		if (callMcpCommonCore != null) {
			callMcpCommonCore.cancel();
		}

		if (callMcpAthena != null) {
			callMcpAthena.cancel();
		}

		if (callSelfName != null) {
			callSelfName.cancel();
		}

		if (callExchange != null) {
			callExchange.cancel();
		}
	}

	private void commonPublicLoaded() {
		// homebase name only that's unique to that profile id
		FortMcpProfile profile = getThisApplication().profileManager.getProfileData("common_public");
		CommonPublicProfileAttributes attributes = (CommonPublicProfileAttributes) profile.stats.attributesObj;
		((ImageView) findViewById(R.id.p_banner)).setImageBitmap(LockerActivity.makeBannerIcon(this, attributes.banner_icon, attributes.banner_color));
	}

	private void commonCoreLoaded() {
		FortMcpProfile profile = getThisApplication().profileManager.getProfileData("common_core");
		menuVbucks.setVisible(true);
		countAndSetVbucks(MainActivity.this, vBucksView);
		findViewById(R.id.main_screen_btn_item_shop).setEnabled(profile != null);
	}

	@SuppressLint("DefaultLocale")
	private void athenaLoaded() {
		profileLc.content();

		if (!getThisApplication().profileManager.hasProfileData("athena")) {
			return;
		}

		TextView txtSeason = findViewById(R.id.p_season);
		ProgressBar xpBar = findViewById(R.id.p_xp_bar);
		TextView txtLvlUpReward = findViewById(R.id.p_lvl_up_reward);
		TextView txtXpProgress = findViewById(R.id.p_xp_progress);

		AthenaProfileAttributes attributes = (AthenaProfileAttributes) getThisApplication().profileManager.getProfileData("athena").stats.attributesObj;
		txtSeason.setBackground(new SeasonBackgroundDrawable(this));
		txtSeason.setText(String.format("Season %,d", attributes.season_num));
		((TextView) findViewById(R.id.p_level)).setText(String.format("%,d", attributes.level));
		boolean battlePassMax = attributes.book_level == 100;
		boolean levelMax = attributes.level == 100;

		if (levelMax) {
			xpBar.setMax(1);
			xpBar.setProgress(1);
			txtLvlUpReward.setVisibility(View.GONE);
			txtXpProgress.setText("MAX");
		} else {
			int maxXP = FortniteCompanionApp.MAX_XPS_S8[attributes.level - 1];
			int next = attributes.book_level + 1;
			xpBar.setMax(maxXP);
			Utils.progressBarSetProgressAnimateFromEmpty(xpBar, attributes.xp);
			txtLvlUpReward.setVisibility(View.VISIBLE);
			((ImageView) findViewById(R.id.p_lvl_up_reward_img)).setImageBitmap(Utils.loadTga(this, battlePassMax ? "/Game/UI/Foundation/Textures/Icons/Items/T-FNBR-SeasonalXP.T-FNBR-SeasonalXP" : "/Game/UI/Foundation/Textures/Icons/Items/T-FNBR-BattlePoints.T-FNBR-BattlePoints"));
			txtLvlUpReward.setText(String.format("%,d", (next % 10 == 0 ? 10 : next % 5 == 0 ? 5 : 2) * (battlePassMax ? 100 : 1)));
			txtXpProgress.setText(String.format("%,d / %,d", attributes.xp, maxXP));
		}

		findViewById(R.id.p_battle_pass_container).setBackground(new BattlePassBackgroundDrawable(this));
		((ImageView) findViewById(R.id.p_battle_pass_img)).setImageBitmap(Utils.loadTga(this, attributes.book_purchased ? "/Game/UI/Foundation/Textures/Icons/Items/T-FNBR-BattlePass.T-FNBR-BattlePass" : "/Game/UI/Foundation/Textures/Icons/Items/T-FNBR-BattlePass-Default.T-FNBR-BattlePass-Default"));
		((TextView) findViewById(R.id.p_battle_pass_type)).setText(attributes.book_purchased ? "Battle Pass" : "Free Pass");
		((TextView) findViewById(R.id.p_battle_pass_tier)).setText(String.format("%,d", attributes.book_level));
		((ImageView) findViewById(R.id.p_battle_pass_stars_img)).setImageBitmap(Utils.loadTga(this, "/Game/UI/Foundation/Textures/Icons/Items/T-FNBR-BattlePoints.T-FNBR-BattlePoints"));
		((TextView) findViewById(R.id.p_battle_pass_stars)).setText(battlePassMax ? "MAX" : TextUtils.concat(Utils.color(String.format("%,d", attributes.book_xp), 0xFFFFFF66), " / 10"));
	}

	private void updateLoginText(String displayName) {
		loginText.setVisibility(displayName == null ? View.GONE : View.VISIBLE);
		loginText.setText(displayName);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		if (requestCode == UPDATE_IF_OK_REQ_CODE && resultCode == RESULT_OK) {
			checkLogin();
		}
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
			case R.id.main_screen_btn_profile:
				startActivity(new Intent(this, ProfileActivity.class));
				break;
			case R.id.main_screen_btn_challenges:
				startActivity(new Intent(this, ChallengesActivity.class));
				break;
			case R.id.main_screen_btn_events:
				startActivity(new Intent(this, EventsActivity.class));
				break;
			case R.id.main_screen_btn_stats:
				AlertDialog dialog = Utils.createEditTextDialog(this, "Epic Name, Email, or Account ID", getString(android.R.string.ok), new Utils.EditTextDialogCallback() {
					@Override
					public void onResult(String s) {
						if (s.length() < 3) {
							return;
						}

						if (s.length() != 32) {
							final Call<GameProfile> call = s.matches("\\S+@\\S+\\.\\S+") ? getThisApplication().accountPublicService.accountByEmail(s) : getThisApplication().accountPublicService.accountByName(s);
							new Thread() {
								@Override
								public void run() {
									try {
										Response<GameProfile> response = call.execute();

										if (response.isSuccessful()) {
											BRStatsActivity.openStats(MainActivity.this, response.body());
										} else {
											Utils.dialogOkNonMain(MainActivity.this, "Can't Search Name", EpicError.parse(response).getDisplayText());
										}
									} catch (IOException e) {
										Utils.throwableDialog(MainActivity.this, e);
									}
								}
							}.start();
							return;
						}

						BRStatsActivity.openStats(MainActivity.this, new GameProfile(s, null));
					}
				});
				dialog.setButton(DialogInterface.BUTTON_NEUTRAL, "My Stats", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						BRStatsActivity.openStats(MainActivity.this, new GameProfile(prefs.getString("epic_account_id", ""), null));
					}
				});
				dialog.show();
				EditText editText = dialog.findViewById(R.id.dialog_edit_text_field);
				editText.setHint(null);
				editText.setSingleLine();
				break;
			case R.id.main_screen_btn_locker:
				startActivity(new Intent(this, LockerActivity.class));
				break;
			case R.id.main_screen_btn_item_shop:
				startActivity(new Intent(this, ItemShopActivity.class));
				break;
			case R.id.main_screen_btn_news:
				Intent intent = new Intent(this, NewsActivity.class);
				intent.putExtra("a", 1);
				startActivity(intent);
				break;
			case R.id.main_screen_btn_stw:
				startActivity(new Intent(this, SaveTheWorldMainActivity.class));
				break;
			case R.id.main_screen_btn_login:
				openLogin();
				break;
		}
	}

	@Override
	public boolean onLongClick(View v) {
		if (v.getId() == R.id.main_screen_btn_news) {
			Intent intent = new Intent(this, NewsActivity.class);
			intent.putExtra("a", 0);
			startActivity(intent);
			return true;
		}

		return false;
	}

	private void openLogin() {
		startActivityForResult(new Intent(this, LoginActivity.class), UPDATE_IF_OK_REQ_CODE);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		menuVbucks = menu.add("V-Bucks").setActionView(vBucksView = (ViewGroup) getLayoutInflater().inflate(R.layout.vbucks, null)).setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_ALWAYS);
		menuVbucks.setVisible(false);
		menu.add(0, 901, 0, R.string.title_activity_settings);
		menu.add(0, 902, 0, "Purchase 1000 V-Bucks");
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == 901) {
			startActivity(new Intent(this, SettingsActivity.class));
		} else if (item.getItemId() == 902) {
			if (!prefs.getBoolean("is_logged_in", false)) {
				return true;
			}

			callExchange = getThisApplication().accountPublicService.oauthExchange();
			new Thread("Exchange Worker") {
				@Override
				public void run() {
					try {
						Response<ExchangeResponse> response = callExchange.execute();

						if (response.isSuccessful()) {
							startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(String.format("https://accounts.epicgames.com/exchange?exchangeCode=%s&redirectUrl=%s", response.body().code, Uri.encode(StoreActivity.getOfferPurchaseUrl("ede05b3c97e9475a8d9be91da65750f0"))))));
						} else {
							Utils.dialogError(MainActivity.this, EpicError.parse(response).getDisplayText());
						}
					} catch (IOException e) {
						Utils.throwableDialog(MainActivity.this, e);
					}
				}
			}.start();
		}

		return super.onOptionsItemSelected(item);
	}

	private static class SeasonBackgroundDrawable extends Drawable {
		private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
		private final Path path = new Path();
		private final float density;

		public SeasonBackgroundDrawable(Context ctx) {
			density = ctx.getResources().getDisplayMetrics().density;
		}

		@Override
		public void draw(@NonNull Canvas canvas) {
			Rect rect = getBounds();
			path.reset();
			path.moveTo(rect.width(), 2.0F * density);
			path.lineTo(rect.width() / 2.0F - density, 6.0F * density);
			path.lineTo(rect.width() / 2.0F + density, 0);
			path.lineTo(0.0F, 2.0F * density);
			path.lineTo(0.0F, rect.height());
			path.lineTo(rect.width(), rect.height() - 3.0F * density);
			path.close();
			paint.setShader(new LinearGradient(0.0F, 0.0F, rect.width(), 0.0F, 0xFF1D66CC, 0xFF4190E2, Shader.TileMode.CLAMP));
			canvas.drawPath(path, paint);
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

	private static class BattlePassBackgroundDrawable extends Drawable {
		private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
		private final Path path = new Path();
		private final float density;

		public BattlePassBackgroundDrawable(Context ctx) {
			density = ctx.getResources().getDisplayMetrics().density;
		}

		@Override
		public void draw(@NonNull Canvas canvas) {
			Rect rect = getBounds();
			float off = density * 12.0F;
			path.reset();
			path.moveTo(rect.width(), off + density * 4.0F);
			path.lineTo(0.0F, off);
			path.lineTo(0.0F, rect.height());
			path.lineTo(rect.width(), rect.height());
			path.close();
			paint.setShader(new LinearGradient(0.0F, 0.0F, rect.width(), 0.0F, 0xFFC87E27, 0xFF863C20, Shader.TileMode.CLAMP));
			canvas.drawPath(path, paint);
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
