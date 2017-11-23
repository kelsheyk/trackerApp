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
            synchronized public void run()
            {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        // Obtain the MapFragment and get notified when the map is ready to be used.
                        MapFragment mapFragment = (MapFragment) getFragmentManager().findFragmentById(R.id.map);
                        mapFragment.getMapAsync(new OnMapReadyCallback()
                        {
                            @Override
                            public void onMapReady(GoogleMap googleMap)
                            {
                                mMap = googleMap;
                                AsyncHttp handler = new AsyncHttp(context, null, userDataIntent);
                                TabLayout mTabLayout = findViewById(R.id.tabs);

                                // Request trackees information every 15 seconds
                                // TODO: Request the selected group/tab data
                                // Need to pull the user's: name, email, phone, lat, lon
                                familyList = handler.getFamilyList();
                                friendList = handler.getFriendList();
                                othersList = handler.getOthersList();
                                Log.i("---->", "run");

                                if(mTabLayout.getSelectedTabPosition() == 0)
                                {
                                    updateGroupLayout(googleMap, familyList);
                                }
                                else if(mTabLayout.getSelectedTabPosition() == 1)
                                {
                                    updateGroupLayout(googleMap, friendList);
                                }
                                else if(mTabLayout.getSelectedTabPosition() == 2)
                                {
                                    updateGroupLayout(googleMap, othersList);
                                }
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

            for(int i = 0; i < lst.size(); i++)
            {
                String name = lst.get(i);
                loc = new LatLng(-33.867+i*.005, 151.206-i*.005);
                map.addMarker(new MarkerOptions().title(name).snippet("In Australia.").position(loc));
                layout.addView(new TrackeeButton(context, userDataIntent, name));
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
