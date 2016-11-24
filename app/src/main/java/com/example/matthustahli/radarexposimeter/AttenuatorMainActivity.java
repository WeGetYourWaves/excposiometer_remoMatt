package com.example.matthustahli.radarexposimeter;

import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.net.wifi.WifiConfiguration;
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
import android.widget.Toast;

import java.lang.reflect.Method;

import static java.lang.Thread.sleep;

public class AttenuatorMainActivity extends AppCompatActivity implements View.OnClickListener{


    ImageButton b_batterie;

    Calibration_Activity calibration;

    String myMode;
    Integer counter=0;
    WifiManager wifi_manager;
    Button b_modeNormal,b_mode21dB,b_mode41dB, b_mode_accumulation,b_chico;
    ProgressBar progressBar;
    LinearLayout layout_settings;
    Handler h = new Handler();
    Intent service;
    final String LOG_TAG = "AttenuatorMainActivity";
    WifiDataBuffer buffer = new WifiDataBuffer();
    MyActivityReceiver AttenuattorMainActivityReceiver = new MyActivityReceiver(LOG_TAG, buffer);




    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_attenuator_main);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        initializeButtons();
        activateClickListener();

        if (savedInstanceState == null) {
            testToLetprogressRun();
        }
        //calibration = new Calibration_Activity(buffer);
        calibration = new Calibration_Activity(buffer);
        Log.d("AttenuatorMainActivity" , "onCreate finished");
    }


    private void initializeButtons(){
        Log.d("AttenuatorMainActivity" , "initializeButtons called");
        b_modeNormal = (Button) findViewById(R.id.b_mode_normal);
        b_mode21dB = (Button) findViewById(R.id.b_mode_21db);
        b_mode41dB = (Button) findViewById(R.id.b_mode_42db);
        b_mode_accumulation = (Button) findViewById(R.id.b_mode_accumulator);
        b_batterie = (ImageButton) findViewById(R.id.b_batterie);
        b_chico = (Button) findViewById(R.id.b_chico);
        layout_settings= (LinearLayout) findViewById(R.id.settings_atStart);
        progressBar= (ProgressBar) findViewById(R.id.progress_bar);
    }

    private void activateClickListener(){
        b_modeNormal.setOnClickListener(this);
        b_batterie.setOnClickListener(this);
        b_mode_accumulation.setOnClickListener(this);
        b_mode41dB.setOnClickListener(this);
        b_mode21dB.setOnClickListener(this);
        b_chico.setOnClickListener(this);
        layout_settings.setVisibility(View.GONE);
        progressBar.setVisibility(ProgressBar.GONE);

    }

    private void testToLetprogressRun() {

        Thread timer = new Thread(new Runnable() {
            @Override
            public void run() {
                Log.d("AttenuatorMainActivity" , "progressbar called");
                progressBar.setMax(26);
                while (calibration == null){
                    progressBar.setProgress(0);
                    try{
                        sleep(50);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                progressBar.setProgress(calibration.progress);
                while (calibration.progress < progressBar.getMax()) {
                    try{
                        sleep(50);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    progressBar.setProgress(calibration.progress);
                }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        progressBar.setVisibility(ProgressBar.GONE);
                        layout_settings.setVisibility(View.VISIBLE);
                    }
                });
            }
        });
        timer.start();
    }



    private void goToNextActivityWithSpecifivMode(String mode) {
        myMode = mode;
        Intent intent = new Intent(AttenuatorMainActivity.this, OverviewScanPlotActivity.class);
        intent.putExtra("MODE", myMode);
        Bundle bundle = new Bundle();
        bundle.putSerializable("Buffer", buffer);
        bundle.putSerializable("Calibration", calibration);
        intent.putExtras(bundle);
        startActivity(intent);
    }

    @Override
    public void onStart() {
        Log.d("AttenuatorMainActivity" , "onStart called");
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(CommunicationService.TRIGGER_Serv2Act);
        registerReceiver(AttenuattorMainActivityReceiver, intentFilter);
        StartService();
        super.onStart();
    }

    @Override
    public void onStop() {
        unregisterReceiver(AttenuattorMainActivityReceiver);
        super.onStop();
    }

    @Override
    protected void onDestroy() {
/*        if(this.isFinishing()) {
            stopService(service);
            wifi_manager.setWifiEnabled(true);// sets wifi back on
            Toast.makeText(this, "app finaly closed", Toast.LENGTH_SHORT).show();
        }*/
        super.onDestroy();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.b_mode_normal:
                goToNextActivityWithSpecifivMode("normal");
                break;
            case R.id.b_mode_21db:
                goToNextActivityWithSpecifivMode("21dB");
                break;
            case R.id.b_mode_42db:
                goToNextActivityWithSpecifivMode("41dB");
                break;
            case R.id.b_mode_accumulator:
                goToNextActivityWithSpecifivMode("accu");
                break;
            case R.id.b_batterie:
                counter++;
                if (counter % 4 == 0)
                    b_batterie.setBackgroundResource(R.drawable.ic_batterie_empty);
                if (counter % 4 == 1) b_batterie.setBackgroundResource(R.drawable.ic_batterie_low);
                if (counter % 4 == 2)
                    b_batterie.setBackgroundResource(R.drawable.ic_batterie_middle);
                if (counter % 4 == 3) b_batterie.setBackgroundResource(R.drawable.ic_batterie_high);
                break;
            case R.id.b_chico:
                counter++;
                ImageView image = (ImageView) findViewById(R.id.andi);
                if (counter%2==0){ image.setVisibility(ImageView.VISIBLE);
                }else{
                    image.setVisibility(ImageView.GONE);
                }
        }
    }

    private void StartService() {
        Intent intent = new Intent(this, CommunicationService.class);
        startService(intent);
    }
}
