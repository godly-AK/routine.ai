package com.example.routineai;

import com.google.gson.JsonObject;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface API {
    // We are using the extremely fast "flash" model
    @POST("v1beta/models/gemini-2.5-flash:generateContent")
    Call<JsonObject> getRoutinePlan(
            @Query("key") String apiKey,
            @Body JsonObject body
    );
}