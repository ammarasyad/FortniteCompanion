package com.tb24.fn.util;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.icu.text.MeasureFormat;
import android.icu.util.Measure;
import android.icu.util.MeasureUnit;
import android.os.Build;
import android.text.Editable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextWatcher;
import android.text.style.ForegroundColorSpan;
import android.text.style.TtsSpan;
import android.util.Log;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import com.google.common.base.Charsets;
import com.google.common.base.Joiner;
import com.google.common.io.CharStreams;
import com.google.gson.Gson;
import com.tb24.fn.R;
import com.tb24.fn.activity.BaseActivity;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public final class Utils {
	public static final Gson DEFAULT_GSON = new Gson();
	public static final String[] STRINGS = {"That wasn't supposed to happen", "There was an error", "We hit a roadblock", "Not the llama you're looking for", "Whoops!", "Uh oh!  Something goofed", "Drat! We're sorry"};
	public static final Random RANDOM = new Random();
	public static final DialogInterface.OnClickListener LISTENER_TO_CANCEL = new DialogInterface.OnClickListener() {
		@Override
		public void onClick(DialogInterface dialog, int which) {
			dialog.cancel();
		}
	};
	private static final int SECONDS_PER_MINUTE = 60;
	private static final int SECONDS_PER_HOUR = 60 * 60;
	private static final int SECONDS_PER_DAY = 24 * 60 * 60;
	private static final Joiner SPACE_JOINER = Joiner.on(' ');
	private static final SimpleDateFormat SIMPLE_DATE_FORMAT = new SimpleDateFormat();
	private static Integer sColorPrimaryDark;
	private static Integer sTextColorPrimary;

	private Utils() {
	}

	/**
	 * Returns elapsed time for the given millis, in the following format:
	 * 2d 5h 40m 29s
	 *
	 * @param context     the application context
	 * @param millis      the elapsed time in milli seconds
	 * @param withSeconds include seconds?
	 * @return the formatted elapsed time
	 */
	// from: https://android.googlesource.com/platform/frameworks/base/+/master/packages/SettingsLib/src/com/android/settingslib/utils/StringUtil.java
	public static CharSequence formatElapsedTime(Context context, double millis, boolean withSeconds) {
		SpannableStringBuilder sb = new SpannableStringBuilder();
		int seconds = (int) Math.floor(millis / 1000);
		if (!withSeconds) {
			// Round up.
			seconds += 30;
		}

		int days = 0, hours = 0, minutes = 0;
		if (seconds >= SECONDS_PER_DAY) {
			days = seconds / SECONDS_PER_DAY;
			seconds -= days * SECONDS_PER_DAY;
		}
		if (seconds >= SECONDS_PER_HOUR) {
			hours = seconds / SECONDS_PER_HOUR;
			seconds -= hours * SECONDS_PER_HOUR;
		}
		if (seconds >= SECONDS_PER_MINUTE) {
			minutes = seconds / SECONDS_PER_MINUTE;
			seconds -= minutes * SECONDS_PER_MINUTE;
		}

		if (Build.VERSION.SDK_INT >= 24) {
			final List<Measure> measureList = new ArrayList<>(4);
			if (days > 0) {
				measureList.add(new Measure(days, MeasureUnit.DAY));
			}
			if (hours > 0) {
				measureList.add(new Measure(hours, MeasureUnit.HOUR));
			}
			if (minutes > 0) {
				measureList.add(new Measure(minutes, MeasureUnit.MINUTE));
			}
			if (withSeconds && seconds > 0) {
				measureList.add(new Measure(seconds, MeasureUnit.SECOND));
			}
			if (measureList.size() == 0) {
				// Everything addable was zero, so nothing was added. We add a zero.
				measureList.add(new Measure(0, withSeconds ? MeasureUnit.SECOND : MeasureUnit.MINUTE));
			}
			final Measure[] measureArray = measureList.toArray(new Measure[measureList.size()]);

			final Locale locale = context.getResources().getConfiguration().locale;
			final MeasureFormat measureFormat = MeasureFormat.getInstance(locale, MeasureFormat.FormatWidth.NARROW);
			sb.append(measureFormat.formatMeasures(measureArray));

			if (measureArray.length == 1 && MeasureUnit.MINUTE.equals(measureArray[0].getUnit())) {
				// Add ttsSpan if it only have minute value, because it will be read as "meters"
				final TtsSpan ttsSpan = new TtsSpan.MeasureBuilder().setNumber(minutes).setUnit("minute").build();
				sb.setSpan(ttsSpan, 0, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
			}
		} else {
			//TODO localization for api < 24
			List<String> list = new ArrayList<>(4);
			if (days > 0) {
				list.add(days + "d");
			}
			if (hours > 0) {
				list.add(hours + "h");
			}
			if (minutes > 0) {
				list.add(minutes + "m");
			}
			if (withSeconds && seconds > 0) {
				list.add(seconds + "s");
			}
			if (list.size() == 0) {
				// Everything addable was zero, so nothing was added. We add a zero.
				list.add(0 + (withSeconds ? "s" : "m"));
			}
			sb.append(SPACE_JOINER.join(list));
		}

		return sb;
	}

	public static AlertDialog createEditTextDialog(Context ctx, @Nullable String title, CharSequence posText, final EditTextDialogCallback callback) {
		View view = LayoutInflater.from(ctx).inflate(R.layout.dialog_edit_text, null);
		final EditText editText = view.findViewById(R.id.dialog_edit_text_field);
		final AlertDialog ad = new AlertDialog.Builder(ctx)
				.setTitle(title)
				.setView(view)
				.setPositiveButton(posText, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						callback.onResult(editText.getText().toString());
					}
				})
				.setNegativeButton(android.R.string.cancel, LISTENER_TO_CANCEL)
				.create();
		ad.setOnShowListener(new DialogInterface.OnShowListener() {
			@Override
			public void onShow(DialogInterface dialog) {
				final Button button = ad.getButton(DialogInterface.BUTTON_POSITIVE);
				button.setEnabled(editText.toString().trim().length() != 0);
				editText.addTextChangedListener(new TextWatcher() {
					@Override
					public void beforeTextChanged(CharSequence s, int start, int count, int after) {
					}

					@Override
					public void onTextChanged(CharSequence s, int start, int before, int count) {
					}

					@Override
					public void afterTextChanged(Editable s) {
						button.setEnabled(s.toString().trim().length() > 0);
					}
				});
				editText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
					@Override
					public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
						if (i == EditorInfo.IME_ACTION_DONE) {
							if (button.isEnabled()) {
								button.callOnClick();
							}

							return true;
						}

						return false;
					}
				});
				editText.requestFocus();
			}
		});
		return ad;
	}

	public static float dp(Resources res, int size) {
		return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, size, res.getDisplayMetrics());
	}

	public static float dpF(Resources res, float size) {
		return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, size, res.getDisplayMetrics());
	}

