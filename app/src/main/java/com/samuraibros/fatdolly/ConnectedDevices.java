package com.samuraibros.fatdolly;

import android.content.Context;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;

public class ConnectedDevices extends BaseActivity {

    //Determines whether activity is running
    public static boolean running = false;


    @Override
    protected void onReceive_helper(Context context, Intent intent) {

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connected_devices);

        registerReceiver(mServerReceiver, mServerIntentFilter);
        mClass_string = ConnectedDevices.class.toString();

        running = true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        running = false;
    }
}

