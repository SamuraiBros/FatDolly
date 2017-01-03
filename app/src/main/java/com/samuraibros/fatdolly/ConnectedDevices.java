package com.samuraibros.fatdolly;

import android.content.Context;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.ViewFlipper;

public class ConnectedDevices extends BaseActivity {

    //Determines whether activity is running
    public static boolean running = false;


    @Override
    protected void onReceive_helper(Context context, Intent intent) {

    }

    @Override
    protected void showLoading_helper(final boolean val) {
        Log.d(getResources().getString(R.string.app_name), mClass_string + ": showLoading: displaying activity screen...");
        if (mViewFlipper == null) {
            setContentView(R.layout.activity_connected_devices);
            mViewFlipper = (ViewFlipper) findViewById(R.id.viewFlipper_connectedDevices);
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
        mClass_string = ConnectedDevices.class.toString();

        running = true;

        initializeLoading();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        running = false;
    }
}

