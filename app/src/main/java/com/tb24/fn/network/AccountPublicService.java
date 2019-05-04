package com.tb24.fn.network;

import com.tb24.fn.model.ExchangeResponse;
import com.tb24.fn.model.GameProfile;
import com.tb24.fn.model.LoginResponse;
import com.tb24.fn.model.VerifyResponse;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.DELETE;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface AccountPublicService {
	@FormUrlEncoded
	@POST("/account/api/oauth/token")
	Call<LoginResponse> oauthTokenPassword(@Header("Authorization") String auth, @Field("grant_type") String grantType, @Field("username") String username, @Field("password") String password, @Field("includePerms") boolean includePerms);

	@POST("/account/api/oauth/token")
	Call<LoginResponse> oauthTokenRefreshToken(@Header("Authorization") String auth, @Field("grant_type") String grantType, @Field("refresh_token") String refreshToken, @Field("includePerms") boolean includePerms);

	@GET("/account/api/oauth/exchange")
	Call<ExchangeResponse> oauthExchange();

	@GET("/account/api/oauth/verify")
	Call<VerifyResponse> oauthVerify(@Query("includePerms") boolean includePerms);

	@DELETE("/account/api/oauth/sessions/kill/{accessToken}")
	Call<Void> oauthSessionsKillAccessToken(@Path("accessToken") String accessToken);

	// killType == OTHERS_ACCOUNT_CLIENT_SERVICE
	@FormUrlEncoded
	@DELETE("/account/api/oauth/sessions/kill")
	Call<Void> oauthSessionsKillWithType(@Field("killType") String killType);

	@GET("/account/api/public/account")
	Call<GameProfile[]> getGameProfilesByIds(@Query("accountId") List<String> ids);
}
