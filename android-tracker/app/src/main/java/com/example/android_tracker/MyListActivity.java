package com.example.android_tracker;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.support.design.widget.TabLayout;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;


import com.google.android.gms.maps.*;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

public class MyListActivity extends FragmentActivity implements View.OnClickListener
{
    private static JsonArray familyList;
    private static JsonArray friendList;
    private static JsonArray othersList;

    private static final int REQUEST_LOCATION = 9003;
    private GoogleMap mMap;
    private Intent userDataIntent;
    Context context = this;
    Timer timer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_list);

        userDataIntent = getIntent();
        findViewById(R.id.manage_button).setOnClickListener(this);

        TabLayout mTabLayout = findViewById(R.id.tabs);
        mTabLayout.setOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                onTabTapped(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                onTabTapped(tab.getPosition());
            }
        });
    }

    @Override
    public void onResume()
    {
        super.onResume();

        if(timer != null)
        {
            timer.cancel();
        }

        timer = new Timer();

        timer.scheduleAtFixedRate(new TimerTask() {
            private JsonArray curList;
            synchronized public void run()
            {
                URL url = null;
                HttpURLConnection urlConnection = null;
                TabLayout mTabLayout = findViewById(R.id.tabs);
                String serverUrl = "http://trackerapp-185915.appspot.com/groupsDroid?userToken=" +
                                    userDataIntent.getStringExtra("userToken") + "&userEmail=" +
                                    userDataIntent.getStringExtra("userEmail") ;

                try
                {
                    Log.i("**-** SERVING URL ---->", serverUrl);

                    int i;
                    String payload = "";
                    url = new URL(serverUrl);
                    urlConnection = (HttpURLConnection) url.openConnection();
                    InputStream is = new BufferedInputStream(urlConnection.getInputStream());

                    while(is != null && (i = is.read()) != -1)
                    {
                        payload += (char)i;
                    }

                    JsonParser parser = new JsonParser();
                    JsonObject json = parser.parse(payload).getAsJsonObject().getAsJsonObject("groupsMembers");

                    // Update lists
                    familyList = json.get("Family").getAsJsonArray();
                    friendList = json.get("Friends").getAsJsonArray();
                    othersList = json.get("Others").getAsJsonArray();
                }
                catch (MalformedURLException e)
                {
                    e.printStackTrace();
                }
                catch (IOException e)
                {
                    e.printStackTrace();
                }
                finally
                {
                    urlConnection.disconnect();
                }

                if(mTabLayout.getSelectedTabPosition() == 0)
                {
                    curList = familyList;
                }
                else if(mTabLayout.getSelectedTabPosition() == 1)
                {
                    curList = friendList;
                }
                else if(mTabLayout.getSelectedTabPosition() == 2)
                {
                    curList = othersList;
                }

                runOnUiThread(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        // Obtain the MapFragment and get notified when the map is ready to be used.
                        MapFragment mapFragment = (MapFragment) getFragmentManager().findFragmentById(R.id.map);
                        mapFragment.getMapAsync(new OnMapReadyCallback()
                        {
                            @Override
                            public void onMapReady(GoogleMap googleMap)
                            {
                                mMap = googleMap;
                                updateGroupLayout(googleMap, curList);
                            }
                        });
                    }
                });
            }
        }, TimeUnit.SECONDS.toMillis(0), TimeUnit.SECONDS.toMillis(60));
    }

    @Override
    public void onPause()
    {
        super.onPause();
        if(timer != null)
        {
            timer.cancel();
        }
    }

    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults)
    {
        if (requestCode == REQUEST_LOCATION)
        {
            if(grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
            {
            }
            else
            {
                Toast.makeText(this, "Permission not granted! ", Toast.LENGTH_SHORT);
            }
        }
    }

    @SuppressLint("MissingPermission")
    void updateGroupLayout(GoogleMap map, final JsonArray lst)
    {
        if(map == null)
        {
            return;
        }

        try
        {
            map.clear();
            LatLng loc = null;
            LinearLayout layout = (LinearLayout) findViewById(R.id.Trackees_layout);
            layout.removeAllViews();

            if(lst == null)
            {
                return;
            }

            int i = 0;
            for(JsonElement element : lst)
            {
                try
                {
                    String email = element.getAsJsonObject().get("email").getAsString();
                    try
                    {
                        double lat = element.getAsJsonObject().get("lat").getAsDouble();
                        double lon = element.getAsJsonObject().get("lon").getAsDouble();
                        userDataIntent.putExtra("lat", lat);
                        userDataIntent.putExtra("lon", lon);

                        loc = new LatLng(lat, lon);
                        map.addMarker(new MarkerOptions().title(email).snippet("In USA").position(loc));
                        layout.addView(new TrackeeButton(context, userDataIntent, email));
                    }
                    catch (Exception e)
                    {
                        layout.addView(new TrackeeButton(context, userDataIntent, email + "   [n/a]"));
                    }

                    i++;
                }
                catch(Exception e)
                {
                }
            }

            if(loc != null)
            {
                map.moveCamera(CameraUpdateFactory.newLatLngZoom(loc, 13));
            }
        }
        catch (Exception e)
        {
        }
    }

    @Override
    protected void onStart()
    {
        super.onStart();
        AsyncHttp navigator = new AsyncHttp(context, findViewById(R.id.tabs), userDataIntent);
    }

    public void onTabTapped(int position)
    {
        switch (position) {
            case 0:
                updateGroupLayout(mMap, familyList);
                break;
            case 1:
                updateGroupLayout(mMap, friendList);
                break;
            case 2:
                updateGroupLayout(mMap, othersList);
                break;
            default:
                Toast.makeText(this, "Tapped " + position, Toast.LENGTH_SHORT);
        }
    }

    @Override
    public void onClick(View view)
    {
        switch (view.getId()) {
            case R.id.manage_button:
                userDataIntent.setClass(context,ManageActivity.class);
                startActivity(userDataIntent);
                break;
        }
    }
}
