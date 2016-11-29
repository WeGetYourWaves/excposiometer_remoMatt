package com.example.matthustahli.radarexposimeter;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Display;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.ScaleAnimation;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

public class TimeLineActivity extends AppCompatActivity implements View.OnClickListener {

    Integer counterB=0, anzahlBalken, activeBar;
    Button b_modeNormal,b_mode21dB, b_mode42dB,b_mode_accumulation,b_switchMode;
    ImageButton b_settings;
    TextView tv_status;
    LinearLayout settings;
    String myMode;
    Animation animationSlideDown;
    Integer peak[]= {302, 400, 6000, 100, 191, 305, 256, 385, 119, 403, 304, 252, 152, 243, 254, 276, 131, 312, 116, 337, 457, 251, 330, 314, 201, 107, 235, 280, 470, 460, 394, 418, 378, 437, 260, 130, 449, 446, 277, 182, 240, 147, 316, 184, 350, 466, 441, 328, 411, 166, 127, 471, 248, 112, 226, 426, 319, 358, 149, 115, 408, 172, 436, 476, 361, 266, 366, 202, 375, 151, 171, 207, 106, 103, 224, 110, 410, 258, 297, 307, 209, 211, 262, 292, 370, 405, 417, 170, 220, 444, 176, 331, 190, 406, 430, 416, 494, 387, 348, 431, 246, 117, 145, 393, 129, 100, 447, 490, 404, 175, 395, 125, 478, 198, 159, 354, 452, 360, 162, 114, 433, 272, 222, 264, 458, 349, 329, 270, 438, 309, 100};
    Integer rms[]= {4000, 1, 200, 3000, 400, 200, 100, 10, 371, 217, 126, 201, 118, 121, 199, 316, 310, 115, 361, 213, 196, 173, 114, 152, 480, 300, 285, 146, 194, 278, 353, 102, 179, 296, 182, 192, 272, 347, 407, 161, 448, 207, 256, 240, 253, 472, 153, 424, 323, 266, 185, 344, 484, 423, 134, 349, 209, 321, 269, 198, 302, 414, 254, 120, 224, 379, 488, 168, 382, 497, 359, 381, 243, 128, 410, 125, 291, 212, 276, 445, 474, 260, 362, 181, 372, 341, 401, 438, 406, 340, 113, 117, 363, 210, 178, 354, 314, 318, 384, 108, 400, 338, 233, 251, 208, 467, 479, 328, 288, 148, 216, 297, 265, 337, 249, 145, 174, 206, 277, 230, 171, 373, 186, 351, 376, 188, 315, 279, 331, 232, 100};


    //variables for timer
    Timer timer;
    Runnable runnable;
    Handler handler;
    int counter=0;
    //int size;

    //variables for plot
    Rectangle coord;
    Display display;
    int colorFix, colorBar, colorActive, colorLimit ;
    double abstandZwischenBalken =5.0; //5dp
    Paint paintFix, paintBar, paintActive, paintLimit;
    ImageView imageView;
    Bitmap bitmap;
    Canvas canvas;
    Point size;

