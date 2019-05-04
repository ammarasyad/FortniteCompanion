package com.tb24.fn.model;

import android.util.Log;

import com.tb24.fn.util.Utils;

import java.io.IOException;

import retrofit2.Response;

public class EpicError {
	public String errorCode;
	public String errorMessage;
	public String[] messageVars;
	public int numericErrorCode;
	public String originatingService;
	public String intent;

	public static EpicError parse(Response<?> response) {
		try {
			String s = response.errorBody().string();
			Log.e("EpicError", s);
			return Utils.GSON.fromJson(s, EpicError.class);
		} catch (IOException e) {
			throw new RuntimeException("Unexpected error whilst parsing error data", e);
		}
	}

	public String getDisplayText() {
		return errorMessage.isEmpty() ? errorCode : errorMessage;
	}
}
