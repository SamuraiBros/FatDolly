package com.samuraibros.fatdolly;

import android.Manifest;
import android.app.AlertDialog;
import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.net.wifi.WifiManager;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class Home extends BaseActivity {

    //Determines whether activity is running
    public static boolean running = false;

    // Tells if permissions granted
    private static boolean permissionsGranted = true;

    @Override
    protected void onReceive_helper(Context context, Intent intent) {

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        registerReceiver(mServerReceiver, mServerIntentFilter);
        mClass = Home.class.toString();

        permissionsGranted = true;

        Configurations.mAudioManager = (AudioManager)getSystemService(Context.AUDIO_SERVICE);

        checkName();
        updateStatus();

        running = true;

        // Listens for permission grant and begins appropriate services
        /*Thread checkPermissionsThread = new CheckPermissionsThread();
        checkPermissionsThread.start();*/
    }


    @Override
    public void onDestroy() {
        super.onDestroy();

        Log.d(getResources().getString(R.string.app_name), "Home: onDestroy starting...");
        running = false;
        Log.d(getResources().getString(R.string.app_name), "Home: onDestroy stopping...");
    }

    /**
     * Checks to see if all requirements are fulfilled before enabling application
     */
    protected void updateStatus() {
        boolean valid_state = true;

        //Reference to the status message textview
        TextView message = (TextView) findViewById(R.id.textview_wifistatus);

        //References to the two app buttons
        Button button_hub = (Button) findViewById(R.id.button_hub);
        Button button_connectToHub = (Button) findViewById(R.id.button_connecttohub);

        //Checks to see if bluetooth is turned on or off
        WifiManager wifi = (WifiManager)getSystemService(Context.WIFI_SERVICE);
        if (!wifi.isWifiEnabled()){
            message.setText(getResources().getString(R.string.message_turnWifiOn));
            valid_state = false;
        }

        //Checks to see if given notificationlistener access
        String enabledAppList = Settings.Secure.getString(this.getContentResolver(), "enabled_notification_listeners");

        // enable bluetooth scanning on Android 6.0.1+
        ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.MEDIA_CONTENT_CONTROL, Manifest.permission.MODIFY_AUDIO_SETTINGS}, 1);

        //request notification access in order to read music metadata
        if (enabledAppList == null) {
            //service is not enabled try to enabled by calling...
            requestAccess();
        } else {
            if (!enabledAppList.contains(getApplicationContext().getPackageName())) {
                //service is not enabled try to enabled by calling...
                requestAccess();
            }
        }

        if (!permissionsGranted && valid_state) {
            message.setText(getResources().getString(R.string.message_grant_notificationlistener));
            valid_state = false;
        }

        //Check to see if name give
        SharedPreferences mPreferences = getSharedPreferences(BaseActivity.PREFERENCES_ID, 0);
        String hubName = mPreferences.getString(getResources().getString(R.string.record_hubName), "");

        if ((hubName == null || hubName.trim().equals("")) && valid_state) {
            message.setText(getResources().getString(R.string.message_setName));
            valid_state = false;
        }

        if (valid_state){
            message.setText("");
            button_connectToHub.setAlpha((float) 1.0);
            button_hub.setAlpha((float) 1.0);
        }
        else {
            button_connectToHub.setAlpha((float) 0.5);
            button_hub.setAlpha((float) 0.5);
        }
        button_hub.setEnabled(valid_state);
        button_connectToHub.setEnabled(valid_state);
    }

    /**
     * Requests access to grant appropriate permissions
     */
    private void requestAccess() {
        // Creates a popup dialog to provide further choices for a response
        final AlertDialog alertDialog = new AlertDialog.Builder(Home.this).create();

        // Sets the title for the popup dialog
        alertDialog.setTitle("Permission Request");
        alertDialog.setMessage("AudHub requires access to your notifications in order to display information such as the title and artist of the current song. Please go to settings to grant this access and restart AudHub");

        // Depending on the state of the device, adds or removes it from the connected devices state, device remains in the list view
        alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, "Go To HubSettings", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                Intent i  = new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS");
                startActivity(i);
                finish();
            }
        });

        // Closes the application
        alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "Deny Access", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                alertDialog.cancel();
            }
        });

        // Displays the dialogue
        alertDialog.show();
    }

    /**
     * Listens for permission grant and begins appropriate services
     */
    /*private class CheckPermissionsThread extends Thread {
        // Reference to the application context
        final Context context;
        // Last permission value
        Boolean last_permission = permissionsGranted;

        public CheckPermissionsThread() {
            context = getApplicationContext();
        }

        public void run() {
            while (Home.running) {
                Home.permissionsGranted = permissionsCheck();

                if (last_permission != Home.permissionsGranted) {
                    last_permission = Home.permissionsGranted;
                    Intent i = new Intent(getResources().getString(R.string.intent_home_updatestatus));
                    sendBroadcast(i);
                }
                try {
                    sleep(10, 0);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }*/

        /**
         * Checks the permissions
         * @return
         */
        /*private boolean permissionsCheck() {
            String enabledAppList = HubSettings.Secure.getString(context.getContentResolver(), "enabled_notification_listeners");
            //request notifcation access in order to read music metadata

            // enable bluetooth scanning on Android 6.0.1+
            ActivityCompat.requestPermissions(Home.this,new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.MEDIA_CONTENT_CONTROL, Manifest.permission.MODIFY_AUDIO_SETTINGS}, 1);

            if (enabledAppList == null) {
                return false;
            } else {
                if (!enabledAppList.contains(context.getPackageName())) {
                    return false;
                }
            }

            return true;
        }
    }*/

    /**
     * Prompts user to enter a name if one is not already defined
     */
    private void checkName() {
        SharedPreferences mPreferences = getSharedPreferences(BaseActivity.PREFERENCES_ID, 0);
        final String hubName = mPreferences.getString(getResources().getString(R.string.preference_hubName), "");

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
                    editor.putString(getResources().getString(R.string.preference_hubName), temp);
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

    /**
     * Starts the Hub activity on button press
     * @param view
     */
    public void gotoHub(View view) {
        if (permissionsGranted) {
            Log.d(getResources().getString(R.string.app_name), "Home:gotoHub: nName: " + Configurations.getHubName(this) + " Length: " + Integer.toString(Configurations.getHubName(this).length()));
            Configurations.addActivityToStack(Hub.class.toString());
            Configurations.setController(true);
            Intent intent = new Intent(this, Hub.class);
            startActivity(intent);
            finish();
        }
        else {
            updateStatus();
        }
    }

    /**
     * Starts the Connect to Hub activity on button press
     * @param view
     */
    public void gotoAddHub(View view) {
        if (permissionsGranted) {
            Log.d(getResources().getString(R.string.app_name), "Home:gotoAddHub: nName: " + Configurations.getHubName(this) + " Length: " + Integer.toString(Configurations.getHubName(this).length()));
            Configurations.addActivityToStack(ConnectToPeer.class.toString());
            Intent intent = new Intent(this, ConnectToPeer.class);
            startActivity(intent);
            finish();
        }
        else {
            updateStatus();
        }
    }
}

