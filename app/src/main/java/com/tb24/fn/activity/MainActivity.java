package com.tb24.fn.activity;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.gson.JsonObject;
import com.tb24.fn.R;
import com.tb24.fn.model.AthenaProfileAttributes;
import com.tb24.fn.model.EpicError;
import com.tb24.fn.model.FortItemStack;
import com.tb24.fn.model.FortMcpResponse;
import com.tb24.fn.model.GameProfile;
import com.tb24.fn.util.Utils;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Response;

public class MainActivity extends BaseActivity implements View.OnClickListener, View.OnLongClickListener {
	private static final int[] MAX_XPS = new int[]{100, 200, 300, 400, 500, 650, 800, 950, 1100, 1250, 1400, 1550, 1700, 1850, 2000, 2150, 2300, 2450, 2600, 2750, 2900, 3050, 3200, 3350, 3500, 3650, 3800, 3950, 4100, 4250, 4400, 4550, 4700, 4850, 5000, 5150, 5300, 5450, 5600, 5800, 6000, 6200, 6400, 6600, 6800, 7000, 7200, 7400, 7600, 7800, 8100, 8400, 8700, 9000, 9300, 9600, 9900, 10200, 10500, 10800, 11200, 11600, 12000, 12400, 12800, 13200, 13600, 14000, 14400, 14800, 15300, 15800, 16300, 16800, 17300, 17800, 18300, 18800, 19300, 19800, 20800, 21800, 22800, 23800, 24800, 25800, 26800, 27800, 28800, 30800, 32800, 34800, 36800, 38800, 40800, 42800, 45800, 49800, 54800};

