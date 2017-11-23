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

public class MyListActivity extends FragmentActivity implements OnMapReadyCallback, View.OnClickListener
{
    private static ArrayList<String> familyList;
    private static ArrayList<String> friendList;
    private static ArrayList<String> othersList;

    private static final int REQUEST_FINE_LOCATION = 9003;
    private static final int REQUEST_COARSE_LOCATION = 9004;
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
        
        // Obtain the MapFragment and get notified when the map is ready to be used.
        MapFragment mapFragment = (MapFragment) getFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        if(mMap != null)
        {
            Log.i("---->", "Not null");
        }

        timer = new Timer();
        final AsyncHttp handler = new AsyncHttp(context, null, userDataIntent);
        final TabLayout mTabLayout = findViewById(R.id.tabs);

        timer.scheduleAtFixedRate(new TimerTask() {
            final GoogleMap map = mMap;
            synchronized public void run()
            {
                // Request trackees information every 15 seconds
                // TODO: Request the selected group/tab data
                // Need to pull the user's: name, email, phone, lat, lon
                if(map == null)
                    Log.i("---->", "Resume Event Null");
                else
                    Log.i("---->", "Resume Event");

                familyList = handler.getFamilyList();
                friendList = handler.getFriendList();
                othersList = handler.getOthersList();


                if(mTabLayout.getSelectedTabPosition() == 0)
                {
                    updateGroupLayout(map, "Fam-");
                }
                else if(mTabLayout.getSelectedTabPosition() == 1)
                {
                    updateGroupLayout(map, "Fri-");
                }
                else if(mTabLayout.getSelectedTabPosition() == 2)
                {
                    updateGroupLayout(map, "Oth-");
                }
            }
        }, TimeUnit.SECONDS.toMillis(0), TimeUnit.SECONDS.toMillis(2));
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
        if (requestCode == REQUEST_FINE_LOCATION)
        {
            if(grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
            {
                // updateGroupLayout("Fam");
            }
            else
            {
                // Permission was denied or request was cancelled
            }
        }
        else if (requestCode == REQUEST_COARSE_LOCATION)
        {
            if(grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
            {
                // updateGroupLayout("Fam");
            }
            else
            {
                // Permission was denied or request was cancelled
            }
        }
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap)
    {
        mMap = googleMap;
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
        {
            // Check Permissions Now
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION,
                                 android.Manifest.permission.ACCESS_COARSE_LOCATION},
                    REQUEST_FINE_LOCATION);
            Log.i("---->", "Returns");
        }
        else
        {
            // updateGroupLayout("Fam");
        }
    }

    @SuppressLint("MissingPermission")
    void updateGroupLayout(GoogleMap map, final String str)
    {
        if(map == null)
        {
            Log.i("---->", "Map Null");
            return;
        }

        try
        {
            map.clear();
            int rows = 16;
            MarkerOptions markers = new MarkerOptions();

            LinearLayout layout = (LinearLayout) findViewById(R.id.Trackees_layout);
            layout.removeAllViews();

            for(int i = 0; i < rows; i++)
            {
                String name = str + " " + i;
                LatLng sydney = new LatLng(-33.867+i*.005, 151.206-i*.005);
                map.moveCamera(CameraUpdateFactory.newLatLngZoom(sydney, 13));
                map.addMarker(new MarkerOptions().title(name).snippet("In Australia.").position(sydney));
                layout.addView(new TrackeeButton(context, userDataIntent, name));
            }
        }
        catch (Exception e)
        {
            Log.w("---->", "Error", e);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        AsyncHttp navigator = new AsyncHttp(context, findViewById(R.id.tabs), userDataIntent);
    }

    public void onTabTapped(int position)
    {
        Log.i("---->", "Position " + position);
        switch (position) {
            case 0:
                Toast.makeText(this, "Family tab", Toast.LENGTH_SHORT).show();
                // TODO: Update family on map
//                updateGroupLayout("Fam");
                break;
            case 1:
                Toast.makeText(this, "Friends", Toast.LENGTH_SHORT).show();
                // TODO: Update friends on map
//                updateGroupLayout("Fri");
                break;
            case 2:
                Toast.makeText(this, "others tab clicked " + position, Toast.LENGTH_SHORT);
                // TODO: Update others on map
//                updateGroupLayout("Oth");
                break;
            default:
                Toast.makeText(this, "Tapped " + position, Toast.LENGTH_SHORT);
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.manage_button:
                userDataIntent.setClass(context,ManageActivity.class);
                startActivity(userDataIntent);
                break;
        }
    }
}
