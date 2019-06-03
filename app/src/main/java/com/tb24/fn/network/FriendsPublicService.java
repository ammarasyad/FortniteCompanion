package com.tb24.fn.network;

import com.google.gson.JsonObject;
import com.tb24.fn.model.BlockedUsers;
import com.tb24.fn.model.Friend;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface FriendsPublicService {
	@GET("/friends/api/public/friends/{id}")
	Call<Friend[]> friends(@Path("id") String id, @Query("includePending") Boolean includePending);

	@GET("/friends/api/public/blocklist/{id}")
	Call<BlockedUsers> blockList(@Path("id") String id);

	@GET("/friends/api/v1/{id}/settings")
	Call<JsonObject> settings(@Path("id") String id);

	@GET("/friends/api/public/list/fortnite/{id}/recentPlayers")
	Call<Friend[]> recentPlayers(@Path("id") String id);
}
