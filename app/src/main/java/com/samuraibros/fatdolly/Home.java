package com.samuraibros.fatdolly;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.EditText;

public class Home extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        //TEST
    }

    /**
     * Prompts user to enter a name if one is not already defined
     */
    private void checkName() {
        SharedPreferences mPreferences = getSharedPreferences(BaseActivity.PREFERENCES_ID, 0);
        String hubName = mPreferences.getString(getResources().getString(R.string.record_hubName), "");

        if (hubName != null)
            Log.d(getResources().getString(R.string.app_name), "Home:CheckName: nName: " + hubName + " Length: " + Integer.toString(hubName.length()));
        else
            Log.d(getResources().getString(R.string.app_name), "Home:CheckName: nName: null");

        if (hubName == null || hubName.trim().equals("")) {
            final AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Name your hub:");
            builder.setCancelable(false);
            // Set up the input
            final EditText input = new EditText(this);
            // Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
            input.setInputType(InputType.TYPE_CLASS_TEXT);
            builder.setView(input);

            // Set up the buttons
            builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    String temp = input.getText().toString();
                    if (temp == null || temp.trim().equals(""))
                        temp = "JaneDoe";

                    SharedPreferences mPreferences = getSharedPreferences(BaseActivity.PREFERENCES_ID, 0);
                    SharedPreferences.Editor editor = mPreferences.edit();
                    editor.putString("HubName", temp);
                    editor.commit();
                }
            });

            builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialog) {
                    checkName();
                }
            });

            builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialog) {
                    checkName();
                }
            });
            builder.show();
        }
    }

    public void gotoConnectToPeer (View view) {
        Intent i = new Intent(this, ConnectToPeer.class);
        startActivity(i);
    }

}