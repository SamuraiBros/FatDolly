package com.samuraibros.fatdolly;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.HashMap;
import java.util.Map;

public class ConnectToDevice extends BaseActivity {

    //Determines whether activity is running
    public static boolean running = false;

    @Override
    protected void onReceive_helper(Context context, Intent intent) {

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connect_to_device);

        registerReceiver(mServerReceiver, mServerIntentFilter);

        mClass = ConnectToDevice.class.toString();

        running = true;

        refresh(null);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        running = false;
    }

    /**
     * Refresh list
     * @param view
     */
    public void refresh(View view) {

    }
}

