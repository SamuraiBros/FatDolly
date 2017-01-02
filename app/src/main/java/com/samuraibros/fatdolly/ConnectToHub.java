package com.samuraibros.fatdolly;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceRequest;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import java.util.HashMap;
import java.util.Map;

public class ConnectToHub extends BaseActivity {

    //Determines whether activity is running
    public static boolean running = false;

    //Determied if the animation is running
    public static Boolean rotate_animation_running = false;

    // Reference to UI list view for device listing
    protected ListView connectToHub_listView;

    private WifiP2pDevice mDevice;
    private WifiP2pConfig config = new WifiP2pConfig();
    private ArrayAdapter peerDevices_arrayAdapter;
    private ListView peerDevices_listView;
    private Map<String, String> peerNametoAddress_map = new HashMap<>();
    private Map<String, WifiP2pDevice> peerAddresstoDevice_map = new HashMap<>();
    private final HashMap<String, String> peerHubs = new HashMap<String, String>();
    private WifiP2pManager.DnsSdTxtRecordListener txtListener;
    private WifiP2pManager.DnsSdServiceResponseListener servListener;

    @Override
    protected void onReceive_helper(Context context, Intent intent) {

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connect_to_hub);

        registerReceiver(mServerReceiver, mServerIntentFilter);
        mClass_string = ConnectToHub.class.toString();

        running = true;

        Intent i;

        // Holds list of already discovered device to prevent duplicates on list
        //discoveredDevices.clear();

        /*if (Build.VERSION.SDK_INT >= 21) {
            mLEScanner = Configurations.mBluetoothAdapter.getBluetoothLeScanner();
            settings = new ScanSettings.Builder()
                    .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                    .build();
            filters = new ArrayList<ScanFilter>();

            scanLeDevice(true);
        }*/

        //Reference to refresh button
        final Button refresh_button = (Button) findViewById(R.id.button_hub_refresh);

