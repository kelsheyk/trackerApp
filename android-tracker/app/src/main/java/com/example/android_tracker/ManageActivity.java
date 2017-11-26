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
import android.widget.ListView;
import android.widget.Spinner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class ManageActivity extends AppCompatActivity{

    ListView tracklist;
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

        tracklist = (ListView) findViewById(R.id.track_list);
        addtrackee = (EditText) findViewById(R.id.add_trackee);
        AddButton = (Button) findViewById(R.id.add_button);
        MyListButton = (Button) findViewById(R.id.list_button);
        groups = (Spinner) findViewById(R.id.group_list);


        String[] items = new String[]{"Family", "Friends", "Other"};
        ArrayAdapter<String> groupadapter = new ArrayAdapter<String>(this,android.R.layout.simple_spinner_item, items);
        groups.setAdapter(groupadapter);

        String[] ListElements = new String[]{};

        List<String> TrackeeArrayList = new ArrayList<String>(Arrays.asList(ListElements));
        AsyncHttp handler = new AsyncHttp(context, null, userDataIntent);
        // TODO: set a listener to spinner
        if (groups.getSelectedItem().toString() == "Family" )
        {
            TrackeeArrayList = handler.getFamilyList();
        }

        if (groups.getSelectedItem().toString() == "Friends" )
        {
            TrackeeArrayList = handler.getFriendList();
        }
        if (groups.getSelectedItem().toString() == "Others" )
        {
            TrackeeArrayList = handler.getOthersList();

        }


        final ArrayAdapter<String> adapter = new ArrayAdapter<String>
                (ManageActivity.this, android.R.layout.simple_list_item_1, TrackeeArrayList);

        tracklist.setAdapter(adapter);

        final List<String> finalTrackeeArrayList = TrackeeArrayList;

        AddButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view)
            {

                finalTrackeeArrayList.add(addtrackee.getText().toString());
                adapter.notifyDataSetChanged();
                AsyncHttp handler = new AsyncHttp(context, null, userDataIntent);

                //TODO: assign added trackee to specified group
                if (groups.getSelectedItem().toString() == "Family" )
                {
             //       handler.getFamilyList();
                    Log.i("===> ", "Group = " + groups.getSelectedItem().toString());
                }
                if (groups.getSelectedItem().toString() == "Friends" )
                {
             //       handler.getFriendList();
                    Log.i("===> ", "Group = " + groups.getSelectedItem().toString());
                }
                if (groups.getSelectedItem().toString() == "Others" )
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


}
