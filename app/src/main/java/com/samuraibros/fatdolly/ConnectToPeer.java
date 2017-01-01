package com.samuraibros.fatdolly;

import android.graphics.Color;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceRequest;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class ConnectToPeer extends BaseActivity {


    private WifiP2pDevice device;
    private WifiP2pConfig config = new WifiP2pConfig();
    private ArrayAdapter peerDevices_arrayAdapter;
    private ListView peerDevices_listView;
    private Map<String, String> peerNametoAddress_map = new HashMap<>();
    private final HashMap<String, String> buddies = new HashMap<String, String>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connect_to_peer);

        mPeerListListener  = new WifiP2pManager.PeerListListener() {
            @Override
            public void onPeersAvailable(WifiP2pDeviceList peers) {
                //Clear the map list and add the new list of name adn address pairings
                peerNametoAddress_map.clear();
                peerDevices_arrayAdapter.clear();
                for (WifiP2pDevice dev : peers.getDeviceList()) {
                    if (!peerNametoAddress_map.containsKey(dev.deviceName) && buddies.containsKey(dev.deviceAddress))
                        peerNametoAddress_map.put(dev.deviceName, dev.deviceAddress);
                }
                peerDevices_arrayAdapter.addAll(peerNametoAddress_map.keySet());
            }
        };

        WifiP2pManager.DnsSdTxtRecordListener txtListener = new WifiP2pManager.DnsSdTxtRecordListener() {
            @Override
        /* Callback includes:
         * fullDomain: full domain name: e.g "printer._ipp._tcp.local."
         * record: TXT record dta as a map of key/value pairs.
         * device: The device running the advertised service.
         */

            public void onDnsSdTxtRecordAvailable(
                    String fullDomain, Map record, WifiP2pDevice device) {
                Log.d(getResources().getString(R.string.app_name), this.toString() + ": DnsSdTxtRecord available -" + record.toString());
                buddies.put(device.deviceAddress, (String) record.get("buddyname"));
                refreshPeers(null);
            }
        };

        WifiP2pManager.DnsSdServiceResponseListener servListener = new WifiP2pManager.DnsSdServiceResponseListener() {
            @Override
            public void onDnsSdServiceAvailable(String instanceName, String registrationType,
                                                WifiP2pDevice resourceType) {

                // Update the device name with the human-friendly version from
                // the DnsTxtRecord, assuming one arrived.
                resourceType.deviceName = buddies
                        .containsKey(resourceType.deviceAddress) ? buddies
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
                    }

                    @Override
                    public void onFailure(int code) {
                        // Command failed.  Check for P2P_UNSUPPORTED, ERROR, or BUSY
                    }
                });

        refreshPeers(null);


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
        config.deviceAddress = device.deviceAddress;
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
        // Gets reference to the UI list view
        peerDevices_listView = (ListView) findViewById(R.id.listView_connectToPeer);

        // Connects the list view to the adapter
        peerDevices_listView.setAdapter(peerDevices_arrayAdapter);

        discoverService();
    }


}
