package com.armongate.mobilepasssdk.service;

import android.util.Log;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;


public class AccessPointService {

    // Listener defined earlier
    public interface ServiceResultListener{
        void onCompleted(String result);
        void onError(Exception error);
    }

    public void remoteOpen(final ServiceResultListener listener) {
        String url = "https://dev4.armon.com.tr:8442/p/v1/7faf6dc2-f317-4d71-ae71-931e45eceab5/report/monthly/userinfo/2020/6";

        Log.i("DENEME", "Remote open requested!");

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest
                (Request.Method.GET, url, null, new Response.Listener<JSONObject>() {

                    @Override
                    public void onResponse(JSONObject response) {
                        Log.i("MobilePass", "Response received " + response.toString());
                        listener.onCompleted(response.toString());
                    }
                }, new Response.ErrorListener() {

                    @Override
                    public void onErrorResponse(VolleyError error) {
                        // TODO: Handle error
                        Log.i("DENEME", "Error received " + error.getLocalizedMessage());
                        listener.onError(error);
                    }
                }){
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                String token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpIjoiYmEyNWY3OTQtYjZmZi00Y2E0LThkNTItZTdjMTEzYzEyMTI3IiwidSI6ImU1MjRjMTMwLTA5NWYtNDM5NC1hMjMyLWM3ODFlYTU5ZjA4ZSIsIm8iOlt7ImkiOiI3ZmFmNmRjMi1mMzE3LTRkNzEtYWU3MS05MzFlNDVlY2VhYjUiLCJkIjpmYWxzZSwicCI6WyJlOnUiLCJpOmIiLCJpOmQiLCJpOmUiLCJpOmMiLCJpOmgiLCJpOmEiLCJpOnUiLCJpOmciLCJpOmN4IiwiaTpmdyIsImk6bmEiLCJpOm5zIiwiaTpiYSIsImk6YnYiLCJsOmQiLCJsOnUiLCJsOnMiLCJhOmEiLCJ1OmIiLCJ1OmQiLCJ1OnciLCJvOmUiLCJnOnIiLCJnOnciLCJqOnIiLCJqOnciLCJqOmwiLCJzOnIiLCJzOnciLCJzOmZyIiwiczpmdyIsInM6ZSIsInA6cyIsInY6ciIsInY6dyIsInY6cCIsInY6YSIsInY6YiIsInY6dCIsInY6diIsInY6cGQiLCJyOnciLCJuOnIiLCJuOnciLCJoOnIiLCJoOnciLCJocDpyIiwiaHA6dyIsImhwOmgiLCJocDpsYSIsImw6bSIsImw6bWQiLCJzYzpyIiwic2M6dyIsImhwOm13Il0sInIiOiI3MzJkZGMyYi1hOGFjLTQ1NDYtOTIyOS0wYTllMjAxMTVjY2EiLCJ1IjpbeyJpIjoiM2M4ZWNjYzctYWQ5MS00OGYwLWFjYzYtMjE5MTE5MTE5N2FmIiwicCI6WyJlOnUiLCJ2OnAiLCJwOnMiXX1dfV0sImlhdCI6MTYxMjkwNTQ2MCwiZXhwIjoxNjEyOTA5MDYwfQ.KEyD5Ypvx7RfZk7Uj6nXyP4Vb6yUQgeKnqUeWt3iUUs";

                Map<String, String>  params = new HashMap<>();
                params.put("Content-Type", "application/json");
                params.put("Accept", "application/json");
                params.put("Authorization", "Bearer " + token);

                return params;
            }
        };

        BaseService.getInstance().addToRequestQueue(jsonObjectRequest);
    }
}
