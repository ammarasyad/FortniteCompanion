package com.tb24.fn.network;

import com.google.gson.JsonObject;
import com.tb24.fn.model.AccountPrivacyResponse;
import com.tb24.fn.model.CalendarTimelineResponse;
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
	@POST("/fortnite/api/game/v2/profile/{id}/client/{command}")
	Call<FortMcpResponse> mcp(@Path("command") String command, @Path("id") String accountId, @Query("profileId") String profileId, @Query("rvn") int rvn, @Body Object payload);

	@GET("/fortnite/api/game/v2/world/info")
	Call<WorldInfoResponse> pveWorldInfo();

	@GET("/fortnite/api/game/v2/privacy/account/{id}")
	Call<AccountPrivacyResponse> getAccountPrivacy(@Path("id") String id);

	@POST("/fortnite/api/game/v2/privacy/account/{id}")
	Call<AccountPrivacyResponse> setAccountPrivacy(@Path("id") String id, @Body AccountPrivacyResponse payload);

	@GET("/fortnite/api/storefront/v2/catalog")
	Call<FortCatalogResponse> storefrontCatalog();

	@GET("/fortnite/api/storefront/v2/keychain")
	Call<String[]> storefrontKeychain();

	@GET("/fortnite/api/calendar/v1/timeline")
	Call<CalendarTimelineResponse> calendarTimeline();

	@GET("/fortnite/api/statsv2/account/{id}")
	Call<FortStatsV2Response> statsV2(@Path("id") String id);

	@GET("/fortnite/api/cloudstorage/user/{id}")
	Call<JsonObject[]> enumerateUserFiles(@Path("id") String id);

	//TODO what method?
	@GET("/fortnite/api/game/v2/events/v2/processPendingRewards/{id}")
	Call<JsonObject[]> eventsProcessPendingRewards(@Path("id") String id);
}
