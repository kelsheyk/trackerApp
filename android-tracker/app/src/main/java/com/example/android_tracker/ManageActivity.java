package com.example.android_tracker;

import android.content.Context;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
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

        //TODO: get tracker list
        String[] ListElements = new String[]{};

        final List<String> TrackeeArrayList = new ArrayList<String>(Arrays.asList(ListElements));

        final ArrayAdapter<String> adapter = new ArrayAdapter<String>
                (ManageActivity.this, android.R.layout.simple_list_item_1, TrackeeArrayList);

        tracklist.setAdapter(adapter);

        AddButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view)
            {

                TrackeeArrayList.add(addtrackee.getText().toString());
                adapter.notifyDataSetChanged();
                //TODO: assign added trackee to specified group

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
