package com.example.android_tracker;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.support.design.widget.TabLayout;
import android.support.v4.app.ActivityCompat;
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

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

public class MyListActivity extends FragmentActivity implements View.OnClickListener
{
    private static ArrayList<String> familyList;
    private static ArrayList<String> friendList;
    private static ArrayList<String> othersList;

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

//        Log.i("*** URL ---->", "A10");
//        if(userDataIntent != null)
//            Log.i("*** URL ---->", userDataIntent.getStringExtra("userToken"));
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
            private ArrayList<String> curList;
            synchronized public void run()
            {
                URL url = null;
                TabLayout mTabLayout = findViewById(R.id.tabs);
                // String serverUrl = "http://trackerapp-185915.appspot.com";
                String serverUrl = "http://trackerapp-185915.appspot.com/groupsDroid?userId=" +
                                    userDataIntent.getStringExtra("userId") +
                                    "&userToken=" + userDataIntent.getStringExtra("userToken") ;
                HttpURLConnection urlConnection = null;

                try
                {
                    Log.i("**-** URL ---->", serverUrl);

                    int i;
                    String payload = "";
                    url = new URL(serverUrl);
                    urlConnection = (HttpURLConnection) url.openConnection();
                    Log.i("** Message ---->", urlConnection.getResponseMessage());
                    urlConnection.getInputStream();
                    InputStream is = new BufferedInputStream(urlConnection.getInputStream());

                    while(is != null && (i = is.read()) != -1)
                    {
                        payload += (char)i;
                    }

                    Log.i("** Payload ---->", payload);
//                    in.
//                                    readStream(in);
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
                                AsyncHttp handler = new AsyncHttp(context, null, userDataIntent);

                                // Request trackees information every 15 seconds
                                // TODO: Request the selected group/tab data
                                // Need to pull the user's: name, email, phone, lat, lon
                                Log.i("---->", "run ATF");
                                updateGroupLayout(googleMap, curList);
                            }
                        });
                    }
                });
            }
        }, TimeUnit.SECONDS.toMillis(0), TimeUnit.SECONDS.toMillis(15));
    }

    @Override
    public void onPause()
    {
        super.onPause();
        if(timer != null)
        {
            Log.i("---->", "Cancel Event");
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
    void updateGroupLayout(GoogleMap map, final ArrayList<String> lst)
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
            for(String name : lst)
            {
                name += i;
                loc = new LatLng(-33.867+i*.005, 151.206-i*.005);
                map.addMarker(new MarkerOptions().title(name).snippet("In Australia.").position(loc));
                layout.addView(new TrackeeButton(context, userDataIntent, name));
                i++;
            }

            if(loc != null)
            {
                map.moveCamera(CameraUpdateFactory.newLatLngZoom(loc, 13));
            }
        }
        catch (Exception e)
        {
            Log.w("---->", "Error", e);
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
