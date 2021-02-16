package com.armongate.mobilepasssdk.service;

import android.content.Context;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;

public class BaseService {

    // Singleton

    private static BaseService  instance = null;
    private BaseService () { }

    public static BaseService getInstance() {
        if (instance == null) {
            instance = new BaseService ();
        }

        return instance;
    }

    // Private Fields

    private RequestQueue requestQueue;
    private Context activeContext;

    // Public Functions

    public void setContext(Context context) {
        this.activeContext = context;
    }

    public RequestQueue getRequestQueue() {
        if (requestQueue == null) {
            // getApplicationContext() is key, it keeps you from leaking the
            // Activity or BroadcastReceiver if someone passes one in.
            requestQueue = Volley.newRequestQueue(activeContext.getApplicationContext());
        }
        return requestQueue;
    }

    public <T> void addToRequestQueue(Request<T> req) {
        getRequestQueue().add(req);
    }
}