	private SharedPreferences prefs;
	private Button loginBtn;
	private boolean loggedIn;
	private MenuItem menuVbucks;
	private ViewGroup vBucksView, profileFrame, profileContainer, profileLoader;
	private Call<FortMcpResponse> callMcpCommonCore;
	private Call<FortMcpResponse> callMcpAthena;
	private Call<GameProfile[]> callSelfName;

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
		profileFrame = findViewById(R.id.p_frame);
		profileContainer = findViewById(R.id.p_root);
		profileLoader = (ViewGroup) profileFrame.getChildAt(1);
		checkLogin();
	}

	private void checkLogin() {
		loggedIn = prefs.getBoolean("is_logged_in", false);
		findViewById(R.id.main_screen_btn_stats).setEnabled(loggedIn);
		findViewById(R.id.main_screen_btn_events).setEnabled(loggedIn);
		findViewById(R.id.main_screen_btn_item_shop).setEnabled(loggedIn);
		findViewById(R.id.main_screen_btn_stw).setEnabled(loggedIn);

		if (loggedIn) {
			updateLogInButtonText("...");
			profileLoader.setVisibility(View.VISIBLE);
			profileContainer.setVisibility(View.INVISIBLE);
			String accountId = prefs.getString("epic_account_id", "");
			callMcpCommonCore = getThisApplication().fortnitePublicService.mcp("QueryProfile", accountId, "common_core", -1, true, new JsonObject());
			new Thread() {
				@Override
				public void run() {
					try {
						Response<FortMcpResponse> execute = callMcpCommonCore.execute();

						if (execute.isSuccessful()) {
							getThisApplication().dataCommonCore = execute.body();
						} else {
							EpicError error = EpicError.parse(execute);

							if (!checkAuthError(error)) {
								Utils.dialogError(MainActivity.this, error.getDisplayText());
							}
						}
					} catch (IOException e) {
						Utils.networkErrorDialog(MainActivity.this, e);
					} finally {
						runOnUiThread(new Runnable() {
							@Override
							public void run() {
								if (getThisApplication().dataCommonCore == null) {
									menuVbucks.setVisible(false);
									return;
								}

								int vBucksQty = 0;

								for (Map.Entry<String, FortItemStack> entry : getThisApplication().dataCommonCore.profileChanges.get(0).profile.items.entrySet()) {
									if (entry.getValue().templateId.equals("Currency:MtxGiveaway")) {
										vBucksQty += entry.getValue().quantity;
									}

									Log.d("FortItemStack", ">> " + entry.getValue().templateId);
								}

								menuVbucks.setVisible(true);
								((TextView) vBucksView.getChildAt(1)).setText(String.format("%,d", vBucksQty));
							}
						});
					}
				}
			}.start();
			callMcpAthena = getThisApplication().fortnitePublicService.mcp("QueryProfile", accountId, "athena", -1, true, new JsonObject());
			new Thread() {
				@Override
				public void run() {
					try {
						Response<FortMcpResponse> execute = callMcpAthena.execute();

						if (execute.isSuccessful()) {
							getThisApplication().dataAthena = execute.body();
						} else {
							EpicError error = EpicError.parse(execute);

							if (!checkAuthError(error)) {
								Utils.dialogError(MainActivity.this, error.getDisplayText());
							}
						}
					} catch (IOException e) {
						Utils.networkErrorDialog(MainActivity.this, e);
					} finally {
						runOnUiThread(new Runnable() {
							@Override
							public void run() {
								findViewById(R.id.main_screen_btn_profile).setEnabled(getThisApplication().dataAthena != null);
								findViewById(R.id.main_screen_btn_locker).setEnabled(getThisApplication().dataAthena != null);
								loadProfile();
							}
						});
					}
				}
			}.start();
			callSelfName = getThisApplication().accountPublicService.getGameProfilesByIds(Collections.singletonList(accountId));
			new Thread() {
				@Override
				public void run() {
					try {
						final Response<GameProfile[]> response = callSelfName.execute();
						runOnUiThread(new Runnable() {
							@Override
							public void run() {
								if (response.isSuccessful()) {
									updateLogInButtonText(response.body()[0].getDisplayName());
								} else {
									validateLoggedIn(EpicError.parse(response));
								}
							}
						});
					} catch (IOException e) {
						Utils.networkErrorDialog(MainActivity.this, e);
					}
				}
			}.start();
		} else {
			updateLogInButtonText(null);
			profileFrame.setVisibility(View.GONE);
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

	private void loadProfile() {
		FortMcpResponse dataAthena = getThisApplication().dataAthena;

		if (dataAthena == null) {
			profileFrame.setVisibility(View.GONE);
			return;
		}

		//TODO animate
		profileLoader.setVisibility(View.INVISIBLE);
		profileContainer.setVisibility(View.VISIBLE);
		profileFrame.setVisibility(View.VISIBLE);
		AthenaProfileAttributes attributes = getThisApplication().gson.fromJson(dataAthena.profileChanges.get(0).profile.stats.attributes, AthenaProfileAttributes.class);
		((TextView) findViewById(R.id.p_season)).setText("Season " + attributes.season_num);
		((TextView) findViewById(R.id.p_level)).setText("" + attributes.level);
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
			((TextView) findViewById(R.id.p_lvl_up_reward)).setText("" + (next % 10 == 0 ? 10 : next % 5 == 0 ? 5 : 2) * (battlePassMax ? 100 : 1));
			((TextView) findViewById(R.id.p_xp_progress)).setText(String.format("%,d / %,d", attributes.xp, i));
		}

		((ImageView) findViewById(R.id.p_battle_pass_img)).setImageDrawable(getDrawable(attributes.book_purchased ? R.drawable.t_fnbr_battlepass_l : R.drawable.t_fnbr_battlepass_default_l));
		((TextView) findViewById(R.id.p_battle_pass_type)).setText(attributes.book_purchased ? "Battle Pass" : "Free Pass");
		((TextView) findViewById(R.id.p_battle_pass_tier)).setText("" + attributes.book_level);
		((TextView) findViewById(R.id.p_battle_pass_stars)).setText(battlePassMax ? "MAX" : (attributes.book_xp + " / 10"));
	}

	private void updateLogInButtonText(String displayName) {
		loginBtn.setText(displayName == null ? "Log in" : ("Logged in as: " + displayName));
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == 0 && resultCode == RESULT_OK) {
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
										Utils.networkErrorDialog(MainActivity.this, e);
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
				EditText editText = dialog.findViewById(R.id.dialog_edit_text_field);
				editText.setHint(null);
				editText.setSingleLine();
				break;
			case R.id.main_screen_btn_item_shop:
				startActivity(new Intent(this, ItemShopActivity.class));
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

												if (response.isSuccessful()) {
													getThisApplication().clearLoginData();
													runOnUiThread(new Runnable() {
														@Override
														public void run() {
															recreate();
														}
													});
												} else {
													Utils.dialogOkNonMain(MainActivity.this, "Can't Log Out", EpicError.parse(response).getDisplayText());
												}
											} catch (IOException e) {
												Utils.networkErrorDialog(MainActivity.this, e);
											}
										}
									}.start();
								}
							})
							.setNegativeButton("No", null)
							.show();
				} else {
					startActivityForResult(new Intent(this, LoginActivity.class), 0);
				}
				break;
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		menuVbucks = menu.add("V-Bucks").setActionView(vBucksView = (ViewGroup) LayoutInflater.from(this).inflate(R.layout.vbucks, null)).setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_ALWAYS).setVisible(false);
		menu.add(0, 900, 0, "Dump Login Data");
		menu.add(0, 901, 0, "Settings");
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == 900) {
			Log.d("MainActivity", "Access Token: " + prefs.getString("epic_account_access_token", null));
			Log.d("MainActivity", "Refresh Token: " + prefs.getString("epic_account_refresh_token", null));
		} else if (item.getItemId() == 901) {
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
