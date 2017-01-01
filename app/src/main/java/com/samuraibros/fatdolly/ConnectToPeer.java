package com.samuraibros.fatdolly;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceRequest;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class ConnectToPeer extends BaseActivity {


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
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connect_to_peer);

        Log.d(getResources().getString(R.string.app_name), "ConnectToPeer: onCreate: started");

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
                    peerHubs.put(device.deviceAddress, (String) record.get(getResources().getString(R.string.record_hubName)));
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

        peerDevices_listView = (ListView) findViewById(R.id.listView_connectToPeer);

        // Sets the responses when the various device in the list view are clicked
        peerDevices_listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3) {
                // Gets the device name and mac address and parses them out of the string
                final String device_name = (String) peerDevices_listView.getItemAtPosition(position);
                final String device_address = peerNametoAddress_map.get(device_name);
                final WifiP2pDevice device = peerAddresstoDevice_map.get(device_address);
                // Creates a popup dialog to provide further choices for a response
                final AlertDialog alertDialog = new AlertDialog.Builder(ConnectToPeer.this).create(); //Read Update
                // Sets the title for the popup dialog
                alertDialog.setTitle(device_name);

                //Adds the device to the connectedDevice list and adds it to the connected state
                alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, "Connect", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        mDevice = device;
                        connectPeer();

                    }

                });

                // Cancels the popup dialogue
                alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        alertDialog.cancel();
                    }
                });

                // Displays the dialogue
                alertDialog.show();
            }
        });

        refreshPeers(null);

        Log.d(getResources().getString(R.string.app_name), "ConnectToPeer: onCreate: ended");


    }

    /**
     * Used to detect available peers that are in range
     */
    private void discoverService() {
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
    }

    /**
     * Used to connect to peer
     */
    private void connectPeer() {
        config.deviceAddress = mDevice.deviceAddress;
        mManager.connect(mChannel, config, new WifiP2pManager.ActionListener() {

            @Override
            public void onSuccess() {
                //success logic
            }

            @Override
            public void onFailure(int reason) {
                //failure logic
            }
        });
    }

    /**
     * Refresh list of peers
     * @param view
     */
    public void refreshPeers(View view) {
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
