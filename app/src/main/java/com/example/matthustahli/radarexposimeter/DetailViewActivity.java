package com.example.matthustahli.radarexposimeter;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.graphics.Point;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.LogRecord;

import static java.lang.Math.log;

public class DetailViewActivity extends AppCompatActivity implements View.OnClickListener {

    private final String CHOOSENFREQ = "my_freq";
    ArrayList<Integer> fixedFreq= new ArrayList<Integer>();
    private ArrayAdapter<LiveMeasure> adapter;
    Integer peak[]= {302, 400, 6000, 100, 191, 305, 256, 385, 119, 403, 304, 252, 152, 243, 254, 276, 131, 312, 116, 337, 457, 251, 330, 314, 201, 107, 235, 280, 470, 460, 394, 418, 378, 437, 260, 130, 449, 446, 277, 182, 240, 147, 316, 184, 350, 466, 441, 328, 411, 166, 127, 471, 248, 112, 226, 426, 319, 358, 149, 115, 408, 172, 436, 476, 361, 266, 366, 202, 375, 151, 171, 207, 106, 103, 224, 110, 410, 258, 297, 307, 209, 211, 262, 292, 370, 405, 417, 170, 220, 444, 176, 331, 190, 406, 430, 416, 494, 387, 348, 431, 246, 117, 145, 393, 129, 100, 447, 490, 404, 175, 395, 125, 478, 198, 159, 354, 452, 360, 162, 114, 433, 272, 222, 264, 458, 349, 329, 270, 438, 309, 100};
    Integer rms[]= {4000, 1, 200, 3000, 400, 200, 100, 10, 371, 217, 126, 201, 118, 121, 199, 316, 310, 115, 361, 213, 196, 173, 114, 152, 480, 300, 285, 146, 194, 278, 353, 102, 179, 296, 182, 192, 272, 347, 407, 161, 448, 207, 256, 240, 253, 472, 153, 424, 323, 266, 185, 344, 484, 423, 134, 349, 209, 321, 269, 198, 302, 414, 254, 120, 224, 379, 488, 168, 382, 497, 359, 381, 243, 128, 410, 125, 291, 212, 276, 445, 474, 260, 362, 181, 372, 341, 401, 438, 406, 340, 113, 117, 363, 210, 178, 354, 314, 318, 384, 108, 400, 338, 233, 251, 208, 467, 479, 328, 288, 148, 216, 297, 265, 337, 249, 145, 174, 206, 277, 230, 171, 373, 186, 351, 376, 188, 315, 279, 331, 232, 100};
    private ArrayList<LiveMeasure> measures = new ArrayList<LiveMeasure>();
    Float maxPeak = 0f;
    Float maxRMS = 0f;
    String myMode;
    private int attenuator;
    private int device_id;
    private char measurement_type = 'P';
    Timer timer;
    final String LOG_TAG = "DetailView";
    DetailViewActivityReceiver detailViewActivityReceiver = new DetailViewActivityReceiver(LOG_TAG);
    Activity_Superclass calibration;



    //TODO variablen für verbesserung
    private int[] freq= new int[4];//frequencies are in MHz  //beinhaltet die zu betrachtenden frequenzen    //make switch funktion that deletes element at certain place and reorders them
    private double[] rms1 = new double[4];
    private double[] peak1 = new double[4];
    private int freq_number = 4;        //tells how many freq are active.. the values of those freq are in freq, u to freq_number..




    //Everything about buttons
    ImageButton b_settings;
    Button b_normal, b_21dB, b_41dB, b_accu;
    LinearLayout settings;
    ListView visibleList;

