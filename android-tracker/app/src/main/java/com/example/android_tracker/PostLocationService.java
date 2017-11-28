package com.example.android_tracker;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import static android.Manifest.permission.ACCESS_FINE_LOCATION;

/**
 * Created by brice on 11/24/17.
 */

public class PostLocationService extends Service {
    private Context context = this;
    private Intent userIntent;
    private Timer timer = null;
    private static int REQUEST_USER_LOCATION = 9005;

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        userIntent = intent;

        timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask()
        {
            synchronized public void run()
            {
                if (ActivityCompat.checkSelfPermission(getApplicationContext(), ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                        ActivityCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
                {
                    ActivityCompat.requestPermissions(null, new String[]{ACCESS_FINE_LOCATION}, REQUEST_USER_LOCATION);
                    return;
                }

                LocationManager lm = (LocationManager)getApplicationContext().getSystemService(LOCATION_SERVICE);
                List<String> providers = lm.getProviders(true);
                Location bestLocation = null;

                for (String provider : providers)
                {
                    Location l = lm.getLastKnownLocation(provider);
                    if (l == null)
                    {
                        continue;
                    }

                    if (bestLocation == null || l.getAccuracy() < bestLocation.getAccuracy())
                    {
                        bestLocation = l;
                    }
                }

                if(bestLocation == null)
                {
                    timer.cancel();
                    return;
                }

                double longitude = bestLocation.getLongitude();
                double latitude = bestLocation.getLatitude();

                URL url = null;
                HttpURLConnection urlConnection = null;
                String serverUrl = "http://trackerapp-185915.appspot.com/postLocation" +
                                    "?userToken=" + userIntent.getStringExtra("userToken") +
                                    "&userEmail=" + userIntent.getStringExtra("userEmail") +
                                    "&lat=" + latitude +
                                    "&lon=" + longitude;

                try
                {
                    int i;
                    String payload = "";
                    url = new URL(serverUrl);
                    urlConnection = (HttpURLConnection) url.openConnection();
                    urlConnection.setDoInput(true);
                    urlConnection.setDoOutput(true);
                    urlConnection.setRequestMethod("POST");

                    OutputStream outputStream = urlConnection.getOutputStream();

                    BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(outputStream));
                    bufferedWriter.flush();

                    urlConnection.connect();

                    InputStream is = new BufferedInputStream(urlConnection.getInputStream());

                    while(is != null && (i = is.read()) != -1)
                    {
                        payload += (char)i;
                    }

                    Log.i("==> response", payload);
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
            }
        }, TimeUnit.SECONDS.toMillis(0), TimeUnit.SECONDS.toMillis(60));

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
