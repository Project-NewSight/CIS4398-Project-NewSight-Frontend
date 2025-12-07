package com.example.newsight.api;

import com.example.newsight.api.models.ClosetResponse;
import com.example.newsight.api.models.MatchResponse;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.Query;
import com.example.newsight.api.models.ClothingItem;
import com.example.newsight.api.models.IdentifyResponse;


public interface ColorCueApi {

    @Multipart
    @POST("/colorcue/add_item")
    Call<ClothingItem> uploadItem(
            @Part("closet_id") RequestBody closetId,
            @Part("genre") RequestBody genre,
            @Part("notes") RequestBody notes,
            @Part MultipartBody.Part front_image,
            @Part MultipartBody.Part tag_front_image,
            @Part MultipartBody.Part tag_back_image
    );


    @Multipart
    @POST("/colorcue/identify_item")
    Call<IdentifyResponse> identifyItem(
            @Part("closet_id") RequestBody closetId,
            @Part MultipartBody.Part file
    );


    @GET("/voice/closet_query")
    Call<ClosetResponse> getCloset(
            @Query("user_id") int userId,
            @Query("color") String color,
            @Query("category") String category
    );

    @Multipart
    @POST("/colorcue/does_this_match")
    Call<MatchResponse> doesThisMatch(
            @Part("user_id") RequestBody userId,
            @Part MultipartBody.Part file
    );

    @Multipart
    @POST("/colorcue/compare_items")
    Call<MatchResponse> compareItems(
            @Part MultipartBody.Part top,
            @Part MultipartBody.Part bottom
    );

}
