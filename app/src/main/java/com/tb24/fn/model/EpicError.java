package com.tb24.fn.model;

import android.util.Log;

import com.google.gson.annotations.Expose;
import com.tb24.fn.util.Utils;

import java.io.IOException;

import retrofit2.Response;

public class EpicError {
	public String errorCode;
	public String errorMessage;
	public String[] messageVars;
	public Integer numericErrorCode;
	public String originatingService;
	public String intent;
	@Expose(serialize = false, deserialize = false)
	public Response<?> response;

	public static EpicError parse(Response<?> response) {
		EpicError out = parse(response, EpicError.class);
		out.response = response;
		return out;
	}

	public static <T extends EpicError> T parse(Response<?> response, Class<T> toErrorClass) {
		try {
			String s = response.errorBody().string();
			Log.e("EpicError", s);
			Log.e("EpicError", String.valueOf(response.code()));
			return Utils.DEFAULT_GSON.fromJson(s, toErrorClass);
		} catch (IOException e) {
			throw new RuntimeException("Unexpected error whilst parsing error data", e);
		}
	}

	public String getDisplayText() {
		return errorMessage.isEmpty() ? errorCode : errorMessage;
	}
}
