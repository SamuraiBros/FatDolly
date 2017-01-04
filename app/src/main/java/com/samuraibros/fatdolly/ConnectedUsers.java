package com.samuraibros.fatdolly;

import android.app.AlertDialog;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.ViewFlipper;

import java.util.ArrayList;

public class ConnectedUsers extends BaseActivity{

    //Determines whether activity is running
    public static boolean running = false;

    @Override
    protected void onReceive_helper(Context context, Intent intent) {

    }

    @Override
    protected void showLoading_helper(final boolean val) {
        Log.d(getResources().getString(R.string.app_name), mClass_string + ": showLoading: displaying activity screen...");
        if (mViewFlipper == null) {
            setContentView(R.layout.activity_connected_users);
            mViewFlipper = (ViewFlipper) findViewById(R.id.viewFlipper_connectedUsers);
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
        mClass_string = ConnectedUsers.class.toString();

        Log.d(getResources().getString(R.string.app_name), "ConnectedUsers: onCreate: starting...");

        // Reference to UI list view for device listing
        final ListView connectedUsers_listView = (ListView) findViewById(R.id.listview_connectedUsers);


        // Adapter to connect to list view
        ArrayAdapter<String> connectedUsers_arrayAdapter =
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

        // Sets the responses when the various device in the list view are clicked
        connectedUsers_listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, final int position, long arg3) {
                // Gets the device name and mac address and parses them out of the string
                final String device_name = (String) connectedUsers_listView.getItemAtPosition(position);
                final String device_mac = Configurations.userNameAddressMapToAddress(device_name);

                // Creates a popup dialog to provide further choices for a response
                final AlertDialog alertDialog = new AlertDialog.Builder(ConnectedUsers.this).create();

                // Sets the title for the popup dialog
                alertDialog.setTitle(device_name);

                String controllerAddress = Configurations.getControllerAddress();
                if (Configurations.isController() || !controllerAddress.equals(device_mac)) {
                    ArrayList<String> permissions = new ArrayList<>();
                    if (!Configurations.isController()) {
                        permissions = Configurations.userAddressToPermissions(controllerAddress);
                    }

                    final boolean playBackControl = permissions.contains(getResources().getString(R.string.permission_control_playback));
                    final boolean control;
                    boolean temp = false;
                    if (permissions != null && permissions.contains(getResources().getString(R.string.permission_control_users)))
                        temp = true;
                    control = temp;

                    if (Configurations.isController() || control) {
                        alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, getResources().getString(R.string.button_set_permissions), new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {

                                final ArrayList<String> mSelectedItems = Configurations.userAddressToPermissions(device_mac);
                                final String[] options = Configurations.USER_PERMISSIONS;
                                final boolean[] values = new boolean[options.length];
                                for (int idx = 0; idx < values.length; idx++) {
                                    values[idx] = mSelectedItems.contains(options[idx]);
                                }

                                // Creates a popup dialog to provide further choices for a response
                                final AlertDialog.Builder permissionDialog_builder = new AlertDialog.Builder(ConnectedUsers.this);

                                // Sets the title for the popup dialog
                                permissionDialog_builder.setTitle(device_name + "'s Permissions");


                                permissionDialog_builder.setMultiChoiceItems(options, values,
                                        new DialogInterface.OnMultiChoiceClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which,
                                                                boolean isChecked) {
                                                if (isChecked) {
                                                    // If the user checked the item, add it to the selected items
                                                    mSelectedItems.add(options[which]);
                                                } else if (mSelectedItems.contains(options[which])) {
                                                    // Else, if the item is already in the array, remove it
                                                    mSelectedItems.remove(options[which]);
                                                }
                                            }
                                        });

                                // Blacklists the selected user
                                permissionDialog_builder.setPositiveButton(getResources().getString(R.string.button_blacklist), new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        if (Configurations.isController()) {
                                            Intent i = new Intent(getResources().getString(R.string.intent_disconnect));
                                            i.putExtra(getResources().getString(R.string.extra_device_address), device_mac);
                                            i.putExtra(getResources().getString(R.string.extra_type), getResources().getString(R.string.value_user));
                                            sendBroadcast(i);
                                            refresh(null);
                                        }
                                        else {
                                            Intent i;
                                            /*Intent intent = new Intent(ConnectedUsers.this, Loading.class);
                                            intent.putExtra(getResources().getString(R.string.extra_sender_class), ConnectToDevice.class.toString());
                                            intent.putExtra(getResources().getString(R.string.extra_loading_type), getResources().getString(R.string.value_disconnect_user));
                                            intent.putExtra(getResources().getString(R.string.extra_device_address), device_mac);
                                            intent.putExtra(getResources().getString(R.string.extra_device_name), device_name);
                                            startActivity(intent);
                                            finish();*/
                                        }
                                    }
                                });

                                // Blacklists the selected user
                                permissionDialog_builder.setNegativeButton(getResources().getString(R.string.button_ok), new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        if (Configurations.isController()) {
                                            Intent i = new Intent(getResources().getString(R.string.intent_updated_user_permissions));
                                            i.putExtra(getResources().getString(R.string.extra_device_address), device_mac);
                                            i.putExtra(getResources().getString(R.string.extra_permissions), mSelectedItems);
                                            sendBroadcast(i);
                                            refresh(null);
                                        }
                                        else {
                                            Intent i;
                                            /*Intent intent = new Intent(ConnectedUsers.this, Loading.class);
                                            intent.putExtra(getResources().getString(R.string.extra_sender_class), ConnectToDevice.class.toString());
                                            intent.putExtra(getResources().getString(R.string.extra_loading_type), getResources().getString(R.string.value_update_user_permissions));
                                            intent.putExtra(getResources().getString(R.string.extra_device_address), device_mac);
                                            intent.putExtra(getResources().getString(R.string.extra_device_name), device_name);
                                            intent.putExtra(getResources().getString(R.string.extra_permissions), mSelectedItems);
                                            startActivity(intent);
                                            finish();*/
                                        }
                                    }
                                });

                                // Blacklists the selected user
                                permissionDialog_builder.setNeutralButton(getResources().getString(R.string.button_cancel), new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {

                                    }
                                });

                                final AlertDialog permissionDialog = permissionDialog_builder.create();
                                // Displays the dialogue
                                permissionDialog.show();
                            }
                        });

                        // Switch user Control
                        alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, getResources().getString(R.string.permission_control_users), new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                /*Intent intent = new Intent(ConnectedUsers.this, Loading.class);
                                intent.putExtra(getResources().getString(R.string.extra_sender_class), ConnectedUsers.class.toString());
                                intent.putExtra(getResources().getString(R.string.extra_loading_type), getResources().getString(R.string.loading_give_control_request));
                                intent.putExtra(getResources().getString(R.string.extra_responder_address), device_mac);
                                intent.putExtra(getResources().getString(R.string.extra_requester_address), Configurations.getHubAddress());
                                intent.putExtra(getResources().getString(R.string.extra_responder_name), device_name);
                                intent.putExtra(getResources().getString(R.string.extra_requester_name), Configurations.getHubName());
                                startActivity(intent);
                                refresh(null);
                                finish();*/
                            }
                        });

                        // Disconnects the selected user
                        alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, getResources().getString(R.string.button_disconnect), new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                if (Configurations.isController()) {
                                    Intent i = new Intent(getResources().getString(R.string.intent_disconnect));
                                    i.putExtra(getResources().getString(R.string.extra_device_address), device_mac);
                                    i.putExtra(getResources().getString(R.string.extra_type), getResources().getString(R.string.value_user));
                                    sendBroadcast(i);
                                }
                                else {
                                    Intent i;
                                    /*Intent intent = new Intent(ConnectedUsers.this, Loading.class);
                                    intent.putExtra(getResources().getString(R.string.extra_sender_class), ConnectToDevice.class.toString());
                                    intent.putExtra(getResources().getString(R.string.extra_loading_type), getResources().getString(R.string.value_disconnect_user));
                                    intent.putExtra(getResources().getString(R.string.extra_device_address), device_mac);
                                    intent.putExtra(getResources().getString(R.string.extra_device_name), device_name);
                                    startActivity(intent);
                                    finish();*/
                                }
                            }
                        });
                    }
                    else {
                        alertDialog.setMessage("You have no control over users!");
                    }
                }
                else if (controllerAddress.equals(device_mac)) {
                    // Disconnects the selected user
                    alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, getResources().getString(R.string.button_take_control), new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            /*Intent intent = new Intent(ConnectedUsers.this, Loading.class);
                            intent.putExtra(getResources().getString(R.string.extra_sender_class), ConnectedUsers.class.toString());
                            intent.putExtra(getResources().getString(R.string.extra_loading_type), getResources().getString(R.string.loading_take_control_request));
                            intent.putExtra(getResources().getString(R.string.extra_responder_address), device_mac);
                            intent.putExtra(getResources().getString(R.string.extra_requester_address), Configurations.getHubAddress());
                            intent.putExtra(getResources().getString(R.string.extra_responder_name), device_name);
                            intent.putExtra(getResources().getString(R.string.extra_requester_name), Configurations.getHubName());
                            startActivity(intent);
                            finish();*/
                        }
                    });
                }
                // Displays the dialogue
                alertDialog.show();
            }
        });
        // Connects the list view to the adapter
        connectedUsers_listView.setAdapter(connectedUsers_arrayAdapter);

        // Reset the list view
        refresh(null);


        running = true;

        initializeLoading();
        Log.d(getResources().getString(R.string.app_name), "ConnectedUsers: onCreate: ended");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        running = false;
    }

    /**
     * Reset the list view after any updates or changes
     * @param view
     */
    public void refresh(View view){
        // Initializes the adapter
        // Gets reference to the UI list view
        final ListView connectedUsers_listView = (ListView) findViewById(R.id.listview_connectedUsers);

        ArrayAdapter connectedUsers_arrayAdapter =
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
        // Connects the list view to the adapter
        connectedUsers_listView.setAdapter(connectedUsers_arrayAdapter);

        // Loop through connected users and add the name and address to the list
        for (String addr : Configurations.getUserAddresses()) {
            String name = Configurations.userAddressToName(addr);
            connectedUsers_arrayAdapter.add(name);
        }
    }
}
