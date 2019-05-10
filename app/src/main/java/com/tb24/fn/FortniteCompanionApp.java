package com.tb24.fn;

import android.app.Application;
import android.graphics.Bitmap;
import android.preference.PreferenceManager;
import android.util.Log;
import android.util.LruCache;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.tb24.fn.event.ProfileUpdateFailedEvent;
import com.tb24.fn.event.ProfileUpdatedEvent;
import com.tb24.fn.model.EpicError;
import com.tb24.fn.model.EventDownloadResponse;
import com.tb24.fn.model.FortBasicDataResponse;
import com.tb24.fn.model.FortItemStack;
import com.tb24.fn.model.FortMcpProfile;
import com.tb24.fn.model.FortMcpResponse;
import com.tb24.fn.model.XGameProfile;
import com.tb24.fn.network.AccountPublicService;
import com.tb24.fn.network.DefaultInterceptor;
import com.tb24.fn.network.EventsPublicServiceLive;
import com.tb24.fn.network.FortniteContentWebsiteService;
import com.tb24.fn.network.FortnitePublicService;
import com.tb24.fn.network.PersonaPublicService;
import com.tb24.fn.util.ERegion;
import com.tb24.fn.util.JsonUtils;

import org.greenrobot.eventbus.EventBus;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class FortniteCompanionApp extends Application {
	public static final String CLIENT_TOKEN_FORTNITE = "basic MzQ0NmNkNzI2OTRjNGE0NDg1ZDgxYjc3YWRiYjIxNDE6OTIwOWQ0YTVlMjVhNDU3ZmI5YjA3NDg5ZDMxM2I0MWE=";
	public static final int MIN_DP_FOR_TWOCOLUMN = 600;
	public final LruCache<String, Bitmap> bitmapCache = new LruCache<>(512);
	public FortniteContentWebsiteService fortniteContentWebsiteService;
	public FortnitePublicService fortnitePublicService;
	public AccountPublicService accountPublicService;
	public PersonaPublicService personaService;
	public EventsPublicServiceLive eventsPublicServiceLive;
	public Map<String, FortMcpProfile> profileData = new HashMap<>();
	public FortBasicDataResponse basicData;
	public EventDownloadResponse eventData;
	public ERegion eventDataRegion;
	public Gson gson = new GsonBuilder().registerTypeAdapter(FortMcpProfile.class, new FortMcpProfile.Serializer()).create();
	public Registry itemRegistry;
	public EventBus eventBus = new EventBus();
	public XGameProfile currentLoggedIn;

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
		profileData.clear();
		eventData = null;
	}

	public void executeProfileChanges(FortMcpResponse response) {
		if (response.profileChanges != null) {
			for (JsonObject obj : response.profileChanges) {
				String changeType = JsonUtils.getStringOr("changeType", obj, "");

				if (changeType.equals("fullProfileUpdate")) {
					FortMcpProfile parsed = gson.fromJson(obj.get("profile"), FortMcpProfile.class);
					profileData.put(response.profileId, parsed);
					eventBus.post(new ProfileUpdatedEvent(response.profileId, parsed));
					Log.d("MCP-Profile", String.format("Full profile update (rev=%d, version=%s@w=%d) for %s accountId=MCP:%s profileId=%s", parsed.rvn, parsed.version, parsed.wipeNumber, currentLoggedIn == null ? "" : currentLoggedIn.getDisplayName(), parsed.accountId, parsed.profileId));
				} else if (changeType.equals("itemAdded")) {
					if (!profileData.containsKey(response.profileId)) {
						return;
					}

					FortMcpProfile profile = profileData.get(response.profileId);
					profile.items.put(obj.get("itemId").getAsString(), gson.fromJson(obj.get("item"), FortItemStack.class));
					eventBus.post(new ProfileUpdatedEvent(response.profileId, profile));
				}
			}
		}

		if (response.multiUpdate != null) {
			for (FortMcpResponse multiUpdateEntry : response.multiUpdate) {
				executeProfileChanges(multiUpdateEntry);
			}
		}
	}

	public Call<FortMcpResponse> requestFullProfileUpdate(final String profileId) {
		final Call<FortMcpResponse> call = fortnitePublicService.mcp("QueryProfile", PreferenceManager.getDefaultSharedPreferences(this).getString("epic_account_id", ""), profileId, -1, true, new JsonObject());
		new Thread() {
			@Override
			public void run() {
				try {
					Response<FortMcpResponse> execute = call.execute();

					if (execute.isSuccessful()) {
						executeProfileChanges(execute.body());
					} else {
						eventBus.post(new ProfileUpdateFailedEvent(profileId, EpicError.parse(execute)));
					}
				} catch (IOException e) {
					Log.e("MCP-Profile", "Failed requesting profile update " + profileId, e);
				}
			}
		}.start();
		return call;
	}
}
