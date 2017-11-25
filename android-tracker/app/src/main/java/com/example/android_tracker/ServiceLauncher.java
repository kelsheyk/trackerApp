package com.example.android_tracker;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * Created by brice on 11/24/17.
 */

public class ServiceLauncher extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if(Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction()))
        {
            Intent serviceIntent = new Intent(context, PostLocationService.class);
            Log.i("--------------", "LAUNCHING SERVICE APP");
            context.startService(serviceIntent);
        }

    }
}
