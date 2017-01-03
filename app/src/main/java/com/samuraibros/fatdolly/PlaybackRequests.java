package com.samuraibros.fatdolly;

import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.ViewFlipper;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class PlaybackRequests extends BaseActivity {

    //Determines whether activity is running
    public static boolean running = false;

    //Current displayed arraylist
    private AlertDialog current_dialog;
    //Current displayed arraylist
    private Map<AlertDialog, String> currentDialogStructureMap = new HashMap<>();

    @Override
    protected void onReceive_helper(Context context, Intent intent) {

    }

    @Override
    protected void showLoading_helper(final boolean val) {
        Log.d(getResources().getString(R.string.app_name), mClass_string + ": showLoading: displaying activity screen...");
        if (mViewFlipper == null) {
            setContentView(R.layout.activity_playback_requests);
            mViewFlipper = (ViewFlipper) findViewById(R.id.viewFlipper_playbackRequests);
        }

        if (!val) {
            mViewFlipper.showNext();
        }
    }

    @Override
    protected void initializeActivity_helper() {

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        registerReceiver(mServerReceiver, mServerIntentFilter);
        mClass_string = PlaybackRequests.class.toString();


        running = true;

        initializeLoading();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        running = false;
    }

    public void createRequest(View view) {
        playBackRequest(PlaybackRequests.this);
    }

    /**
     * Refresh list
     * @param view
     */
    public void refresh(View view) {
        Log.d(getResources().getString(R.string.app_name), "PlaybackRequests: Refresh: Refreshing List...");
        // Initializes the adapter
        ArrayAdapter requests_arrayAdapter  =
                new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1) {

                    @Override
                    public View getView(int position, View convertView,
                                        ViewGroup parent) {
                        View view =super.getView(position, convertView, parent);

                        TextView textView=(TextView) view.findViewById(android.R.id.text1);

            /*YOUR CHOICE OF COLOR*/
                        textView.setTextColor(Color.WHITE);

                        return view;
                    }
                };
        // Gets reference to the UI list view
        ListView requests_listView = (ListView) findViewById(R.id.listview_requests);

        // Connects the list view to the adapter
        requests_listView.setAdapter(requests_arrayAdapter);

        // Loop through connected users and add the name and address to the list
        Set<String> requests = Configurations.getRequests();
        int idx = 0;
        for (String structure : requests) {
            String address = Configurations.requestsStructureToAddress(structure);
            String name = Configurations.userAddressToName(address);
            name += " Wants to ";
            if (Configurations.getHubAddress().equals(address))
                name = "You Want to ";
            String type = Configurations.requestsStructureToType(structure);
            String request = Configurations.requestsStructureToRequest(structure);
            int duration = Configurations.requestsStructureToDuration(structure);
            int start_time = Configurations.requestsStructureToStart(structure);
            int curr_time = Calendar.getInstance().get(Calendar.SECOND);
            int time_diff = (curr_time - start_time) * 1000;
            Log.d(getResources().getString(R.string.app_name), "PlaybackRequests:Displaying request: Time difference: " + Integer.toString(time_diff));
            duration -= time_diff;
            Configurations.setRequestStructureIndex(idx, structure);
            requests_arrayAdapter.add(name + request);
            idx++;
        }
        if (current_dialog != null && current_dialog.isShowing()) {
            Log.d(getResources().getString(R.string.app_name), "PlaybackRequests: Refresh: Refreshing dialog...");
            String structure = currentDialogStructureMap.get(current_dialog);
            final String[] selection = new String[Configurations.REQUEST_PLAYBACK_CHOICES.length];
            ArrayList<String> responses = Configurations.requestStructureToResponse(structure);
            for (int i = 0; i < selection.length; i++){
                String choice = Configurations.REQUEST_PLAYBACK_CHOICES[i];
                selection[i] = choice + " : " + Collections.frequency(responses, choice);
            }

            ListView list = current_dialog.getListView();
            ArrayAdapter adapter = (ArrayAdapter)list.getAdapter();
            adapter.clear();
            adapter.addAll(selection);
            adapter.notifyDataSetChanged();
        }
    }

}
