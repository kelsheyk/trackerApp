package com.example.android_tracker;

import android.content.Context;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.loopj.android.http.RequestParams;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;


public class ManageActivity extends AppCompatActivity implements View.OnClickListener{

    LinearLayout tracklist;
    EditText addtrackee;
    private Spinner groups;
    private Intent userDataIntent;
    Context context = this;
    List<String> TrackeeArrayList;
    Timer timer;


    private static JsonArray familyList;
    private static JsonArray friendList;
    private static JsonArray othersList;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage);

        userDataIntent = getIntent();

        tracklist = (LinearLayout) findViewById(R.id.track_list);
        addtrackee = (EditText) findViewById(R.id.add_trackee);
        findViewById(R.id.add_button).setOnClickListener(this);
        findViewById(R.id.list_button).setOnClickListener(this);
        groups = (Spinner) findViewById(R.id.group_list);


        String[] items = new String[]{"Family", "Friends", "Other"};
        final ArrayAdapter<String> groupadapter = new ArrayAdapter<String>(this,android.R.layout.simple_spinner_item, items);
        groups.setAdapter(groupadapter);
        groups.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                String selectedGroup = adapterView.getItemAtPosition(i).toString();
                onGroupChoice(selectedGroup);
            }
            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        TrackeeArrayList = new ArrayList<String>();

    }


    @Override
    protected void onResume() {
        super.onResume();

        if(timer != null)
        {
            timer.cancel();
        }

        timer = new Timer();

        timer.scheduleAtFixedRate(new TimerTask() {
            private JsonArray curList;
            synchronized public void run()
            {
                URL url = null;
                HttpURLConnection urlConnection = null;
                String serverUrl = "http://trackerapp-185915.appspot.com/groupsDroid?userToken=" +
                        userDataIntent.getStringExtra("userToken") + "&userEmail=" +
                        userDataIntent.getStringExtra("userEmail") ;

                try
                {
                    int i;
                    String payload = "";
                    url = new URL(serverUrl);
                    urlConnection = (HttpURLConnection) url.openConnection();
                    urlConnection.getInputStream();
                    InputStream is = new BufferedInputStream(urlConnection.getInputStream());

                    while(is != null && (i = is.read()) != -1)
                    {
                        payload += (char)i;
                    }

                    JsonParser parser = new JsonParser();
                    JsonObject json = parser.parse(payload).getAsJsonObject().getAsJsonObject("groupsMembers");

                    // Update lists
                    familyList = json.get("Family").getAsJsonArray();
                    friendList = json.get("Friends").getAsJsonArray();
                    othersList = json.get("Others").getAsJsonArray();

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

                String selectedGroup = groups.getSelectedItem().toString();
                switch (selectedGroup)
                {
                    case "Family":
                        curList = familyList;
                        break;
                    case "Friends":
                        curList = friendList;
                        break;
                    case "Others":
                        curList = othersList;
                        break;
                }

                runOnUiThread(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        updateTrackeeList(curList);
                    }
                });
            }
        }, TimeUnit.SECONDS.toMillis(0), TimeUnit.SECONDS.toMillis(30));
    }

    void updateTrackeeList(final JsonArray trackeeArrayList) {
        int i = 0;
        tracklist.removeAllViews();

        if(trackeeArrayList == null)
        {
            return;
        }

        for (JsonElement element : trackeeArrayList)
        {
            String email = element.getAsJsonObject().get("email").getAsString();
            tracklist.addView(new TrackeeButton(context, userDataIntent, email));
            i++;
        }

    }

    private void onGroupChoice (String selectedGroup)
    {

        switch (selectedGroup) {
            case "Family":
                updateTrackeeList(familyList);
                break;

            case "Friends":
                updateTrackeeList(friendList);
                break;

            case "Others":
                updateTrackeeList(othersList);
                break;

        }

    }

    @Override
    public void onClick(View view) {
        switch (view.getId())
        {
            case R.id.list_button:
                userDataIntent.setClass(context, MyListActivity.class);
                startActivity(userDataIntent);
                break;

            case R.id.add_button:

                // Add newTrackee to android frontend
                String newTrackee = addtrackee.getText().toString();
                TrackeeArrayList.add(newTrackee);
                tracklist.addView(new TrackeeButton(context, userDataIntent,newTrackee));


                //TODO: Add newTrackee to backend
                if ((groups.getSelectedItem().toString()).equals("Family"))
                {
                    Log.i("===> ", "Group = " + groups.getSelectedItem().toString());
                }
                if ((groups.getSelectedItem().toString()).equals("Friends"))
                {
                    Log.i("===> ", "Group = " + groups.getSelectedItem().toString());
                }
                if ((groups.getSelectedItem().toString()).equals("Others"))
                {
                    Log.i("===> ", "Group = " + groups.getSelectedItem().toString());
                }


        }
    }
}
