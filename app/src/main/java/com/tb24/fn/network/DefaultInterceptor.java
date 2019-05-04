package com.tb24.fn.network;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;

import java.io.IOException;
import java.util.Locale;

import okhttp3.Request;
import okhttp3.Response;

public class DefaultInterceptor implements okhttp3.Interceptor {
	private final SharedPreferences defaultSharedPreferences;

	public DefaultInterceptor(Context ctx) {
		defaultSharedPreferences = PreferenceManager.getDefaultSharedPreferences(ctx);
	}

	@NonNull
	@Override
	public Response intercept(@NonNull Chain chain) throws IOException {
		Request.Builder builder = chain.request().newBuilder();

		if (defaultSharedPreferences.contains("epic_account_token_type") && defaultSharedPreferences.contains("epic_account_access_token") && chain.request().headers().get("Authorization") == null) {
			builder.addHeader("Authorization", defaultSharedPreferences.getString("epic_account_token_type", "") + " " + defaultSharedPreferences.getString("epic_account_access_token", ""));
		}

		builder.addHeader("Accept-Language", localeToBcp47Language(Locale.getDefault()));
		return chain.proceed(builder.build());
	}

	/*
	 * From https://github.com/apache/cordova-plugin-globalization/blob/master/src/android/Globalization.java#L140
	 * @Description: Returns a well-formed ITEF BCP 47 language tag representing
	 * the locale identifier for the client's current locale
	 *
	 * @Return: String: The BCP 47 language tag for the current locale
	 */
	private static String localeToBcp47Language(Locale loc) {
		final char SEP = '-';       // we will use a dash as per BCP 47
		String language = loc.getLanguage();
		String region = loc.getCountry();
		String variant = loc.getVariant();

		// special case for Norwegian Nynorsk since "NY" cannot be a variant as per BCP 47
		// this goes before the string matching since "NY" wont pass the variant checks
		if (language.equals("no") && region.equals("NO") && variant.equals("NY")) {
			language = "nn";
			region = "NO";
			variant = "";
		}

		if (language.isEmpty() || !language.matches("\\p{Alpha}{2,8}")) {
			language = "und";       // Follow the Locale#toLanguageTag() implementation
			// which says to return "und" for Undetermined
		} else if (language.equals("iw")) {
			language = "he";        // correct deprecated "Hebrew"
		} else if (language.equals("in")) {
			language = "id";        // correct deprecated "Indonesian"
		} else if (language.equals("ji")) {
			language = "yi";        // correct deprecated "Yiddish"
		}

		// ensure valid country code, if not well formed, it's omitted
		if (!region.matches("\\p{Alpha}{2}|\\p{Digit}{3}")) {
			region = "";
		}

		// variant subtags that begin with a letter must be at least 5 characters long
		if (!variant.matches("\\p{Alnum}{5,8}|\\p{Digit}\\p{Alnum}{3}")) {
			variant = "";
		}

		StringBuilder bcp47Tag = new StringBuilder(language);
		if (!region.isEmpty()) {
			bcp47Tag.append(SEP).append(region);
		}
		if (!variant.isEmpty()) {
			bcp47Tag.append(SEP).append(variant);
		}

		return bcp47Tag.toString();
	}
}
