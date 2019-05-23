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
	public static final String FORTNITECONTENT_WEBSITE = "https://fortnitecontent-website-prod07.ol.epicgames.com";
	public static final String FORTNITE_PUBLIC_SERVICE = "https://fortnite-public-service-prod11.ol.epicgames.com";
	public static final String ACCOUNT_PUBLIC_SERVICE = "https://account-public-service-prod03.ol.epicgames.com";
	public static final String PERSONA_PUBLIC_SERVICE = "https://persona-public-service-prod06.ol.epicgames.com";
	public static final String EVENTS_PUBLIC_SERVICE = "https://events-public-service-live.ol.epicgames.com";
	public static final int[] MAX_XPS_S8 = new int[]{100, 200, 300, 400, 500, 650, 800, 950, 1100, 1250, 1400, 1550, 1700, 1850, 2000, 2150, 2300, 2450, 2600, 2750, 2900, 3050, 3200, 3350, 3500, 3650, 3800, 3950, 4100, 4250, 4400, 4550, 4700, 4850, 5000, 5150, 5300, 5450, 5600, 5800, 6000, 6200, 6400, 6600, 6800, 7000, 7200, 7400, 7600, 7800, 8100, 8400, 8700, 9000, 9300, 9600, 9900, 10200, 10500, 10800, 11200, 11600, 12000, 12400, 12800, 13200, 13600, 14000, 14400, 14800, 15300, 15800, 16300, 16800, 17300, 17800, 18300, 18800, 19300, 19800, 20800, 21800, 22800, 23800, 24800, 25800, 26800, 27800, 28800, 30800, 32800, 34800, 36800, 38800, 40800, 42800, 45800, 49800, 54800};
	public static final int MIN_DP_FOR_TWOCOLUMN = 600;
	public final Gson gson = new GsonBuilder().registerTypeAdapter(FortMcpProfile.class, new FortMcpProfile.Serializer()).create();
	public final EventBus eventBus = new EventBus();
	public final LruCache<String, Bitmap> bitmapCache = new LruCache<String, Bitmap>(16 * 1024 * 1024) {
		@Override
		protected int sizeOf(String key, Bitmap value) {
			return value.getByteCount();
		}
	};
	public static RarityData[] sRarityData;
	public OkHttpClient okHttpClient;
	public FortniteContentWebsiteService fortniteContentWebsiteService;
	public FortnitePublicService fortnitePublicService;
	public AccountPublicService accountPublicService;
	public PersonaPublicService personaPublicService;
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
		Retrofit.Builder retrofitBuilder = new Retrofit.Builder().client(okHttpClient = builder.build()).addConverterFactory(GsonConverterFactory.create(gson));
		fortniteContentWebsiteService = retrofitBuilder.baseUrl(FORTNITECONTENT_WEBSITE).build().create(FortniteContentWebsiteService.class);
		fortnitePublicService = retrofitBuilder.baseUrl(FORTNITE_PUBLIC_SERVICE).build().create(FortnitePublicService.class);
		accountPublicService = retrofitBuilder.baseUrl(ACCOUNT_PUBLIC_SERVICE).build().create(AccountPublicService.class);
		personaPublicService = retrofitBuilder.baseUrl(PERSONA_PUBLIC_SERVICE).build().create(PersonaPublicService.class);
		eventsPublicServiceLive = retrofitBuilder.baseUrl(EVENTS_PUBLIC_SERVICE).build().create(EventsPublicServiceLive.class);
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
