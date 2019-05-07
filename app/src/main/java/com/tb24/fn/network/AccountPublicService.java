package com.tb24.fn.network;

import com.tb24.fn.model.ExchangeResponse;
import com.tb24.fn.model.ExternalAuth;
import com.tb24.fn.model.GameProfile;
import com.tb24.fn.model.LoginResponse;
import com.tb24.fn.model.VerifyResponse;
import com.tb24.fn.model.XGameProfile;

import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.http.DELETE;
import retrofit2.http.Field;
import retrofit2.http.FieldMap;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface AccountPublicService {
	/**
	 * grant_type: password; fields: username, password
	 * grant_type: refresh_token; fields: refresh_token
	 * grant_type: otp; fields: otp, challenge
	 */
	@FormUrlEncoded
	@POST("/account/api/oauth/token")
	Call<LoginResponse> oauthToken(@Header("Authorization") String auth, @Field("grant_type") String grantType, @FieldMap Map<String, String> fields, @Field("includePerms") boolean includePerms);

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
	Call<GameProfile[]> accountsMultiple(@Query("accountId") List<String> ids);

	@GET("/account/api/public/account/{id}")
	Call<XGameProfile> account(@Query("id") String id);

	@GET("/account/api/public/account/{id}/externalAuths")
	Call<ExternalAuth[]> accountExternalAuths(@Query("id") String id);
}
