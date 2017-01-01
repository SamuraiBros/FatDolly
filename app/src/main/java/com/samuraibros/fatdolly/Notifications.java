package com.samuraibros.fatdolly;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

public class Notifications extends BaseActivity {

    // Adapter to connect to list view
    ArrayAdapter<String> notifications_arrayAdapter;
    // Reference to UI list view for device listing
    ListView notifications_listView;
    //Determines whether activity is running
    public static boolean running = false;

    @Override
    protected void onReceive_helper(Context context, Intent intent) {

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notifications);

        registerReceiver(mReceiver, mServerIntentFilter);
        mClass = Notifications.class.toString();

        Log.d("AudHub", "Notifications: onCreate: starting...");

        // Reset the list view
        if (!running)
            refresh(null);

        // Sets the responses when the various device in the list view are clicked
        notifications_listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, final int position, long arg3) {
                // Gets the device name and mac address and parses them out of the string
                final String message = (String) notifications_listView.getItemAtPosition(position);
                String delims = "is";
                String[] tokens = message.split(delims);
                final String sender;
                final String type;
                if (tokens.length == 2) {
                    sender = Configurations.userNameAddressMapToAddress(tokens[0].trim());
                    type = tokens[1].trim();
                }
                else {
                    sender = "";
                    type = "";
                }

                // Creates a popup dialog to provide further choices for a response
                final AlertDialog alertDialog = new AlertDialog.Builder(Notifications.this).create();

                // Sets the title for the popup dialog
                alertDialog.setTitle(message);

                if (type.contains("requesting to take control")) {
                    // Accepts the request
                    alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, getResources().getString(R.string.button_accept), new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            Configurations.dequeueNotification(message);
                            /*Intent intent = new Intent(Notifications.this, Loading.class);
                            intent.putExtra(getResources().getString(R.string.extra_sender_class), Notifications.class.toString());
                            intent.putExtra(getResources().getString(R.string.extra_loading_type), getResources().getString(R.string.loading_switch_control));
                            intent.putExtra(getResources().getString(R.string.extra_requester_address), sender);
                            intent.putExtra(getResources().getString(R.string.extra_responder_address), Configurations.getHubAddress());
                            intent.putExtra(getResources().getString(R.string.extra_type), getResources().getString(R.string.value_take));
                            startActivity(intent);
                            refresh(null);
                            finish();*/
                        }
                    });

                    // Declines the request
                    alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, getResources().getString(R.string.button_deny), new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            Intent i = new Intent(getResources().getString(R.string.intent_controlresponse));
                            i.putExtra(getResources().getString(R.string.extra_type), getResources().getString(R.string.value_take));
                            i.putExtra(getResources().getString(R.string.extra_requester_address), sender);
                            i.putExtra(getResources().getString(R.string.extra_responder_address), Configurations.getHubAddress());
                            i.putExtra(getResources().getString(R.string.extra_accept), false);
                            sendBroadcast(i);
                            Configurations.dequeueNotification(message);
                            refresh(null);
                            close(prev_screen);
                        }
                    });
                }
                else if (type.contains("requesting to share control")) {
                    // Accepts the request
                    alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, getResources().getString(R.string.button_accept), new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            Configurations.dequeueNotification(message);
                            /*Intent intent = new Intent(Notifications.this, Loading.class);
                            intent.putExtra(getResources().getString(R.string.extra_sender_class), Notifications.class.toString());
                            intent.putExtra(getResources().getString(R.string.extra_loading_type), getResources().getString(R.string.loading_waiting_for_users));
                            intent.putExtra(getResources().getString(R.string.extra_requester_address), sender);
                            intent.putExtra(getResources().getString(R.string.extra_responder_address), Configurations.getHubAddress());
                            intent.putExtra(getResources().getString(R.string.extra_type), getResources().getString(R.string.value_share));
                            startActivity(intent);
                            refresh(null);
                            finish();*/
                        }
                    });

                    // Declines the request
                    alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, getResources().getString(R.string.button_deny), new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            Intent i = new Intent(getResources().getString(R.string.intent_controlresponse));
                            i.putExtra(getResources().getString(R.string.extra_type), getResources().getString(R.string.value_share));
                            i.putExtra(getResources().getString(R.string.extra_requester_address), sender);
                            i.putExtra(getResources().getString(R.string.extra_responder_address), Configurations.getHubAddress());
                            i.putExtra(getResources().getString(R.string.extra_accept), false);
                            sendBroadcast(i);
                            Configurations.dequeueNotification(message);
                            refresh(null);
                            close(prev_screen);
                        }
                    });
                }
                else if (type.contains("giving you control")) {
                    // Accepts the request
                    alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, getResources().getString(R.string.button_accept), new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            Configurations.dequeueNotification(message);
                            /*Intent intent = new Intent(Notifications.this, Loading.class);
                            intent.putExtra(getResources().getString(R.string.extra_sender_class), Notifications.class.toString());
                            intent.putExtra(getResources().getString(R.string.extra_loading_type), getResources().getString(R.string.loading_switch_control));
                            intent.putExtra(getResources().getString(R.string.extra_requester_address), sender);
                            intent.putExtra(getResources().getString(R.string.extra_responder_address), Configurations.getHubAddress());
                            intent.putExtra(getResources().getString(R.string.extra_type), getResources().getString(R.string.value_give));
                            startActivity(intent);
                            refresh(null);
                            finish();*/
                        }
                    });

                    // Declines the request
                    alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, getResources().getString(R.string.button_deny), new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            Intent i = new Intent(getResources().getString(R.string.intent_controlresponse));
                            i.putExtra(getResources().getString(R.string.extra_type), getResources().getString(R.string.value_give));
                            i.putExtra(getResources().getString(R.string.extra_accept), false);
                            sendBroadcast(i);
                            Configurations.dequeueNotification(message);
                            refresh(null);
                            close(prev_screen);
                        }
                    });
                }
                else {
                    // Clear Notifications list
                    alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, getResources().getString(R.string.button_clear), new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            Configurations.dequeueNotification(message);
                            refresh(null);
                        }
                    });
                }

                // Displays the dialogue
                alertDialog.show();
            }
        });

        running = true;
        Log.d("AudHub", "Notifications: onCreate: ended");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        running = false;
    }
    /**
     * Clear list view
     * @param view
     */
    public void clearNotifications(View view) {
        // Initializes the adapter
        // Gets reference to the UI list view
        notifications_listView = (ListView) findViewById(R.id.listview_notifications);

        notifications_arrayAdapter =
                new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1) {

                    @Override
                    public View getView(int position, View convertView,
                                        ViewGroup parent) {
                        // Gets the device name and mac address and parses them out of the string
                        final String device_info = (String) notifications_listView.getItemAtPosition(position);
                        String delims = "\n";
                        String[] tokens = device_info.split(delims);
                        View view = super.getView(position, convertView, parent);

                        TextView textView = (TextView) view.findViewById(android.R.id.text1);
                        textView.setTextColor(Color.WHITE);
                        return view;
                    }
                };

        // Connects the list view to the adapter
        notifications_listView.setAdapter(notifications_arrayAdapter);
        Configurations.dequeueNotification(null);
        // Loop through connected users and add the name and address to the list
        for (String mess : Configurations.getNotifications()) {
            notifications_arrayAdapter.add(mess);
        }
    }
    /**
     * Reset the list view after any updates or changes
     * @param view
     */
    public void refresh(View view){
        // Initializes the adapter
        // Gets reference to the UI list view
        notifications_listView = (ListView) findViewById(R.id.listview_notifications);

        notifications_arrayAdapter =
                new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1) {

                    @Override
                    public View getView(int position, View convertView,
                                        ViewGroup parent) {
                        // Gets the device name and mac address and parses them out of the string
                        final String device_info = (String) notifications_listView.getItemAtPosition(position);

                        View view = super.getView(position, convertView, parent);

                        TextView textView = (TextView) view.findViewById(android.R.id.text1);
                        textView.setTextColor(Color.WHITE);
                        return view;
                    }
                };

        // Connects the list view to the adapter
        notifications_listView.setAdapter(notifications_arrayAdapter);
        // Loop through connected users and add the name and address to the list
        for (String mess : Configurations.getNotifications()) {
            notifications_arrayAdapter.add(mess);
        }
    }
}