    Runnable runnable;
    Handler handler;
    int counter=0;
    int size;
    double maxPlot, minPlot;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail_view);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        adapter=new MyListAdapter();

        //load intent from previous activity
        getChoosenFreqFromIntent();
        //putFrequenciesIntoDoubleArray();

        //everything with the list population and keeping it updated
        populateMeasurements();
        populateListView();             // this plots the data to the layout
        handleClicksOnList();

        //activate all Buttons and listeners
        initializeButtons();
        setButtonsOnClickListener();
        activateTouch();
        activateEditText();
        activateAddButton();
        activateValueUpdater(); // funktion von matthias für listenupdate alle x sec..

       // Toast.makeText(this,String.valueOf(freq),Toast.LENGTH_SHORT).show();










        // // TODO: 27.10.16 data from remo
    /*
    make clock for update.
    DetailView liveValues = new DetailView(array with frequencies);
    liveValues.peak    -- gets my an array with the values to the frequencies in the same order.
   liveValues.rms
     */

    }



//ArrayList<ObjectName> arraylist  = extras.getParcelableArrayList("arraylist");

//TODO save instance when leaving and comming back.
/*
  private void saveToSharedPref(ArrayList<Integer> toSave){
        SharedPreferences sp = getSharedPreferences("storedFrequencies", MODE_PRIVATE); //PreferenceManager.getDefaultSharedPreferences(SaveMainActivity.this);
        SharedPreferences.Editor  edit= sp.edit();
        //edit.putString("ArraySize", String.valueOf(fixedFreq.size()));
        for (int i=0; i < toSave.size();i++) {
            edit.putInt(String.valueOf(i), toSave.get(i));
        }
        edit.commit();
    }

    private void loadFromSharedPref(){
        SharedPreferences sp = getSharedPreferences("myValue", MODE_PRIVATE); //PreferenceManager.getDefaultSharedPreferences(SaveMainActivity.this);
        //String size = sp.getString("ArraySize", "0");
        fixedFreq.clear();

        for (int i=0; i<8 ;i++){
            fixedFreq.add(sp.getInt(String.valueOf(i), 0));
            Log.d("ArraySavedShow "+String.valueOf(i)+" : ",String.valueOf(fixedFreq.get(i)));
        }
        for(int i=8;i>0; i--){
            if(fixedFreq.get(i-1) == 0){
                Log.d("ArraySavedShowremove "+String.valueOf(i-1)+" : ",String.valueOf(fixedFreq.get(i-1)));
                fixedFreq.remove(i-1);
            }
            else{Log.d("ArraySavedshowClear "+String.valueOf(i-1)+" : ",String.valueOf(fixedFreq.get(i-1)));}
        }
    }

*/

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

    public Integer modeMaxSize(){
        int maxSizeInVolt=0;
        switch (myMode){
            case "normal mode":
                maxSizeInVolt=50;
                break;
            case "-21 dB":
                maxSizeInVolt=500;
                break;
            case "-42 dB":
                maxSizeInVolt=5000;
                break;
            case "LNA on":
                maxSizeInVolt=5;
                break;
        }
        return maxSizeInVolt;
    }

    public float getMySizeComparedToMax(Integer myValueIn){
        if(myValueIn<=1){ return 0;}
        double maxValue= log(modeMaxSize());
        double myValue = log(myValueIn);
        double returnSize;
        if(maxValue<=myValue){myValue=maxValue;}
        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        float barWidth=size.x/3;

        returnSize = (myValue/maxValue)*barWidth;
        //Log.d("returnSize", String.valueOf(returnSize));
        return (float) returnSize;
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
        visibleList = (ListView) findViewById(R.id.list_live_data);
    }

    private void setButtonsOnClickListener() {
        b_normal.setOnClickListener(this);
        b_21dB.setOnClickListener(this);
        b_41dB.setOnClickListener(this);
        b_accu.setOnClickListener(this);
        b_settings.setOnClickListener(this);
    }

    public void onClick(View v) {

        switch (v.getId()) {
            case R.id.b_mode_normal:
                myMode = "normal mode";
                closeSettingLayoutAndUpdateList();
                Toast.makeText(this, "normal", Toast.LENGTH_SHORT).show();
                break;
            case R.id.b_mode_21db:
                myMode = "-21 dB";
                closeSettingLayoutAndUpdateList();
                Toast.makeText(this, "21 dB", Toast.LENGTH_SHORT).show();
                break;
            case R.id.b_mode_42db:
                myMode = "-42 dB";
                closeSettingLayoutAndUpdateList();
                Toast.makeText(this, "42 dB", Toast.LENGTH_SHORT).show();
                break;
            case R.id.b_mode_LNA:
                myMode = "LNA on";
                closeSettingLayoutAndUpdateList();
                Toast.makeText(this, "verstärkt", Toast.LENGTH_SHORT).show();
                break;
            case R.id.setting_button:
                if (settings.getVisibility() == LinearLayout.VISIBLE) {
                    settings.setVisibility(LinearLayout.GONE);
                    visibleList.setVisibility(ListView.VISIBLE);
                } else {
                    settings.setVisibility(LinearLayout.VISIBLE);
                    visibleList.setVisibility(ListView.GONE);
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
                size = measures.size();
                final int index = counter % size;

                measures.set(index, new LiveMeasure(fixedFreq.get(index), 0, rms[counter % rms.length], peak[counter % peak.length]));
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
                LiveMeasure chosenFrequency = measures.get(position);
                Toast.makeText(DetailViewActivity.this, String.valueOf(chosenFrequency.getFrequency()), Toast.LENGTH_SHORT).show();
                //go to new activity
                Intent intent = new Intent(DetailViewActivity.this, TimeLineActivity.class);
                intent.putExtra("frequency" ,chosenFrequency.getFrequency());
                intent.putExtra("myMode",myMode);
                intent.putExtra("type", measurement_type);
                startActivity(intent);
            }
        });


        //todo, update bar size
        //long click on iten- mark to be deleted
        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick( AdapterView<?> parent, View view, int position, long id) {

                //option 1. relode list activity
                measures.remove(position);          //take from list
                //option 2. remove from data and from adapter
                adapter.notifyDataSetChanged();     //update list
                return true;        //true means, i  have handled the event and it should stop here.. if i put false, it will trigger a normal click when removing my finger.
            }
        });
    }




    //   ------------------------add new freq to list section----------------------------
