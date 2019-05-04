package com.tb24.fn.network;

import com.tb24.fn.model.GameProfile;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface PersonaPublicService {
	@GET("/persona/api/public/account/lookup")
	Call<GameProfile> getAccountIdByDisplayName(@Query("q") String displayName);
}
