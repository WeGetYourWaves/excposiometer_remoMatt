package com.example.matthustahli.radarexposimeter;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.graphics.Point;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.ScaleAnimation;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import static java.lang.Math.log;
import static java.lang.Math.round;

public class DetailViewActivity extends AppCompatActivity implements View.OnClickListener {

    private final String CHOOSENFREQ = "my_freq";
    ArrayList<Integer> fixedFreq= new ArrayList<Integer>();
    private ArrayAdapter<LiveMeasure> adapter;
    private ArrayList<LiveMeasure> measures = new ArrayList<LiveMeasure>();
    Float maxPeak = 0f;
    Float maxRMS = 0f;
    String myMode;
    private int attenuator;
    private int device_id;
    private static final char measurement_type = 'A';
    Timer timer;
    final String LOG_TAG = "DetailView";
    DetailViewActivityReceiver detailViewActivityReceiver = new DetailViewActivityReceiver(LOG_TAG);
    Activity_Superclass calibration;
    boolean makePlotRunning=true;
    Display display;
    Point size;
    float barWidthMax, textViewSize;
    int colorButtonActiveMode, colorButtonInactive;
    Animation animationSlideDown;
    boolean goingToNextActivity = true;




    //TODO variablen für verbesserung
    private int[] freq= {0,0,0,0,0,0};//frequencies are in MHz  //beinhaltet die zu betrachtenden frequenzen    //make switch funktion that deletes element at certain place and reorders them
    private double[] rms = new double[6];
    private double[] peak = new double[6];
    private int freq_number = 6;        //tells how many freq are active.. the values of those freq are in freq, u to freq_number..




    //Everything about buttons
    ImageButton b_settings;
    Button b_normal, b_21dB, b_41dB, b_accu, b_startStop;
    LinearLayout settings;
    ListView visibleList;

