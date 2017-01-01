package com.samuraibros.fatdolly;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
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
import android.util.Log;

import java.util.HashMap;
import java.util.Map;

public abstract class BaseActivity extends AppCompatActivity {

    //Preferences ID
    public static final String PREFERENCES_ID = "AUDHUB";
    protected WifiP2pManager mManager;
    protected WifiP2pManager.Channel mChannel;
    protected BroadcastReceiver mReceiver;
    protected IntentFilter mIntentFilter;
    protected WifiP2pManager.PeerListListener mPeerListListener;
    protected WifiP2pDnsSdServiceRequest serviceRequest;

    /*HubService mService;
    boolean mBound = false;

    //Service connections
    private ServiceConnection MyHubService_Conn  = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            HubService.LocalBinder binder = (HubService.LocalBinder) service;
            mService = binder.getService();
            mBound = true;
            Log.d("AudHub", "Hub: onServiceConnected...");
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mBound = false;
            Log.d("AudHub", "Hub: onServiceDisconnected...");
        }
    };*/


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mManager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        mChannel = mManager.initialize(this, getMainLooper(), null);
        mReceiver = new WiFiDirectBroadcastReceiver(mManager, mChannel, this);

        mIntentFilter = new IntentFilter();
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

        startRegistration();
        //Service bindning to ensure that service does not continue in background after closing
        /*Intent i = new Intent(this, HubService.class);
        bindService(i, MyHubService_Conn, Context.BIND_AUTO_CREATE);*/
    }

    /* register the broadcast receiver with the intent values to be matched */
    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mReceiver, mIntentFilter);
    }
    /* unregister the broadcast receiver */
    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mReceiver);
    }

    private void startRegistration() {
        //  Create a string map containing information about your service.
        Map record = new HashMap();
        SharedPreferences mPreferences = getSharedPreferences(BaseActivity.PREFERENCES_ID, 0);
        String hubName = mPreferences.getString("HubName", "");
        record.put("HubName", hubName);

        // Service information.  Pass it an instance name, service type
        // _protocol._transportlayer , and the map containing
        // information other devices will want once they connect to this one.
        WifiP2pDnsSdServiceInfo serviceInfo =
                WifiP2pDnsSdServiceInfo.newInstance("FatDollySerVice", "_presence._tcp", record);

        // Add the local service, sending the service info, network channel,
        // and listener that will be used to indicate success or failure of
        // the request.
        mManager.addLocalService(mChannel, serviceInfo, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                // Command successful! Code isn't necessarily needed here,
                // Unless you want to update the UI or add logging statements.
                Log.d(getResources().getString(R.string.app_name), "BaseActivity: startRegistration: success");
            }

            @Override
            public void onFailure(int arg0) {
                Log.d(getResources().getString(R.string.app_name), "BaseActivity: startRegistration: failed");
            }
        });
    }

}
