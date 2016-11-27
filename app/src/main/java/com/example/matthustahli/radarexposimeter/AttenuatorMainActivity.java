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
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import static java.lang.Thread.sleep;

public class AttenuatorMainActivity extends AppCompatActivity implements View.OnClickListener{


    ImageButton b_batterie;

    String myMode;
    Integer counter=0;
    WifiManager wifi_manager;
    Button b_modeNormal,b_mode21dB,b_mode41dB, b_modeLNA, b_chico;
    TextView id_device;
    ProgressBar progressBar;
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

        Log.d("AttenuatorMainActivity" , "onCreate finished");
    }


    private void initializeButtons(){
        Log.d("AttenuatorMainActivity" , "initializeButtons called");
        id_device = (TextView)findViewById(R.id.id_device);
        b_modeNormal = (Button) findViewById(R.id.b_mode_normal);
        b_mode21dB = (Button) findViewById(R.id.b_mode_21db);
        b_mode41dB = (Button) findViewById(R.id.b_mode_42db);
        b_modeLNA = (Button) findViewById(R.id.b_mode_LNA);
        b_batterie = (ImageButton) findViewById(R.id.b_batterie);
        b_chico = (Button) findViewById(R.id.b_chico);
        layout_settings= (LinearLayout) findViewById(R.id.settings_atStart);
        progressBar= (ProgressBar) findViewById(R.id.progress_bar);
    }

    private void activateClickListener(){
        b_modeNormal.setOnClickListener(this);
        b_batterie.setOnClickListener(this);
        b_modeLNA.setOnClickListener(this);
        b_mode41dB.setOnClickListener(this);
        b_mode21dB.setOnClickListener(this);
        b_chico.setOnClickListener(this);
        layout_settings.setVisibility(View.GONE);
        progressBar.setMax(100);
        progressBar.setVisibility(ProgressBar.VISIBLE);

    }

    private void goToNextActivityWithSpecifivMode(String mode) {
        myMode = mode;
        Intent intent = new Intent(AttenuatorMainActivity.this, OverviewScanPlotActivity.class);
        intent.putExtra("MODE", myMode);
        startActivity(intent);
    }

    @Override
    public void onStart() {
        Log.d("AttenuatorMainActivity" , "onStart called");
        StartService();

        super.onStart();
    }

    private void StartService() {
        Intent intent = new Intent(this, CommunicationService.class);
        startService(intent);
    }

    @Override
    public void onResume(){
        super.onResume();
        Log.d(LOG_TAG, "in onResume");
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(CommunicationService.TRIGGER_Serv2Act);
        registerReceiver(attenuatorMainActivityReceiver, intentFilter);

    }

    @Override
    public void onPause(){
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

/*        if(this.isFinishing()) {
            stopService(service);
            wifi_manager.setWifiEnabled(true);// sets wifi back on
            Toast.makeText(this, "app finaly closed", Toast.LENGTH_SHORT).show();
        }*/
        super.onDestroy();
    }

    @Override
    public void onClick(View v) {
        Log.d(LOG_TAG, "in onClick()");
        switch (v.getId()){
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
/*           case R.id.b_batterie:
                counter++;
                if (counter % 4 == 0)
                    b_batterie.setBackgroundResource(R.drawable.ic_batterie_empty);
                if (counter % 4 == 1) b_batterie.setBackgroundResource(R.drawable.ic_batterie_low);
                if (counter % 4 == 2)
                    b_batterie.setBackgroundResource(R.drawable.ic_batterie_middle);
                if (counter % 4 == 3) b_batterie.setBackgroundResource(R.drawable.ic_batterie_high);
                break;*/
            case R.id.b_chico:
                counter++;
                ImageView image = (ImageView) findViewById(R.id.andi);
                if (counter%2==0){ image.setVisibility(ImageView.VISIBLE);
                }else{
                    image.setVisibility(ImageView.GONE);
                }
        }
    }


    protected byte[] split_packet (int start, int end, byte[] packet){

        int length = end - start + 1;
        byte[] splitted = new byte[length];
        for (int i = 0; i < length; i++){
            splitted[i] = packet[i + start];
        }

        return splitted;
    }

    //receiver of the AttenuatorMainActivity which receives the data from the CommunicationService
    public class AttenuatorMainActivityReceiver extends BroadcastReceiver {
        final String LOG_TAG;

        public AttenuatorMainActivityReceiver(String LOG_TAG){
            this.LOG_TAG = LOG_TAG;

        }
        @Override
        public void onReceive(Context arg0, Intent data) {
            Log.d(LOG_TAG, "MyActivityReceiver in onReceive");
            byte[] orgData = data.getByteArrayExtra(CommunicationService.DATA_BACK);
            if (orgData != null) {

                if(new String(split_packet(4, 7, orgData)).equals("PROG")) {
                    Progress_Packet_Exposi progPack = new Progress_Packet_Exposi(orgData);
                    progressBar.setProgress(progPack.get_progress());
                    Log.d(LOG_TAG, "got PROG from ESP and set Progressbar");

                    if(progressBar.getProgress() >= progressBar.getMax()){
                        Log.d(LOG_TAG, "ProgressThread done");

                        runOnUiThread(new Runnable() {

                            @Override
                            public void run() {
                                progressBar.setVisibility(ProgressBar.GONE);
                                layout_settings.setVisibility(View.VISIBLE);
                                Log.d(LOG_TAG, "Progressbar GONE");
                            }
                        });
                    }
                }

                if(new String(split_packet(4, 7, orgData)).equals("DRDY")) {

                    Log.d(LOG_TAG, "got DRDY from ESP");
                    Ready_Packet_Exposi ready_packet_exposi = new Ready_Packet_Exposi(orgData);
                    setId_device(ready_packet_exposi.get_device_id());
                    setBatteryStatus(ready_packet_exposi.get_battery_charge());
                    Cal_Packet_Trigger trigger = new Cal_Packet_Trigger(ready_packet_exposi.get_device_id(), 0);
                    sendTrigger(trigger.get_packet());
                    Log.d(LOG_TAG, "sent calTrigger to ESP");
                }
            }

        }
    }

    void setBatteryStatus(int percentage){
        if(percentage < 15) b_batterie.setBackgroundResource(R.drawable.ic_batterie_empty);
        else if (percentage < 30) b_batterie.setBackgroundResource(R.drawable.ic_batterie_low);
        else if (percentage < 70) b_batterie.setBackgroundResource(R.drawable.ic_batterie_middle);
        else b_batterie.setBackgroundResource(R.drawable.ic_batterie_high);
        Log.d(LOG_TAG, "battery percentage set");
    }

    void setId_device (int id){
        id_device.setText("ID: " + Integer.toString(id));
        Log.d(LOG_TAG, "id_device set");

    }
}
