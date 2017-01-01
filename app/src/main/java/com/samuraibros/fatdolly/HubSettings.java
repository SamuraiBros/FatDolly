package com.samuraibros.fatdolly;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.RatingBar;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.Toast;

public class HubSettings extends BaseActivity {

    //Determines whether activity is running
    public static boolean running = false;

    @Override
    protected void onReceive_helper(Context context, Intent intent) {

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hub_settings);

        registerReceiver(mServerReceiver, mServerIntentFilter);
        mClass = HubSettings.class.toString();

        final EditText hubName_editText = (EditText) findViewById(R.id.edittext_hubName);
        hubName_editText.setText(Configurations.getHubName(this));
        hubName_editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                String input = s.toString();
                if (!input.equals("")) {
                    Configurations.setHubName(HubSettings.this, input);

                    SharedPreferences mPreferences = getSharedPreferences(Configurations.PREFERENCES_ID, 0);
                    SharedPreferences.Editor editor = mPreferences.edit();
                    editor.putString("HubName", input);
                    editor.commit();
                }
            }
        });

        Spinner spinner = (Spinner) findViewById(R.id.spinner_theme);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.theme_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

                switch (position) {
                    case 0:
                        Toast.makeText(parent.getContext(), "Blue", Toast.LENGTH_SHORT).show();
                        break;
                    case 1:
                        Toast.makeText(parent.getContext(), "Orange!", Toast.LENGTH_SHORT).show();
                        break;
                    case 2:
                        Toast.makeText(parent.getContext(), "Purple!", Toast.LENGTH_SHORT).show();
                        break;
                    case 3:
                        Toast.makeText(parent.getContext(), "Green!", Toast.LENGTH_SHORT).show();
                        break;
                    case 4:
                        Toast.makeText(parent.getContext(), "Yellow!", Toast.LENGTH_SHORT).show();
                        break;
                    case 5:
                        Toast.makeText(parent.getContext(), "Orange!", Toast.LENGTH_SHORT).show();
                        break;
                    case 6:
                        Toast.makeText(parent.getContext(), "Black!", Toast.LENGTH_SHORT).show();
                        break;
                    case 7:
                        Toast.makeText(parent.getContext(), "Night!", Toast.LENGTH_SHORT).show();
                        break;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        Switch switch_1 = (Switch) findViewById(R.id.switch_forgetDevice);
        switch_1.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked){
                    Toast.makeText(getApplicationContext(), getResources().getString(R.string.button_yes), Toast.LENGTH_SHORT).show();
                }else{
                    Toast.makeText(getApplicationContext(), getResources().getString(R.string.button_no), Toast.LENGTH_SHORT).show();
                }

            }
        });
        Switch switch_2 = (Switch) findViewById(R.id.switch_saveHubs);
        switch_2.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked){
                    Toast.makeText(getApplicationContext(), getResources().getString(R.string.button_yes), Toast.LENGTH_SHORT).show();
                }else{
                    Toast.makeText(getApplicationContext(), getResources().getString(R.string.button_no), Toast.LENGTH_SHORT).show();
                }

            }
        });

        RatingBar bar = (RatingBar) findViewById(R.id.ratingbar_ratings);
        bar.setOnRatingBarChangeListener(new RatingBar.OnRatingBarChangeListener() {

            @Override
            public void onRatingChanged(RatingBar ratingBar, float rating, boolean fromUser) {
                if(fromUser){
                    Toast.makeText(getApplicationContext(),Float.toString(rating),Toast.LENGTH_SHORT).show();
                }
            }
        });

        running = true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        running = false;
    }
}