//lets me add frequency to my listview
    private void activateEditText(){
        EditText editText = (EditText) findViewById(R.id.edittext_new_freq);
        editText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                ImageButton b_add_freq = (ImageButton) findViewById(R.id.b_add_freq_to_list);
                b_add_freq.setVisibility(ImageButton.VISIBLE);
            }
        });

    }

    //adds the new freq to my list
    private void activateAddButton() {

        final ImageButton b_add_freq = (ImageButton) findViewById(R.id.b_add_freq_to_list);
        b_add_freq.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                EditText editText = (EditText) findViewById(R.id.edittext_new_freq);
                if(editText.length()!=0){
                    int freq = Integer.parseInt(editText.getText().toString());
                    //close if textview is empty or not in range
                    if(0<= freq && freq<=130){
                        measures.add(new LiveMeasure(freq,0,rms[freq],peak[freq]));
                        adapter.notifyDataSetChanged();
                    }else{
                        Toast.makeText(DetailViewActivity.this,"OUT OF RANGE", Toast.LENGTH_SHORT).show();}
                }
                editText.getText().clear();
                editText.clearFocus();
                InputMethodManager inputManager = (InputMethodManager) DetailViewActivity.this.getSystemService(Context.INPUT_METHOD_SERVICE);
                inputManager.hideSoftInputFromWindow(DetailViewActivity.this.getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
                b_add_freq.setVisibility(ImageButton.GONE);
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
        for(int i = 0; i< fixedFreq.size();i++){
            int freq = fixedFreq.get(i);
            measures.add(new LiveMeasure( freq ,0,rms[freq], peak[freq]));
        }
    }


    // get choosen frequencies from OverViewPlot
    private void getChoosenFreqFromIntent() {
        Intent intent = getIntent();
        Bundle bundle = intent.getExtras();
        fixedFreq = bundle.getIntegerArrayList(CHOOSENFREQ);
        //String hello = fixedFreq.toString();
        //Toast.makeText(DetailViewActivity.this, hello, Toast.LENGTH_SHORT).show();
        //get my mode
        myMode = intent.getStringExtra("MODE");
        if (myMode == "-21 dB")  attenuator = 1;
        else if (myMode == "LNA on")  attenuator = 3;
        else if(myMode == "normal mode")   attenuator = 0;
        else attenuator = 2;
        measurement_type = intent.getCharExtra("type", 'P');
        Toast.makeText(DetailViewActivity.this,myMode,Toast.LENGTH_SHORT).show();
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
            freqText.setText(String.valueOf(updateActiveFrequency(currentMeasure.getFrequency())) + " GHz");

            //set median
            TextView rmsBar = (TextView) itemView.findViewById(R.id.textview_rms);
            TextView rmsText = (TextView) itemView.findViewById(R.id.show_rms);
            if(currentMeasure.getRMS()<-1){
                if(currentMeasure.getRMS()<-2.5){
                    rmsText.setText(" < min");
                }else{
                    rmsText.setText(" > max");
                }
            }else{
                rmsText.setText(String.valueOf(currentMeasure.getRMS()) + " V/m");
            }
            rmsBar.setWidth((int) getMySizeComparedToMax(currentMeasure.getRMS()));


            //set peak
            TextView peakBar = (TextView) itemView.findViewById(R.id.textview_peak);
            TextView peakText = (TextView) itemView.findViewById(R.id.show_peak);
            if(currentMeasure.getPeak()<-1){
                if(currentMeasure.getPeak()<-2.5){
                    peakText.setText(" < min");
                }else{
                    peakText.setText(" > max");
                }
            }else{
                peakText.setText(String.valueOf(currentMeasure.getPeak()) + " V/m");
            }
            peakBar.setWidth((int) getMySizeComparedToMax(currentMeasure.getPeak()));

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

    private double updateActiveFrequency(int position) {
        double toShow = 0.5 + 0.1 * position;
        toShow = Math.round(toShow * 10);  // runden auf ##.#
        toShow = toShow / 10;
        return toShow;
    }

    private synchronized void updateFreq_number(int newFREQNUMB){
        freq_number = newFREQNUMB;
    }


    private synchronized void updatePeak(double newPeak, int freq_in){
        int i = Arrays.binarySearch(freq, freq_in);
        if (i < peak1.length && i >= 0) peak1[i] = newPeak;
    }

    private synchronized void updateRMS(double newRMS, int freq_in){
        int i = Arrays.binarySearch(freq, freq_in);
        if (i < rms1.length && i >= 0)rms1[i] = newRMS;
    }

    public synchronized int readFreq_number(){
        return freq_number;
    }

    public synchronized double[] readPeak(){
        return peak1;
    }

    public synchronized double[] readRMS(){
        return rms1;
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
                    Log.d(LOG_TAG, "saved Calibration Tables");

                    DetailView_Packet_Trigger detailView_packet_trigger = new DetailView_Packet_Trigger(device_id, attenuator, freq, measurement_type);
                    sendTrigger(detailView_packet_trigger.get_packet());
                    Log.d(LOG_TAG, "sent SCAN Trigger");

                }
                else if(new String(split_packet(4, 7, orgData)).equals("DETV")){

                    Log.d(LOG_TAG, "got DETV data");
                    Data_Packet_Exposi packetExposi = new Data_Packet_Exposi(orgData);
                    int freq = packetExposi.get_frequency();
                    int rms_exposi = packetExposi.get_rawData_rms();
                    int peak_exposi = packetExposi.get_rawData_peak();

                    double rms = calibration.get_rms(attenuator,freq, rms_exposi);
                    double peak = calibration.get_peak(attenuator, freq, peak_exposi);
                    updatePeak(peak, freq);
                    updateRMS(rms, freq);
                    //TODO: makePlot();
                }
                else if (new String(split_packet(4, 7, orgData)).equals("EROR")){

                    Log.d(LOG_TAG, "got EROR packet");
                    Error_Packet_Exposi error_packet = new Error_Packet_Exposi(orgData);
                    int errorCode = error_packet.get_errorCode();
                    String errorMessage = error_packet.get_errorMessage();
                    if (errorCode == 1){
                        //connection to ESP lost
                        //handlesActivatingDropDown(0);
                    }
                    else if (errorCode == 2){
                        //handlesActivatingDropDown(1);
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
}
