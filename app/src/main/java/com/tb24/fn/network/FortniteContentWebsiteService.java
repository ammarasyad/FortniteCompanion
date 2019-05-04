package com.tb24.fn.network;

import com.tb24.fn.model.FortBasicDataResponse;

import retrofit2.Call;
import retrofit2.http.GET;

public interface FortniteContentWebsiteService {
	@GET("/content/api/pages/fortnite-game")
	Call<FortBasicDataResponse> getBasicData();
}
