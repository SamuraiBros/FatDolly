package com.samuraibros.fatdolly;

import android.app.Application;
import android.app.AlertDialog;
import android.app.Service;
import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHeadset;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.MediaMetadataRetriever;
import android.media.RemoteController;
import android.os.Binder;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.os.Vibrator;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import static java.lang.Thread.sleep;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.UUID;
import java.util.Vector;
import java.util.concurrent.Semaphore;


public class Configurations extends Application {
    //UUID for which to make connections with
    public static final UUID MY_UUID = UUID.fromString("7b7c01fa-d45a-4e27-aeec-1fbf147bc628");
    //Preferences ID
    public static final String PREFERENCES_ID = "AUDHUB";
    //User permission
    public static final String[] USER_PERMISSIONS = {"Control Playback", "Request Playback", "Control Volume", "Control Discoverability", "Control Users", "Control Devices"};
    //Holds choices for responding to a playback request
    public static final String[] REQUEST_PLAYBACK_CHOICES = {"Continue the Current Song", "Skip the Current Song", "Play the Previous Song"};
    //Holds choices for making a playback request
    public static final String[] REQUEST_PLAYBACK_OPTIONS = {REQUEST_PLAYBACK_CHOICES[1], REQUEST_PLAYBACK_CHOICES[2]};
    //Maps user names to addresses
    private static Map<String, String> userNameAddressMaps = new HashMap<String, String>();
    //Maps user addresses to names
    private static Map<String, String> userAddressNameMap = new HashMap<String, String>();
    //Maps device addresses to sockets
    private static Map<String, BluetoothSocket> deviceAddressSocketMap = new HashMap<String, BluetoothSocket>();
    //Maps device addresses to the device
    private static Map<String, BluetoothDevice> deviceAddressDeviceMap = new HashMap<String, BluetoothDevice>();
    //Holds list of confirmed and connected user addresses
    private static Set<String> userAddressSet = new HashSet<String>();
    //Holds list of user addresses pending confirmed connection
    private static Set<String> userAddressPendingSet = new HashSet<String>();
    //Holds list of device addresses
    private static Set<String> deviceAddressSet = new HashSet<String>();
    //Maps device names to addresses
    private static Map<String, String> deviceNameAddressMap = new HashMap<>();
    //Maps device addresses to names
    private static Map<String, String> deviceAddressNameMap = new HashMap<>();
    //Mutex lock for user data
    private static Semaphore userDataLock = new Semaphore(1);
    //Mutex lock for device data
    private static Semaphore deviceDataLock = new Semaphore(1);
    //Maps user addresses to permissions
    private static Map<String, ArrayList<String>> userAddressPermissionMap = new HashMap<String, ArrayList<String>>();
    //Holds the controller name
    private static String controllerName = "";
    //Maps devices addreses to their connection status
    private static Map<String, Boolean> deviceAddressConnectedMap = new HashMap<String, Boolean>();
    //Reference to default bluetooth adapter
    private static BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    //Determines whether thread is still running
    private static boolean running = false;
    //Reference to global audio manager
    public static AudioManager mAudioManager = null;
    //Temporary value holding system volume
    private static int volume_level = 0;
    //Holds song metadata
    private static String song_artist;
    private static String song_title;
    //Reference to own mac address
    private static String mAddress = "00:15:83:CA:A1:E2";
    //Reference to device mac address
    private static String controllerAddress = null;
    //List of notifications
    private static Vector<String> notificationsVector = new Vector<>();
    //Determines whether there are unseen notifications
    private static boolean newNotification = false;
    //Mutex for newNotification
    private static Semaphore notificationsDataLock = new Semaphore(1);
    //Reference to Vibrator
    public static Vibrator mVibrator;
    //Determines type of hub
    private static boolean hostHub = false;
    //Holds socket connecting to hub in control
    private static BluetoothSocket controllerSocket = null;
    //Notifications animation
    public static final Animation notificationsAnimation = new AlphaAnimation((float)1.0, (float).5); // Change alpha from fully visible to invisible;
    //Determines whether the service is fully initialized
    private static boolean started = false;
    //Holds the set of playback requests
    private static Set<String> requestStructureSet = new HashSet<>();
    //Maps request structures to the response list
    private static Map<String, ArrayList<String>> requestsStructureResponsesMap = new HashMap<>();
    //Maps request structure to the responder list
    private static Map<String, ArrayList<String>> requestsStructureRespondersMap = new HashMap<>();
    //Mutex lock for request data
    private static Semaphore requestsDataLock = new Semaphore(1);
    //Maps request index in the PlaybackRequests activity list to the structure
    private static Map<Integer, String> requestsIndexStructureMap = new HashMap<>();
    //Maps request structures to their cound down timers
    private static Map<String, RequestsCountDownTimer> requestsStructureTimerMap = new HashMap<>();
    //Determines whether the thread is running
    private boolean ConnectA2dpThreadRunning = false;
    //Determines whether the thread is running
    private boolean QueryA2dpThreadRunning = false;
    //Stack to keep track of activities
    private static Stack<String> activitiesStack = new Stack<>();
    //Reference to class string
    private final String mClass = Configurations.class.toString();


    /**
     * Extends the CountDownTimer class to allow better interface with structures and AlertDialogs
     */
    public class RequestsCountDownTimer extends CountDownTimer {
        //String representation of the time remaining
        private String time_string = " - 0:00";
        //Reference to the currently showing dialog
        private AlertDialog mDialog = null;
        //Reference to the dialog title to be connected to
        private String mDialog_title = "";
        //Reference to the structure this timer belongs to
        private final String structure;
        //Reference to the request type that created the dialog
        private String requestType;
        //Amount of time remaining on timer
        private long time;