        rotate_animation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                ConnectToHub.rotate_animation_running = true;
                refresh_button.setEnabled(false);
                refresh_button.setAlpha((float).5);
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                ConnectToHub.rotate_animation_running = false;
                refresh_button.setEnabled(true);
                refresh_button.setAlpha((float)1);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });

        rotate_animation.setDuration(2000);
        rotate_animation.setRepeatCount(5);
        refresh_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                v.clearAnimation();
                v.startAnimation(rotate_animation);
                refresh(null);
            }
        });

        mPeerListListener  = new WifiP2pManager.PeerListListener() {
            @Override
            public void onPeersAvailable(WifiP2pDeviceList peers) {
                //Clear the map list and add the new list of name and address pairings
                boolean new_peer = false;
                for (WifiP2pDevice dev : peers.getDeviceList()) {
                    //if (!peerNametoAddress_map.containsKey(dev.deviceName) && peerHubs.containsKey(dev.deviceAddress)) {
                    if (!peerNametoAddress_map.containsKey(dev.deviceName)) {
                        //Log.d(getResources().getString(R.string.app_name), "ConnectToPeer:onPeersAvailable: New peer available -" + peerHubs.get(dev.deviceAddress));
                        Log.d(getResources().getString(R.string.app_name), "ConnectToPeer:onPeersAvailable: New peer available -" + dev.deviceName);
                        //peerNametoAddress_map.put(peerHubs.get(dev.deviceAddress), dev.deviceAddress);
                        peerNametoAddress_map.put(dev.deviceName, dev.deviceAddress);
                        peerAddresstoDevice_map.put(dev.deviceAddress, dev);
                        new_peer = true;
                    }
                    else {
                        Log.d(getResources().getString(R.string.app_name), "ConnectToPeer:onPeersAvailable: Old Peer or other device -" + dev.deviceName);
                    }
                }

                if (new_peer) {
                    peerDevices_arrayAdapter.clear();
                    peerDevices_arrayAdapter.addAll(peerNametoAddress_map.keySet());
                }

                /*if (peerNametoAddress_map.isEmpty()) {
                    refreshPeers(null);
                }*/
            }
        };

        txtListener = new WifiP2pManager.DnsSdTxtRecordListener() {
            @Override
        /* Callback includes:
         * fullDomain: full domain name: e.g "printer._ipp._tcp.local."
         * record: TXT record dta as a map of key/value pairs.
         * device: The device running the advertised service.
         */

            public void onDnsSdTxtRecordAvailable(String fullDomain, Map record, WifiP2pDevice device) {
                Log.d(getResources().getString(R.string.app_name), "ConnectToPeer: DnsSdTxtRecord available -" + record.toString());
                if (!peerHubs.keySet().contains(device.deviceAddress)) {
                    Log.d(getResources().getString(R.string.app_name), "ConnectToPeer: NEW DnsSdTxtRecord available -" + record.toString());
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
                /*WiFiDirectServicesList fragment = (WiFiDirectServicesList) getFragmentManager()
                        .findFragmentById(R.id.frag_peerlist);
                WiFiDevicesAdapter adapter = ((WiFiDevicesAdapter) fragment
                        .getListAdapter());

                adapter.add(resourceType);
                adapter.notifyDataSetChanged();*/
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
                        Log.d(getResources().getString(R.string.app_name), "ConnectToPeer: AddingServiceRequest Success");
                    }

                    @Override
                    public void onFailure(int code) {
                        // Command failed.  Check for P2P_UNSUPPORTED, ERROR, or BUSY
                        Log.d(getResources().getString(R.string.app_name), "ConnectToPeer: AddingServiceRequest Failed");
                    }
                });

        peerDevices_listView = (ListView) findViewById(R.id.listview_connectToHub);

        // Sets the responses when the various device in the list view are clicked
        peerDevices_listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3) {
                // Gets the device name and mac address and parses them out of the string
                final String device_name = (String) peerDevices_listView.getItemAtPosition(position);
                final String device_address = peerNametoAddress_map.get(device_name);
                final WifiP2pDevice device = peerAddresstoDevice_map.get(device_address);
                // Creates a popup dialog to provide further choices for a response
                final android.app.AlertDialog alertDialog = new android.app.AlertDialog.Builder(ConnectToHub.this).create(); //Read Update
                // Sets the title for the popup dialog
                alertDialog.setTitle(device_name);

                //Adds the device to the connectedDevice list and adds it to the connected state
                alertDialog.setButton(android.app.AlertDialog.BUTTON_NEGATIVE, "Connect", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        mDevice = device;
                        connectPeer();

                    }

                });

                // Cancels the popup dialogue
                alertDialog.setButton(android.app.AlertDialog.BUTTON_POSITIVE, "Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        alertDialog.cancel();
                    }
                });

                // Displays the dialogue
                alertDialog.show();
            }
        });

        refresh(null);

        /*// Initializes the adapter
        search_arrayAdapter =
                new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1);
        // Gets reference to the UI list view
        connectToHub_listView = (ListView) findViewById(R.id.listview_connectToHub);

        // Connects the list view to the adapter
        connectToHub_listView.setAdapter(search_arrayAdapter);

        // Sets the responses when the various device in the list view are clicked
        connectToHub_listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3) {
                // Gets the device name and mac address and parses them out of the string
                String device_name_temp = (String) connectToHub_listView.getItemAtPosition(position);
                String[] tokens = device_name_temp.split("'");
                final String device_name = tokens[0];
                final String device_mac = discoveredDevices.get(device_name);
                Log.d("AudhHub", mClass_string + ": ClickListener: Device Address: " + device_mac);

                // Creates a popup dialog to provide further choices for a response
                final AlertDialog alertDialog = new AlertDialog.Builder(ConnectToHub.this).create(); //Read Update
                // Sets the title for the popup dialog
                alertDialog.setTitle(device_name);

                // Starts connection to the selected hub
                alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, "Share Control", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        Intent intent = new Intent(ConnectToHub.this, Loading.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        intent.putExtra(getResources().getString(R.string.extra_sender_class), ConnectToHub.class.toString());
                        intent.putExtra(getResources().getString(R.string.extra_loading_type), getResources().getString(R.string.loading_share_control_request));
                        intent.putExtra(getResources().getString(R.string.extra_requester_address), Configurations.getHubAddress());
                        intent.putExtra(getResources().getString(R.string.extra_responder_address), device_mac);
                        intent.putExtra(getResources().getString(R.string.extra_responder_name), device_name);
                        intent.putExtra(getResources().getString(R.string.extra_requester_name), Configurations.getHubName());
                        startActivity(intent);
                        finish();
                    }
                });


                 * REMOVED FOR NOW TO EXCLUDE COMPLICATION OF TAKING CONTROL OF A HUB THAT ALREADY HAS A USER CONNECTED
                 * SINCE THE USER ALREADY CONNECTED IS UNAWARE OF THE USER MAKING THE REQUEST IN CURRENT IMPLEMENTATION
                 // Cancels the popup dialogue
                 alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "Take Control", new DialogInterface.OnClickListener() {
                 public void onClick(DialogInterface dialog, int which) {
                 Intent intent = new Intent(ConnectToHub.this, Loading.class);
                 intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                 intent.putExtra(getResources().getString(R.string.extra_sender_class), ConnectToHub.class.toString());
                 intent.putExtra(getResources().getString(R.string.extra_loading_type), "TakeControlRequest");
                 intent.putExtra("RequestingAddress", Configurations.getHubAddress());
                 intent.putExtra(getResources().getString(R.string.extra_device_address), device_mac);
                 startActivity(intent);
                 finish();

                 }
                 });

                // Displays the dialogue
                alertDialog.show();
            }
        });*/

        refresh_button.clearAnimation();
        refresh_button.startAnimation(rotate_animation);
        refresh(null);

        running = true;
        Log.d(getResources().getString(R.string.app_name), mClass_string + ": onCreate: ended");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        running = false;
    }

    /**
     * Used to detect available peers that are in range
     */
    private void discoverService() {
        Log.d(getResources().getString(R.string.app_name), "discoverService(): starting...");
        mManager.discoverServices(mChannel, new WifiP2pManager.ActionListener() {

            @Override
            public void onSuccess() {
                // Success!
            }

            @Override
            public void onFailure(int code) {
                // Command failed.  Check for P2P_UNSUPPORTED, ERROR, or BUSY
                if (code == WifiP2pManager.P2P_UNSUPPORTED) {
                    Log.d(getResources().getString(R.string.app_name), "P2P isn't supported on this device.");
                }
            }
        });
        Log.d(getResources().getString(R.string.app_name), "discoverService(): ended...");
    }

    /**
     * Used to connect to peer
     */
    private void connectPeer() {
        Log.d(getResources().getString(R.string.app_name), "connectPeer(): starting...");
        config.deviceAddress = mDevice.deviceAddress;
        final String address = mDevice.deviceAddress;
        final String name = mDevice.deviceName;
        mManager.connect(mChannel, config, new WifiP2pManager.ActionListener() {

            @Override
            public void onSuccess() {
                //success logic
                Log.d(getResources().getString(R.string.app_name), mClass_string + "connectPeer(): Connected to Peer");
                Configurations.setController(false);
                Configurations.setControllerAddress(address);
                Configurations.setControllerName(name);
            }

            @Override
            public void onFailure(int reason) {
                //failure logic
                Log.d(getResources().getString(R.string.app_name), mClass_string + "connectPeer(): Failed to connect to Peer");
            }
        });

        gotoHub_helper();
        Log.d(getResources().getString(R.string.app_name), "connectPeer(): ended...");
    }

    @Override
    protected void gotoHub_helper() {
        Intent intent = new Intent(ConnectToHub.this, Loading.class);
        Configurations.setController(false);
        intent.putExtra(getResources().getString(R.string.extra_sender_class), mClass_string);
        intent.putExtra(getResources().getString(R.string.extra_loading_type), getResources().getString(R.string.loading_hub));
        intent.putExtra(getResources().getString(R.string.extra_loading_class), ConnectToHub.class.toString());
        startActivity(intent);
        finish();
    }

    /**
     * Refresh list
     * @param view
     */
    public void refresh(View view) {
        peerNametoAddress_map.clear();
        //peerHubs.clear();
        // Initializes the adapter
        peerDevices_arrayAdapter  =
                new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1) {

                    @Override
                    public View getView(int position, View convertView,
                                        ViewGroup parent) {
                        View view =super.getView(position, convertView, parent);

                        TextView textView=(TextView) view.findViewById(android.R.id.text1);

            /*YOUR CHOICE OF COLOR*/
                        textView.setTextColor(Color.WHITE);

                        return view;
                    }
                };

        // Connects the list view to the adapter
        peerDevices_listView.setAdapter(peerDevices_arrayAdapter);

        discoverService();
    }
}