    Runnable runnable;
    Handler handler;
    int counter=0;
    int colorLimit, colorBar,colorEmpty;
    double maxPlotP, minPlotP, minPlotR, maxPlotR;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail_view);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        adapter = new MyListAdapter();

        //load intent from previous activity
        getChoosenFreqFromIntent();
        initializeValues();
        //putFrequenciesIntoDoubleArray();

        //everything with the list population and keeping it updated
        populateMeasurements();
        populateListView();             // this plots the data to the layout
        handleClicksOnList();
        Toast.makeText(this,String.valueOf(freq_number),Toast.LENGTH_SHORT).show();

        //activate all Buttons and listeners
        initializeButtons();
        setButtonsOnClickListener();
        activateTouch();
        //activateValueUpdater(); // funktion von matthias für listenupdate alle x sec..

    }


    private void initializeValues(){
        colorBar = DetailViewActivity.this.getResources().getColor(R.color.normalBar);
        colorLimit = DetailViewActivity.this.getResources().getColor(R.color.limitBar);
        colorEmpty = Color.TRANSPARENT;
    }
    public void onStart() {
        Log.d(LOG_TAG , "onStart called");
        super.onStart();
    }

    public void onStop(){
        Log.d(LOG_TAG , "onStop called");
        super.onStop();
    }

    @Override
    protected void onPause() {
        Log.d(LOG_TAG, "in onPause");
        super.onPause();
        unregisterReceiver(detailViewActivityReceiver);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(LOG_TAG, "in onResume");
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(CommunicationService.TRIGGER_Serv2Act);
        registerReceiver(detailViewActivityReceiver, intentFilter);
        StartService();
        RequestCALD();
    }

    //------------------------calculate the size of the value bar ------------------------



    public float getMySizeComparedToMax(double meassurement, char peakOrRms){
        double rVal=0;
        if(meassurement == 'P'){
            if(meassurement<5100){      //if too high, we get 5500
                rVal = (((log(meassurement)-log(minPlotP))/(log(maxPlotP)-log(minPlotP))*barWidthMax));
            }else{
                rVal=barWidthMax;
            }
        }else{
            if(meassurement<5100){
                rVal = (((log(meassurement)-log(minPlotR))/(log(maxPlotR)-log(minPlotR))*barWidthMax));
            }else{
                rVal=barWidthMax;
            }
        }
        return (float) rVal;
    }



    //------------------------------------------------------------------------------------------------



    private void initializeButtons() {
        //initialize Buttons
        b_normal = (Button) findViewById(R.id.b_mode_normal);
        b_21dB = (Button) findViewById(R.id.b_mode_21db);
        b_41dB = (Button) findViewById(R.id.b_mode_42db);
        b_accu = (Button) findViewById(R.id.b_mode_LNA);
        settings = (LinearLayout) findViewById(R.id.layout_setting);
        b_settings = (ImageButton) findViewById(R.id.setting_button);
        b_startStop = (Button) findViewById(R.id.startStopButton);
        visibleList = (ListView) findViewById(R.id.list_live_data);
        display = getWindowManager().getDefaultDisplay();
        size = new Point();
        display.getSize(size);
        barWidthMax=size.x/3;
        textViewSize = size.x/4;
        colorButtonActiveMode = DetailViewActivity.this.getResources().getColor(R.color.active_mode_button_color);
        colorButtonInactive = DetailViewActivity.this.getResources().getColor(R.color.inactive_mode_button_color);
    }

    private void setButtonsOnClickListener() {
        b_normal.setOnClickListener(this);
        b_21dB.setOnClickListener(this);
        b_41dB.setOnClickListener(this);
        b_accu.setOnClickListener(this);
        b_settings.setOnClickListener(this);
        b_startStop.setOnClickListener(this);
    }

    private void changeColorOfSelectedMode(){
        b_normal.setBackgroundColor(colorButtonInactive);
        b_21dB.setBackgroundColor(colorButtonInactive);
        b_41dB.setBackgroundColor(colorButtonInactive);
        b_accu.setBackgroundColor(colorButtonInactive);
        switch (myMode){
            case "normal mode":
                b_normal.setBackgroundColor(colorButtonActiveMode);
                break;
            case "-21 dB":
                b_21dB.setBackgroundColor(colorButtonActiveMode);
                break;
            case "-42 dB":
                b_41dB.setBackgroundColor(colorButtonActiveMode);
                break;
            case "LNA on":
                b_accu.setBackgroundColor(colorButtonActiveMode);
                break;
        }
    }

    public void onClick(View v) {
        DetailView_Packet_Trigger DetViewStop = new DetailView_Packet_Trigger(device_id, attenuator, freq_number, freq, (char) 0);

        switch (v.getId()) {
            case R.id.b_mode_normal:
                attenuator = 0;
                myMode = "normal mode";
                change_MinMaxPlot();
                makePlotRunning = true;
                b_startStop.setText("Stop");
                closeSettingLayoutAndUpdateList();
                sendTrigger(DetViewStop.get_packet());
                DetailView_Packet_Trigger detailView_packet_trigger0 = new DetailView_Packet_Trigger(device_id, attenuator, freq_number, freq, measurement_type);
                sendTrigger(detailView_packet_trigger0.get_packet());
                Toast.makeText(this, "normal", Toast.LENGTH_SHORT).show();
                break;
            case R.id.b_mode_21db:
                attenuator = 1;
                myMode = "-21 dB";
                change_MinMaxPlot();
                makePlotRunning = true;
                b_startStop.setText("Stop");
                closeSettingLayoutAndUpdateList();
                sendTrigger(DetViewStop.get_packet());
                DetailView_Packet_Trigger detailView_packet_trigger1 = new DetailView_Packet_Trigger(device_id, attenuator, freq_number, freq, measurement_type);
                sendTrigger(detailView_packet_trigger1.get_packet());
                Toast.makeText(this, "21 dB", Toast.LENGTH_SHORT).show();
                break;
            case R.id.b_mode_42db:
                attenuator = 2;
                myMode = "-42 dB";
                change_MinMaxPlot();
                makePlotRunning = true;
                b_startStop.setText("Stop");
                closeSettingLayoutAndUpdateList();
                sendTrigger(DetViewStop.get_packet());
                DetailView_Packet_Trigger detailView_packet_trigger2 = new DetailView_Packet_Trigger(device_id, attenuator, freq_number, freq, measurement_type);
                sendTrigger(detailView_packet_trigger2.get_packet());
                Toast.makeText(this, "42 dB", Toast.LENGTH_SHORT).show();
                break;
            case R.id.b_mode_LNA:
                attenuator = 3;
                myMode = "LNA on";
                change_MinMaxPlot();
                makePlotRunning = true;
                b_startStop.setText("Stop");
                closeSettingLayoutAndUpdateList();
                sendTrigger(DetViewStop.get_packet());
                DetailView_Packet_Trigger detailView_packet_trigger3 = new DetailView_Packet_Trigger(device_id, attenuator, freq_number, freq, measurement_type);
                sendTrigger(detailView_packet_trigger3.get_packet());
                Toast.makeText(this, "verstärkt", Toast.LENGTH_SHORT).show();
                break;
            case R.id.setting_button:
                if (settings.getVisibility() == LinearLayout.VISIBLE) {
                    settings.setVisibility(LinearLayout.GONE);
                    visibleList.setVisibility(ListView.VISIBLE);
                } else {
                    settings.setVisibility(LinearLayout.VISIBLE);
                    visibleList.setVisibility(ListView.GONE);
                    changeColorOfSelectedMode();
                }
                break;
            case R.id.startStopButton:
                if(makePlotRunning == true){
                    sendTrigger(DetViewStop.get_packet());
                    makePlotRunning=false;
                    b_startStop.setText("Start");

                }else {
                    DetailView_Packet_Trigger detailView_packet_triggerRun = new DetailView_Packet_Trigger(device_id, attenuator, freq_number, freq, measurement_type);
                    sendTrigger(detailView_packet_triggerRun.get_packet());
                    makePlotRunning=true;
                    b_startStop.setText("Stop");
                }
                break;
        }
    }

    private void activateValueUpdater(){
        counter =0;
        handler = new Handler();
        timer = new Timer();
        runnable = new Runnable(){
            public void run() {
                final int index = counter % freq_number;
                measures.set(index, new LiveMeasure(freq[index], rms[index], peak[index]));
                adapter.notifyDataSetChanged();
                counter++;
            }
        };
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                handler.post(runnable);
                    }
        },0,300);
    }

    public void closeSettingLayoutAndUpdateList(){
        settings.setVisibility(LinearLayout.GONE);
        visibleList.setVisibility(ListView.VISIBLE);
        adapter.notifyDataSetChanged();
    }

    public void activateTouch(){
        View touchView = findViewById(R.id.activity_detail_view);
        //views.add(findViewById(R.id.list_live_data));
        //views.add(findViewById(R.id.edittext_new_freq));

        touchView.setOnTouchListener(new View.OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                settings = (LinearLayout) findViewById(R.id.layout_setting);

                final int action = event.getAction();
                switch (action & MotionEvent.ACTION_MASK) {
                    case MotionEvent.ACTION_DOWN: {
                        try {
                            if (settings.getVisibility() != LinearLayout.GONE) {
                                settings.setVisibility(LinearLayout.GONE);
                                visibleList.setVisibility(ListView.VISIBLE);
                            }
                        } catch (NullPointerException e) {
                        }
                        break;
                    }
                }
                return true;
            }
        });
    }



    private void handleClicksOnList() {
        final ListView listView = (ListView) findViewById(R.id.list_live_data);
            //short click on item- go to specific plot
            listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override   //display choosen frequency in toast..
                //from here, go to other activity which shows smaller plot..
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    if(goingToNextActivity==true){
                    LiveMeasure chosenFrequency = measures.get(position);
                    Toast.makeText(DetailViewActivity.this, String.valueOf(chosenFrequency.getFrequency()), Toast.LENGTH_SHORT).show();
                    //go to new activity
                    Intent intent = new Intent(DetailViewActivity.this, TimeLineActivity.class);
                    intent.putExtra("frequency", chosenFrequency.getFrequency());
                    Toast.makeText(DetailViewActivity.this, "given: " + String.valueOf(chosenFrequency.getFrequency()), Toast.LENGTH_SHORT).show();
                    intent.putExtra("myMode", myMode);
                    startActivity(intent);
                    }
                }
            });
    }

    //   ----------------------------------------------------------------------------------------------------


    private void populateListView() {
        ListView list = (ListView) findViewById(R.id.list_live_data);
        list.setAdapter(adapter);
    }

    //fill arrayList with values
    private void populateMeasurements() {
        for(int i = 0; i< freq_number;i++){
            measures.add(new LiveMeasure( freq[i],rms[i], peak[i]));
        }
    }


    // get choosen frequencies from OverViewPlot
    synchronized private void getChoosenFreqFromIntent() {
        Intent intent = getIntent();
        Bundle bundle = intent.getExtras();
        fixedFreq = bundle.getIntegerArrayList(CHOOSENFREQ);
        freq_number = fixedFreq.size();
        for(int i=0;i<freq_number;i++){
            int quicky = 500+ 100*fixedFreq.get(i);
            freq[i]= quicky;   //updateActiveFrequency macht aus übergebener arrayposition die frequenz in GHZ->*1000 = MHz
        }
        //Toast.makeText(this,String.valueOf(freq[3]),Toast.LENGTH_SHORT).show();
        myMode = intent.getStringExtra("MODE");
        if (myMode == "-21 dB")  attenuator = 1;
        else if (myMode == "LNA on")  attenuator = 3;
        else if(myMode == "normal mode")   attenuator = 0;
        else attenuator = 2;
        Toast.makeText(DetailViewActivity.this,myMode,Toast.LENGTH_SHORT).show();
    }


    private void makePlot(int frequency){
            int index = 0;
            for (int i = 0; i < 6; i++) {
                if (frequency == freq[i]) {
                    index = i;
                }
            }
            measures.set(index, new LiveMeasure(readFreq()[index], readRMS()[index], readPeak()[index]));
            Log.i("data Update: ", String.valueOf(readFreq()[index]) + ", " + String.valueOf(readPeak()[index]));
            adapter.notifyDataSetChanged(); //thats where the magic happens.. see class below
    }

    public class MyListAdapter extends ArrayAdapter<LiveMeasure> {

        public MyListAdapter() {
            //super-because i need to call the base class constructor..general constructor to be an instance of this class, and then, we need to tell in witch view we are in..
            //.this gives me the pointer to the class..  then tell how each thing should look like (done with item_view.) and then give the values..
            super(DetailViewActivity.this, R.layout.one_list_item, measures); //as i am in inner class, i have a reverence to my outer class... so i dont need to populate the list...
        }
        @Override   //position in array,
        public View getView(int position, View convertView, ViewGroup parent) {
            //make shure there is a view to work with
            View itemView = convertView;
            if (convertView == null) {
                itemView = getLayoutInflater().inflate(R.layout.one_list_item, parent, false);          //??????????
            }

            //HERE I POPULATE THE UI OF THE LIST
            //find measurement to work with. the object at certain position
            LiveMeasure currentMeasure = measures.get(position);

            //Fill the view, connect elements to layout item_view..
            // ImageView listImage = (ImageView) itemView.findViewById(R.id.list_icon);    //findview on this specific itemView..is new for every new list layer..
            // listImage.setImageResource(R.drawable.radar);


            //Fill the text views
            //set frequency
            TextView freqText = (TextView) itemView.findViewById(R.id.textview_freq);
            //double quicky = currentMeasure.getFrequency()*0.001;
            freqText.setText(String.valueOf(GHz(currentMeasure.getFrequency())) + " GHz");
            freqText.setWidth((int) textViewSize);
            //freqText.setMaxWidth((int)textViewSize);

            //set median
            TextView rmsBar = (TextView) itemView.findViewById(R.id.textview_rms);
            TextView rmsText = (TextView) itemView.findViewById(R.id.show_rms);
            rmsText.setWidth((int)barWidthMax);
            if(currentMeasure.getRMS()<-1){
                if(currentMeasure.getRMS()<-2.5){
                    rmsText.setText(" < min");
                    rmsBar.setBackgroundColor(colorEmpty);
                }else{
                    rmsText.setText(" > max");
                    rmsBar.setBackgroundColor(colorLimit);
                    rmsBar.setWidth((int) getMySizeComparedToMax(5500,'R'));    //for shure to high..
                }
            }else{
                rmsText.setText(String.valueOf(roundDouble(currentMeasure.getRMS())) + " V/m");
                rmsBar.setBackgroundColor(colorBar);
                //rmsBar.setWidth((int) getMySizeComparedToMax(5500,'R'));    //for shure to high..
                rmsBar.setWidth( (int) getMySizeComparedToMax(currentMeasure.getRMS(),'R'));
            }


            //set peak
            TextView peakBar = (TextView) itemView.findViewById(R.id.textview_peak);
            TextView peakText = (TextView) itemView.findViewById(R.id.show_peak);
            peakText.setWidth((int)barWidthMax);
            if(currentMeasure.getPeak()<-1){
                if(currentMeasure.getPeak()<-2.5){
                    peakText.setText(" < min");
                    peakBar.setBackgroundColor(colorEmpty);
                }else{
                    peakText.setText(" > max");
                    peakBar.setBackgroundColor(colorLimit);
                    peakBar.setWidth((int) getMySizeComparedToMax(5500,'P'));
                }
            }else{
                peakText.setText(String.valueOf(roundDouble(currentMeasure.getPeak())) + " V/m");
                peakBar.setBackgroundColor(colorBar);
                //peakBar.setWidth((int) getMySizeComparedToMax(5500,'P'));
                peakBar.setWidth((int) getMySizeComparedToMax(currentMeasure.getPeak(),'P'));
            }
            return itemView;
        }
    }


    public void sendTrigger(byte[] TriggerPack) {
        Intent intent = new Intent();
        intent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
        intent.setAction(CommunicationService.ACTION_FROM_ACTIVITY);
        intent.putExtra(CommunicationService.TRIGGER_Act2Serv, TriggerPack);
        sendBroadcast(intent);
    }

    private double GHz(int MHz) {
        double toShow = MHz/1000.0;
        //toShow = Math.round(toShow * 10);  // runden auf ##.#
       // toShow = toShow / 10;
        return toShow;
    }

    private double roundDouble(double toRound){
        double output = toRound*100;
        output = round(output);
        output = output/100.0;
        return output;
    }

    private synchronized void updateFreq_number(int newFREQNUMB){
        freq_number = newFREQNUMB;
    }


    private synchronized void updatePeak(double newPeak, int freq_in){
        int index=0;
        for(int i=0; i<6; i++){
            if(freq_in == freq[i]){index=i;}
        }
        // int i = Arrays.binarySearch(freq, freq_in);
        if (index < peak.length && index >= 0) peak[index] = newPeak;
    }

    private synchronized void updateRMS(double newRMS, int freq_in){
        int index=0;
        for(int i=0; i<6; i++){
            if(freq_in == freq[i]){index=i;}
        }
        if (index < rms.length && index >= 0) rms[index] = newRMS;
    }

    public synchronized int readFreq_number(){
        return freq_number;
    }

    public synchronized double[] readPeak(){
        return peak;
    }

    public synchronized double[] readRMS(){
        return rms;
    }

    public synchronized int[] readFreq(){
        return freq;
    }

    protected byte[] split_packet (int start, int end, byte[] packet){

        int length = end - start + 1;
        byte[] splitted = new byte[length];
        for (int i = 0; i < length; i++){
            splitted[i] = packet[i + start];
        }

        return splitted;
    }

    public class DetailViewActivityReceiver extends BroadcastReceiver {
        final String LOG_TAG;

        public DetailViewActivityReceiver(String LOG_TAG){
            this.LOG_TAG = LOG_TAG;
        }
        @Override
        public void onReceive(Context arg0, Intent data) {
            Log.d(LOG_TAG, "MyActivityReceiver in onReceive");
            byte[] orgData = data.getByteArrayExtra(CommunicationService.DATA_BACK);
            if (orgData != null) {
                if(new String(split_packet(4, 7, orgData)).equals("CALD")) {
                    Cal_Packet_Exposi cal_packet_exposi = new Cal_Packet_Exposi(orgData);
                    device_id = cal_packet_exposi.get_device_id();
                    calibration = new Activity_Superclass(orgData);
                    change_MinMaxPlot();
                    Log.d(LOG_TAG, "saved Calibration Tables");

                    DetailView_Packet_Trigger detailView_packet_trigger = new DetailView_Packet_Trigger(device_id, attenuator, freq_number, freq, measurement_type);
                    sendTrigger(detailView_packet_trigger.get_packet());
                    Log.d(LOG_TAG, "sent DETV Trigger");

                }
                else if(new String(split_packet(4, 7, orgData)).equals("DETV")){

                    Log.d(LOG_TAG, "got DETV data");
                    Data_Packet_Exposi packetExposi = new Data_Packet_Exposi(orgData);
                    int freq = packetExposi.get_frequency();
                    int rms_exposi = packetExposi.get_rawData_rms();
                    int peak_exposi = packetExposi.get_rawData_peak();
                    double rms = calibration.get_rms(attenuator,freq, rms_exposi);
                    double peak = calibration.get_peak(attenuator, freq, peak_exposi);

                    if (peak != -1){
                        updatePeak(peak, freq);
                    }
                    if(rms != -1){
                        updateRMS(rms, freq);

                    }
                    makePlot(freq);
                }
                else if (new String(split_packet(4, 7, orgData)).equals("EROR")){

                    Log.d(LOG_TAG, "got EROR packet");
                    Error_Packet_Exposi error_packet = new Error_Packet_Exposi(orgData);
                    int errorCode = error_packet.get_errorCode();
                    String errorMessage = error_packet.get_errorMessage();

                    if (errorCode == 1){
                        //connection to ESP lost
                        ConnectionLostDropDown(0);
                    }
                }
            }
        }
    }

    private void StartService() {
        Intent intent = new Intent(this, CommunicationService.class);
        startService(intent);
    }

    private void RequestCALD(){
        Log.d(LOG_TAG, "Requested Calipack");
        Intent intent = new Intent();
        intent.setAction(CommunicationService.ACTION_FROM_ACTIVITY);
        intent.putExtra(CommunicationService.COMMAND_Act2Serv, CommunicationService.CMD_getCALI);
        sendBroadcast(intent);
    }

    public void change_MinMaxPlot() {
        maxPlotP = calibration.get_maxPlot(attenuator, 'P');
        minPlotP = calibration.get_minPlot(attenuator, 'P');
        maxPlotR = calibration.get_maxPlot(attenuator, 'R');
        minPlotR = calibration.get_minPlot(attenuator, 'R');
    }

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