        /**
         * Initializes the class
         */
        public RequestsCountDownTimer(long duration, String struct) {
            //Calls the parent constructor
            super(duration, 10);
            //Sets local data
            structure = struct;
            time = duration;
            time_string = " - " + duration + ":00";
        }

        /**
         * Called when starting a AlertDialog that uses the structure this timer belongs to
         */
        public void setDialog(AlertDialog dialog, String title, String type) {
            //Sets local data
            mDialog_title = title;
            dialog.setTitle(mDialog_title + time_string);
            mDialog = dialog;
            requestType = type;

            //If timeout, shows the clear button
            if (time == 0) {
                showClear();
            }
        }

        /**
         * Called each time the timer decrements by the determined amount
         * @param millisUntilFinished
         */
        @Override
        public void onTick(long millisUntilFinished) {
            //Updates local data
            running = true;
            time = millisUntilFinished;
            time = millisUntilFinished;
            long secs = (millisUntilFinished/1000);
            long msecs = millisUntilFinished/10 - (secs * 100);
            time_string = " - " + secs + ":" + msecs;

            //Sets the dialog title if there is a connected dialog
            if (mDialog != null) {
                mDialog.setTitle(mDialog_title + time_string);
            }
        }

        /**
         * Actions to perform at timeout
         */
        @Override
        public void onFinish() {
            //Updates local data
            running = false;
            time = 0;
            time_string = " - 0:00";

            //Updates dialog title and adds the Clear button if there is a connected dialog
            if (mDialog != null && mDialog.isShowing()) {
                mDialog.setTitle(mDialog_title + time_string);
                showClear();
            }

            //Cancels a dialog if it was made during a playback request
            if (requestType != null && requestType.equals(getResources().getString(R.string.intent_create_playback_request))) {
                mDialog.cancel();
            }

            mDialog = null;

            //Sends the structure to fulfill the playback request
            Intent i = new Intent("PlaybackRequestTimeout");
            i.putExtra(getResources().getString(R.string.extra_structure), structure);
            sendBroadcast(i);
        }

        /**
         * Adds the clear button to the dialog after timeout
         */
        private void showClear() {
            if (mDialog != null) {
                mDialog.setTitle(mDialog_title + time_string);
                mDialog.setButton(AlertDialog.BUTTON_NEGATIVE, "Clear", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //Removes the request from the set of request data holders
                        removePlaybackRequest(structure);
                        //Refreshes the PlaybackRequest activity request list
                        Intent intent = new Intent("UpdatePlaybackRequestList");
                        sendBroadcast(intent);
                    }
                });

