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
import android.net.NetworkInfo;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceInfo;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceRequest;
import android.os.AsyncTask;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public abstract class BaseActivity extends AppCompatActivity {

    //Preferences ID
    public static final String PREFERENCES_ID = "AUDHUB";
    protected static WifiP2pManager mManager;
    protected static WifiP2pManager.Channel mChannel;
    protected static WifiP2pManager.PeerListListener mPeerListListener;
    protected IntentFilter mIntentFilter;
    protected IntentFilter mServerIntentFilter;
    protected WifiP2pDnsSdServiceRequest serviceRequest;
    protected WifiP2pDnsSdServiceInfo serviceInfo;
    //Reference to the screen that called notifications
    protected Class<?> prev_screen;
    protected String mClass_string = BaseActivity.class.toString();
    protected boolean discoveryEnabled = false;
    protected boolean managerInitialized = false;

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

    //Determines if waiting for certain message
    private ArrayList<String> pending;

    //Loacal intent to hold pending information
    private Intent pendingData;

    //Determines whether activity is running
    public static boolean running = false;

    //Local reference to message
    private TextView message_textView;

    protected ViewFlipper mViewFlipper;

    //Toggled if connection interrupts loading
    private boolean connected_failed = false;

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
            Log.d(getResources().getString(R.string.app_name), mClass_string + ": onServiceConnected...");
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mBound = false;
            Log.d(getResources().getString(R.string.app_name), mClass_string + ": onServiceDisconnected...");
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
            if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)) {
                // Check to see if Wi-Fi is enabled and notify appropriate activity
                int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);
                if (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED) {
                    // Wifi P2P is enabled
                } else {
                    // Wi-Fi P2P is not enabled
                }
            } else if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)) {
                // Call WifiP2pManager.requestPeers() to get a list of current peers
                // request available peers from the wifi p2p manager. This is an
                // asynchronous call and the calling activity is notified with a
                // callback on PeerListListener.onPeersAvailable()
                if (mManager != null) {
                    mManager.requestPeers(mChannel, mPeerListListener);
                }
            } else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {
                // Respond to new connection or disconnections
                WifiP2pGroup wifi_group = (WifiP2pGroup) intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_GROUP);
                NetworkInfo info = (NetworkInfo) intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);
                Log.d(getResources().getString(R.string.app_name), mClass_string + ": BroadcastReceiver: isGroupOwner: " + Boolean.toString(wifi_group.isGroupOwner()) + " Number peers: " + Integer.toString(wifi_group.getClientList().size()));

                Toast t = Toast.makeText(BaseActivity.this, mClass_string + ": BroadcastReceiver: isGroupOwner: " + Boolean.toString(wifi_group.isGroupOwner()) + " Number peers: " + Integer.toString(wifi_group.getClientList().size()), Toast.LENGTH_LONG);
                t.show();

                if (wifi_group.getClientList().size() > 0) {
                    t = Toast.makeText(BaseActivity.this, "isOwner:" + Boolean.toString(wifi_group.isGroupOwner()), Toast.LENGTH_LONG);
                    t.show();

                    for (WifiP2pDevice dev : wifi_group.getClientList()) {
                        Configurations.setUserInformation(BaseActivity.this, dev.deviceAddress, dev.deviceName, new ArrayList<String>(), getResources().getString(R.string.value_acceptor), true);
                    }
                }


            } else if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)) {
                // Respond to this device's wifi state changing
            } else if (action.equals("Next Song")) {
                Toast.makeText(getApplicationContext(), "NEXT SONG", Toast.LENGTH_SHORT).show();
            }
            else {
                onReceive_helper(context, intent);
            }
        }

    };



    protected abstract void onReceive_helper(Context context, Intent intent);

    protected  void showLoading(final boolean val) {
        Log.d(getResources().getString(R.string.app_name), mClass_string + ": showLoading: starting...");
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                    showLoading_helper(val);
            }
        });
        Log.d(getResources().getString(R.string.app_name), mClass_string + ": showLoading: ending...");
    }

    protected abstract void showLoading_helper(final boolean val);

    protected void initializeActivity() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                initializeActivity_helper();
            }
        });
    }


    protected abstract void initializeActivity_helper();

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

        mServerIntentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        mServerIntentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        mServerIntentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        mServerIntentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
        mServerIntentFilter.addAction(getResources().getString(R.string.intent_next_song));


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
        

        pending = new ArrayList<>();
        pendingData = new Intent();

        String loading_type = getIntent().getStringExtra(getResources().getString(R.string.extra_loading_type));

        if (loading_type != null) {
            showLoading(true);

            //Update loading message
            message_textView = (TextView) findViewById(R.id.textView_loadingOverlayMessage);
            message_textView.setText(loading_type);

            connected_failed = false;
        } else {
            showLoading(false);
        }


        //Service bindning to ensure that service does not continue in background after closing
        /*Intent i = new Intent(this, Configurations.class);
        bindService(i, MyConfigurations_Conn, Context.BIND_AUTO_CREATE);*/
    }

    protected void initializeLoading() {
        String loading_type = getIntent().getStringExtra(getResources().getString(R.string.extra_loading_type));
        if (loading_type != null) {
            if (loading_type.equals(getResources().getString(R.string.loading_hub)) || loading_type.equals(getResources().getString(R.string.loading_connect_to_hub))) {
                LoadHubThread thread = new LoadHubThread(loading_type);
                thread.start();
            }
        }
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

    protected void disconnect() {
        if (mManager != null && mChannel != null) {
            mManager.requestGroupInfo(mChannel, new WifiP2pManager.GroupInfoListener() {
                @Override
                public void onGroupInfoAvailable(WifiP2pGroup group) {
                    if (group != null && mManager != null && mChannel != null) {
                        mManager.removeGroup(mChannel, new WifiP2pManager.ActionListener() {

                            @Override
                            public void onSuccess() {
                                Log.d(getResources().getString(R.string.app_name), mClass_string + ": disconnect(): removeGroup onSuccess -");
                            }

                            @Override
                            public void onFailure(int reason) {
                                Log.d(getResources().getString(R.string.app_name), mClass_string + ": disconnect(): removeGroup onFailure -" + reason);
                            }
                        });

                        /*try {
                            Method getNetworkId = WifiP2pGroup.class.getMethod("getNetworkId");
                            Integer networkId = (Integer) getNetworkId.invoke(group);
                            Method deletePersistentGroup = WifiP2pManager.class.getMethod("deletePersistentGroup",
                                    WifiP2pManager.Channel.class, Integer.class, WifiP2pManager.ActionListener.class);
                            deletePersistentGroup.invoke(mManager, mChannel, networkId, null);
                        } catch (NoSuchMethodException e) {
                            Log.e("WIFI", "Could not delete persistent group", e);
                        } catch (InvocationTargetException e) {
                            Log.e("WIFI", "Could not delete persistent group", e);
                        } catch (IllegalAccessException e) {
                            Log.e("WIFI", "Could not delete persistent group", e);
                        }*/
                    }
                }
            });
        }
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
            mManager.clearLocalServices(mChannel, new WifiP2pManager.ActionListener() {
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

    protected void initializeWifiManager() {
        Log.d(getResources().getString(R.string.app_name), mClass_string + ": initializeWifiManager(): starting...");
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

        txtListener = new WifiP2pManager.DnsSdTxtRecordListener() {
            @Override
            public void onDnsSdTxtRecordAvailable(String fullDomain, Map record, WifiP2pDevice device) {
                Log.d(getResources().getString(R.string.app_name), mClass_string + ": DnsSdTxtRecord available -" + record.toString());
                if (!peerHubs.keySet().contains(device.deviceAddress)) {
                    Log.d(getResources().getString(R.string.app_name), mClass_string + ": NEW DnsSdTxtRecord available -" + record.toString());
                    peerHubs.put(device.deviceAddress, (String) record.get(getResources().getString(R.string.record_hub_name)));
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
                        Log.d(getResources().getString(R.string.app_name), mClass_string + ": InitializeWifiManager Success");
                        if (Configurations.isController()) {
                            mManager.createGroup(mChannel, new WifiP2pManager.ActionListener() {
                                @Override
                                public void onSuccess() {
                                    managerInitialized = true;
                                }

                                @Override
                                public void onFailure(int reason) {
                                    managerInitialized = false;
                                }
                            });
                        }
                        else {
                            managerInitialized = true;
                        }

                    }

                    @Override
                    public void onFailure(int code) {
                        // Command failed.  Check for P2P_UNSUPPORTED, ERROR, or BUSY
                        Log.d(getResources().getString(R.string.app_name), mClass_string + ": AddingServiceRequest Failed");
                        Log.d(getResources().getString(R.string.app_name), mClass_string + ": InitializeWifiManager Failed");
                        managerInitialized = false;
                    }
                });
        Log.d(getResources().getString(R.string.app_name), mClass_string + ": initializeWifiManager(): ending...");
    }

    protected void stopDiscoveryService() {
        Log.d(getResources().getString(R.string.app_name), mClass_string + ": stopDiscoveryService(): starting...");

        mManager.stopPeerDiscovery(mChannel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                mManager.clearServiceRequests(mChannel, new WifiP2pManager.ActionListener() {
                    @Override
                    public void onSuccess() {
                        mManager.clearLocalServices(mChannel, new WifiP2pManager.ActionListener() {
                            @Override
                            public void onSuccess() {
                                Log.d(getResources().getString(R.string.app_name), mClass_string + ": stopDiscoveryService(): succeeded.");
                                discoveryEnabled = false;
                            }

                            @Override
                            public void onFailure(int reason) {
                                Log.d(getResources().getString(R.string.app_name), mClass_string + ": stopDiscoveryService(): failed clearLocalServices...");
                            }
                        });
                    }

                    @Override
                    public void onFailure(int reason) {
                        Log.d(getResources().getString(R.string.app_name), mClass_string + ": stopDiscoveryService(): failed clearServiceRequests...");
                    }
                });
            }

            @Override
            public void onFailure(int reason) {
                Log.d(getResources().getString(R.string.app_name), mClass_string + ": stopDiscoveryService(): failed stopPeerDiscovery...");
            }
        });

        Log.d(getResources().getString(R.string.app_name), mClass_string + ": stopDiscoveryService(): ending...");

     }

    /**
     * Used to detect available peers that are in range
     */
    protected void startDiscoveryService() {
        Log.d(getResources().getString(R.string.app_name), mClass_string + ": startDiscoveryService(): starting...");

        mManager.discoverServices(mChannel, new WifiP2pManager.ActionListener() {

            @Override
            public void onSuccess() {
                // Success!
                Log.d(getResources().getString(R.string.app_name), mClass_string + ": startDiscoveryService(): P2P discovery success.");
                discoveryEnabled = true;
            }

            @Override
            public void onFailure(int code) {
                // Command failed.  Check for P2P_UNSUPPORTED, ERROR, or BUSY
                Log.d(getResources().getString(R.string.app_name), mClass_string + ": startDiscoveryService(): P2P discovery failed.");
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
        Log.d(getResources().getString(R.string.app_name), mClass_string + ": startDiscoveryService(): ended...");
    }

    /**
     * Updates the loading message
     */
    protected void updateLoadingMessage(final String message) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                TextView message_textVie = (TextView) findViewById(R.id.textView_loadingOverlayMessage);
                message_textVie.setText(message);
            }
        });
    }
    
    /**
     * Closes and opens given activity
     * @param
     */
    protected void close (Class c){
        Intent intent;
        if (c == null) {
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

        if (mClass_string.equals(Hub.class.toString()) || mClass_string.equals(ConnectToHub.class.toString())) {
            // Displays the dialogue
            alertDialog.show();
        }
        else {
            onBackPressed_helper();
        }

    }

    private void onBackPressed_helper() {
        if (mClass_string.equals(ConnectToHub.class.toString()) || mClass_string.equals(Hub.class.toString())) {
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
        String next = "Next Song";
        Client client = new Client("192.168.49.1",8888,next);
        client.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
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
        if (mClass_string.equals(Home.class.toString())) {
            gotoConnectToHub_helper();
        }
        else{
            alertDialog.show();
        }
    }


    protected void gotoConnectToHub_helper() {
        Intent intent = new Intent(BaseActivity.this, ConnectToHub.class);
        Configurations.setController(false);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
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
        if (mClass_string.equals(Home.class.toString())) {
            gotoHub_helper();
        }
        else{
            alertDialog.show();
        }
    }

    protected void gotoHub_helper() {
        Intent intent = new Intent(BaseActivity.this, Hub.class);
        if (!mClass_string.equals(ConnectToHub.class.toString()))
            Configurations.setController(true);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra(getResources().getString(R.string.extra_sender_class), mClass_string);
        intent.putExtra(getResources().getString(R.string.extra_loading_type), getResources().getString(R.string.loading_hub));
        intent.putExtra(getResources().getString(R.string.extra_loading_class), ConnectToHub.class.toString());
        startActivity(intent);
        finish();
    }

    private void connectWithServer() {
        Log.d(getResources().getString(R.string.app_name), "Hub:Connect to Server: Start Connection... ");
        try {
            if (clientSocket == null) {
                clientSocket = new Socket();
                clientSocket.bind(null);
                clientSocket.connect((new InetSocketAddress(host, port)), 500);
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

    /**
     * Initialize the hub
     */
    private class LoadHubThread extends Thread {
        private final String loading_type;
        public LoadHubThread(String l_t) {
            loading_type = l_t;
        }

        public void run() {
            Log.d(getResources().getString(R.string.app_name), mClass_string + ": LoadHubThread is running");
            Iterator<String> message = Arrays.asList(getResources().getStringArray(R.array.loading_hub_message)).iterator();

            try {
                sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            String message_string = message.next();
            Log.d(getResources().getString(R.string.app_name), mClass_string + ": LoadHubThread " + message_string);
            updateLoadingMessage(message_string);

            Configurations.resetConfigurations(BaseActivity.this);

            try {
                sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            message_string = message.next();
            Log.d(getResources().getString(R.string.app_name), mClass_string + ": LoadHubThread " + message_string);
            updateLoadingMessage(message_string);

            if (managerInitialized) {
                stopDiscoveryService();

                while (discoveryEnabled) {
                    try {
                        sleep(10);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }

            message_string = message.next();
            Log.d(getResources().getString(R.string.app_name), mClass_string + ": LoadHubThread " + message_string);
            updateLoadingMessage(message_string);

            if (mManager != null) {
                disconnect();
            }
            initializeWifiManager();
            disconnect();

            while (!managerInitialized) {
                try {
                    sleep(10);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            try {
                sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            message_string = message.next();
            Log.d(getResources().getString(R.string.app_name), mClass_string + ": LoadHubThread " + message_string);
            updateLoadingMessage(message_string);

            startDiscoveryService();

            while (!discoveryEnabled) {
                try {
                    sleep(10);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            try {
                sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }


            if (Configurations.isController())
                startRegistration();
            else
                removeRegistration();

            while (registration_status == null) {
                try {
                    sleep(10);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            message_string = message.next();

            Log.d(getResources().getString(R.string.app_name), mClass_string + ": LoadHubThread " + message_string);
            updateLoadingMessage(message_string);

            while ((!registration_status.equals(getResources().getString(R.string.value_registration_succeeded)) && Configurations.isController()) ||
                    (!registration_status.equals(getResources().getString(R.string.value_unregistration_succeeded)) && !Configurations.isController())) {
                try {
                    sleep(10);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if (registration_status.equals(getResources().getString(R.string.value_registration_failed))) {
                    resetRegistration();
                    startRegistration();
                } else if (registration_status.equals(getResources().getString(R.string.value_unregistration_failed))) {
                    resetRegistration();
                    removeRegistration();
                }
            }
            resetRegistration();

            message_string = message.next();
            Log.d(getResources().getString(R.string.app_name), mClass_string + ": LoadHubThread " + message_string);

            if (loading_type.equals(getResources().getString(R.string.loading_hub)) && !Configurations.isController()) {
                Configurations.setControllerIP(null);

                while (Configurations.getControllerIP() == null) {
                    BaseActivity.mManager.requestConnectionInfo(mChannel, new WifiP2pManager.ConnectionInfoListener() {
                        @Override
                        public void onConnectionInfoAvailable(WifiP2pInfo info) {
                            InetAddress temp = info.groupOwnerAddress;
                            Configurations.setControllerIP(temp);
                        }
                    });

                    try {
                        sleep(200);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }


                }
            }



            message_string = message.next();
            Log.d(getResources().getString(R.string.app_name), mClass_string + ": LoadHubThread " + message_string);
            updateLoadingMessage(message_string);

            try {
                sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }


            /*if (loading_type.equals(getResources().getString(R.string.loading_hub))) {
                gotoHub_helper();
            } else if (loading_type.equals(getResources().getString(R.string.loading_connect_to_hub))) {
                gotoConnectToHub_helper();
            }*/
            showLoading(false);
            initializeActivity();
            Log.d(getResources().getString(R.string.app_name), mClass_string + ": LoadHubThread is finishing");

        }
    }
}
