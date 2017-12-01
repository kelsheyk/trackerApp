package com.example.android_tracker;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

import cz.msebera.android.httpclient.Header;

/**
 * Created by carmina on 11/17/17.
 */

public class AsyncHttp extends AppCompatActivity{

    private static final String BASE_URL = "https://trackerapp-185915.appspot.com";

    private Context context;
    private View viewToModify;
    private Intent userDataIntent;
    private AsyncHttpClient client;
    private int trackingIndex = 0;
    private static String phoneNumber;

    private static int REQUEST_USER_PHONE = 9004;


    public AsyncHttp(Context context, View view, Intent userDataIntent)
    {
        this.context = context;
//        this.viewToModify = view;
//        this.userDataIntent = userDataIntent;
        this.client = new AsyncHttpClient();

        try
        {
            TelephonyManager tMgr = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED
                    && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(null, new String[]{TELEPHONY_SERVICE}, REQUEST_USER_PHONE);
            }

            phoneNumber = tMgr.getLine1Number();
        }
        catch (Exception e)
        {
            phoneNumber = "Not available";
        }
    }

    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults)
    {
        if (requestCode == REQUEST_USER_PHONE)
        {
            if(grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
            {
            }
            else
            {
                Toast.makeText(this, "Phone permission not granted! ", Toast.LENGTH_SHORT);
            }
        }
    }


    private void get(String url, RequestParams params, AsyncHttpResponseHandler responseHandler)
    {
        client.get(getAbsoluteUrl(url), params, responseHandler);
    }

    private void post(String url, RequestParams params, AsyncHttpResponseHandler responseHandler) {
        client.post(url, params, responseHandler);
    }

    private String getAbsoluteUrl(String relativeUrl)
    {
        String userPhone = "userPhone";
        String uriRequested = BASE_URL + relativeUrl;

        if (!uriRequested.contains("?"))
        {
            uriRequested += "?" + userPhone + "=" + phoneNumber;
        }
        else
        {
            uriRequested += "&" + userPhone + "=" + phoneNumber;
        }

        Log.i("==> URI ", uriRequested);
        return uriRequested;
    }

    public String getTrackedUserData()
    {
        return "someone";
    }

    public void postMyLocation(final String serverUrl)
    {
        HttpURLConnection urlConnection = null;

        try
        {
            int i;
            this.post(serverUrl, null, new AsyncHttpResponseHandler() {
                @Override
                public void onSuccess(int statusCode, Header[] headers, byte[] responseBody)
                {
                }

                @Override
                public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {

                }
            });
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

}
