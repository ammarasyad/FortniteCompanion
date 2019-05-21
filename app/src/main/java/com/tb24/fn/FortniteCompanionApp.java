package com.tb24.fn;

import android.app.Application;
import android.graphics.Bitmap;
import android.preference.PreferenceManager;
import android.util.LruCache;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.tb24.fn.model.EventDownloadResponse;
import com.tb24.fn.model.FortBasicDataResponse;
import com.tb24.fn.model.FortItemStack;
import com.tb24.fn.model.FortMcpProfile;
import com.tb24.fn.model.RarityData;
import com.tb24.fn.model.XGameProfile;
import com.tb24.fn.network.AccountPublicService;
import com.tb24.fn.network.DefaultInterceptor;
import com.tb24.fn.network.EventsPublicServiceLive;
import com.tb24.fn.network.FortniteContentWebsiteService;
import com.tb24.fn.network.FortnitePublicService;
import com.tb24.fn.network.PersonaPublicService;
import com.tb24.fn.util.ERegion;
import com.tb24.fn.util.ItemUtils;
import com.tb24.fn.util.Utils;

import org.greenrobot.eventbus.EventBus;

import okhttp3.Cache;
import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class FortniteCompanionApp extends Application {
	public static final String CLIENT_TOKEN_FORTNITE = "basic MzQ0NmNkNzI2OTRjNGE0NDg1ZDgxYjc3YWRiYjIxNDE6OTIwOWQ0YTVlMjVhNDU3ZmI5YjA3NDg5ZDMxM2I0MWE=";
	public static final int MIN_DP_FOR_TWOCOLUMN = 600;
	public final Gson gson = new GsonBuilder().registerTypeAdapter(FortMcpProfile.class, new FortMcpProfile.Serializer()).create();
	public final EventBus eventBus = new EventBus();
	public final LruCache<String, Bitmap> bitmapCache = new LruCache<>(512);
	public static RarityData[] sRarityData;
	public FortniteContentWebsiteService fortniteContentWebsiteService;
	public FortnitePublicService fortnitePublicService;
	public AccountPublicService accountPublicService;
	public PersonaPublicService personaService;
	public EventsPublicServiceLive eventsPublicServiceLive;
	public FortBasicDataResponse basicData;
	public EventDownloadResponse eventData;
	public XGameProfile currentLoggedIn;
	public ERegion eventDataRegion;
	public Registry itemRegistry;
	public ProfileManager profileManager;

	@Override
	public void onCreate() {
		super.onCreate();
		OkHttpClient.Builder builder = new OkHttpClient.Builder();
		builder.cache(new Cache(getCacheDir(), 4 * 1024 * 1024));
		builder.addInterceptor(new DefaultInterceptor(this));
//		builder.addInterceptor(new HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY));
		Retrofit.Builder retrofitBuilder = new Retrofit.Builder().client(builder.build()).addConverterFactory(GsonConverterFactory.create(gson));
		fortniteContentWebsiteService = retrofitBuilder.baseUrl("https://fortnitecontent-website-prod07.ol.epicgames.com").build().create(FortniteContentWebsiteService.class);
		fortnitePublicService = retrofitBuilder.baseUrl("https://fortnite-public-service-prod11.ol.epicgames.com").build().create(FortnitePublicService.class);
		accountPublicService = retrofitBuilder.baseUrl("https://account-public-service-prod03.ol.epicgames.com").build().create(AccountPublicService.class);
		personaService = retrofitBuilder.baseUrl("https://persona-public-service-prod06.ol.epicgames.com").build().create(PersonaPublicService.class);
		eventsPublicServiceLive = retrofitBuilder.baseUrl("https://events-public-service-live.ol.epicgames.com").build().create(EventsPublicServiceLive.class);
		itemRegistry = new Registry(this);
		FortItemStack.sRegistry = itemRegistry;
		ItemUtils.sSetData = gson.fromJson(Utils.getStringFromAssets(getAssets(), "Game/Athena/Items/Cosmetics/Metadata/CosmeticSets.json"), JsonArray.class).get(0).getAsJsonObject();
		ItemUtils.sUserFacingTagsData = gson.fromJson(Utils.getStringFromAssets(getAssets(), "Game/Athena/Items/Cosmetics/Metadata/CosmeticUserFacingTags.json"), JsonArray.class).get(0).getAsJsonObject();
		sRarityData = gson.fromJson(Utils.getStringFromAssets(getAssets(), "RarityData.json"), RarityData[].class);
//		dbgRarityData();
		profileManager = new ProfileManager(this);
	}

//	private void dbgRarityData() {
//		for (int i = 0; i < sRarityData.length; i++) {
//			RarityData rarity = sRarityData[i];
//			System.out.printf("Rarity index %d: #%08X #%08X #%08X #%08X #%08X\n", i, rarity.Color1.asInt(), rarity.Color2.asInt(), rarity.Color3.asInt(), rarity.Color4.asInt(), rarity.Color5.asInt());
//		}
//	}

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
		profileManager.profileData.clear();
		eventData = null;
	}
}
