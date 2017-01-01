package com.samuraibros.fatdolly;

import android.app.Activity;
import android.os.Bundle;

/**
 * Created by Takondwa Kakusa on 1/1/2017.
 */

public class HostServer extends BaseActivity{
    //FileServerAsyncTask serverAsyncTask;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.host_server);
        //serverAsyncTask = new FileServerAsyncTask();
    }
}
