package com.samuraibros.fatdolly;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceInfo;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceRequest;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.Button;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public abstract class BaseActivity extends AppCompatActivity {

    //Preferences ID
    public static final String PREFERENCES_ID = "AUDHUB";
    protected static WifiP2pManager mManager;
    protected static WifiP2pManager.Channel mChannel;
    protected BroadcastReceiver mReceiver;
    protected static WifiP2pManager.PeerListListener mPeerListListener;
    protected IntentFilter mIntentFilter;
    protected IntentFilter mServerIntentFilter;
    protected WifiP2pDnsSdServiceRequest serviceRequest;
    protected WifiP2pDnsSdServiceInfo serviceInfo;
    //Reference to the screen that called notifications
    protected Class<?> prev_screen;
    protected String mClass_string = BaseActivity.class.toString();

    //Reference to notifications button
    protected Button button_notifications;

    //Socket for sending as client
    Socket clientSocket = null;
    private PrintWriter out = null;
    private BufferedReader in = null;

    //String used to store the host address
    String host;

    //Port that is being sent to
    int port;

    //Animation for rotation
    protected RotateAnimation rotate_animation = new RotateAnimation(0, 360,
            Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF,
            0.5f);

    protected String registration_status;

    protected final HashMap<String, String> peerHubs = new HashMap<String, String>();
    protected WifiP2pManager.DnsSdTxtRecordListener txtListener;
    protected WifiP2pManager.DnsSdServiceResponseListener servListener;

    /*Configurations mService;
    boolean mBound = false;

    //Service connections
    private ServiceConnection MyConfigurations_Conn  = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            Configurations.LocalBinder binder = (Configurations.LocalBinder) service;
            mService = binder.getService();
            mBound = true;
            Log.d(getResources().getString(R.string.app_name), "Hub: onServiceConnected...");
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mBound = false;
            Log.d(getResources().getString(R.string.app_name), "Hub: onServiceDisconnected...");
        }
    };*/

    /**
     * Listens for "RefreshConnectedUsers" intents
     * @param
     */
    protected final BroadcastReceiver mServerReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.d(getResources().getString(R.string.app_name), mClass_string + ": BroadcastReceiver: Action to " + action);
            //Checks to see if bluetooth on/off status changes
            if (action.equals("")) {
            }
            else {
                onReceive_helper(context, intent);
            }
        }

    };

    protected abstract void onReceive_helper(Context context, Intent intent);

    public BaseActivity() {

    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mIntentFilter = new IntentFilter();
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

        mServerIntentFilter = new IntentFilter();

        mManager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        mChannel = mManager.initialize(this, getMainLooper(), null);
        mPeerListListener = new WifiP2pManager.PeerListListener() {
            @Override
            public void onPeersAvailable(WifiP2pDeviceList peers) {
                Log.d(getResources().getString(R.string.app_name), mClass_string + ": onPeersAvailable: received...");
                ArrayList<WifiP2pDevice> peer_list = new ArrayList<>(peers.getDeviceList());
                Intent i = new Intent(getResources().getString(R.string.intent_on_peers_available));
                i.putParcelableArrayListExtra(getResources().getString(R.string.extra_peer_list), peer_list);
                sendBroadcast(i);
            }
        };
        mReceiver = new WiFiDirectBroadcastReceiver();

        txtListener = new WifiP2pManager.DnsSdTxtRecordListener() {
            @Override
            public void onDnsSdTxtRecordAvailable(String fullDomain, Map record, WifiP2pDevice device) {
                Log.d(getResources().getString(R.string.app_name), mClass_string + ": DnsSdTxtRecord available -" + record.toString());
                if (!peerHubs.keySet().contains(device.deviceAddress)) {
                    Log.d(getResources().getString(R.string.app_name), mClass_string + ": NEW DnsSdTxtRecord available -" + record.toString());
                    peerHubs.put(device.deviceAddress, (String) record.get(getResources().getString(R.string.record_hub_name)));
                    discoverService();
                }
            }
        };

        servListener = new WifiP2pManager.DnsSdServiceResponseListener() {
            @Override
            public void onDnsSdServiceAvailable(String instanceName, String registrationType,
                                                WifiP2pDevice resourceType) {

                // Update the device name with the human-friendly version from
                // the DnsTxtRecord, assuming one arrived.
                resourceType.deviceName = peerHubs
                        .containsKey(resourceType.deviceAddress) ? peerHubs
                        .get(resourceType.deviceAddress) : resourceType.deviceName;

                // Add to the custom adapter defined specifically for showing
                // wifi devices.

                Log.d(getResources().getString(R.string.app_name), "onBonjourServiceAvailable " + instanceName);
            }
        };

        mManager.setDnsSdResponseListeners(mChannel, servListener, txtListener);

        serviceRequest = WifiP2pDnsSdServiceRequest.newInstance();
        mManager.addServiceRequest(mChannel,
                serviceRequest,
                new WifiP2pManager.ActionListener() {
                    @Override
                    public void onSuccess() {
                        // Success!
                        Log.d(getResources().getString(R.string.app_name), mClass_string + ": AddingServiceRequest Success");
                    }

                    @Override
                    public void onFailure(int code) {
                        // Command failed.  Check for P2P_UNSUPPORTED, ERROR, or BUSY
                        Log.d(getResources().getString(R.string.app_name), mClass_string + ": AddingServiceRequest Failed");
                    }
                });

        //Retrieves the reference to the calling activity class
        String class_name = getIntent().getStringExtra(getResources().getString(R.string.extra_sender_class));
        if (class_name != null) {
            Log.d(getResources().getString(R.string.app_name), mClass_string + ": Sender Class: " + class_name);
        }
        if (class_name != null && !class_name.equals("")) {
            try {
                String[] tokens = class_name.split(" ");
                prev_screen = Class.forName(tokens[tokens.length - 1]);
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }

        //Service bindning to ensure that service does not continue in background after closing
        /*Intent i = new Intent(this, Configurations.class);
        bindService(i, MyConfigurations_Conn, Context.BIND_AUTO_CREATE);*/
    }

    /* register the broadcast receiver with the intent values to be matched */
    @Override
    protected void onResume() {
        super.onResume();
    }
    /* unregister the broadcast receiver */
    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mServerReceiver);
    }

    protected void startRegistration() {
        Log.d(getResources().getString(R.string.app_name), mClass_string + ": startRegistration: starting...");
        //  Create a string map containing information about your service.
        Map record = new HashMap();
        SharedPreferences mPreferences = getSharedPreferences(BaseActivity.PREFERENCES_ID, 0);
        String hubName = mPreferences.getString(getResources().getString(R.string.record_hub_name), "");
        record.put(getResources().getString(R.string.record_hub_name), hubName);

        // Service information.  Pass it an instance name, service type
        // _protocol._transportlayer , and the map containing
        // information other devices will want once they connect to this one.
        serviceInfo = WifiP2pDnsSdServiceInfo.newInstance("FatDollySerVice", "_presence._tcp", record);

        // Add the local service, sending the service info, network channel,
        // and listener that will be used to indicate success or failure of
        // the request.
        mManager.addLocalService(mChannel, serviceInfo, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                // Command successful! Code isn't necessarily needed here,
                // Unless you want to update the UI or add logging statements.
                Log.d(getResources().getString(R.string.app_name), mClass_string + ": startRegistration: success");
                registration_status = getResources().getString(R.string.value_registration_succeeded);
            }

            @Override
            public void onFailure(int arg0) {
                Log.d(getResources().getString(R.string.app_name), mClass_string + ": startRegistration: failed");
                registration_status = getResources().getString(R.string.value_registration_failed);
            }
        });
        Log.d(getResources().getString(R.string.app_name), mClass_string + ": startRegistration: ending...");
    }

    protected void removeRegistration() {
        Log.d(getResources().getString(R.string.app_name), mClass_string + ": removeRegistration: starting...");
        if (serviceInfo == null) {
            Log.d(getResources().getString(R.string.app_name), mClass_string + ": removeRegistration: success");
            registration_status = getResources().getString(R.string.value_unregistration_succeeded);
        } else {
            mManager.removeLocalService(mChannel, serviceInfo, new WifiP2pManager.ActionListener() {
                @Override
                public void onSuccess() {
                    // Command successful! Code isn't necessarily needed here,
                    // Unless you want to update the UI or add logging statements.
                    Log.d(getResources().getString(R.string.app_name), mClass_string + ": removeRegistration: success");
                    registration_status = getResources().getString(R.string.value_unregistration_succeeded);
                }

                @Override
                public void onFailure(int arg0) {
                    Log.d(getResources().getString(R.string.app_name), mClass_string + ": removeRegistration: failed");
                    registration_status = getResources().getString(R.string.value_unregistration_failed);
                }
            });
        }
        Log.d(getResources().getString(R.string.app_name), mClass_string + ": removeRegistration: ending...");
    }

    protected void resetRegistration() {
        registration_status = null;
    }

    /**
     * Used to detect available peers that are in range
     */
    protected void discoverService() {
        Log.d(getResources().getString(R.string.app_name), mClass_string + ": discoverService(): starting...");

        mManager.discoverServices(mChannel, new WifiP2pManager.ActionListener() {

            @Override
            public void onSuccess() {
                // Success!
                Log.d(getResources().getString(R.string.app_name), mClass_string + ": discoverService(): P2P discovery success.");
            }

            @Override
            public void onFailure(int code) {
                // Command failed.  Check for P2P_UNSUPPORTED, ERROR, or BUSY
                Log.d(getResources().getString(R.string.app_name), mClass_string + ": discoverService(): P2P discovery failed.");
                if (code == WifiP2pManager.P2P_UNSUPPORTED) {
                    Log.d(getResources().getString(R.string.app_name), mClass_string + ": P2P isn't supported on this device.");
                }
                else if (code == WifiP2pManager.BUSY) {
                    Log.d(getResources().getString(R.string.app_name), mClass_string + ": P2P is busy.");
                }
                else if (code == WifiP2pManager.ERROR) {
                    Log.d(getResources().getString(R.string.app_name), mClass_string + ": P2P Unknown Error.");
                    if (code == WifiP2pManager.NO_SERVICE_REQUESTS) {
                        Log.d(getResources().getString(R.string.app_name), mClass_string + ": P2P No Service Requests.");
                    }
                }
            }
        });
        Log.d(getResources().getString(R.string.app_name), mClass_string + ": discoverService(): ended...");
    }

    /**
     * Closes and opens given activity
     * @param
     */
    protected void close (Class c){
        Intent intent;
        if (c == Loading.class || c == null) {
            intent = new Intent(this, Home.class);
        }
        else {
            intent = new Intent(this, c);
        }
        startActivity(intent);
        finish();        
    }

    /**
     * Return to the Home screen on back button press
     */
    @Override
    public void onBackPressed() {
        final AlertDialog alertDialog = new AlertDialog.Builder(this).create();

        // Sets the title for the popup dialog
        alertDialog.setTitle("You are leaving this screen!");
        alertDialog.setMessage("Are you sure?");

        // Removes the device from the connected state and removes it from the list view
        alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, getResources().getString(R.string.button_yes), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                onBackPressed_helper();
            }
        });

        // Cancels the popup dialogue
        alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, getResources().getString(R.string.button_no), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                alertDialog.cancel();
            }
        });

        if (mClass_string.equals(Hub.class.toString()) || mClass_string.equals(ConnectToHub.class.toString()) || mClass_string.equals(Loading.class.toString())) {
            // Displays the dialogue
            alertDialog.show();
        }
        else {
            onBackPressed_helper();
        }

    }

    private void onBackPressed_helper() {
        if (mClass_string.equals(Loading.class.toString()) || mClass_string.equals(ConnectToHub.class.toString()) || mClass_string.equals(Hub.class.toString())) {
            close(Home.class);
        } else {
            //Retrieves the reference to the calling activity class
            String class_name = Configurations.getPreviousActivity();
            if (class_name != null) {
                Log.d(getResources().getString(R.string.app_name), mClass_string + ": Sender Class: " + class_name);
            }
            Class last_screen = null;
            if (class_name != null && !class_name.equals("")) {
                try {
                    String[] tokens = class_name.split(" ");
                    last_screen = Class.forName(tokens[tokens.length - 1]);
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }
            close(last_screen);
        }
    }

    public void clearRequests(View view) {
        Intent i = new Intent(getResources().getString(R.string.intent_clear_playback_requests));
        sendBroadcast(i);
    }

    /**
     * Creates a playback request
     * @param context of the bound activity
     */
    public void playBackRequest(final Context context) {
        Log.d(getResources().getString(R.string.app_name), mClass_string + ": playBackRequest: starting...");
        ArrayList<String> permissions = new ArrayList<>();
        //Get the permissions
        if (!Configurations.isController()) {
            String controllerAddress = Configurations.getControllerAddress();
            permissions = Configurations.userAddressToPermissions(controllerAddress);
        }

        //Check if the Request Playback permission is given
        boolean playBackRequest = permissions.contains(getResources().getString(R.string.permission_request_playback));

        //If has apprpriate permissions, create the request
        if ((playBackRequest || Configurations.isController()) && !Configurations.getUserAddresses().isEmpty()) {
            final ArrayList<String> structure = new ArrayList<>();
            structure.add(Configurations.getHubAddress());
            final ArrayList<String> response = new ArrayList<>();

            // Creates a popup dialog to provide further choices for a response
            final AlertDialog.Builder durationDialog_builder = new AlertDialog.Builder(context);
            final String[] durations = {"6 seconds", "10 seconds", "15 seconds"};

            durationDialog_builder.setTitle("How long do users have to respond?");

            durationDialog_builder.setItems(durations, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    structure.add(durations[which]);
                    int time = Calendar.getInstance().get(Calendar.SECOND);
                    structure.add(Integer.toString(time));
                    Intent i = new Intent(getResources().getString(R.string.intent_create_playback_request));
                    i.putExtra(getResources().getString(R.string.extra_device_address), Configurations.getHubAddress());
                    String struct = TextUtils.join("-", structure);
                    i.putExtra(getResources().getString(R.string.extra_structure), struct);
                    i.putExtra(getResources().getString(R.string.extra_response), response.get(0));
                    context.sendBroadcast(i);
                }
            });

            final AlertDialog durationDialog = durationDialog_builder.create();

            // Creates a popup dialog to provide further choices for a response
            final AlertDialog.Builder playbackTypeDialog_builder = new AlertDialog.Builder(context);
            final String[] playback_types = Configurations.REQUEST_PLAYBACK_OPTIONS;

            playbackTypeDialog_builder.setTitle("What would you like to happen?");
            playbackTypeDialog_builder.setItems(playback_types, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    response.add(playback_types[which]);
                    structure.add(playback_types[which]);
                    durationDialog.show();
                }
            });
            final AlertDialog playbackTypeDialog = playbackTypeDialog_builder.create();

            // Creates a popup dialog to provide further choices for a response
            final AlertDialog.Builder requestTypeDialog_builder = new AlertDialog.Builder(context);
            final String[] request_types = {"Poll"};

            requestTypeDialog_builder.setTitle("What kind of request?");
            requestTypeDialog_builder.setItems(request_types, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    structure.add(request_types[which]);
                    playbackTypeDialog.show();
                }
            });
            final AlertDialog requestTypeDialog = requestTypeDialog_builder.create();
            requestTypeDialog.show();
        }
        //Notifies user that there are no users connected
        else if (Configurations.getUserAddresses().isEmpty()) {
            // Creates a popup dialog to provide further choices for a response
            final AlertDialog.Builder dialog_builder = new AlertDialog.Builder(context);

            // Sets the title for the popup dialog
            dialog_builder.setTitle("Your request was almost started");
            dialog_builder.setMessage("But since you don't have any users connected, why try...");
            final AlertDialog dialog = dialog_builder.create();
            // Displays the dialogue
            dialog.show();
        }
        //Notifies user that there are no appropriate permissions
        else {
            // Creates a popup dialog to provide further choices for a response
            final AlertDialog.Builder dialog_builder = new AlertDialog.Builder(context);

            // Sets the title for the popup dialog
            dialog_builder.setTitle("Trying to Change the Song?");
            dialog_builder.setMessage("...well, you can't");
            final AlertDialog dialog = dialog_builder.create();
            // Displays the dialogue
            dialog.show();
        }
        Log.d(getResources().getString(R.string.app_name), mClass_string + ": playBackRequest: ended...");
    }

    /**
     * Switches to the Connected Devices screen when the button is pressed
     * @param view
     */
    public void gotoConnDevices (View view) {
        Intent intent = new Intent(this, ConnectedDevices.class);
        Configurations.addActivityToStack(mClass_string);
        intent.putExtra(getResources().getString(R.string.extra_sender_class), mClass_string);
        startActivity(intent);
        finish();
    }


    /**
     * Switches to the requests screen when the button is pressed
     * @param view
     */
    public void gotoRequests (View view) {
        Intent intent = new Intent(this, PlaybackRequests.class);
        Configurations.addActivityToStack(mClass_string);
        intent.putExtra(getResources().getString(R.string.extra_sender_class), mClass_string);
        startActivity(intent);
        finish();
    }

    /**
     * Switches to the Connected Users screen when the button is pressed
     * @param view
     */
    public void gotoConnUsers (View view) {
        Intent intent = new Intent(this, ConnectedUsers.class);
        Configurations.addActivityToStack(mClass_string);
        intent.putExtra(getResources().getString(R.string.extra_sender_class), mClass_string);
        startActivity(intent);
        finish();
    }

    public void gotoNotifications(View view) {
        Intent intent = new Intent(this, Notifications.class);
        Configurations.addActivityToStack(mClass_string);
        intent.putExtra(getResources().getString(R.string.extra_sender_class), mClass_string);
        startActivity(intent);
        finish();
    }

    public void gotoSettings(View view) {
        Intent intent = new Intent(this, HubSettings.class);
        Configurations.addActivityToStack(mClass_string);
        intent.putExtra(getResources().getString(R.string.extra_sender_class), mClass_string);
        startActivity(intent);
        finish();
    }

    /**
     * Switches to the Connected to Devices screen when the button is pressed
     * @param view
     */
    public void gotoAddDevices(View view){
        Intent intent = new Intent(this, ConnectToDevice.class);
        Configurations.addActivityToStack(mClass_string);
        intent.putExtra(getResources().getString(R.string.extra_sender_class), mClass_string);
        startActivity(intent);
        finish();
    }

    /**
     * Start button animation
     */
    public void newNotification() {
        Configurations.mVibrator.vibrate(500);
        button_notifications = (Button) findViewById(R.id.button_notifications);
        if (button_notifications != null) {
            button_notifications.startAnimation(Configurations.notificationsAnimation);
            button_notifications.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(final View view) {
                    view.clearAnimation();
                    gotoNotifications(null);
                }
            });
        }
    }

    /**
     * Requests that the Media Play button click is simulated
     * @param view
     */
    public void playSong(View view){
        Intent i = new Intent("ChangePlayback");
        i.putExtra("Change", "Play");
        sendBroadcast(i);
    }

    /**
     * Requests that the Media Pause button click is simulated
     * @param view
     */
    public void pauseSong(View view){
        Intent i = new Intent("ChangePlayback");
        i.putExtra("Change", "Pause");
        sendBroadcast(i);
    }

    /**
     * Requests that the Media Next button click is simulated
     * @param view
     */
    public void nextSong(View view){

        /*
        Intent i = new Intent("ChangePlayback");
        i.putExtra("Change", "Next");
        sendBroadcast(i);
        */
    }

    /**
     * Requests that the Media Previos button click is simulated
     * @param view
     */
    public void prevSong(View view){
        Intent i = new Intent("ChangePlayback");
        i.putExtra("Change", "Previous");
        sendBroadcast(i);
    }

    /**
     * Requests disconnection from the hub
     * @param view
     */
    public void disconnect (View view){
        Intent i = new Intent("Disconnect");
        i.putExtra("Type", "DevicesUsers");
        sendBroadcast(i);
        close(Home.class);
    }

    /**
     * Switches to the Connected Users screen when the button is pressed
     * @param view
     */
    public void gotoConnectToHub (View view) {
        // Creates a popup dialog to provide further choices for a response
        final AlertDialog alertDialog = new AlertDialog.Builder(BaseActivity.this).create();

        // Sets the title for the popup dialog
        alertDialog.setTitle("Connect to Other Hub");
        alertDialog.setMessage("Are you sure? This will disconnect all your current users and devices!");

        // Switches to hub
        alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, getResources().getString(R.string.button_yes), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                gotoConnectToHub_helper();
            }
        });

        // Stays in current hub
        alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, getResources().getString(R.string.button_no), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                alertDialog.cancel();
            }
        });

        // Displays the dialogue
        if (mClass_string.equals(Home.class.toString()) || mClass_string.equals(Loading.class.toString())) {
            gotoConnectToHub_helper();
        }
        else{
            alertDialog.show();
        }
    }

    protected void gotoConnectToHub_helper() {
        Intent intent = new Intent(BaseActivity.this, Loading.class);
        Configurations.setController(false);
        intent.putExtra(getResources().getString(R.string.extra_sender_class), mClass_string);
        intent.putExtra(getResources().getString(R.string.extra_loading_type), getResources().getString(R.string.loading_connect_to_hub));
        intent.putExtra(getResources().getString(R.string.extra_loading_class), ConnectToHub.class.toString());
        startActivity(intent);
        finish();
    }

    /**
     * Switches to the Connected Users screen when the button is pressed
     * @param view
     */
    public void gotoHub (View view) {
        // Creates a popup dialog to provide further choices for a response
        final AlertDialog alertDialog = new AlertDialog.Builder(BaseActivity.this).create();

        // Sets the title for the popup dialog
        alertDialog.setTitle("Switch over to your Hub");
        alertDialog.setMessage("Are you sure? This will disconnect you from the current hub!");

        // Switches to hub
        alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, getResources().getString(R.string.button_yes), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {

            }
        });

        // Stays in current hub
        alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, getResources().getString(R.string.button_no), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                alertDialog.cancel();
            }
        });

        // Displays the dialogue
        if (mClass_string.equals(Home.class.toString()) || mClass_string.equals(Loading.class.toString())) {
            gotoHub_helper();
        }
        else{
            alertDialog.show();
        }
    }

    protected void gotoHub_helper() {
        Intent intent = new Intent(BaseActivity.this, Loading.class);
        Configurations.setController(true);
        intent.putExtra(getResources().getString(R.string.extra_sender_class), mClass_string);
        intent.putExtra(getResources().getString(R.string.extra_loading_type), getResources().getString(R.string.loading_hub));
        intent.putExtra(getResources().getString(R.string.extra_loading_class), ConnectToHub.class.toString());
        startActivity(intent);
        finish();
    }
