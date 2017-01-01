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
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

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
        Log.d("AudHub", "Loading: BroadcastReceiver: Action to " + action);

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_loading);

        // Register the BroadcastReceiver
        registerReceiver(mServerReceiver, mServerIntentFilter);

        mClass = Loading.class.toString();

        pending = new ArrayList<>();
        pendingData = new Intent();


        //Update loading message
        message_textView = (TextView) findViewById(R.id.textview_loadingMessage);
        String loadingType = getIntent().getStringExtra(getResources().getString(R.string.extra_loading_type));
        message_textView.setText(loadingType);

        running = true;
        connected_failed = false;

    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d("AudHub", "Loading: onDestroy starting...");
        running = false;
        Log.d("AudHub", "Loading: onDestroy stopping...");

    }

    /**
     * Updates the loading message
     */
    private void updateLoadingMessage(String message) {
        message_textView.setText(message);
    }


    private void switchToHub() {
        Log.d("AudHub", "Loading: SwitchToHub");
        Intent intent = new Intent(this, Hub.class);
        intent.putExtra(getResources().getString(R.string.extra_sender_class), Loading.class.toString());
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    private void switchToConnHub() {
        Log.d("AudHub", "Loading: SwitchToConnHub");
        Intent intent = new Intent(this, ConnectToHub.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }
}

