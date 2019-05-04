package com.tb24.fn.network;

import com.google.gson.JsonElement;
import com.tb24.fn.model.AccountCompetitiveData;
import com.tb24.fn.model.EventDownloadResponse;
import com.tb24.fn.model.LeaderboardsResponse;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface EventsPublicServiceLive {
	// /api/v1/players/Fortnite/bd7021824e39499d8be7936cf1e1189d
	@GET("/api/v1/players/Fortnite/{accountId}")
	Call<AccountCompetitiveData> getEventDataForAccount(@Path("accountId") String accountId);

	// /api/v1/events/Fortnite/download/bd7021824e39499d8be7936cf1e1189d?region=ASIA&platform=Windows&teamAccountIds=bd7021824e39499d8be7936cf1e1189d
	@GET("/api/v1/events/Fortnite/download/{accountId}")
	Call<EventDownloadResponse> download(@Path("accountId") String accountId, @Query("region") String region, @Query("platform") String platform, @Query("teamAccountIds") String teamAccountIds);

	// /api/v1/events/Fortnite/epicgames_OnlineOpen_Week2_ASIA/history/bd7021824e39499d8be7936cf1e1189d
	@GET("/api/v1/events/Fortnite/{eventId}/history/{accountId}")
	Call<JsonElement[]> getEventHistoryForAccount(@Path("eventId") String eventId, @Path("accountId") String accountId);

	// /api/v1/leaderboards/Fortnite/epicgames_OnlineOpen_Week2_ASIA/OnlineOpen_Week2_ASIA_Event2/bd7021824e39499d8be7936cf1e1189d?page=0&rank=0&teamAccountIds=&appId=Fortnite&showLiveSessions=false
	@GET("/api/v1/leaderboards/Fortnite/{eventId}/{eventWindowId}/{accountId}")
	Call<LeaderboardsResponse> leaderboards(@Path("eventId") String eventId, @Path("eventWindowId") String eventWindowId, @Path("accountId") String accountId, @Query("page") int page, @Query("rank") int rank, @Query("teamAccountIds") String teamAccountIds, @Query("appId") String appId, @Query("showLiveSessions") boolean showLiveSessions);
}
