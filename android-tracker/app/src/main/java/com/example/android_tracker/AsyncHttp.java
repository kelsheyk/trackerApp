package com.example.android_tracker;

import android.content.Context;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import java.util.ArrayList;

/**
 * Created by carmina on 11/17/17.
 */

public class AsyncHttp extends AppCompatActivity{

    private static final String BASE_URL = "https://trackerapp-185915.appspot.com";

    private Context context;
    private View viewToModify;
    private Intent userDataIntent;
    private AsyncHttpClient client;
    private boolean userIsSignedIn;
    private int trackingIndex = 0;

    public AsyncHttp(Context context, View view, Intent userDataIntent)
    {
        this.context = context;
//        this.viewToModify = view;
//        this.userDataIntent = userDataIntent;
        // this.userIsSignedIn = (userDataIntent.getStringExtra("userId") != null);
        this.client = new AsyncHttpClient();
    }

    private void get(String url, RequestParams params, AsyncHttpResponseHandler responseHandler) {
        client.get(getAbsoluteUrl(url), params, responseHandler);
    }

    private void post(String url, RequestParams params, AsyncHttpResponseHandler responseHandler) {
        client.post(getAbsoluteUrl(url), params, responseHandler);
    }

    private String getAbsoluteUrl(String relativeUrl) {
        String REQ = "userEmail";
        String uriRequested = BASE_URL + relativeUrl;

        if (userIsSignedIn) {
            String userEmail = userDataIntent.getStringExtra(REQ);
            if (!uriRequested.contains("?")) {
                uriRequested += "?" + REQ + "=" + userEmail;
            } else {
                uriRequested += "&" + REQ + "=" + userEmail;
            }
        }

        return uriRequested;
    }

   // private void updateListLayout(final JsonArray tracking)
   // {


   // }
    private  void trackingList(byte[] response)
    {
        try
        {
            String s = new String(response);
            JsonParser parser = new JsonParser();
            JsonObject json = parser.parse(s).getAsJsonObject();
            JsonArray tracked = json.get("tracked_people").getAsJsonArray();
     //       updateListLayout(tracked);
        }
        catch(Exception e)
        {
       //     updateListLayout(null);
        }
    }

    public ArrayList<String> getFamilyList()
    {
        ArrayList<String> ls = new ArrayList<String>();
        ls.add("1--ab1");
        ls.add("1--ab2");
        ls.add("1--ab3");
        ls.add("1--ab4");
        ls.add("1--ab5");
        return ls;
    }

    public ArrayList<String> getFriendList()
    {
        ArrayList<String> ls = new ArrayList<String>();
        ls.add("2--ab1");
        ls.add("2--ab2");
        ls.add("2--ab3");
        ls.add("2--ab4");
        ls.add("2--ab5");
        return ls;
    }

    public ArrayList<String> getOthersList()
    {
        ArrayList<String> ls = new ArrayList<String>();
        ls.add("3--ab1");
        ls.add("3--ab2");
        ls.add("3--ab3");
        ls.add("3--ab4");
        ls.add("3--ab5");
        return ls;
    }

    public String getTrackedUserData()
    {
        return "someone";
    }
}
