package com.tb24.fn.activity;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
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

import com.tb24.fn.FortniteCompanionApp;
import com.tb24.fn.R;
import com.tb24.fn.event.ProfileUpdateFailedEvent;
import com.tb24.fn.event.ProfileUpdatedEvent;
import com.tb24.fn.model.AthenaProfileAttributes;
import com.tb24.fn.model.EpicError;
import com.tb24.fn.model.FortMcpResponse;
import com.tb24.fn.model.GameProfile;
import com.tb24.fn.model.XGameProfile;
import com.tb24.fn.util.LoadingViewController;
import com.tb24.fn.util.Utils;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.IOException;
import java.net.HttpURLConnection;

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
	private LoadingViewController profileLc;

	public static boolean checkAuthError(EpicError error) {
		boolean isForbidden = error.response.code() == HttpURLConnection.HTTP_FORBIDDEN;
		boolean invalidToken = error.errorCode.equals("errors.com.epicgames.common.oauth.invalid_token") || error.numericErrorCode == 1014;
		boolean tokenVerificationFailed = error.errorCode.equals("errors.com.epicgames.common.authentication.token_verification_failed") || error.numericErrorCode == 1031;
		boolean authenticationFailed = error.errorCode.equals("errors.com.epicgames.common.oauth.authentication_failed") || error.numericErrorCode == 1032;
		return isForbidden || invalidToken || tokenVerificationFailed || authenticationFailed;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
//		Log.i("a", String.valueOf(getResources().getDisplayMetrics().density));
		setContentView(R.layout.activity_main);
//		getActionBar().setSubtitle("DEBUG VERSION -- by Armzyy");
		findViewById(R.id.main_screen_btn_stats).setOnClickListener(this);
		findViewById(R.id.main_screen_btn_item_shop).setOnClickListener(this);
		findViewById(R.id.main_screen_btn_news).setOnClickListener(this);
		findViewById(R.id.main_screen_btn_news).setOnLongClickListener(this);
		findViewById(R.id.main_screen_btn_events).setOnClickListener(this);
		findViewById(R.id.main_screen_btn_profile).setOnClickListener(this);
		findViewById(R.id.main_screen_btn_locker).setOnClickListener(this);
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
				return !getThisApplication().profileManager.profileData.containsKey("athena");
			}
		};
		checkLogin();
		getThisApplication().eventBus.register(this);
	}

	private void checkLogin() {
		boolean loggedIn = prefs.getBoolean("is_logged_in", false);
		findViewById(R.id.main_screen_btn_stats).setEnabled(loggedIn);
		findViewById(R.id.main_screen_btn_events).setEnabled(loggedIn);
		findViewById(R.id.main_screen_btn_locker).setEnabled(loggedIn);
		findViewById(R.id.main_screen_btn_stw).setEnabled(loggedIn);
		loginBtn.setVisibility(loggedIn ? View.GONE : View.VISIBLE);

		if (loggedIn) {
			updateLogInText("...");
			profileLc.loading();
			String accountId = prefs.getString("epic_account_id", "");
			callMcpCommonPublic = getThisApplication().profileManager.requestFullProfileUpdate("common_public");
			callMcpCommonCore = getThisApplication().profileManager.requestFullProfileUpdate("common_core");
			callMcpAthena = getThisApplication().profileManager.requestFullProfileUpdate("athena");
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
									updateLogInText((getThisApplication().currentLoggedIn = response.body()).getDisplayName());
								} else {
									validateLoggedIn(EpicError.parse(response));
								}
							}
						});
					} catch (IOException e) {
						Utils.throwableDialog(MainActivity.this, e);
					}
				}
			}.start();
		} else {
			updateLogInText(null);
			profileLc.content();
			openLogin();
		}
	}

	@Subscribe(threadMode = ThreadMode.MAIN)
	public void onProfileUpdated(ProfileUpdatedEvent event) {
		if (event.profileId.equals("common_public")) {
			// TODO display self banner
		} else if (event.profileId.equals("common_core")) {
			menuVbucks.setVisible(true);
			countAndSetVbucks(MainActivity.this, vBucksView);
			findViewById(R.id.main_screen_btn_item_shop).setEnabled(event.profileObj != null);
		} else if (event.profileId.equals("athena")) {
			findViewById(R.id.main_screen_btn_profile).setEnabled(event.profileObj != null);
			displayAthenaLevelAndBattlePass();
		}
	}

	@Subscribe(threadMode = ThreadMode.MAIN)
	public void onProfileUpdateFailed(ProfileUpdateFailedEvent event) {
		if (event.profileId.equals("athena") && !getThisApplication().profileManager.profileData.containsKey("athena")) {
			profileLc.content();
		}
	}

	@Subscribe(threadMode = ThreadMode.MAIN)
	public void onLoggedOut(LoggedOutEvent event) {
		recreate();
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
	}

	private void validateLoggedIn(EpicError error) {
		if (checkAuthError(error)) {
			getThisApplication().clearLoginData();
			new AlertDialog.Builder(MainActivity.this)
					.setTitle("You have been logged out")
					.setMessage("Please log in again")
					.setPositiveButton(android.R.string.ok, null)
					.setOnDismissListener(new DialogInterface.OnDismissListener() {
						@Override
						public void onDismiss(DialogInterface dialog) {
							recreate();
						}
					})
					.show();
		}
	}

	private void displayAthenaLevelAndBattlePass() {
		profileLc.content();

		if (!getThisApplication().profileManager.profileData.containsKey("athena")) {
			return;
		}

		AthenaProfileAttributes attributes = (AthenaProfileAttributes) getThisApplication().profileManager.profileData.get("athena").stats.attributesObj;
		((TextView) findViewById(R.id.p_season)).setText("Season " + attributes.season_num);
		((TextView) findViewById(R.id.p_level)).setText(String.valueOf(attributes.level));
		int i = FortniteCompanionApp.MAX_XPS_S8[attributes.level - 1];
		boolean battlePassMax = attributes.book_level == 100;
		boolean levelMax = attributes.level == 100;
		ProgressBar progressBar = findViewById(R.id.p_xp_bar);
		View lvlUpReward = findViewById(R.id.p_lvl_up_reward);
		TextView xpProgress = findViewById(R.id.p_xp_progress);

		if (levelMax) {
			progressBar.setMax(1);
			progressBar.setProgress(1);
			lvlUpReward.setVisibility(View.GONE);
			xpProgress.setText("MAX");
		} else {
			progressBar.setMax(i);
			Utils.progressBarSetProgressAnimateFromEmpty(progressBar, attributes.xp);
			lvlUpReward.setVisibility(View.VISIBLE);
			int next = attributes.book_level + 1;
			((ImageView) findViewById(R.id.p_lvl_up_reward_img)).setImageBitmap(Utils.loadTga(this, battlePassMax ? "/Game/UI/Foundation/Textures/Icons/Items/T-FNBR-SeasonalXP.T-FNBR-SeasonalXP" : "/Game/UI/Foundation/Textures/Icons/Items/T-FNBR-BattlePoints.T-FNBR-BattlePoints"));
			((TextView) lvlUpReward).setText(String.valueOf((next % 10 == 0 ? 10 : next % 5 == 0 ? 5 : 2) * (battlePassMax ? 100 : 1)));
			xpProgress.setText(String.format("%,d / %,d", attributes.xp, i));
		}

		((ImageView) findViewById(R.id.p_battle_pass_img)).setImageBitmap(Utils.loadTga(this, attributes.book_purchased ? "/Game/UI/Foundation/Textures/Icons/Items/T-FNBR-BattlePass.T-FNBR-BattlePass" : "/Game/UI/Foundation/Textures/Icons/Items/T-FNBR-BattlePass-Default.T-FNBR-BattlePass-Default"));
		((TextView) findViewById(R.id.p_battle_pass_type)).setText(attributes.book_purchased ? "Battle Pass" : "Free Pass");
		((TextView) findViewById(R.id.p_battle_pass_tier)).setText(String.valueOf(attributes.book_level));
		((ImageView) findViewById(R.id.p_battle_pass_stars_img)).setImageBitmap(Utils.loadTga(this, "/Game/UI/Foundation/Textures/Icons/Items/T-FNBR-BattlePoints.T-FNBR-BattlePoints"));
		((TextView) findViewById(R.id.p_battle_pass_stars)).setText(battlePassMax ? "MAX" : (attributes.book_xp + " / 10"));
	}

	private void updateLogInText(String displayName) {
		loginText.setVisibility(displayName == null ? View.GONE : View.VISIBLE);
		loginText.setText(displayName);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == UPDATE_IF_OK_REQ_CODE && resultCode == RESULT_OK) {
			checkLogin();
		}
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
			case R.id.main_screen_btn_stats:
				AlertDialog dialog = Utils.createEditTextDialog(this, "Epic Display Name or Account ID", getString(android.R.string.ok), new Utils.EditTextDialogCallback() {
					@Override
					public void onResult(String s) {
						if (s.length() < 3) {
							return;
						}

						if (s.length() != 32) {
							final Call<GameProfile> call = getThisApplication().personaService.getAccountIdByDisplayName(s);
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
				dialog.setButton(DialogInterface.BUTTON_NEUTRAL, "My stats", new DialogInterface.OnClickListener() {
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
			case R.id.main_screen_btn_item_shop:
				startActivityForResult(new Intent(this, ItemShopActivity.class), UPDATE_IF_OK_REQ_CODE);
				break;
			case R.id.main_screen_btn_news:
				Intent intent = new Intent(this, NewsActivity.class);
				intent.putExtra("a", 1);
				startActivity(intent);
				break;
			case R.id.main_screen_btn_events:
				startActivity(new Intent(this, EventsActivity.class));
				break;
			case R.id.main_screen_btn_profile:
				startActivity(new Intent(this, ProfileActivity.class));
				break;
			case R.id.main_screen_btn_stw:
				startActivity(new Intent(this, StwWorldInfoActivity.class));
				break;
			case R.id.main_screen_btn_locker:
				startActivity(new Intent(this, LockerActivity.class));
				break;
			case R.id.main_screen_btn_login:
				openLogin();
				break;
		}
	}

	private void openLogin() {
		startActivityForResult(new Intent(this, LoginActivity.class), UPDATE_IF_OK_REQ_CODE);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		menuVbucks = menu.add("V-Bucks").setActionView(vBucksView = (ViewGroup) getLayoutInflater().inflate(R.layout.vbucks, null)).setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_ALWAYS);
		menuVbucks.setVisible(false);
		menu.add(0, 901, 0, R.string.title_activity_settings);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == 901) {
			startActivity(new Intent(this, SettingsActivity.class));
		}

		return super.onOptionsItemSelected(item);
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
}
