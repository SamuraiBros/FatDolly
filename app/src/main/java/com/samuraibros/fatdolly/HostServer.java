package com.samuraibros.fatdolly;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

/**
 * Created by Takondwa Kakusa on 1/1/2017.
 */

public class HostServer extends BaseActivity{
    //FileServerAsyncTask serverAsyncTask;

    @Override
    protected void onReceive_helper(Context context, Intent intent) {

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.host_server);
        //serverAsyncTask = new FileServerAsyncTask();
    }
}