    //TODO variablen für verbesserung
    private char measurement_type = 'P';
    int freq;//frequencies are in MHz  //beinhaltet die zu betrachtenden frequenzen    //make switch funktion that deletes element at certain place and reorders them
    double rms1;
    double peak1;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_time_line);
        getSettingsFromIntent();

        initalizeButtonsAndIcons();
        activateOnclickListener();
        ActivateTouchOnPlot();

        //draws plot, not ready yet
        //startTimeLine();
    }


    public void getSettingsFromIntent() {
        Intent intent = getIntent();
        myMode = intent.getStringExtra("myMode");
        freq = intent.getIntExtra("frequency",0);
        freq = 500 + freq*100;        //freq = value of freq MHz;
    }


    private void startTimeLine() {
        counter =0;
        handler = new Handler();
        timer = new Timer();
        runnable = new Runnable(){
            public void run() {
                //draw plot here!!!
                //final int index = counter % size;
                //measures.set(index, new LiveMeasure(fixedFreq.get(index), 0, rms[counter % rms.length], peak[counter % peak.length]));


                counter++;
            }
        };
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                handler.post(runnable);
            }
        },0,200);  // time when new bar appears.
    }

    public void makePlot() {
        double[] rms = {readRMS()}; //quickfix
        double[] peak = {readPeak()};//quickfix
        canvas.drawColor(Color.WHITE);
        canvas.drawRect(0,(float) (size.y*0.15),size.x,(float) (size.y*0.14),paintLimit);
        imageView.setImageBitmap(bitmap);
        if(measurement_type=='R') {
            coord = new Rectangle(anzahlBalken, abstandZwischenBalken, size.x, size.y, rms, myMode,0.95,0.85);
        }else {
            coord = new Rectangle(anzahlBalken, abstandZwischenBalken, size.x, size.y, peak, myMode, 0.95, 0.85);
        }
        for (int i = 0; i < anzahlBalken; i++) {
            canvas.drawRect(coord.getLeft(i), coord.getTop(i), coord.getRight(i), coord.getBottom(i), paintBar);      //somehow i get bottom wrong!
            Log.d("values plot", String.valueOf(coord.getTop(i)));
        }
        imageView.setImageBitmap(bitmap);
    }


    public void SetUpValuesForPlot() {
        display = getWindowManager().getDefaultDisplay();
        size = new Point();
        display.getSize(size);
        imageView = (ImageView) findViewById(R.id.image_bitmap);
        bitmap = Bitmap.createBitmap(size.x, size.y, Bitmap.Config.ARGB_8888);
        paintFix = new Paint();
        paintBar = new Paint();
        paintActive= new Paint();
        paintLimit= new Paint();
        colorFix = TimeLineActivity.this.getResources().getColor(R.color.fixedBar);
        colorBar = TimeLineActivity.this.getResources().getColor(R.color.normalBar);
        colorActive = TimeLineActivity.this.getResources().getColor(R.color.activeBar);
        colorLimit = TimeLineActivity.this.getResources().getColor(R.color.limitBar);
        paintFix.setColor(colorFix);
        paintLimit.setColor(colorLimit);
        paintFix.setStyle(Paint.Style.FILL);
        paintBar.setColor(colorBar);
        paintBar.setStyle(Paint.Style.FILL);
        paintActive.setColor(colorActive);
        paintActive.setStyle(Paint.Style.FILL);
        canvas = new Canvas(bitmap);
    }

    private void initalizeButtonsAndIcons() {
        b_modeNormal = (Button) findViewById(R.id.b_mode_normal);
        b_mode21dB = (Button) findViewById(R.id.b_mode_21db);
        b_mode42dB = (Button) findViewById(R.id.b_mode_42db);
        b_mode_accumulation = (Button) findViewById(R.id.b_mode_LNA);
        b_settings = (ImageButton) findViewById(R.id.setting_button);
        b_switchMode = (Button) findViewById(R.id.switch_to_peak);
        tv_status = (TextView) findViewById(R.id.tv_status);
        settings = (LinearLayout) findViewById(R.id.layout_setting);
    }

    private void activateOnclickListener() {
        b_modeNormal.setOnClickListener(this);
        b_mode21dB.setOnClickListener(this);
        b_mode42dB.setOnClickListener(this);
        b_mode_accumulation.setOnClickListener(this);
        b_settings.setOnClickListener(this);
        b_switchMode.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.b_mode_normal:
                myMode = "normal";
                break;
            case R.id.b_mode_21db:
                myMode = "21dB";
                break;
            case R.id.b_mode_42db:
                myMode = "42dB";
                break;
            case R.id.b_mode_LNA:
                myMode = "accu";
                break;
            case R.id.setting_button:
                if (settings.getVisibility() == LinearLayout.VISIBLE) {
                    settings.setVisibility(LinearLayout.GONE);
                } else {
                    settings.setVisibility(LinearLayout.VISIBLE);
                }
                break;
            case R.id.switch_to_peak:
                counterB ++;
                if(counterB%2==0) {
                    tv_status.setText("Peak");
                    b_switchMode.setText("RMS");
                }else{
                    tv_status.setText("RMS");
                    b_switchMode.setText("Peak");
                }
                break;
        }
    }

    public void ActivateTouchOnPlot() {

        View touchView = findViewById(R.id.activity_time_line);
        touchView.setOnTouchListener(new View.OnTouchListener() {

            public boolean onTouch(View v, MotionEvent event) {
                final int action = event.getAction();
                switch (action & MotionEvent.ACTION_MASK) {
                    case MotionEvent.ACTION_DOWN: {
                        try {
                            if (settings.getVisibility() != LinearLayout.GONE) {
                                settings.setVisibility(LinearLayout.GONE);
                            }
                        } catch (NullPointerException e) {}
                        //int position = returnPosition((int) event.getX());
                        //changeBarColorToActiv(position);
                        break;
                    }
                    case MotionEvent.ACTION_MOVE: {
                        //int position = returnPosition((int) event.getX());
                        //changeBarColorToActiv(position);
                        break;
                    }
                }
                return true; //true= we handled the event!!
            }
        });
    }

    //gets position from coordinates
    public int returnPosition(int x) {
        int i = 0;
        int fromLeftToRight = (int) coord.getLeft(i);
        while (fromLeftToRight < x) {
            if (i == anzahlBalken) {        //boundary condition on right edge
                return i;
            }
            i++;
            fromLeftToRight = (int) coord.getLeft(i);
        }
        if (i <= 0) {       //boundary condition on left edge
            return 0;
        }
        return i - 1;
    }

    //change color of only one bar and sets up textview
    public void changeBarColorToActiv(int position) {
        //desactivate last visited bar
        changeBarColorToNOTactiv(activeBar);
        activeBar = position;       //with this position we can also use the add button to put it in a list!
        //update textview
        //set color to active
        canvas.drawRect(coord.getLeft(position), coord.getTop(position), coord.getRight(position), coord.getBottom(position), paintActive);
        imageView.setImageBitmap(bitmap);
    }

    //changes Bar back to gray color
    private void changeBarColorToNOTactiv(Integer position) {
        canvas.drawRect(coord.getLeft(position), coord.getTop(position), coord.getRight(position), coord.getBottom(position), paintBar);
        imageView.setImageBitmap(bitmap);
    }

    //handles the allert bar, for example when connection is lost.
    private void opensLostConnection( Integer downOrUp) {
        //sets listener, and handles drop down and drop up
        animationSlideDown = AnimationUtils.loadAnimation(this, R.anim.anim_drop_down);
        final LinearLayout layout_dropDown = (LinearLayout) findViewById(R.id.layout_dropDown);
        TextView allert_text = (TextView) findViewById(R.id.textView_dropDownAllert);
        if(downOrUp==0){
            layout_dropDown.setVisibility(View.VISIBLE);
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


    private synchronized void updatePeak(double newPeak){
        peak1 = newPeak;
    }

    private synchronized void updateRMS(double newRMS){
        rms1 = newRMS;
    }

    public synchronized double readPeak(){
        return peak1;
    }

    public synchronized double readRMS(){
        return rms1;
    }

    public synchronized int readFreq(){
        return freq;
    }
}

