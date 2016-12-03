//package com.example.matthustahli.radarexposimeter;
//
//import android.app.Service;
//import android.content.Intent;
//import android.os.IBinder;
//import android.support.annotation.Nullable;
//import android.widget.Toast;
//
///**
// * Created by matthustahli on 07.11.16.
// */
//
//public class MasterSlaveService extends Service {
//    @Nullable
//    @Override
//    public IBinder onBind(Intent intent) {
//        return null;
//    }
//
//    @Override
//    public void onDestroy() {
//        Toast.makeText(this, "service stoped/app closed", Toast.LENGTH_SHORT).show();
//        super.onDestroy();
//    }
//
//    @Override
//    public int onStartCommand(Intent intent, int flags, int startId) {
//        Toast.makeText(this, "service started", Toast.LENGTH_SHORT).show();
//        return START_STICKY; //lets service run until closed
//    }
//
//}
