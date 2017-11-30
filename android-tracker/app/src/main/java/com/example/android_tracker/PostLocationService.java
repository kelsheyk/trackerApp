package com.example.android_tracker;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.widget.Toast;
import java.util.Timer;


import static android.Manifest.permission.ACCESS_FINE_LOCATION;

/**
 * Created by brice on 11/24/17.
 */

public class PostLocationService extends Service
{
    /*---------- Listener class to get coordinates ------------- */
    private class MyLocationListener implements LocationListener
    {
        @Override
        public void onLocationChanged(Location loc)
        {
            double longitude = loc.getLongitude();
            double latitude = loc.getLatitude();

            String serverUrl = "http://trackerapp-185915.appspot.com/postLocation" +
                    "?userToken=" + userIntent.getStringExtra("userToken") +
                    "&userEmail=" + userIntent.getStringExtra("userEmail") +
                    "&lat=" + latitude +
                    "&lon=" + longitude;

            AsyncHttp tsk = new AsyncHttp(getApplicationContext(), null, userIntent);
            tsk.postMyLocation(serverUrl);
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {}

        @Override
        public void onProviderDisabled(String provider) {}

        @Override
        public void onProviderEnabled(String provider) {}
    }

    private Context context = this;
    private Intent userIntent;
    private static int REQUEST_USER_LOCATION = 9005;

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        userIntent = intent;

        if (ActivityCompat.checkSelfPermission(getApplicationContext(), ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
        {
            ActivityCompat.requestPermissions(null, new String[]{ACCESS_FINE_LOCATION}, REQUEST_USER_LOCATION);
            return START_STICKY_COMPATIBILITY;
        }

        LocationManager lm = (LocationManager)getApplicationContext().getSystemService(LOCATION_SERVICE);
        if(lm.isProviderEnabled(LocationManager.GPS_PROVIDER) )
        {
            LocationListener locationListener = new MyLocationListener();
            lm.requestLocationUpdates( LocationManager.GPS_PROVIDER, 5000, 10, locationListener);
        }

        return START_STICKY_COMPATIBILITY;
    }

    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults)
    {
        if (requestCode == REQUEST_USER_LOCATION)
        {
            if(grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
            {
            }
            else
            {
                Toast.makeText(this, "Location permission not granted! ", Toast.LENGTH_SHORT);
                stopSelf();
            }
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent)
    {
        return null;
    }
}