//	public static Bitmap getBitmapFromAssets(Context ctx, String fileName) {
//		AssetManager assetManager = ctx.getAssets();
//		InputStream istr;
//
//		try {
//			istr = assetManager.open(fileName);
//		} catch (IOException e) {
//			e.printStackTrace();
//			return null;
//		}
//
//		Bitmap bitmap = BitmapFactory.decodeStream(istr);
//
//		try {
//			istr.close();
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
//
//		return bitmap;
//	}

	public static String getStringFromAssets(AssetManager assetManager, String fileName) {
		String s;

		try (InputStream inputStream = assetManager.open(fileName)) {
			s = CharStreams.toString(new InputStreamReader(inputStream, Charsets.UTF_8));
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}

		return s;
	}

	public static void throwableDialog(Activity activity, Throwable e) {
		dialogOkNonMain(activity, STRINGS[RANDOM.nextInt(STRINGS.length - 1)], userFriendlyNetError(e), "Continue");
	}

	public static void dialogError(Activity activity, CharSequence msg) {
		dialogOkNonMain(activity, STRINGS[RANDOM.nextInt(STRINGS.length - 1)], msg, "Continue");
	}

	public static void dialogOkNonMain(final Activity activity, final CharSequence title, final CharSequence message) {
		dialogOkNonMain(activity, title, message, activity.getString(android.R.string.ok));
	}

	public static void dialogOkNonMain(final Activity activity, final CharSequence title, final CharSequence message, final CharSequence okText) {
		if (activity != null && !activity.isDestroyed()) {
			activity.runOnUiThread(new Runnable() {
				@Override
				public void run() {
					new AlertDialog.Builder(activity)
							.setTitle(title)
							.setMessage(message)
							.setPositiveButton(okText, null)
							.show();
				}
			});
		}
	}

	public static Spanned color(CharSequence s, int color) {
		return span(s, new ForegroundColorSpan(color));
	}

	public static Spanned span(CharSequence s, Object what) {
		SpannableString spannableString = new SpannableString(s);
		spannableString.setSpan(what, 0, s.length(), 0);
		return spannableString;
	}

	public static Spanned makeItDark(CharSequence s, Context ctx) {
		if (s == null) {
			return null;
		}

		if (sColorPrimaryDark == null) {
			TypedValue typedValue = new TypedValue();
			TypedArray arr = ctx.obtainStyledAttributes(typedValue.data, new int[]{android.R.attr.colorAccent});
			sColorPrimaryDark = arr.getColor(0, 0xFFFF00FF);
			arr.recycle();
		}

		return color(s, sColorPrimaryDark);
	}

	public static int getTextColorPrimary(Context ctx) {
		if (sTextColorPrimary == null) {
			TypedValue typedValue = new TypedValue();
			TypedArray arr = ctx.obtainStyledAttributes(typedValue.data, new int[]{android.R.attr.textColorPrimary});
			sTextColorPrimary = arr.getColor(0, 0xFFFF00FF);
			arr.recycle();
		}

		return sTextColorPrimary;
	}

	public static String formatDateSimple(Date time) {
		return SIMPLE_DATE_FORMAT.format(time);
	}

	public static boolean isSameDay(final Date date1, final Date date2) {
		if (date1 == null || date2 == null) {
			throw new IllegalArgumentException("The date must not be null");
		}
		final Calendar cal1 = Calendar.getInstance();
		cal1.setTime(date1);
		final Calendar cal2 = Calendar.getInstance();
		cal2.setTime(date2);
		return isSameDay(cal1, cal2);
	}

	public static boolean isSameDay(final Calendar cal1, final Calendar cal2) {
		if (cal1 == null || cal2 == null) {
			throw new IllegalArgumentException("The date must not be null");
		}
		return cal1.get(Calendar.ERA) == cal2.get(Calendar.ERA) &&
				cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
				cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR);
	}

	public static CharSequence userFriendlyNetError(Throwable e) {
		return e instanceof IOException ? "Connection to the server failed." : e.getLocalizedMessage();
	}

	public static Bitmap loadTga(BaseActivity activity, String uPath) {
		Bitmap bitmap = activity.getThisApplication().bitmapCache.get(uPath);

		synchronized (activity.getThisApplication().bitmapCache) {
			if (bitmap == null) {
				try {
					InputStream is = activity.getAssets().open(parseUPath(uPath) + ".tga");
					byte[] buffer = new byte[is.available()];
					is.read(buffer);
					is.close();
					int[] pixels = TGAReader.read(buffer, TGAReader.ARGB);
					int width = TGAReader.getWidth(buffer);
					int height = TGAReader.getHeight(buffer);
					bitmap = Bitmap.createBitmap(pixels, 0, width, width, height, Bitmap.Config.ARGB_8888);
				} catch (IOException e) {
					Log.w("TGAReader", "Failed loading image\n" + e.toString());
				}

				if (bitmap != null) {
					activity.getThisApplication().bitmapCache.put(uPath, bitmap);
				}
			}
		}

		return bitmap;
	}

	public static String toHexString(byte[] bytes) {
		StringBuilder hexString = new StringBuilder();

		for (byte b : bytes) {
			String hex = Integer.toHexString(0xFF & b);

			if (hex.length() == 1) {
				hexString.append('0');
			}

			hexString.append(hex);
		}

		return hexString.toString();
	}

	public static void progressBarSetProgressAnimateFromEmpty(ProgressBar progressBar, int val) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
			progressBar.setProgress(0);
			progressBar.setProgress(val, true);
		} else {
			progressBar.setProgress(val);
		}
	}

	public static <T> T cloneObjectUsingJson(T object, Type typeOfT) {
//		try {
		return DEFAULT_GSON.fromJson(DEFAULT_GSON.toJson(object, typeOfT), typeOfT);
//		} catch (JsonSyntaxException e) {
//			e.printStackTrace();
//			return null;
//		}
	}

	public static String parseUPath(String path) {
		if (path == null) {
			return null;
		} else if (path.equals("None")) {
			return "";
		}

		String s = path.substring(1, path.lastIndexOf('.'));
		return s.endsWith("_L") || s.endsWith("-L") ? s.substring(0, s.length() - 2) : s;
	}

	public static String parseUPath2(String path) {
		if (path == null) {
			return null;
		} else if (path.equals("None")) {
			return "";
		}

		return path.substring(path.lastIndexOf('/') + 1, path.lastIndexOf('.'));
	}

	public static PackageInfo getPackageInfo(Context context) {
		PackageInfo pInfo = null;

		try {
			pInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
		} catch (PackageManager.NameNotFoundException ignored) {
		}

		return pInfo;
	}

	public interface EditTextDialogCallback {
		void onResult(String s);
	}
}
