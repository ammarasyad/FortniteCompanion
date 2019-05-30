package com.tb24.fn.network;

import com.google.gson.JsonObject;

import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface CatalogPublicService {
	@GET("/catalog/api/shared/bulk/offers")
	Call<Map<String, JsonObject>> bulkOffers(@Query("id") List<String> ids);
}
