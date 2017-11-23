package com.example.android_tracker;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.textclassifier.TextClassifier;
import android.widget.LinearLayout;
import com.google.gson.JsonObject;

/**
 * Created by brice on 10/23/17.
 */

public class TrackeeButton extends android.support.v7.widget.AppCompatButton // implements AsyncResponse
{
    String name;
    Context context;
    Intent singleTrackIntent;


    public TrackeeButton(final Context con, Intent intent, String name)
    {
        super(con);
        this.context = con;
        singleTrackIntent = intent;

        this.setText(name);
        this.setTextSize(16);
        this.setClickable(true);
        this.setLayoutParams(new LinearLayout.LayoutParams( LinearLayout.LayoutParams.MATCH_PARENT,
                                                            LinearLayout.LayoutParams.WRAP_CONTENT, 2.0f));

        this.setOnClickListener(new OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                singleTrackIntent.setClass(context, SingleTrackActivity.class);
                context.startActivity(singleTrackIntent);
            }
        });
    }

}
