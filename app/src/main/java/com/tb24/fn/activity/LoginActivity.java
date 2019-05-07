package com.tb24.fn.activity;

import android.app.AlertDialog;
import android.app.LoaderManager.LoaderCallbacks;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.google.common.collect.ImmutableMap;
import com.tb24.fn.FortniteCompanionApp;
import com.tb24.fn.R;
import com.tb24.fn.model.EpicError;
import com.tb24.fn.model.LoginResponse;
import com.tb24.fn.model.TwoFactorAuthExtendedError;
import com.tb24.fn.util.LoadingViewController;
import com.tb24.fn.util.Utils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Response;

import static android.Manifest.permission.READ_CONTACTS;

public class LoginActivity extends BaseActivity implements LoaderCallbacks<Cursor>, OnClickListener {
	private static final int REQUEST_READ_CONTACTS = 0;
	private AutoCompleteTextView mEmailView;
	private EditText mPasswordView;
	private View mProgressView;
	private View mLoginFormView;
	private boolean running;
	private SharedPreferences prefs;
	private Call<LoginResponse> loginRequest;
	private Call<LoginResponse> twoFaRequest;
	private LoadingViewController lc;
	private Button mEmailSignInButton;
	private Button mGetOneTimePasswordButton;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_login);
		setupActionBar();
		prefs = PreferenceManager.getDefaultSharedPreferences(this);
		mEmailView = findViewById(R.id.email);
		populateAutoComplete();

		mPasswordView = findViewById(R.id.password);
		mPasswordView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
			@Override
			public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
				if (id == EditorInfo.IME_ACTION_DONE || id == EditorInfo.IME_NULL) {
					attemptLogin();
					return true;
				}
				return false;
			}
		});

		mEmailSignInButton = findViewById(R.id.email_sign_in_button);
		mEmailSignInButton.setOnClickListener(this);
		mGetOneTimePasswordButton = findViewById(R.id.btn_get_one_time_password);
		mGetOneTimePasswordButton.setOnClickListener(this);

		mLoginFormView = findViewById(R.id.login_form);
		mProgressView = findViewById(R.id.login_progress);
		lc = new LoadingViewController(mProgressView, mLoginFormView, (TextView) null);
		lc.content();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();

		if (loginRequest != null) {
			loginRequest.cancel();
		}

		if (twoFaRequest != null) {
			twoFaRequest.cancel();
		}
	}

	private void populateAutoComplete() {
		if (!mayRequestContacts()) {
			return;
		}

		getLoaderManager().initLoader(0, null, this);
	}

	private boolean mayRequestContacts() {
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
			return true;
		}
		if (checkSelfPermission(READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
			return true;
		}
		if (shouldShowRequestPermissionRationale(READ_CONTACTS)) {
			// TODO: alert the user with a Snackbar/AlertDialog giving them the permission rationale
			// To use the Snackbar from the design support library, ensure that the activity extends
			// AppCompatActivity and uses the Theme.AppCompat theme.
		} else {
			requestPermissions(new String[]{READ_CONTACTS}, REQUEST_READ_CONTACTS);
		}
		return false;
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
		if (requestCode == REQUEST_READ_CONTACTS) {
			if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
				populateAutoComplete();
			}
		}
	}

	private void attemptLogin() {
		if (running) {
			return;
		}

		mEmailView.setError(null);
		mPasswordView.setError(null);
		final String email = mEmailView.getText().toString();
		final String password = mPasswordView.getText().toString();
		boolean cancel = false;
		View focusView = null;

		if (!TextUtils.isEmpty(password) && !isPasswordValid(password)) {
			mPasswordView.setError(getString(R.string.error_invalid_password));
			focusView = mPasswordView;
			cancel = true;
		}

		if (TextUtils.isEmpty(email)) {
			mEmailView.setError(getString(R.string.error_field_required));
			focusView = mEmailView;
			cancel = true;
		} else if (!isEmailValid(email)) {
			mEmailView.setError(getString(R.string.error_invalid_email));
			focusView = mEmailView;
			cancel = true;
		}

		if (cancel) {
			focusView.requestFocus();
		} else {
			lc.loading();
			running = true;
			handleLogin(email, password);
		}
	}

	private void handleLogin(final String email, String password) {
		loginRequest = getThisApplication().accountPublicService.oauthToken("basic " + FortniteCompanionApp.CLIENT_TOKEN_FORTNITE, "password", ImmutableMap.of("username", email, "password", password), false);
		new Thread() {
			@Override
			public void run() {
				try {
					final Response<LoginResponse> response = loginRequest.execute();

					if (response.isSuccessful()) {
						loginSucceded(response);
					} else {
						runOnUiThread(new Runnable() {
							@Override
							public void run() {
								final TwoFactorAuthExtendedError error = EpicError.parse(response, TwoFactorAuthExtendedError.class);

								if (error.errorCode.equals("errors.com.epicgames.common.two_factor_authentication.required") || error.numericErrorCode == 1042 && error.metadata.twoFactorMethod.equals("email")) {
									AlertDialog dialog = Utils.createEditTextDialog(LoginActivity.this, "Enter your security code", getString(R.string.action_login), new Utils.EditTextDialogCallback() {
										@Override
										public void onResult(String s) {
											handleTwoFa(s, error);
										}
									});
									dialog.setMessage(String.format("Your account has two-factor security enabled. Enter the security code emailed to you at %s.", email));
									dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
										@Override
										public void onCancel(DialogInterface dialog) {
											unshowAsync();
										}
									});
									dialog.setCanceledOnTouchOutside(false);
									dialog.show();
									((EditText) dialog.findViewById(R.id.dialog_edit_text_field)).setHint("Security code");
								} else {
									unshowAsync();
									//The security code you entered was not valid.
									errored(error.getDisplayText());
								}
							}
						});
					}
				} catch (IOException e) {
					unshowAsync();
					errored(Utils.userFriendlyNetError(e));
				}
			}
		}.start();
	}

	private void handleTwoFa(String s, TwoFactorAuthExtendedError error) {
		twoFaRequest = getThisApplication().accountPublicService.oauthToken("basic " + FortniteCompanionApp.CLIENT_TOKEN_FORTNITE, "otp", ImmutableMap.of("otp", s, "challenge", error.challenge), false);
		new Thread() {
			@Override
			public void run() {
				try {
					Response<LoginResponse> twoFaResponse = twoFaRequest.execute();

					if (twoFaResponse.isSuccessful()) {
						loginSucceded(twoFaResponse);
					} else {
						unshowAsync();
						errored(EpicError.parse(twoFaResponse).getDisplayText());
					}
				} catch (IOException e) {
					unshowAsync();
					errored(Utils.userFriendlyNetError(e));
				}
			}
		}.start();
	}

	private void errored(CharSequence errorMessage) {
		Utils.dialogOkNonMain(this, "Login Failed", errorMessage);
	}

	private void loginSucceded(Response<LoginResponse> response) {
		LoginResponse data = response.body();
		prefs.edit()
				.putBoolean("is_logged_in", true)
				.putString("epic_account_token_type", data.token_type)
				.putLong("epic_account_expires_at", data.expires_at.getTime())
				.putString("epic_account_refresh_token", data.refresh_token)
				.putString("epic_account_access_token", data.access_token)
				.putString("epic_account_id", data.account_id)
				.apply();
		setResult(RESULT_OK);
		finish();
	}

	private void unshowAsync() {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				running = false;
				lc.content();
			}
		});
	}

	private boolean isEmailValid(String email) {
		return email.contains("@") && email.contains(".");
	}

	private boolean isPasswordValid(String password) {
//		return password.length() > 4;
		return !password.isEmpty();
	}

	@Override
	public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
		return new CursorLoader(this,
				// Retrieve data rows for the device user's 'profile' contact.
				Uri.withAppendedPath(ContactsContract.Profile.CONTENT_URI,
						ContactsContract.Contacts.Data.CONTENT_DIRECTORY), ProfileQuery.PROJECTION,

				// Select only email addresses.
				ContactsContract.Contacts.Data.MIMETYPE +
						" = ?", new String[]{ContactsContract.CommonDataKinds.Email
				.CONTENT_ITEM_TYPE},

				// Show primary email addresses first. Note that there won't be
				// a primary email address if the user hasn't specified one.
				ContactsContract.Contacts.Data.IS_PRIMARY + " DESC");
	}

	@Override
	public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
		List<String> emails = new ArrayList<>();
		cursor.moveToFirst();
		while (!cursor.isAfterLast()) {
			emails.add(cursor.getString(ProfileQuery.ADDRESS));
			cursor.moveToNext();
		}

		addEmailsToAutoComplete(emails);
	}

	@Override
	public void onLoaderReset(Loader<Cursor> cursorLoader) {

	}

	private void addEmailsToAutoComplete(List<String> emailAddressCollection) {
		//Create adapter to tell the AutoCompleteTextView what to show in its dropdown list.
		ArrayAdapter<String> adapter =
				new ArrayAdapter<>(LoginActivity.this,
						android.R.layout.simple_dropdown_item_1line, emailAddressCollection);

		mEmailView.setAdapter(adapter);
	}

	@Override
	public void onClick(View v) {
		if (v == mEmailSignInButton) {
			attemptLogin();
		} else if (v == mGetOneTimePasswordButton) {
			startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://accounts.epicgames.com/account/oneTimePassword")));
		}
	}


	private interface ProfileQuery {
		String[] PROJECTION = {
				ContactsContract.CommonDataKinds.Email.ADDRESS,
				ContactsContract.CommonDataKinds.Email.IS_PRIMARY,
		};

		int ADDRESS = 0;
		int IS_PRIMARY = 1;
	}

}

