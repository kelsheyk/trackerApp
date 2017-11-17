package com.example.android_tracker;

import android.app.TabActivity;
import android.content.Context;
import android.content.Intent;
import android.support.design.widget.TabLayout;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.TabHost;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;



public class MyListActivity extends FragmentActivity implements OnMapReadyCallback, View.OnClickListener {

    private GoogleMap mMap;
    private Intent userDataIntent;
    Context context = this;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_list);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

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
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Add a marker in Sydney and move the camera

        // Change LatLng to tracking locations
        LatLng sydney = new LatLng(-34, 151);
        mMap.addMarker(new MarkerOptions().position(sydney).title("Marker in Sydney"));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney));
    }

    @Override
    protected void onStart() {
        super.onStart();
        AsyncHttp navigator = new AsyncHttp(context, findViewById(R.id.tabs), userDataIntent);
    }

    public void onTabTapped(int position) {
        switch (position) {
            case 0:
                Toast.makeText(this, "Family tab", Toast.LENGTH_SHORT).show();

                break;
            case 1:
                Toast.makeText(this, "Friends", Toast.LENGTH_SHORT).show();
                break;
            case 2:
                Toast.makeText(this, "others tab clicked " + position, Toast.LENGTH_SHORT);
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
