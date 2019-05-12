package com.tb24.fn.activity;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
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

import retrofit2.Call;
import retrofit2.Response;

public class MainActivity extends BaseActivity implements View.OnClickListener, View.OnLongClickListener {
	private static final int[] MAX_XPS = new int[]{100, 200, 300, 400, 500, 650, 800, 950, 1100, 1250, 1400, 1550, 1700, 1850, 2000, 2150, 2300, 2450, 2600, 2750, 2900, 3050, 3200, 3350, 3500, 3650, 3800, 3950, 4100, 4250, 4400, 4550, 4700, 4850, 5000, 5150, 5300, 5450, 5600, 5800, 6000, 6200, 6400, 6600, 6800, 7000, 7200, 7400, 7600, 7800, 8100, 8400, 8700, 9000, 9300, 9600, 9900, 10200, 10500, 10800, 11200, 11600, 12000, 12400, 12800, 13200, 13600, 14000, 14400, 14800, 15300, 15800, 16300, 16800, 17300, 17800, 18300, 18800, 19300, 19800, 20800, 21800, 22800, 23800, 24800, 25800, 26800, 27800, 28800, 30800, 32800, 34800, 36800, 38800, 40800, 42800, 45800, 49800, 54800};
	private static final int UPDATE_IF_OK_REQ_CODE = 0;

	private SharedPreferences prefs;
	private Button loginBtn;
	private boolean loggedIn;
	private MenuItem menuVbucks;
	private ViewGroup vBucksView, profileFrame, profileContent, profileLoader;
	private Call<FortMcpResponse> callMcpCommonPublic;
	private Call<FortMcpResponse> callMcpCommonCore;
	private Call<FortMcpResponse> callMcpAthena;
	private Call<XGameProfile> callSelfName;
	private LoadingViewController profileLc;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.i("a", String.valueOf(getResources().getDisplayMetrics().density));
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
		profileFrame = findViewById(R.id.profile_frame);
		profileContent = findViewById(R.id.p_root);
		DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
		boolean sideBySide = (displayMetrics.widthPixels / displayMetrics.density) >= FortniteCompanionApp.MIN_DP_FOR_TWOCOLUMN;
		((LinearLayout) findViewById(R.id.activity_main_root)).setOrientation(sideBySide ? LinearLayout.HORIZONTAL : LinearLayout.VERTICAL);

		if (sideBySide) {
			profileFrame.getLayoutParams().width = (int) Utils.dp(getResources(), 300);
		}

