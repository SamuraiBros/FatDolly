package com.samuraibros.fatdolly;

import android.app.AlertDialog;
import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.res.Configuration;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.StringTokenizer;

public class Loading extends BaseActivity {

    //Determines if waiting for certain message
    private ArrayList<String> pending;

    //Loacal intent to hold pending information
    private Intent pendingData;

    //Determines whether activity is running
    public static boolean running = false;

    //Local reference to message
    private TextView message_textView;

    //Toggled if connection interrupts loading
    private boolean connected_failed = false;

    @Override
    protected void onReceive_helper(Context context, Intent intent) {
        String action = intent.getAction();
        Log.d(getResources().getString(R.string.app_name), mClass_string + ": BroadcastReceiver: Action to " + action);

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_loading);

        // Register the BroadcastReceiver
        registerReceiver(mServerReceiver, mServerIntentFilter);

        mClass_string = Loading.class.toString();

        pending = new ArrayList<>();
        pendingData = new Intent();


        //Update loading message
        message_textView = (TextView) findViewById(R.id.textview_loadingMessage);
        String loading_type = getIntent().getStringExtra(getResources().getString(R.string.extra_loading_type));
        message_textView.setText(loading_type);

        running = true;
        connected_failed = false;

        if (loading_type.equals(getResources().getString(R.string.loading_hub)) || loading_type.equals(getResources().getString(R.string.loading_connect_to_hub))) {
            LoadHubThread thread = new LoadHubThread(loading_type);
            thread.start();
        }

    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(getResources().getString(R.string.app_name), mClass_string + ": onDestroy starting...");
        running = false;
        Log.d(getResources().getString(R.string.app_name), mClass_string + ": onDestroy stopping...");

    }

    /**
     * Updates the loading message
     */
    private void updateLoadingMessage(String message) {
        TextView message_textVie = (TextView) findViewById(R.id.textview_loadingMessage);
        message_textVie.setText(message);
    }


    /**
     * Initialize the hub
     */
    private class LoadHubThread extends Thread {
        private final String loading_type;
        public LoadHubThread(String l_t) {
            loading_type = l_t;
        }

        public void run() {
            Log.d(getResources().getString(R.string.app_name), mClass_string + ": LoadHubThread is running");
            Iterator<String> message = Arrays.asList(getResources().getStringArray(R.array.loading_hub_message)).iterator();

            String message_string = message.next();
            Log.d(getResources().getString(R.string.app_name), mClass_string + ": LoadHubThread " + message_string);
            //updateLoadingMessage(message_string);

            Configurations.resetConfigurations(Loading.this);

            try {
                sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            if (Configurations.isController()) {
                startRegistration();

                while (registration_status == null) {
                    try {
                        sleep(10);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                message_string = message.next();
                Log.d(getResources().getString(R.string.app_name), mClass_string + ": LoadHubThread " + message_string);
                //updateLoadingMessage(message_string);

                while (!registration_status.equals(getResources().getString(R.string.value_registration_succeeded))) {
                    try {
                        sleep(10);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    if (registration_status.equals(getResources().getString(R.string.value_registration_failed))) {
                        resetRegistration();
                        startRegistration();
                    }
                }
                resetRegistration();

            } else {
                removeRegistration();

                while (registration_status == null) {
                    try {
                        sleep(10);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                message_string = message.next();
                Log.d(getResources().getString(R.string.app_name), mClass_string + ": LoadHubThread " + message_string);
                //updateLoadingMessage(message_string);

                while (!registration_status.equals(getResources().getString(R.string.value_unregistration_succeeded))) {
                    try {
                        sleep(10);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    if (registration_status.equals(getResources().getString(R.string.value_unregistration_failed))) {
                        resetRegistration();
                        startRegistration();
                    }
                }
                resetRegistration();

                Configurations.setControllerIP(null);

                if (loading_type.equals(getResources().getString(R.string.loading_hub))) {

                    while (Configurations.getControllerIP() == null) {
                        try {
                            sleep(100);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        BaseActivity.mManager.requestConnectionInfo(mChannel, new WifiP2pManager.ConnectionInfoListener() {
                            @Override
                            public void onConnectionInfoAvailable(WifiP2pInfo info) {
                                InetAddress temp = info.groupOwnerAddress;
                                Log.d(getResources().getString(R.string.app_name), mClass_string + ": LoadHubThread ");
                                Configurations.setControllerIP(temp);
                            }
                        });
                    }
                }
            }

            try {
                sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            message_string = message.next();
            Log.d(getResources().getString(R.string.app_name), mClass_string + ": LoadHubThread " + message_string);
            //updateLoadingMessage(message_string);

            try {
                sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            if (loading_type.equals(getResources().getString(R.string.loading_hub))) {
                gotoHub_helper();
            }
            else if (loading_type.equals(getResources().getString(R.string.loading_connect_to_hub))) {
                gotoConnectToHub_helper();
            }
            Log.d(getResources().getString(R.string.app_name), mClass_string + ": LoadHubThread is finishing");
        }
    }

    @Override
    protected void gotoHub_helper() {
        Log.d(getResources().getString(R.string.app_name), mClass_string + ": gotoHub_helper: SwitchToHub");
        Intent intent = new Intent(this, Hub.class);
        intent.putExtra(getResources().getString(R.string.extra_sender_class), Loading.class.toString());
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    protected void gotoConnectToHub_helper() {
        Log.d(getResources().getString(R.string.app_name), mClass_string + ": gotoConnectToHub_helper: SwitchToConnHub");
        Intent intent = new Intent(this, ConnectToHub.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }
}

