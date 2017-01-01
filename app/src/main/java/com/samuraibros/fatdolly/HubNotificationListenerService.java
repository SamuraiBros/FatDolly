package com.samuraibros.fatdolly;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaMetadataRetriever;
import android.media.RemoteController;
import android.os.Binder;
import android.os.IBinder;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;

public class HubNotificationListenerService extends NotificationListenerService implements RemoteController.OnClientUpdateListener {
    //Reference to remote controller
    private RemoteController mRemoteController;
    //Binder given to bound activities
    private final IBinder mBinder = new LocalBinder();

    /**
     * Class used for the client Binder.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with IPC.
     */
    public class LocalBinder extends Binder {
        HubNotificationListenerService getService() {
            // Return this instance of LocalService so clients can call public methods
            return HubNotificationListenerService.this;
        }
    }
    /**
     * Listens for various intents
     * @param
     */
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            //Gets the actions of the intent
            String action = intent.getAction();
            Log.d(getResources().getString(R.string.app_name), "HubNotoficationService: BroadcastReceiver: Action to " + action);


        }
    };

    /**
     * Initialize service
     */
    @Override
    public void onCreate() {
        super.onCreate();

        //Initialize the media controller
        mRemoteController = new RemoteController(getApplicationContext(), this);
        if(!((AudioManager)getApplicationContext().getSystemService(Context.AUDIO_SERVICE)).registerRemoteController(mRemoteController)) {
            Log.e("HubNotificationService", "Failed to register Remote Controller");
        }
    }

    /**
     * Service destructor
     */
    @Override
    public void onDestroy() {
        super.onDestroy();
        //Unregisters the media controller
        ((AudioManager) getApplicationContext().getSystemService(Context.AUDIO_SERVICE)).unregisterRemoteController(mRemoteController);
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(getResources().getString(R.string.app_name), "HubService: onBind: new binding wtih " + intent.getAction());
        return mBinder;
    }

    //****************************************************************************

    /**
     * Receives metadata update changes
     * @param metadataEditor
     */
    @Override
    public void onClientMetadataUpdate(RemoteController.MetadataEditor metadataEditor) {
        Log.d(getResources().getString(R.string.app_name), "HubNotificationService: onClientMetadataUpdate: starting...");

        //Retrieves the song title
        String song_title = metadataEditor.getString(MediaMetadataRetriever.METADATA_KEY_TITLE, "Unknown");
        //If no song title, ensures that a value is given
        if (song_title.equals("") || song_title == null) {
            song_title = "Unknown";
        }
        //Retrieves the artist
        String song_artist = metadataEditor.getString(MediaMetadataRetriever.METADATA_KEY_ALBUMARTIST, "Unknown");
        //If no artist, ensures that a value is given
        if (song_artist.equals("") || song_artist == null) {
            song_artist = "Unknown";
        }

        Intent i = new Intent("ChangeMetadata");
        i.putExtra("SongTitle", song_title);
        i.putExtra("SongArtist", song_artist);
        sendBroadcast(i);
        Log.d(getResources().getString(R.string.app_name), "HubNotificationService: onClientMetadataUpdate: Ended");
    }

    @Override
    public void onClientChange(boolean clearing) {

    }

    @Override
    public void onClientPlaybackStateUpdate(int state) {

    }

    @Override
    public void onClientPlaybackStateUpdate(int state, long stateChangeTimeMs, long currentPosMs, float speed) {

    }

    @Override
    public void onClientTransportControlUpdate(int transportControlFlags) {

    }
    //****************************************************************************

    // Methods related to notification listener that are not used here

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {

    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {

    }

    //****************************************************************************
}
