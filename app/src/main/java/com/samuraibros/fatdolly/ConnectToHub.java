package com.samuraibros.fatdolly;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import java.util.HashMap;
import java.util.Map;

public class ConnectToHub extends BaseActivity {

    //Determines whether activity is running
    public static boolean running = false;

    //Determied if the animation is running
    public static Boolean rotate_animation_running = false;

    // Reference to UI list view for device listing
    protected ListView connectToHub_listView;

    @Override
    protected void onReceive_helper(Context context, Intent intent) {

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connect_to_hub);

        registerReceiver(mServerReceiver, mServerIntentFilter);
        mClass = ConnectToHub.class.toString();

        running = true;

        Intent i;

        // Holds list of already discovered device to prevent duplicates on list
        //discoveredDevices.clear();

        /*if (Build.VERSION.SDK_INT >= 21) {
            mLEScanner = HubService.mBluetoothAdapter.getBluetoothLeScanner();
            settings = new ScanSettings.Builder()
                    .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                    .build();
            filters = new ArrayList<ScanFilter>();

            scanLeDevice(true);
        }*/

        //Reference to refresh button
        final Button refresh_button = (Button) findViewById(R.id.button_hub_refresh);

        rotate_animation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                ConnectToHub.rotate_animation_running = true;
                refresh_button.setEnabled(false);
                refresh_button.setAlpha((float).5);
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                ConnectToHub.rotate_animation_running = false;
                refresh_button.setEnabled(true);
                refresh_button.setAlpha((float)1);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });

        rotate_animation.setDuration(2000);
        rotate_animation.setRepeatCount(5);
        refresh_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                v.clearAnimation();
                v.startAnimation(rotate_animation);
                refresh(null);
            }
        });

        /*// Initializes the adapter
        search_arrayAdapter =
                new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1);
        // Gets reference to the UI list view
        connectToHub_listView = (ListView) findViewById(R.id.listview_connectToHub);

        // Connects the list view to the adapter
        connectToHub_listView.setAdapter(search_arrayAdapter);

        // Sets the responses when the various device in the list view are clicked
        connectToHub_listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3) {
                // Gets the device name and mac address and parses them out of the string
                String device_name_temp = (String) connectToHub_listView.getItemAtPosition(position);
                String[] tokens = device_name_temp.split("'");
                final String device_name = tokens[0];
                final String device_mac = discoveredDevices.get(device_name);
                Log.d("AudhHub", "ConnectToHub: ClickListener: Device Address: " + device_mac);

                // Creates a popup dialog to provide further choices for a response
                final AlertDialog alertDialog = new AlertDialog.Builder(ConnectToHub.this).create(); //Read Update
                // Sets the title for the popup dialog
                alertDialog.setTitle(device_name);

                // Starts connection to the selected hub
                alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, "Share Control", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        Intent intent = new Intent(ConnectToHub.this, Loading.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        intent.putExtra(getResources().getString(R.string.extra_sender_class), ConnectToHub.class.toString());
                        intent.putExtra(getResources().getString(R.string.extra_loading_type), getResources().getString(R.string.loading_share_control_request));
                        intent.putExtra(getResources().getString(R.string.extra_requester_address), HubService.getHubAddress());
                        intent.putExtra(getResources().getString(R.string.extra_responder_address), device_mac);
                        intent.putExtra(getResources().getString(R.string.extra_responder_name), device_name);
                        intent.putExtra(getResources().getString(R.string.extra_requester_name), HubService.getHubName());
                        startActivity(intent);
                        finish();
                    }
                });


                 * REMOVED FOR NOW TO EXCLUDE COMPLICATION OF TAKING CONTROL OF A HUB THAT ALREADY HAS A USER CONNECTED
                 * SINCE THE USER ALREADY CONNECTED IS UNAWARE OF THE USER MAKING THE REQUEST IN CURRENT IMPLEMENTATION
                 // Cancels the popup dialogue
                 alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "Take Control", new DialogInterface.OnClickListener() {
                 public void onClick(DialogInterface dialog, int which) {
                 Intent intent = new Intent(ConnectToHub.this, Loading.class);
                 intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                 intent.putExtra(getResources().getString(R.string.extra_sender_class), ConnectToHub.class.toString());
                 intent.putExtra(getResources().getString(R.string.extra_loading_type), "TakeControlRequest");
                 intent.putExtra("RequestingAddress", HubService.getHubAddress());
                 intent.putExtra(getResources().getString(R.string.extra_device_address), device_mac);
                 startActivity(intent);
                 finish();

                 }
                 });

                // Displays the dialogue
                alertDialog.show();
            }
        });*/

        refresh_button.clearAnimation();
        refresh_button.startAnimation(rotate_animation);
        refresh(null);

        running = true;
        Log.d("AudHub", "ConnectToHub: onCreate: ended");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        running = false;
    }

    /**
     * Switches to the Connected Users screen when the button is pressed
     * @param view
     */
    public void gotoHub (View view) {
        // Creates a popup dialog to provide further choices for a response
        final AlertDialog alertDialog = new AlertDialog.Builder(ConnectToHub.this).create();

        // Sets the title for the popup dialog
        alertDialog.setTitle("Switch over to your Hub");
        alertDialog.setMessage("Are you sure?");

        // Switches to hub
        alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, getResources().getString(R.string.button_yes), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                /*Intent intent = new Intent(ConnectToHub.this, Loading.class);
                intent.putExtra(getResources().getString(R.string.extra_sender_class), mClass);
                intent.putExtra(getResources().getString(R.string.extra_loading_type), getResources().getString(R.string.intent_switch_to_connHub));
                intent.putExtra(getResources().getString(R.string.extra_loading_class), Hub.class.toString());
                startActivity(intent);
                finish();*/
            }
        });

        // Stays in current hub
        alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, getResources().getString(R.string.button_no), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                alertDialog.cancel();
            }
        });

        // Displays the dialogue
        alertDialog.show();
    }

    /**
     * Refresh list
     * @param view
     */
    public void refresh(View view) {
        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        /*discoveredDevices.clear();

        // Initializes the adapter
        search_arrayAdapter  =
                new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1) {

                    @Override
                    public View getView(int position, View convertView,
                                        ViewGroup parent) {
                        View view =super.getView(position, convertView, parent);

                        TextView textView=(TextView) view.findViewById(android.R.id.text1);

                        textView.setTextColor(Color.WHITE);

                        return view;
                    }
                };
        // Gets reference to the UI list view
        connectToHub_listView = (ListView) findViewById(R.id.listview_connectToHub);

        // Connects the list view to the adapter
        connectToHub_listView.setAdapter(search_arrayAdapter);

        mBluetoothAdapter.startDiscovery();*/
    }
}