                //Refreshes the PlaybackRequest activity request list
                Intent intent = new Intent("UpdatePlaybackRequestList");
                sendBroadcast(intent);
            }
        }

        /**
         * Returns the about of time remaining on the timer
         * @return
         */
        public long getTime() {
            return time;
        }
    }

    // Manage Notifications
    /**
     * Removes a message from the queue
     * @param message
     */
    public static void dequeueNotification(String message) {
        Log.d("AudHub", "HubService: dequeueNotification: starting...");
        //Removes selects message
        if (message != null && !message.equals("")) {
            try {
                notificationsDataLock.acquire();
                Log.d("AudHub", "HubService: dequeueNotification: Acquired notification lock");
                newNotification = false;
                notificationsVector.remove(message);
                Log.d("AudHub", "HubService: dequeueNotification: Removed " + message + " from queue");
                notificationsDataLock.release();
                Log.d("AudHub", "HubService: dequeueNotification: Released notification lock");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        //Removes all messages that don't require user response
        else {
            try {
                notificationsDataLock.acquire();
                Log.d("AudHub", "HubService: dequeueNotification: Acquired notification lock");
                Vector<String> messages = (Vector<String>) notificationsVector.clone();
                for (String mess : messages) {
                    if (!mess.contains("is ")) {
                        Log.d("AudHub", "HubService: dequeueNotification: Removed " + mess + " from queue");
                        notificationsVector.remove(mess);
                    }
                }
                notificationsDataLock.release();
                Log.d("AudHub", "HubService: dequeueNotification: Released notification lock");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        Log.d("AudHub", "HubService: dequeueNotification: ended");
    }

    // Small utilities
    /**
     * Converts a byte array into a string representation
     * @param b
     */
    public static String byteArrayToString(byte[] b)
    {
        return   new String(b);
    }

    /**
     * Converts an string to a byte array representation
     * @param s
     */
    public static byte[] stringToByteArray(String s)
    {
        return s.getBytes();
    }

    // Manage Requests


    /**
     * Returns the creator address of a request
     * @param structure
     * @return
     */
    public static String requestsStructureToAddress(String structure) {
        String[] tokens = structure.split("-");
        String address = tokens[0];
        return address;
    }

    /**
     * Returns the request type of a request
     * @param structure
     * @return
     */
    public static String requestsStructureToType(String structure) {
        String[] tokens = structure.split("-");
        String requestType = tokens[1];
        return requestType;
    }

    /**
     * Returns the request made
     * @param structure
     * @return
     */
    public static String requestsStructureToRequest(String structure) {
        String[] tokens = structure.split("-");
        String request = tokens[2];
        return request;
    }

    /**
     * Returns the selected duration of the request
     * @param structure
     * @return
     */
    public static int requestsStructureToDuration(String structure) {
        String[] tokens = structure.split("-");
        int duration = Integer.parseInt(tokens[3].split(" ")[0]) * 1000;
        return duration;
    }

    /**
     * Returns when the request was started
     * @param structure
     * @return
     */
    public static int requestsStructureToStart(String structure) {
        String[] tokens = structure.split("-");
        int start_time = Integer.parseInt(tokens[4]);
        return start_time;
    }

    // Accessors


    /**
     * Returns the name of the user from the address
     * @param address
     * @return
     */
    public static String userAddressToName(String address) {
        String temp = "";
        try {
            userDataLock.acquire();
            temp = userAddressNameMap.get(address);
            userDataLock.release();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return temp;
    }

    /**
     * Returns the address of the user from the name
     * @param name
     * @return
     */
    public static String userNameAddressMapToAddress(String name) {
        String temp = "";
        try {
            userDataLock.acquire();
            temp = userNameAddressMaps.get(name);
            userDataLock.release();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return temp;
    }

    /**
     * Returns the address of the device from the name
     * @param name of the device
     * @return
     */
    public static String deviceNameToAddress(String name) {
        String temp = "";
        try {
            deviceDataLock.acquire();
            temp = deviceNameAddressMap.get(name);
            deviceDataLock.release();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return temp;
    }

    /**
     * Returns the name of the device from the address
     * @param address
     * @return
     */
    public static String deviceAddressToName(String address) {
        String temp = "";
        try {
            deviceDataLock.acquire();
            temp = deviceAddressNameMap.get(address);
            deviceDataLock.release();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return temp;
    }


    /**
     * Returns the socket of the connection from the device
     * @param address
     * @return
     */
    public static BluetoothSocket deviceAddressToSocket(String address) {
        BluetoothSocket temp = null;
        try {
            deviceDataLock.acquire();
            temp = deviceAddressSocketMap.get(address);
            deviceDataLock.release();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return temp;
    }

    /**
     * Returns the device from the address
     * @param address
     * @return
     */
    public static BluetoothDevice deviceAddressToDevice(String address) {
        BluetoothDevice temp = null;
        try {
            deviceDataLock.acquire();
            temp = deviceAddressDeviceMap.get(address);
            deviceDataLock.release();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return temp;
    }

    /**
     * Returns the set of user addresses
     * @return
     */
    public static Set<String> getUserAddresses() {
        Set<String> temp = null;
        try {
            userDataLock.acquire();
            temp = userAddressSet;
            userDataLock.release();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return temp;
    }

    /**
     * Returns the set of device addresses
     * @return
     */
    public static Set<String> getDeviceAddresses() {
        Set<String> temp = null;
        try {
            deviceDataLock.acquire();
            temp = deviceAddressSet;
            deviceDataLock.release();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return temp;
    }

    /**
     * Returns the set of requests
     * @return
     */
    public static Set<String> getRequests() {
        Set<String> temp = null;
        try {
            requestsDataLock.acquire();
            temp = requestStructureSet;
            requestsDataLock.release();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return temp;
    }

    /**
     * Returns the request structure from its index in the requests list in PlaybackRequests activity
     * @param index
     * @return
     */
    public static String requestIndexToStructure(int index) {
        String temp = null;
        try {
            requestsDataLock.acquire();
            temp = requestsIndexStructureMap.get(index);
            requestsDataLock.release();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return temp;
    }

    /**
     * Returns the timer managing the request structure
     * @param structure
     * @return
     */
    public static RequestsCountDownTimer requestStructureToTimer(String structure) {
        RequestsCountDownTimer temp = null;
        try {
            requestsDataLock.acquire();
            temp = requestsStructureTimerMap.get(structure);
            requestsDataLock.release();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return temp;
    }

    /**
     * Returns the list of responders from the structure
     * @param structure
     * @return
     */
    public static ArrayList<String> requestStructureToResponders(String structure) {
        ArrayList<String> temp = null;
        try {
            requestsDataLock.acquire();
            temp = requestsStructureRespondersMap.get(structure);
            requestsDataLock.release();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return temp;
    }

    /**
     * Returns the list of responses from the structure
     * @param structure
     * @return
     */
    public static ArrayList<String> requestStructureToResponse(String structure) {
        ArrayList<String> temp = null;
        try {
            requestsDataLock.acquire();
            temp = requestsStructureResponsesMap.get(structure);
            requestsDataLock.release();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return temp;
    }

    /**
     * Returns the device name maps
     * @return
     */
    public static Map<String, String> getDeviceNames() {
        Map<String, String> temp = null;
        try {
            deviceDataLock.acquire();
            temp = deviceNameAddressMap;
            deviceDataLock.release();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return temp;
    }

    /**
     * Returns the list of the device hubs
     * @return
     */
    public static Map<String, String> getDeviceHubs() {
        Map<String, String> temp = null;
        try {
            deviceDataLock.acquire();
            temp = deviceAddressNameMap;
            deviceDataLock.release();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return temp;
    }

    /**
     * Returns the device connection status
     * @return
     */
    public static Map<String, Boolean> getDeviceAdded() {
        Map<String, Boolean> temp = null;
        try {
            deviceDataLock.acquire();
            temp = deviceAddressConnectedMap;
            deviceDataLock.release();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return temp;
    }


    /**
     * Returns the hub name
     * @return
     */
    public static String getHubName(Context c) {
        String temp = "";
        try {
            userDataLock.acquire();
            SharedPreferences mPreferences = c.getSharedPreferences(BaseActivity.PREFERENCES_ID, 0);
            temp = mPreferences.getString(c.getResources().getString(R.string.preference_hubName), "");
            userDataLock.release();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return temp;
    }

    /**
     * Returns the user permissions from the address
     * @param address
     * @return
     */
    public static ArrayList<String> userAddressToPermissions(String address) {
        ArrayList<String> temp = null;
        try {
            userDataLock.acquire();
            temp = userAddressPermissionMap.get(address);
            userDataLock.release();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return temp;
    }

    /**
     * Returns the device connection from the address
     * @param address
     * @return
     */
    public static boolean deviceAddressToAdded(String address) {
        boolean temp = false;
        try {
            deviceDataLock.acquire();
            if (deviceAddressConnectedMap.get(address) != null)
                temp = deviceAddressConnectedMap.get(address);
            deviceDataLock.release();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return temp;
    }

    /**
     * Returns whether the service is running
     * @return
     */
    public static boolean isRunning() {
        return running;
    }

    /**
     * Returns whether the service is officially started
     * @return
     */
    public static boolean isStarted() {
        return started;
    }

    /**
     * Returns the current volume
     * @return
     */
    public static int getVolumeLevel() {
        int temp = 0;
        try {
            userDataLock.acquire();
            temp = volume_level;
            userDataLock.release();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return temp;
    }

    /**
     * Returns the current metadata
     * @return
     */
    public static String[] getMetadata() {
        String[] temp = new String[2];
        try {
            userDataLock.acquire();
            temp[0] = song_artist;
            temp[1] = song_title;
            userDataLock.release();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return temp;
    }

    /**
     * Returns whether the service is the controller
     * @return
     */
    public static boolean isController() {
        boolean temp = false;
        try {
            userDataLock.acquire();
            temp = hostHub;
            userDataLock.release();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return temp;
    }

    /**
     * Returns the device address
     * @return
     */
    public static String getHubAddress() {
        String temp = "";
        try {
            userDataLock.acquire();
            temp = mAddress;
            userDataLock.release();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return temp;
    }

    /**
     * Returns the list of notifications
     * @return
     */
    public static Vector<String> getNotifications() {
        Vector<String> temp = null;
        try {
            notificationsDataLock.acquire();
            temp = notificationsVector;
            notificationsDataLock.release();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return temp;
    }

    /**
     * Returns whether there is a new connection
     * @return
     */
    public static boolean hasNewNotification() {
        boolean temp = false;
        try {
            notificationsDataLock.acquire();
            temp = newNotification;
            notificationsDataLock.release();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return temp;
    }

    /**
     * Returns the socket to the controller hub
     * @return
     */
    public static BluetoothSocket getControllerSocket() {
        BluetoothSocket temp = null;
        try {
            userDataLock.acquire();
            temp = controllerSocket;
            userDataLock.release();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return temp;
    }


    /**
     * Returns the number of connected users
     * @return
     */
    public static int getNumberConfirmedUsers() {
        int temp = 0;
        try {
            userDataLock.acquire();
            temp = userAddressSet.size();
            userDataLock.release();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return temp;
    }

    /**
     * Returns the number of connected users
     * @return
     */
    public static int getNumberPendingUsers() {
        int temp = 0;
        try {
            userDataLock.acquire();
            temp = userAddressPendingSet.size();
            userDataLock.release();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return temp;
    }

    /**
     * Returns whether the given address is in the address list
     * @param address
     * @return
     */
    public static boolean userAddressSetContains(String address) {
        boolean temp = false;
        try {
            userDataLock.acquire();
            temp = userAddressSet.contains(address);
            userDataLock.release();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return temp;
    }

    /**
     * Returns whether the device address is in the device list
     * @param address
     * @return
     */
    public static boolean deviceAddressSetContains(String address) {
        boolean temp = false;
        try {
            deviceDataLock.acquire();
            temp = deviceAddressSet.contains(address);
            deviceDataLock.release();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return temp;
    }

    /**
     * Returns the name of the controller
     * @return
     */
    public static String getControllerName() {
        String temp = "";
        try {
            userDataLock.acquire();
            temp = controllerName;
            userDataLock.release();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return temp;
    }

    /**
     * Returns the address the controller hub
     * @return
     */
    public static String getControllerAddress() {
        String temp = "";
        try {
            userDataLock.acquire();
            if (controllerSocket != null)
                temp = controllerSocket.getRemoteDevice().getAddress();
            userDataLock.release();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return temp;
    }

    //Mutators

    /**
     * Sets the hub name
     * @param hubName
     */
    public static void setHubName(Context c, String hubName) {
        try {
            userDataLock.acquire();
            SharedPreferences mPreferences = c.getSharedPreferences(BaseActivity.PREFERENCES_ID, 0);
            SharedPreferences.Editor editor = mPreferences.edit();
            editor.putString(c.getResources().getString(R.string.preference_hubName), hubName);
            editor.commit();
            userDataLock.release();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Sets the index of the current structure
     * @param index
     * @param structure
     */
    public static void setRequestStructureIndex(int index, String structure) {
        try {
            requestsDataLock.acquire();
            requestsIndexStructureMap.put(index, structure);
            requestsDataLock.release();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Gets system volume update
     */
    public static void updateVolumeFromSystem() {
        if (isController())
            volume_level = Configurations.mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
    }

    /**
     * Sets the volumen level
     * @param level
     * @param internal
     */
    private void setVolumeLevel(int level, boolean internal) {
        /*Log.d(getResources().getString(R.string.app_name), "HubService: setVolumeLevel: starting....");
        try {
            userDataLock.acquire();
            Log.d(getResources().getString(R.string.app_name), "HubService: setVolumeLevel: Acquired user lock");
            volume_level = level;
            if (!internal && !hostHub) {
                Intent i = new Intent("UpdateHubVolume");
                sendBroadcast(i);
            }
            else if (internal && !hostHub) {
                for (String addr : userAddressSet) {
                    if (!addr.equals(mAddress))
                        sendRemoteMessage(addr,"VolumeChange," + Integer.toString(volume_level) + ';', false);
                }
            }
            else if (!internal && hostHub){
                Intent intent = new Intent("UpdateHubVolume");
                sendBroadcast(intent);
                Log.d(getResources().getString(R.string.app_name), "HubService: setVolumeLevel: Setting Volume level to: " + volume_level);
                mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, volume_level, 0);
            }
            else if (internal && hostHub) {
                Log.d(getResources().getString(R.string.app_name), "HubService: setVolumeLevel: Setting Volume level to: " + volume_level);
                mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, volume_level, 0);
                for (String addr : userAddressSet) {
                    if (!addr.equals(mAddress))
                        sendRemoteMessage(addr, "VolumeChange," + Integer.toString(volume_level) + ';', false);
                }
            }
            userDataLock.release();
            Log.d(getResources().getString(R.string.app_name), "HubService: setVolumeLevel: Released user lock");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        Log.d(getResources().getString(R.string.app_name), "HubService: setVolumeLevel: Ended");*/
    }

    /**
     * Resets the new notification
     */
    public static void resetNewNotification() {
        try {
            notificationsDataLock.acquire();
            newNotification = false;
            notificationsDataLock.release();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Sets the metadata for the current music
     * @param artist
     * @param title
     * @param internal
     */
    private void setMetadata(String artist, String title, boolean internal, boolean thread_safe) {
        if (thread_safe) {
            try {
                userDataLock.acquire();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        song_title = title;
        song_artist = artist;
        if(!internal) {
            Intent i = new Intent("UpdateHubMetadata");
            sendBroadcast(i);
        }
        if (thread_safe)
            userDataLock.release();

    }

    /**
     * Resets the metadata for the current music
     */
    private void resetMetadata(boolean thread_safe) {
        if (thread_safe) {
            try {
                userDataLock.acquire();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        song_title = "Unknown";
        song_artist = "Unknown";
        Intent i = new Intent("UpdateHubMetadata");
        sendBroadcast(i);
        if (thread_safe)
            userDataLock.release();
    }

    /**
     * Sets the permissions for the current user
     * @param address
     * @param permissions
     * @param type
     * @param thread_safe
     */
    private void setUserPermissions(String address, ArrayList<String> permissions, String type, boolean thread_safe) {
        /*Log.d(getResources().getString(R.string.app_name), "HubService: setUserPermissions: starting...");
        if (thread_safe) {
            try {
                userDataLock.acquire();
                Log.d(getResources().getString(R.string.app_name), "HubService: setUserPermissions: Acquired user lock");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        ArrayList<String> old_permissions = userAddressPermissionMap.get(address);
        if (old_permissions == null) {
            old_permissions = new ArrayList<String>();
        }
        Log.d(getResources().getString(R.string.app_name), "HubService: setUserPermissions: Updating permissions for " + address + " with type " + type + " to " + TextUtils.join(", ", permissions));
        userAddressPermissionMap.put(address, permissions);
        if (hostHub && type.equals("Update")) {
            sendUpdatedUserData("GLOBAL", "Update", false);
        }
        else if (!hostHub && type.equals("Update") && controllerAddress.equals(address)){
            for (String permission : old_permissions) {
                if (!permissions.contains(permission)) {
                    queueNotification("You can no longer " + permission.toLowerCase());
                }
            }
            for (String permission : permissions) {
                if (!old_permissions.contains(permission)) {
                    queueNotification("You can now " + permission.toLowerCase());
                }
            }
            broadcastNotification(null);
            Intent i = new Intent("UserPermissionsUpdated");
            sendBroadcast(i);
        }
        if (thread_safe) {
            userDataLock.release();
            Log.d(getResources().getString(R.string.app_name), "HubService: setUserPermissions: Released user lock");
        }

        Log.d(getResources().getString(R.string.app_name), "HubService: setUserPermissions: Ended");*/
    }

    /**
     * Removes the playback request from the list
     * @param structure
     */
    public void removePlaybackRequest(String structure) {
        try {
            requestsDataLock.acquire();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        requestStructureSet.remove(structure);
        requestsStructureTimerMap.remove(structure);
        requestsStructureRespondersMap.remove(structure);
        requestsStructureResponsesMap.remove(structure);

        requestsDataLock.release();

        Intent i = new Intent(getResources().getString(R.string.intent_refreshPlaybackRequests));
    }

    /**
     * Removes all the playback requests
     */
    public static void removePlaybackRequests() {
        try {
            requestsDataLock.acquire();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        for (String structure : requestStructureSet) {
            long time = requestsStructureTimerMap.get(structure).time;
            if (time == 0) {
                requestsStructureTimerMap.remove(structure);
                requestsStructureRespondersMap.remove(structure);
                requestsStructureResponsesMap.remove(structure);
            }
        }
        requestStructureSet.clear();

        requestsDataLock.release();
    }

    /**
     * Adds activity to the stack
     * @param activity
     */
    public static void addActivityToStack(String activity) {
        /*if (activity.equals(Loading.class.toString())) {
            activitiesStack.clear();
            activitiesStack.add(Home.class.toString());
        }
        else {
            activitiesStack.add(activity);
        }*/
    }

    /**
     * Returns the previous activity from the stack
     * @return
     */
    public static String getPreviousActivity() {
        if (activitiesStack.isEmpty())
            return Home.class.toString();

        return activitiesStack.pop();
    }
    /**
     * Sets the controller hub
     * @param val
     */
    public static void setController(boolean val) {
        hostHub = val;
    }

    /**
     * Sets the device information
     * @param address
     * @param name
     * @param device
     * @param connect
     * @param type
     * @param thread_safe
     * @return
     */
    private boolean setDeviceInformation(String address, String name, BluetoothDevice device, boolean connect, String type, boolean thread_safe) {
        Log.d(getResources().getString(R.string.app_name), "HubService: setDeviceInformation: starting...");
        boolean new_device = false;
        /*
        if (address == null) {
            Log.d(getResources().getString(R.string.app_name), "HubService: setDeviceInformation: address is null");
            Log.d(getResources().getString(R.string.app_name), "HubService: setDeviceInformation: Ended");
            return new_device;
        }


        if (thread_safe) {
            try {
                deviceDataLock.acquire();
                Log.d(getResources().getString(R.string.app_name), "HubService: setDeviceInformation: Acquired device lock");
            } catch (InterruptedException e) {
                e.printStackTrace();
                return new_device;
            }
        }

        if (connect && type.contains("Connect")) {
            Log.d(getResources().getString(R.string.app_name), "HubService: setDeviceInformation: Part 1");
            for (String addr : deviceAddressSet) {
                if (!address.equals(addr)) {
                    Log.d(getResources().getString(R.string.app_name), "HubService: setDeviceInformation: Part 1.1");
                    Log.d(getResources().getString(R.string.app_name), "HubService: setDeviceInformation: Adding " + addr);
                    //deviceAddressConnectedMap.put(address, false);
                    deviceAddressConnectedMap.put(addr, false);
                }
            }
            if (hostHub) {
                Log.d(getResources().getString(R.string.app_name), "HubService: setDeviceInformation: Part 1.2");
                Thread connectedDevice_thread = new ConnectA2dpThread(device, name, "Connect");
                connectedDevice_thread.start();
            }
        } else if (type.contains("Connect")) {
            Log.d(getResources().getString(R.string.app_name), "HubService: setDeviceInformation: Part 2");
            if (hostHub) {
                Log.d(getResources().getString(R.string.app_name), "HubService: setDeviceInformation: Part 2.1");
                Thread connectedDevice_thread = new ConnectA2dpThread(device, name, "Disconnect");
                connectedDevice_thread.start();
            }
        }


        if (!deviceAddressSet.contains(address)) {
            Log.d(getResources().getString(R.string.app_name), "HubService: setDeviceInformation: Part 3");
            Log.d(getResources().getString(R.string.app_name), "HubService: setDeviceInformation: Creating device data for " + address);
            deviceAddressSet.add(address);
            deviceNameAddressMap.put(name, address);
            deviceAddressNameMap.put(address, name);
            deviceAddressDeviceMap.put(address, device);
            deviceAddressConnectedMap.put(address, connect);
            new_device = true;
        }
        else {
            Log.d(getResources().getString(R.string.app_name), "HubService: setDeviceInformation: Part 4");
            Log.d(getResources().getString(R.string.app_name), "HubService: setDeviceInformation: Updating device data for " + address);
            deviceAddressConnectedMap.put(address, connect);
        }

        if (hostHub) {
            Log.d(getResources().getString(R.string.app_name), "HubService: setDeviceInformation: Part 5");
            sendUpdatedDeviceData(false);
        }

        //Updates the connected devices list
        Intent i = new Intent("RefreshConnectedDevices");
        sendBroadcast(i);

        if (thread_safe) {
            deviceDataLock.release();
            Log.d(getResources().getString(R.string.app_name), "HubService: setDeviceInformation: Released device lock");
        }

        Log.d(getResources().getString(R.string.app_name), "HubService: setDeviceInformation: Ended");*/
        return new_device;
    }


    /**
     * Sets the user infromation
     * @param address
     * @param name
     * @param socket
     * @param permissions
     * @param type
     * @param thread_safe
     * @return
     */
    private boolean setUserInformation(String address, String name, BluetoothSocket socket, ArrayList<String> permissions, String type, boolean thread_safe) {
        Log.d(getResources().getString(R.string.app_name), "HubService: setUserInformation: starting...");
        boolean new_user = false;
        /*if (thread_safe) {
            try {
                userDataLock.acquire();
                Log.d(getResources().getString(R.string.app_name), "HubService: setUserInformation: Acquired user lock");
            } catch (InterruptedException e) {
                e.printStackTrace();
                return new_user;
            }
        }

        if (permissions == null) {
            Log.d(getResources().getString(R.string.app_name), "HubService: setUserInformation: Permissions are null");
            permissions = new ArrayList<String>();
        }
        if (type.equals(getResources().getString(R.string.value_usertouser))) {
            Log.d(getResources().getString(R.string.app_name), "HubService: setUserInformation: Part 1");
            // Checks to make sure hub is not already connected to user
            if (socket == null) {
                Log.d(getResources().getString(R.string.app_name), "HubService: setUserInformation: Part 1.1");
                //Connects to user
                Thread connection = new ConnectUserToUserThread(mBluetoothAdapter.getRemoteDevice(address), getApplicationContext());
                connection.start();
            }
            else {
                Log.d(getResources().getString(R.string.app_name), "HubService: setUserInformation: Part 1.2");
                Log.d(getResources().getString(R.string.app_name), "HubService: setUserInformation: UserToUser data Update for: " + address);
                userAddressSet.add(address);
                userNameAddressMaps.put(name, address);
                userAddressNameMap.put(address, name);
                userAddressSocketMap.put(address, socket);
                setUserPermissions(address, permissions, getResources().getString(R.string.value_connection), false);
                userAddressThreadMap.put(address, new ConnectionListenerThread(socket, getResources().getString(R.string.value_asuserfromuser)));
                userAddressThreadMap.get(address).start();

                Intent i;


                //Send user confirmation
                Intent intent = new Intent(getResources().getString(R.string.intent_sendremotemessage));
                intent.putExtra(getResources().getString(R.string.extra_device_address), address);
                intent.putExtra(getResources().getString(R.string.extra_message), getResources().getString(R.string.value_user_confirmation) + "," + mAddress + "," + getResources().getString(R.string.value_querry) + ";");
                sendBroadcast(intent);

                //Update user connection list
                i = new Intent(getResources().getString(R.string.intent_refreshconnectedusers));
                sendBroadcast(i);
            }
        }
        else if (type.equals(getResources().getString(R.string.value_acceptor))) {
            Log.d(getResources().getString(R.string.app_name), "HubService: setUserInformation: Part 2");
            //Send remote user its user number
            Log.d(getResources().getString(R.string.app_name), "HubService: setUserInformation: Acceptor Update For: " + address);
            try {
                socket.getOutputStream().write(Utilities.stringToByteArray(getResources().getString(R.string.request_volume_change) + "," + Integer.toString(volume_level) + ';'));
                socket.getOutputStream().write(Utilities.stringToByteArray(getResources().getString(R.string.request_metadata_change) + "," + song_title + "," + song_artist + ";"));
                Log.d(getResources().getString(R.string.app_name), "HubService: setUserInformation: Sending " + address + " Volume and Metadata infromation");
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (hostHub) {
                Log.d(getResources().getString(R.string.app_name), "HubService: setUserInformation: Part 2.1");
                userAddressPendingSet.add(address);
                Log.d(getResources().getString(R.string.app_name), "HubService: setUserInformation: Added: " + address + " to pending user set");
            }
            else {
                Log.d(getResources().getString(R.string.app_name), "HubService: setUserInformation: Part 2.2");
                userAddressSet.add(address);
                Log.d(getResources().getString(R.string.app_name), "HubService: setUserInformation: Added: " + address + " to active user set");
            }

            userNameAddressMaps.put(name, address);
            userAddressNameMap.put(address, name);
            setUserPermissions(address, permissions, getResources().getString(R.string.value_connection), false);
            userAddressSocketMap.put(address, socket);
            //Start thread to manage user connection
            if (hostHub) {
                Log.d(getResources().getString(R.string.app_name), "HubService: setUserInformation: Part 2.3");
                userAddressThreadMap.put(address, new ConnectionListenerThread(socket, getResources().getString(R.string.value_ascontrollerfromuser)));
            }
            else {
                Log.d(getResources().getString(R.string.app_name), "HubService: setUserInformation: Part 2.4");
                userAddressThreadMap.put(address, new ConnectionListenerThread(socket, getResources().getString(R.string.value_asuserfromuser)));
                Intent i = new Intent("UserConfirmation");
                sendBroadcast(i);
            }

            userAddressThreadMap.get(address).start();
        }
        else  if (type.equals(getResources().getString(R.string.value_requester))) {
            Log.d(getResources().getString(R.string.app_name), "HubService: setUserInformation: Part 3");
            Log.d(getResources().getString(R.string.app_name), "HubService: setUserInformation: Requestor update for: " + address);
            //userAddressSet.add(address);
            userAddressPendingSet.add(address);
            userNameAddressMaps.put(name, address);
            userAddressNameMap.put(address, name);
            setUserPermissions(address, permissions, getResources().getString(R.string.value_connection), false);
            userAddressThreadMap.put(address, new ConnectionListenerThread(controllerSocket, getResources().getString(R.string.value_asuserfromcontroller)));
            userAddressThreadMap.get(address).start();
            userAddressSocketMap.put(address, controllerSocket);
            controllerThread = userAddressThreadMap.get(address);
            controllerAddress = address;

            //Update user connection list
            Intent i;

            i = new Intent(getResources().getString(R.string.intent_initialuserinformationset));
            sendBroadcast(i);
        }
        else  if (type.equals(getResources().getString(R.string.value_exists))) {
            Log.d(getResources().getString(R.string.app_name), "HubService: setUserInformation: Part 4");
            Log.d(getResources().getString(R.string.app_name), "HubService: setUserInformation: Existing update for: " + address);
            userNameAddressMaps.put(name, address);
            userAddressNameMap.put(address, name);
            setUserPermissions(address, permissions, getResources().getString(R.string.value_update), false);
        }

        if (thread_safe) {
            userDataLock.release();
            Log.d(getResources().getString(R.string.app_name), "HubService: setUserInformation: Released user lock");
        }

        Log.d(getResources().getString(R.string.app_name), "HubService: setUserInformation: Ended");*/
        return new_user;
    }

    // CleanUp

    /**
     * Clears the device information
     * @param address
     * @param name
     * @param thread_safe
     */
    private void clearDeviceInformation(String address, String name, boolean thread_safe) {
        /*Log.d(getResources().getString(R.string.app_name), "HubService: clearDeviceInformation: starting...");
        if (thread_safe) {
            try {
                deviceDataLock.acquire();
                Log.d(getResources().getString(R.string.app_name), "HubService: clearDeviceInformation: Acquired device lock");
            } catch (InterruptedException e) {
                e.printStackTrace();
                return;
            }
        }

        if (address != null && !deviceAddressSet.contains(address)) {
            Log.d(getResources().getString(R.string.app_name), "HubService: clearDeviceInformation: Part 1");
            Log.d(getResources().getString(R.string.app_name), "HubService: clearDeviceInformation: Removed " + address + " from device set");
            deviceAddressSet.remove(address);
            //deviceNameAddressMap.remove(name);
            //deviceAddressNameMap.remove(address);
            deviceAddressDeviceMap.remove(address);
            //deviceAddressConnectedMap.remove(address);
            sendUpdated_deviceDisconnection(address, false);
            // Update the device connection list
            Intent i = new Intent("RefreshConnectedDevices");
            sendBroadcast(i);
        }
        else {
            Log.d(getResources().getString(R.string.app_name), "HubService: clearDeviceInformation: Part 2");
            Log.d(getResources().getString(R.string.app_name), "HubService: clearDeviceInformation: cleared all device data");
            deviceAddressSet.clear();
            deviceNameAddressMap.clear();
            deviceAddressNameMap.clear();
            deviceAddressDeviceMap.clear();
            deviceAddressConnectedMap.clear();
            if (hostHub) {
                Log.d(getResources().getString(R.string.app_name), "HubService: clearDeviceInformation: Part 2.1");
                sendUpdatedDeviceData(false);
            }
            // Update the device connection list
            Intent i = new Intent("RefreshConnectedDevices");
            sendBroadcast(i);
        }


        if (thread_safe) {
            deviceDataLock.release();
            Log.d(getResources().getString(R.string.app_name), "HubService: clearDeviceInformation: released device lock");
        }

        Log.d(getResources().getString(R.string.app_name), "HubService: clearDeviceInformation: ended");
        */
    }


    /**
     * Clears the user information
     * @param address
     * @param name
     * @param thread_safe
     */
    private void clearUserInformation(String address, String name, boolean thread_safe) {
        Log.d(getResources().getString(R.string.app_name), "HubService: clearUserInformation: starting");
        /*if (thread_safe) {
            try {
                userDataLock.acquire();
                Log.d(getResources().getString(R.string.app_name), "HubService: clearUserInformation: Acquired user lock");
            } catch (InterruptedException e) {
                e.printStackTrace();
                return;
            }
        }

        if (address != null && userAddressSet.contains(address) && !mAddress.equals(address)) {
            Log.d(getResources().getString(R.string.app_name), "HubService: clearUserInformation: Part 1");
            Log.d(getResources().getString(R.string.app_name), "HubService: clearUserInformation: Removed " + address + "from user set");
            userAddressSet.remove(address);
            //userAddressPendingSet.add(addr);
            userNameAddressMaps.remove(name);
            userAddressNameMap.remove(address);

            // Close the socket
            if (userAddressSocketMap.get(address) != null) {
                Log.d(getResources().getString(R.string.app_name), "HubService: clearUserInformation: Part 1.1");
                try {
                    userAddressSocketMap.get(address).close();
                    Log.d(getResources().getString(R.string.app_name), "HubService: clearUserInformation: Closed socket to " + address);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            userAddressSocketMap.remove(address);

            userAddressPermissionMap.remove(address);
            if (userAddressThreadMap.get(address) != null) {
                Log.d(getResources().getString(R.string.app_name), "HubService: clearUserInformation: Part 1.2");
                userAddressThreadMap.get(address).interrupt();
            }
            userAddressThreadMap.remove(address);

            // Update the user connection list
            Intent i = new Intent("RefreshConnectedUsers");
            sendBroadcast(i);
        }
        else {
            Log.d(getResources().getString(R.string.app_name), "HubService: clearUserInformation: Part 2");
            // Close the sockets
            for (String addr : userAddressSet) {
                if (userAddressSocketMap.get(addr) != null){
                    Log.d(getResources().getString(R.string.app_name), "HubService: clearUserInformation: Part 2.1");
                    try {
                        userAddressSocketMap.get(addr).close();
                        Log.d(getResources().getString(R.string.app_name), "HubService: clearUserInformation: Closed socket to " + addr);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

            for (String addr : userAddressSet) {
                if (userAddressThreadMap.get(addr) != null){
                    Log.d(getResources().getString(R.string.app_name), "HubService: clearUserInformation: Part 2.1");
                    userAddressThreadMap.get(addr).interrupt();
                }
            }

            if (controllerSocket != null) {
                Log.d(getResources().getString(R.string.app_name), "HubService: clearUserInformation: Part 2.2");
                try {
                    controllerSocket.close();
                    Log.d(getResources().getString(R.string.app_name), "HubService: clearUserInformation: Closed socket to controller");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            if (controllerThread != null) {
                Log.d(getResources().getString(R.string.app_name), "HubService: clearUserInformation: Part 2.2");
                controllerThread.interrupt();
                Log.d(getResources().getString(R.string.app_name), "HubService: clearUserInformation: Closed socket to controller");
            }

            Log.d(getResources().getString(R.string.app_name), "HubService: clearUserInformation: Cleared all user data");
            controllerThread = null;
            controllerSocket = null;
            controllerAddress = null;

            userAddressSet.clear();
            userAddressPendingSet.clear();
            userNameAddressMaps.clear();
            userAddressNameMap.clear();

            userAddressPermissionMap.clear();

            userAddressThreadMap.clear();
            userAddressSocketMap.clear();

            // Update the user connection list
            Intent i = new Intent("RefreshConnectedUsers");
            sendBroadcast(i);
        }

        if (thread_safe) {
            userDataLock.release();
            Log.d(getResources().getString(R.string.app_name), "HubService: clearUserInformation: released user lock");
        }*/

        Log.d(getResources().getString(R.string.app_name), "HubService: clearUserInformation: ended");
    }
}