		profileLc = new LoadingViewController(profileFrame, profileContent, "No profile data") {
			@Override
			public boolean shouldShowEmpty() {
				return !getThisApplication().profileManager.profileData.containsKey("athena");
			}
		};
		checkLogin();
		getThisApplication().eventBus.register(this);
	}

	private void checkLogin() {
		loggedIn = prefs.getBoolean("is_logged_in", false);
		findViewById(R.id.main_screen_btn_stats).setEnabled(loggedIn);
		findViewById(R.id.main_screen_btn_events).setEnabled(loggedIn);
		findViewById(R.id.main_screen_btn_locker).setEnabled(loggedIn);
		findViewById(R.id.main_screen_btn_stw).setEnabled(loggedIn);

		if (loggedIn) {
			updateLogInButtonText("...");
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
									updateLogInButtonText((getThisApplication().currentLoggedIn = response.body()).getDisplayName());
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
			updateLogInButtonText(null);
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
	public void onProfileUpdateFailed(ProfileUpdatedEvent event) {
		if (event.profileId.equals("athena") && !getThisApplication().profileManager.profileData.containsKey("athena")) {
			profileLc.content();
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
	}

	private void validateLoggedIn(EpicError error) {
		if (checkAuthError(error)) {
			getThisApplication().clearLoginData();
			new AlertDialog.Builder(MainActivity.this)
					.setTitle("You have been logged out")
					.setMessage("Log in again to continue using some features")
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

	public static boolean checkAuthError(EpicError error) {
		boolean invalidToken = error.errorCode.equals("errors.com.epicgames.common.oauth.invalid_token") || error.numericErrorCode == 1014;
		boolean tokenVerificationFailed = error.errorCode.equals("errors.com.epicgames.common.authentication.token_verification_failed") || error.numericErrorCode == 1031;
		boolean authenticationFailed = error.errorCode.equals("errors.com.epicgames.common.oauth.authentication_failed") || error.numericErrorCode == 1032;
		return invalidToken || tokenVerificationFailed || authenticationFailed;
	}

	private void displayAthenaLevelAndBattlePass() {
		profileLc.content();

		if (!getThisApplication().profileManager.profileData.containsKey("athena")) {
			return;
		}

		//TODO animate
		AthenaProfileAttributes attributes = (AthenaProfileAttributes) getThisApplication().profileManager.profileData.get("athena").stats.attributesObj;
		((TextView) findViewById(R.id.p_season)).setText("Season " + attributes.season_num);
		((TextView) findViewById(R.id.p_level)).setText(String.valueOf(attributes.level));
		int i = MAX_XPS[attributes.level - 1];
		ProgressBar progressBar = findViewById(R.id.progressBar);
		boolean battlePassMax = attributes.book_level == 100;
		boolean levelMax = attributes.level == 100;

		if (levelMax) {
			progressBar.setMax(1);
			progressBar.setProgress(1);
			findViewById(R.id.p_lvl_up_reward).setVisibility(View.GONE);
			((TextView) findViewById(R.id.p_xp_progress)).setText("MAX");
		} else {
			progressBar.setMax(i);
			progressBar.setProgress(attributes.xp);
			findViewById(R.id.p_lvl_up_reward).setVisibility(View.VISIBLE);
			int next = attributes.book_level + 1;
			((ImageView) findViewById(R.id.p_lvl_up_reward_img)).setImageDrawable(getDrawable(battlePassMax ? R.drawable.t_fnbr_seasonalxp_l : R.drawable.t_fnbr_battlepoints_l));
			((TextView) findViewById(R.id.p_lvl_up_reward)).setText(String.valueOf((next % 10 == 0 ? 10 : next % 5 == 0 ? 5 : 2) * (battlePassMax ? 100 : 1)));
			((TextView) findViewById(R.id.p_xp_progress)).setText(String.format("%,d / %,d", attributes.xp, i));
		}

		((ImageView) findViewById(R.id.p_battle_pass_img)).setImageDrawable(getDrawable(attributes.book_purchased ? R.drawable.t_fnbr_battlepass_l : R.drawable.t_fnbr_battlepass_default_l));
		((TextView) findViewById(R.id.p_battle_pass_type)).setText(attributes.book_purchased ? "Battle Pass" : "Free Pass");
		((TextView) findViewById(R.id.p_battle_pass_tier)).setText(String.valueOf(attributes.book_level));
		((TextView) findViewById(R.id.p_battle_pass_stars)).setText(battlePassMax ? "MAX" : (attributes.book_xp + " / 10"));
	}

	private void updateLogInButtonText(String displayName) {
		loginBtn.setText(displayName == null ? "Log in" : ("Logged in as: " + displayName));
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
				if (loggedIn) {
					new AlertDialog.Builder(this)
							.setTitle("Log out?")
							.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog, int which) {
									new Thread("Logout Worker") {
										@Override
										public void run() {
											Call<Void> call = getThisApplication().accountPublicService.oauthSessionsKillAccessToken(prefs.getString("epic_account_access_token", null));

											try {
												Response<Void> response = call.execute();

//												if (response.isSuccessful()) {
												getThisApplication().clearLoginData();
												runOnUiThread(new Runnable() {
													@Override
													public void run() {
														recreate();
													}
												});
//												} else {
//													Utils.dialogOkNonMain(MainActivity.this, "Can't Log Out", EpicError.parse(response).getDisplayText());
//												}
											} catch (IOException e) {
												Utils.throwableDialog(MainActivity.this, e);
											}
										}
									}.start();
								}
							})
							.setNegativeButton("No", null)
							.show();
				} else {
					openLogin();
				}
				break;
		}
	}

	private void openLogin() {
		startActivityForResult(new Intent(this, LoginActivity.class), UPDATE_IF_OK_REQ_CODE);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		menuVbucks = menu.add("V-Bucks").setActionView(vBucksView = (ViewGroup) LayoutInflater.from(this).inflate(R.layout.vbucks, null)).setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_ALWAYS);
		menuVbucks.setVisible(false);
		menu.add(0, 901, 0, "Settings");
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
