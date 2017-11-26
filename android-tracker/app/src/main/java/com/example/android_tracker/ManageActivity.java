package com.example.android_tracker;

import android.content.Context;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class ManageActivity extends AppCompatActivity{

    LinearLayout tracklist;
    EditText addtrackee;
    Button AddButton;
    Button MyListButton;
    private Spinner groups;
    private Intent userDataIntent;
    Context context = this;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage);

        userDataIntent = getIntent();

        tracklist = (LinearLayout) findViewById(R.id.track_list);
        addtrackee = (EditText) findViewById(R.id.add_trackee);
        AddButton = (Button) findViewById(R.id.add_button);
        MyListButton = (Button) findViewById(R.id.list_button);
        groups = (Spinner) findViewById(R.id.group_list);


        String[] items = new String[]{"Family", "Friends", "Other"};
        ArrayAdapter<String> groupadapter = new ArrayAdapter<String>(this,android.R.layout.simple_spinner_item, items);
        groups.setAdapter(groupadapter);

        String[] ListElements = new String[]{};

        final List<String> TrackeeArrayList = new ArrayList<String>(Arrays.asList(ListElements));
        AsyncHttp handler = new AsyncHttp(context, null, userDataIntent);


        TrackeeArrayList.addAll(handler.getFamilyList());
        TrackeeArrayList.addAll(handler.getFriendList());
        TrackeeArrayList.addAll(handler.getOthersList());

        updateTrackeeList(TrackeeArrayList);


        AddButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view)
            {

                TrackeeArrayList.add(addtrackee.getText().toString());
                tracklist.addView(new TrackeeButton(context, userDataIntent,addtrackee.getText().toString()));
                AsyncHttp handler = new AsyncHttp(context, null, userDataIntent);

                //TODO: assign added trackee to specified group
                if ((groups.getSelectedItem().toString()).equals("Family"))
                {
             //       handler.getFamilyList();
                    Log.i("===> ", "Group = " + groups.getSelectedItem().toString());
                }
                if ((groups.getSelectedItem().toString()).equals("Friends"))
                {
             //       handler.getFriendList();
                    Log.i("===> ", "Group = " + groups.getSelectedItem().toString());
                }
                if ((groups.getSelectedItem().toString()).equals("Others"))
                {
               //     handler.getOthersList();
                    Log.i("===> ", "Group = " + groups.getSelectedItem().toString());
                }

            }
        });

        MyListButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view)
            {
                userDataIntent.setClass(context, MyListActivity.class);
                startActivity(userDataIntent);
            }
        });
    }

    private void updateTrackeeList(List<String> trackeeArrayList) {
        for(int i = 0; i < trackeeArrayList.size(); i++)
        {
            String name = trackeeArrayList.get(i).toString();
            tracklist.addView(new TrackeeButton(context, userDataIntent, name));
        }

    }


}
