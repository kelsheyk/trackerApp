package com.example.android_tracker;

import android.content.Context;
import android.content.Intent;
import android.support.design.widget.TabLayout;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

public class SingleTrackActivity extends FragmentActivity implements View.OnClickListener
{
    private Timer timer;
    private GoogleMap mMap;
    private Spinner radius;
    private Context context = this;
    private Intent userDataIntent;
    private Marker oldOriginMarker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_single_track);

        userDataIntent = getIntent();

        radius = (Spinner) findViewById(R.id.radius_spinner);
        findViewById(R.id.single_mylist_button).setOnClickListener(this);
        findViewById(R.id.single_set_area_button).setOnClickListener(this);

        Integer[] items = new Integer[]{1,2,5,10,50,10,200,400,1000,2000};
        ArrayAdapter<Integer> adapter = new ArrayAdapter<Integer>(this,android.R.layout.simple_spinner_item, items);
        radius.setAdapter(adapter);
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
                        MapFragment mapFragment = (MapFragment) getFragmentManager().findFragmentById(R.id.single_map);
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
                                String name = handler.getTrackedUserData();
                                Log.i("---->", "run");

                                LatLng loc = new LatLng(-33.867+20*.005, 151.206-20*.005);
                                mMap.addMarker(new MarkerOptions().title(name).snippet("In Australia.").position(loc));
                                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(loc, 13));

                                mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener(){
                                    @Override
                                    public void onMapClick(LatLng point)
                                    {
                                        // TODO Auto-generated method stub
                                        if(oldOriginMarker != null)
                                        {
                                            oldOriginMarker.remove();
                                        }

                                        oldOriginMarker = mMap.addMarker(new MarkerOptions().position(point).title("Alert Origin"));
                                    }
                                });
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
            Log.i("---->", "Cancel Event s");
            timer.cancel();
        }
    }

    @Override
    public void onClick(View v)
    {
        switch (v.getId())
        {
            case R.id.single_set_area_button:
            {
                if(oldOriginMarker == null || radius.getSelectedItem() == null)
                {
                    Toast.makeText(context, "Origin or radius not selected on map!", Toast.LENGTH_SHORT).show();
                    return;
                }
                Log.i("===> ", "Val = " + radius.getSelectedItem().toString());
                break;
            }
            case R.id.single_mylist_button:
            {
                this.finish();
                break;
            }
            default:
            {

                break;
            }
        }
    }
}