/*
    private void connectWithServer() {
        Log.d(getResources().getString(R.string.app_name), "Hub:Connect to Server: Start Connection... " + Configurations.getControllerIP().getHostAddress());
        try {
            if (clientSocket == null) {
                clientSocket = new Socket(Configurations.getControllerIP().getHostAddress(), 8888);
                if (clientSocket == null)
                {
                    Log.d(getResources().getString(R.string.app_name), "Hub:Connect to Server: Client Socket is NULL ");
                }
                out = new PrintWriter(clientSocket.getOutputStream());
                Log.d(getResources().getString(R.string.app_name), "Hub:Connect to Server: Out: " + out.toString());
                if (out == null)
                {
                    Log.d(getResources().getString(R.string.app_name), "Hub:Connect to Server: Out is NULL ");
                }
                in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                Log.d(getResources().getString(R.string.app_name), "Hub:Connect to Server: In: " + in.toString());
                if (in == null)
                {
                    Log.d(getResources().getString(R.string.app_name), "Hub:Connect to Server: In is NULL ");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        Log.d(getResources().getString(R.string.app_name), "Hub:Connect to Server: Connection Started...");
    }

    private void disConnectWithServer() {
        if (clientSocket != null) {
            if (clientSocket.isConnected()) {
                try {
                    in.close();
                    out.close();
                    clientSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void sendMessage(String message) {
        Log.d(getResources().getString(R.string.app_name), "Hub:Send a Message: Start Sending... " + message);
        if (message != null) {
            connectWithServer();
            out.write(message);
            out.flush();
            disConnectWithServer();
        }
        Log.d(getResources().getString(R.string.app_name), "Hub:Send a Message: Done Sending...");
    }
    */
}
