package com.tb24.fn.network;

import com.google.gson.JsonElement;
import com.tb24.fn.model.FortCatalogResponse;
import com.tb24.fn.model.FortMcpResponse;
import com.tb24.fn.model.FortStatsV2Response;
import com.tb24.fn.model.WorldInfoResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface FortnitePublicService {
	@GET("/fortnite/api/statsv2/account/{id}")
	Call<FortStatsV2Response> getStatsV2ForAccountId(@Path("id") String id);

	@POST("/fortnite/api/game/v2/profile/{id}/client/{command}")
	Call<FortMcpResponse> mcp(@Path("command") String command, @Path("id") String accountId, @Query("profileId") String profileId, @Query("rvn") int rvn, @Query("leanResponse") boolean leanResponse, @Body JsonElement payload);

	@GET("/fortnite/api/storefront/v2/catalog")
	Call<FortCatalogResponse> catalog();

	@GET("/fortnite/api/game/v2/world/info")
	Call<WorldInfoResponse> pveWorldInfo();
}
