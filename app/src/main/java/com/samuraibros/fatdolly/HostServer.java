package com.samuraibros.fatdolly;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ViewFlipper;

/**
 * Created by Takondwa Kakusa on 1/1/2017.
 */

public class HostServer extends BaseActivity{
    //FileServerAsyncTask serverAsyncTask;

    @Override
    protected void onReceive_helper(Context context, Intent intent) {

    }

    @Override
    protected void showLoading_helper(final boolean val) {
        Log.d(getResources().getString(R.string.app_name), mClass_string + ": showLoading: displaying activity screen...");
        if (mViewFlipper == null) {
            setContentView(R.layout.host_server);
            mViewFlipper = (ViewFlipper) findViewById(R.id.viewFlipper_hoseServer);
        }

        if (!val) {
            mViewFlipper.showNext();
        }
    }

    @Override
    protected void initializeActivity_helper() {

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //serverAsyncTask = new FileServerAsyncTask();
    }
}
