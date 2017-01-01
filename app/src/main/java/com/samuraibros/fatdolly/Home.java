package com.samuraibros.fatdolly;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

public class Home extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        //TEST
    }

    public void gotoConnectToPeer (View view) {
        Intent i = new Intent(this, ConnectToPeer.class);
        startActivity(i);
    }
}