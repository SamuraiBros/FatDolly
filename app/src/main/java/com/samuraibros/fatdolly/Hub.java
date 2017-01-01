package com.samuraibros.fatdolly;

import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import java.util.ArrayList;

public class Hub extends BaseActivity {

    // References to the media control buttons
    private Button[] mediaControlButtons = new Button[4];

    // Reference to volume seekbar
    private SeekBar volume_seekbar;

    private Button discoverableButton;

    // Reference to metadata display
    private TextView metadata_textView;

    // Reference to hub info text view
    private TextView hubName_TextView;
    // Reference to connection type text view
    private TextView connectionType_TextView;
    // Reference to caption text view
    private TextView caption_TextView;
    // Reference to volume icon
    private ImageView volume_ImageView;

    //Determines whether activity is running
    public static boolean running = false;

    //Determines if Hub has been previously started
    public static boolean started = false;

    @Override
    protected void onReceive_helper(Context context, Intent intent) {

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hub);

        //Get the needed configurations items
        registerReceiver(mServerReceiver, mServerIntentFilter);
        mClass = Hub.class.toString();

        Log.d(getResources().getString(R.string.app_name), "Hub:Starting Hub: Bound Service...");
        // Update the hub name and controller info
        hubName_TextView = (TextView) findViewById(R.id.textview_hubName);

        if (Configurations.isController())
            hubName_TextView.setText(Configurations.getHubName(this) + "'s Hub");
        else
            hubName_TextView.setText(Configurations.getControllerName() + "'s Hub");

