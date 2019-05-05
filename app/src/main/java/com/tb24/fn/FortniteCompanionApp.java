package com.tb24.fn;

import android.app.Application;
import android.preference.PreferenceManager;

import com.google.gson.Gson;
import com.tb24.fn.model.EventDownloadResponse;
import com.tb24.fn.model.FortBasicDataResponse;
import com.tb24.fn.model.FortMcpResponse;
import com.tb24.fn.network.AccountPublicService;
import com.tb24.fn.network.DefaultInterceptor;
import com.tb24.fn.network.EventsPublicServiceLive;
import com.tb24.fn.network.FortniteContentWebsiteService;
import com.tb24.fn.network.FortnitePublicService;
import com.tb24.fn.network.PersonaPublicService;
import com.tb24.fn.util.ERegion;

import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class FortniteCompanionApp extends Application {
	public static final String CLIENT_TOKEN_FORTNITE = "MzQ0NmNkNzI2OTRjNGE0NDg1ZDgxYjc3YWRiYjIxNDE6OTIwOWQ0YTVlMjVhNDU3ZmI5YjA3NDg5ZDMxM2I0MWE=";
	public FortniteContentWebsiteService fortniteContentWebsiteService;
	public FortnitePublicService fortnitePublicService;
	public AccountPublicService accountPublicService;
	public PersonaPublicService personaService;
	public EventsPublicServiceLive eventsPublicServiceLive;
	public FortMcpResponse dataCommonPublic;
	public FortMcpResponse dataCommonCore;
	public FortMcpResponse dataAthena;
	public FortMcpResponse dataCampaign;
	public FortMcpResponse dataMetadata;
	public FortBasicDataResponse basicData;
	public EventDownloadResponse eventData;
	public ERegion eventDataRegion;
	public Gson gson = new Gson();
	public Registry itemRegistry;
//	public AthenaProfileAttributes apa;

	@Override
	public void onCreate() {
		super.onCreate();
		OkHttpClient.Builder builder = new OkHttpClient.Builder();
		builder.addInterceptor(new DefaultInterceptor(this));
//		builder.addInterceptor(new HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY));
		Retrofit.Builder retrofitBuilder = new Retrofit.Builder().client(builder.build()).addConverterFactory(GsonConverterFactory.create(gson));
		fortniteContentWebsiteService = retrofitBuilder.baseUrl("https://fortnitecontent-website-prod07.ol.epicgames.com").build().create(FortniteContentWebsiteService.class);
		fortnitePublicService = retrofitBuilder.baseUrl("https://fortnite-public-service-prod11.ol.epicgames.com").build().create(FortnitePublicService.class);
		accountPublicService = retrofitBuilder.baseUrl("https://account-public-service-prod03.ol.epicgames.com").build().create(AccountPublicService.class);
		personaService = retrofitBuilder.baseUrl("https://persona-public-service-prod06.ol.epicgames.com").build().create(PersonaPublicService.class);
		eventsPublicServiceLive = retrofitBuilder.baseUrl("https://events-public-service-live.ol.epicgames.com").build().create(EventsPublicServiceLive.class);
		itemRegistry = new Registry(this);
	}

	public void clearLoginData() {
		PreferenceManager.getDefaultSharedPreferences(this)
				.edit()
				.putBoolean("is_logged_in", false)
				.remove("epic_account_token_type")
				.remove("epic_account_expires_at")
				.remove("epic_account_refresh_token")
				.remove("epic_account_access_token")
				.remove("epic_account_id")
				.apply();
		dataCommonCore = null;
		dataAthena = null;
		eventData = null;
	}
}
