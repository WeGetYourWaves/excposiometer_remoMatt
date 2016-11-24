package com.example.matthustahli.radarexposimeter;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;

/**
 * Created by Remo on 24.11.2016.
 */

public class MyActivityBroadcaster extends AppCompatActivity {

    public void sendTrigger(byte[] TriggerPack) {
        Intent intent = new Intent();
        intent.setAction(CommunicationService.ACTION_FROM_ACTIVITY);
        intent.putExtra(CommunicationService.TRIGGER_Act2Serv, TriggerPack);
        sendBroadcast(intent);


    }
}