        Log.d(getResources().getString(R.string.app_name), "Hub:Starting Hub: Set Name...");
        connectionType_TextView = (TextView) findViewById(R.id.textview_connectionType);
        caption_TextView = (TextView) findViewById(R.id.textview_caption);
        Button top_right = (Button) findViewById(R.id.button_topRight);
        if (Configurations.isController()) {
            connectionType_TextView.setText("Connected as Host");
            caption_TextView.setText("Your music, your way");
            top_right.setBackground(getResources().getDrawable(R.mipmap.ic_otherhub));
            top_right.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    goToConnHub(null);
                }
            });
        }
        else {
            connectionType_TextView.setText("Connected as User");
            caption_TextView.setText("Their music, your way");
            top_right.setBackground(getResources().getDrawable(R.mipmap.ic_myhub));
            top_right.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    gotoHub(null);
                }
            });
        }

        Log.d(getResources().getString(R.string.app_name), "Hub:Starting Hub: Set Connection Type...");

        metadata_textView = (TextView) findViewById(R.id.textview_metadata);
        String[] metadata = Configurations.getMetadata();
        metadata_textView.setText(metadata[1] + " by " + metadata[0]);

        mediaControlButtons[0] = (Button) findViewById(R.id.button_previous);
        mediaControlButtons[0].setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ArrayList<String> permissions = new ArrayList<>();
                if (!Configurations.isController()) {
                    String controllerAddress = Configurations.getControllerAddress();
                    permissions = Configurations.userAddressToPermissions(controllerAddress);
                }
                boolean playBackControl = permissions.contains(getResources().getString(R.string.permission_control_playback));
                if (Configurations.isController() || playBackControl) {
                    prevSong(null);
                }
                else {
                    playBackRequest(Hub.this);
                }
            }
        });
        mediaControlButtons[1] = (Button) findViewById(R.id.button_pause);
        mediaControlButtons[1].setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ArrayList<String> permissions = new ArrayList<>();
                if (!Configurations.isController()) {
                    String controllerAddress = Configurations.getControllerAddress();
                    permissions = Configurations.userAddressToPermissions(controllerAddress);
                }
                boolean playBackControl = permissions.contains(getResources().getString(R.string.permission_control_playback));
                if (Configurations.isController() || playBackControl) {
                    pauseSong(null);
                }
                else {
                    playBackRequest(Hub.this);
                }
            }
        });
        mediaControlButtons[2] = (Button) findViewById(R.id.button_play);
        mediaControlButtons[2].setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ArrayList<String> permissions = new ArrayList<>();
                if (!Configurations.isController()) {
                    String controllerAddress = Configurations.getControllerAddress();
                    permissions = Configurations.userAddressToPermissions(controllerAddress);
                }
                boolean playBackControl = permissions.contains(getResources().getString(R.string.permission_control_playback));
                if (Configurations.isController() || playBackControl) {
                    playSong(null);
                }
                else {
                    playBackRequest(Hub.this);
                }
            }
        });
        mediaControlButtons[3] = (Button) findViewById(R.id.button_next);
        mediaControlButtons[3].setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ArrayList<String> permissions = new ArrayList<>();
                if (!Configurations.isController()) {
                    String controllerAddress = Configurations.getControllerAddress();
                    permissions = Configurations.userAddressToPermissions(controllerAddress);
                }
                boolean playBackControl = permissions.contains(getResources().getString(R.string.permission_control_playback));
                if (Configurations.isController() || playBackControl) {
                    nextSong(null);
                }
                else {
                    playBackRequest(Hub.this);
                }
            }
        });

        discoverableButton = (Button) findViewById(R.id.button_discoverable);
        discoverableButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ArrayList<String> permissions = new ArrayList<>();
                if (!Configurations.isController()) {
                    String controllerAddress = Configurations.getControllerAddress();
                    permissions = Configurations.userAddressToPermissions(controllerAddress);
                }
                boolean discoverabilityControl = permissions.contains(getResources().getString(R.string.permission_control_discoverability));
                if (Configurations.isController() || discoverabilityControl) {
                   //makeDiscoverable(null);
                }
                else {
                    discoverableRequest();
                }
            }
        });

        volume_ImageView = (ImageView) findViewById(R.id.imageview_volume);
        volume_seekbar = (SeekBar) findViewById(R.id.seekbar_volume);
        Log.d(getResources().getString(R.string.app_name), "Hub:Starting Hub: Set Media Control...");
        Configurations.updateVolumeFromSystem();
        int maxVolume = Configurations.mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        volume_seekbar.setMax(maxVolume);

        Log.d(getResources().getString(R.string.app_name), "Hub:HubSettings Volume onCreate: " + Integer.toString(Configurations.getVolumeLevel()));
        volume_seekbar.setProgress(Configurations.getVolumeLevel());

        volume_seekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            boolean internal = false;
            int volume = 0;
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if (internal) {
                    Intent i = new Intent(getResources().getString(R.string.intent_update_volume));
                    i.putExtra(getResources().getString(R.string.extra_volume), volume);
                    sendBroadcast(i);
                }
                internal = false;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                ArrayList<String> permissions = new ArrayList<>();
                if (!Configurations.isController()) {
                    String controllerAddress = Configurations.getControllerAddress();
                    permissions = Configurations.userAddressToPermissions(controllerAddress);
                }
                final boolean volumeControl = permissions.contains(getResources().getString(R.string.permission_control_volume));
                if (Configurations.isController() || volumeControl) {
                    internal = true;
                }
                else {
                    playBackRequest(getApplicationContext());
                }
            }

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (internal)
                    volume = progress;
            }
        });

        updateControlViews();

        Log.d(getResources().getString(R.string.app_name), "Hub:Starting Hub: Started Proxy...");


        Log.d(getResources().getString(R.string.app_name), "Hub:Started before: " + Boolean.toString(started));
        /*if (!started) {
            started = true;
            if (Configurations.isController()) {
                Log.d(getResources().getString(R.string.app_name), "Hub:Starting query");
                queryDeviceConnections(Hub.this, getIntent().getStringArrayListExtra("Connections"));
            }
        }*/

        running = true;
        Log.d(getResources().getString(R.string.app_name), "Hub:Started...");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        running = false;
    }

    private void discoverableRequest() {
        // Creates a popup dialog to provide further choices for a response
        final AlertDialog.Builder dialog_builder = new AlertDialog.Builder(this);

        // Sets the title for the popup dialog
        dialog_builder.setTitle("Make Host Device Discoverable");
        dialog_builder.setMessage("Top level stuff here. Sadly, you don't have appropriate permissions to do this.");
        final AlertDialog dialog = dialog_builder.create();
        // Displays the dialogue
        dialog.show();
    }

    private void updateControlViews() {

        ArrayList<String> permissions = new ArrayList<>();
        if (!Configurations.isController()) {
            String controllerAddress = Configurations.getControllerAddress();
            permissions = Configurations.userAddressToPermissions(controllerAddress);
        }

        boolean playBackControl = permissions.contains(getResources().getString(R.string.permission_control_playback));

        for ( Button b : mediaControlButtons) {
            if (!Configurations.isController() && !playBackControl) {
                b.setAlpha(.2f);
            }
            else {
                b.setAlpha(1f);
            }
        }

        Log.d(getResources().getString(R.string.app_name), "Hub:Starting Hub: Set Metadata...");


        boolean discoverabilityControl = permissions.contains(getResources().getString(R.string.permission_control_discoverability));
        discoverableButton = (Button) findViewById(R.id.button_discoverable);

        if (!Configurations.isController() && !discoverabilityControl) {
            discoverableButton.setAlpha(.2f);
        }
        else {
            discoverableButton.setAlpha(1f);
        }

        boolean volumeControl = permissions.contains(getResources().getString(R.string.permission_control_volume));
        volume_seekbar = (SeekBar) findViewById(R.id.seekbar_volume);
        volume_ImageView = (ImageView) findViewById(R.id.imageview_volume);

        if (!Configurations.isController() && !volumeControl) {
            volume_seekbar.setEnabled(false);
            volume_seekbar.setAlpha(.2f);
            volume_ImageView.setAlpha(.2f);
        }
        else {
            volume_seekbar.setEnabled(true);
            volume_seekbar.setAlpha(1f);
            volume_ImageView.setAlpha(1f);
        }

    }


    /**
     * Switches to the Connected Users screen when the button is pressed
     * @param view
     */
    public void goToConnHub (View view) {
        // Creates a popup dialog to provide further choices for a response
        final AlertDialog alertDialog = new AlertDialog.Builder(Hub.this).create();

        // Sets the title for the popup dialog
        alertDialog.setTitle("Connect to Other Hub");
        alertDialog.setMessage("Are you sure? This will disconnect all your current users and devices!");

        // Switches to hub
        alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, getResources().getString(R.string.button_yes), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                /*Intent intent = new Intent(Hub.this, Loading.class);
                intent.putExtra(getResources().getString(R.string.extra_sender_class), mClass);
                intent.putExtra(getResources().getString(R.string.extra_loading_type), getResources().getString(R.string.intent_switch_to_connHub));
                intent.putExtra(getResources().getString(R.string.extra_loading_class), ConnectToHub.class.toString());
                startActivity(intent);
                finish();*/
            }
        });

        // Stays in current hub
        alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, getResources().getString(R.string.button_no), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                alertDialog.cancel();
            }
        });

        // Displays the dialogue
        alertDialog.show();
    }

    /**
     * Switches to the Connected Users screen when the button is pressed
     * @param view
     */
    public void gotoHub (View view) {
        // Creates a popup dialog to provide further choices for a response
        final AlertDialog alertDialog = new AlertDialog.Builder(Hub.this).create();

        // Sets the title for the popup dialog
        alertDialog.setTitle("Switch over to your Hub");
        alertDialog.setMessage("Are you sure? This will disconnect you from the current hub!");

        // Switches to hub
        alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, getResources().getString(R.string.button_yes), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                /*Intent intent = new Intent(Hub.this, Loading.class);
                intent.putExtra(getResources().getString(R.string.extra_sender_class), mClass);
                intent.putExtra(getResources().getString(R.string.extra_loading_type), getResources().getString(R.string.intent_switch_to_hub));
                intent.putExtra(getResources().getString(R.string.extra_loading_class), ConnectToHub.class.toString());
                startActivity(intent);
                finish();*/
            }
        });

        // Stays in current hub
        alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, getResources().getString(R.string.button_no), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                alertDialog.cancel();
            }
        });

        // Displays the dialogue
        alertDialog.show();
    }
}
