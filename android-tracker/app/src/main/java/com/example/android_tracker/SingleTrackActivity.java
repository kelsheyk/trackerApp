package com.example.android_tracker;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.location.Location;
import android.support.design.widget.TabLayout;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
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

public class SingleTrackActivity extends FragmentActivity implements View.OnClickListener
{
    private Timer timer;
    private GoogleMap mMap;
    private Spinner radius;
    private Context context = this;
    private Intent userDataIntent;
    private Marker oldOriginMarker;
    private double lat;
    private double lon;

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
                URL url = null;
                HttpURLConnection urlConnection = null;
                TabLayout mTabLayout = findViewById(R.id.tabs);
                String serverUrl = "http://trackerapp-185915.appspot.com/singleDroid" +
                        "?userToken=" + userDataIntent.getStringExtra("userToken") +
                        "&userEmail=" + userDataIntent.getStringExtra("userEmail") +
                        "&trackeeEmail=" + userDataIntent.getStringExtra("trackeeEmail");

                try
                {
                    Log.i("**-- Tracking URL --**", serverUrl);

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
                    JsonObject json = parser.parse(payload).getAsJsonObject();

                    if(json.get("lat").getAsString().isEmpty() || json.get("lat").getAsString().isEmpty())
                    {
                        lat = lon = 999;
                    }
                    else
                    {
                        lat = json.get("lat").getAsDouble();
                        lon = json.get("lon").getAsDouble();
                    }

                    Log.i("**-- Response --**", json.toString());
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

                runOnUiThread(new Runnable() {
                    LatLng loc;
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
                                mMap.clear();

                                if(lat == 999 || lon == 999)
                                {
                                    return;
                                }

                                loc = new LatLng(lat, lon);
                                mMap.addMarker(new MarkerOptions().title(userDataIntent.getStringExtra("trackeeEmail")).snippet("In Australia.").position(loc));
                                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(loc, 13));

                                if(oldOriginMarker != null)
                                {
                                    mMap.addMarker(new MarkerOptions().position(oldOriginMarker.getPosition()).title("Alert Origin"));
                                }

                                Switch alerton = (Switch) findViewById(R.id.alert_switch);

                                if((oldOriginMarker != null) && alerton.isChecked())
                                {
                                    // compute the distance
                                    float[] results = new float[1];
                                    Location.distanceBetween(oldOriginMarker.getPosition().latitude,
                                                             oldOriginMarker.getPosition().longitude,
                                                             loc.latitude, loc.longitude, results);

                                    Float distToOrigin = results[0] / 1600; // Converting merters to miles
                                    Float rad = Float.valueOf(radius.getSelectedItem().toString());

                                    if(distToOrigin >= rad)
                                    {
                                        findViewById(R.id.alert_on).setBackgroundColor(Color.RED);
                                    }
                                    else if(distToOrigin + .3f >= rad)
                                    {
                                        findViewById(R.id.alert_on).setBackgroundColor(Color.YELLOW);
                                    }
                                    else
                                    {
                                        findViewById(R.id.alert_on).setBackgroundColor(Color.WHITE);
                                    }
                                }


                                mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener()
                                {
                                    @Override
                                    public void onMapClick(LatLng point) {
                                        mMap.clear();
                                        if (oldOriginMarker != null) {
                                            oldOriginMarker.remove();
                                        }

                                        if (loc.latitude != 999 && loc.longitude != 999)
                                        {
                                            mMap.addMarker(new MarkerOptions().title(userDataIntent.getStringExtra("trackeeEmail")).snippet("In Australia.").position(loc));
                                            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(loc, 13));
                                            oldOriginMarker = mMap.addMarker(new MarkerOptions().position(point).title("Alert Origin"));
                                        }
                                    }
                                });
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
