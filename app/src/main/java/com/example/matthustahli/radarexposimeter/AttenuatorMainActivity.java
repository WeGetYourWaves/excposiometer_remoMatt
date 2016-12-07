package com.example.matthustahli.radarexposimeter;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.ScaleAnimation;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import static java.lang.Thread.sleep;

public class AttenuatorMainActivity extends AppCompatActivity implements View.OnClickListener {


    ImageButton b_batterie;
    Animation animationSlideDown;
    String myMode;
    WifiManager wifi_manager;
    Button b_modeNormal, b_mode21dB, b_mode41dB, b_modeLNA;
    TextView id_device, text;
    ProgressBar progressBar;
    boolean goingToNextActivity=true;
    LinearLayout layout_settings;
    Handler h = new Handler();
    final String LOG_TAG = "AttenuatorMainActivity";
    final AttenuatorMainActivityReceiver attenuatorMainActivityReceiver = new AttenuatorMainActivityReceiver(LOG_TAG);


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_attenuator_main);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

        initializeButtons();
        activateClickListener();

        Log.d("AttenuatorMainActivity", "onCreate finished");
    }


    private void initializeButtons() {
        Log.d("AttenuatorMainActivity", "initializeButtons called");
        id_device = (TextView) findViewById(R.id.id_device);
        b_modeNormal = (Button) findViewById(R.id.b_mode_normal);
        b_mode21dB = (Button) findViewById(R.id.b_mode_21db);
        b_mode41dB = (Button) findViewById(R.id.b_mode_42db);
        b_modeLNA = (Button) findViewById(R.id.b_mode_LNA);
        b_batterie = (ImageButton) findViewById(R.id.b_batterie);
        layout_settings = (LinearLayout) findViewById(R.id.settings_atStart);
        progressBar = (ProgressBar) findViewById(R.id.progress_bar);
        text = (TextView) findViewById(R.id.tv_under_progressBar);
    }

    private void activateClickListener() {
        b_modeNormal.setOnClickListener(this);
        b_batterie.setOnClickListener(this);
        b_modeLNA.setOnClickListener(this);
        b_mode41dB.setOnClickListener(this);
        b_mode21dB.setOnClickListener(this);
        layout_settings.setVisibility(View.GONE);
        progressBar.setMax(100);
        progressBar.setVisibility(ProgressBar.VISIBLE);

    }

    private void goToNextActivityWithSpecifivMode(String mode) {
        if(goingToNextActivity==true){
        myMode = mode;
        Intent intent = new Intent(AttenuatorMainActivity.this, OverviewScanPlotActivity.class);
        intent.putExtra("MODE", myMode);
        startActivity(intent);
        }
    }

    @Override
    public void onStart() {
        Log.d("AttenuatorMainActivity", "onStart called");
        StartService();

        super.onStart();
    }

    private void StartService() {
        Intent intent = new Intent(this, CommunicationService.class);
        startService(intent);
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(LOG_TAG, "in onResume");
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(CommunicationService.TRIGGER_Serv2Act);
        registerReceiver(attenuatorMainActivityReceiver, intentFilter);

    }

    @Override
    public void onPause() {
        Log.d(LOG_TAG, "in onPause");
        super.onPause();
        unregisterReceiver(attenuatorMainActivityReceiver);
    }

    private void sendTrigger(byte[] TriggerPack) {
        Intent intent = new Intent();
        intent.setAction(CommunicationService.ACTION_FROM_ACTIVITY);
        intent.putExtra(CommunicationService.TRIGGER_Act2Serv, TriggerPack);
        sendBroadcast(intent);
    }

    @Override
    public void onStop() {
        Log.d(LOG_TAG, "in onstop()");
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        Log.d(LOG_TAG, "in ondestroy()");

        super.onDestroy();

        if (this.isFinishing()) {
            StopService();
            Toast.makeText(this, "app finaly closed", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onClick(View v) {
        Log.d(LOG_TAG, "in onClick()");
        switch (v.getId()) {
            case R.id.b_mode_normal:
                goToNextActivityWithSpecifivMode("normal mode");
                break;
            case R.id.b_mode_21db:
                goToNextActivityWithSpecifivMode("-21 dB");
                break;
            case R.id.b_mode_42db:
                goToNextActivityWithSpecifivMode("-42 dB");
                break;
            case R.id.b_mode_LNA:
                goToNextActivityWithSpecifivMode("LNA on");
                break;
        }
    }


    protected byte[] split_packet(int start, int end, byte[] packet) {

        int length = end - start + 1;
        byte[] splitted = new byte[length];
        for (int i = 0; i < length; i++) {
            splitted[i] = packet[i + start];
        }

        return splitted;
    }

    //receiver of the AttenuatorMainActivity which receives the data from the CommunicationService
    public class AttenuatorMainActivityReceiver extends BroadcastReceiver {
        final String LOG_TAG;

        public AttenuatorMainActivityReceiver(String LOG_TAG) {
            this.LOG_TAG = LOG_TAG;

        }

        @Override
        public void onReceive(Context arg0, Intent data) {
            Log.d(LOG_TAG, "MyActivityReceiver in onReceive");
            byte[] orgData = data.getByteArrayExtra(CommunicationService.DATA_BACK);
            if (orgData != null) {

                if (new String(split_packet(4, 7, orgData)).equals("PROG")) {
                    Progress_Packet_Exposi progPack = new Progress_Packet_Exposi(orgData);
                    progressBar.setProgress(progPack.get_progress());
                    Log.d(LOG_TAG, "got PROG from ESP and set Progressbar");

                    if (progressBar.getProgress() >= progressBar.getMax()) {
                        Log.d(LOG_TAG, "ProgressThread done");

                        runOnUiThread(new Runnable() {

                            @Override
                            public void run() {
                                progressBar.setVisibility(ProgressBar.GONE);
                                layout_settings.setVisibility(View.VISIBLE);
                                text.setVisibility(View.GONE);
                                Log.d(LOG_TAG, "Progressbar GONE");
                            }
                        });
                    }
                }

                if (new String(split_packet(4, 7, orgData)).equals("DRDY")) {

                    Log.d(LOG_TAG, "got DRDY from ESP");
                    Ready_Packet_Exposi ready_packet_exposi = new Ready_Packet_Exposi(orgData);
                    setId_device(ready_packet_exposi.get_device_id(), ready_packet_exposi.get_battery_charge());
                    setBatteryStatus(ready_packet_exposi.get_battery_charge());
                    Cal_Packet_Trigger trigger = new Cal_Packet_Trigger(ready_packet_exposi.get_device_id(), 0);
                    sendTrigger(trigger.get_packet());
                    Log.d(LOG_TAG, "sent calTrigger to ESP");
                } else if (new String(split_packet(4, 7, orgData)).equals("EROR")) {

                    Log.d(LOG_TAG, "got EROR packet");
                    Error_Packet_Exposi error_packet = new Error_Packet_Exposi(orgData);
                    int errorCode = error_packet.get_errorCode();
                    String errorMessage = error_packet.get_errorMessage();
                    if (errorCode == 1) {
                        //connection to ESP lost
                        ConnectionLostDropDown(0);
                    }
                }
            }

        }
    }

    //todo //percentage...
    void setBatteryStatus(int percentage) {
        if (percentage < 15) b_batterie.setBackgroundResource(R.drawable.ic_batterie_empty);
        else if (percentage < 30) b_batterie.setBackgroundResource(R.drawable.ic_batterie_low);
        else if (percentage < 70) b_batterie.setBackgroundResource(R.drawable.ic_batterie_middle);
        else b_batterie.setBackgroundResource(R.drawable.ic_batterie_high);
        Log.d(LOG_TAG, "battery percentage set");
    }

    void setId_device(int id, int percentage) {
        id_device.setText("Battery: " + Integer.toString(percentage) + "%" + '\n' + "ID: " + Integer.toString(id));
        Log.d(LOG_TAG, "id_device set");
        text.setText("Calibrating");
    }

    private void StopService() {
        Log.d(LOG_TAG, "StopService called");
        Intent i = new Intent(this, CommunicationService.class);
        stopService(i);

//        Intent intent = new Intent();
//        intent.setAction(CommunicationService.ACTION_FROM_ACTIVITY);
//        intent.putExtra(CommunicationService.COMMAND_Act2Serv,
//                CommunicationService.CMD_STOP);
    }

    //0 for down, 1 for up
    //handles the allert bar, for example when connection is lost.
    private void ConnectionLostDropDown( Integer downOrUp) {
        //sets listener, and handles drop down and drop up
        animationSlideDown = AnimationUtils.loadAnimation(this, R.anim.anim_drop_down);
        final LinearLayout layout_dropDown = (LinearLayout) findViewById(R.id.layout_dropDown);
        TextView allert_text = (TextView) findViewById(R.id.textView_dropDownAllert);
        if(downOrUp==0){
            goingToNextActivity=false;
            layout_dropDown.setVisibility(View.VISIBLE);
            layout_dropDown.bringToFront();
            ScaleAnimation scale = new ScaleAnimation(1,1,0,1);
            scale.setDuration(400);
            allert_text.startAnimation(scale);
        }else{
            ScaleAnimation scale = new ScaleAnimation(1,1,1,0);
            scale.setDuration(400);
            allert_text.startAnimation(scale);
            scale.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {}
                @Override
                public void onAnimationEnd(Animation animation) {
                    layout_dropDown.setVisibility(View.GONE);}
                @Override
                public void onAnimationRepeat(Animation animation) {}
            });
        }
    }
}
