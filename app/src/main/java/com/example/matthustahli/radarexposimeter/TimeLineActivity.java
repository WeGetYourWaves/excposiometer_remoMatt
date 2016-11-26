package com.example.matthustahli.radarexposimeter;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
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

public class TimeLineActivity extends AppCompatActivity implements View.OnClickListener {

    Integer counter=0, anzahlBalken, activeBar;
    Button b_modeNormal,b_mode21dB, b_mode42dB,b_mode_accumulation,b_switchMode;
    ImageButton b_settings;
    TextView tv_status;
    LinearLayout settings;
    String myMode;
    Rectangle coord;
    Animation animationSlideDown;
    Paint paint;
    ImageView imageView;
    Bitmap bitmap;
    Canvas canvas;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_time_line);
        initalizeButtonsAndIcons();
        activateOnclickListener();
        startTimeLine();
    }

    private void startTimeLine() {
        //getValuesForPlot();  //(start von jedem der 30 balken)

        //
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
                counter ++;
                if(counter%2==0) {
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
       /* final TextView xCoord = (TextView) findViewById(R.id.coord_x);
        final TextView yCoord = (TextView) findViewById(R.id.coord_y);
        */
        View touchView = findViewById(R.id.activity_overview_scan_plot);
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
                        /*xCoord.setText(String.valueOf((int) event.getX()));
                        yCoord.setText(String.valueOf((int) event.getY()));*/
                        int position = returnPosition((int) event.getX());
                        changeBarColorToActiv(position);
                        break;
                    }
                    case MotionEvent.ACTION_MOVE: {
                        /*xCoord.setText("x: " + String.valueOf((int) event.getX()));
                        yCoord.setText("y: " + String.valueOf((int) event.getY()));*/
                        int position = returnPosition((int) event.getX());
                        changeBarColorToActiv(position);
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
        int color = TimeLineActivity.this.getResources().getColor(R.color.activeBar);
        paint.setColor(color);
        canvas.drawRect(coord.getLeft(position), coord.getTop(position), coord.getRight(position), coord.getBottom(position), paint);
        imageView.setImageBitmap(bitmap);
    }

    //changes Bar back to gray color
    private void changeBarColorToNOTactiv(Integer position) {
        paint.setColor(Color.parseColor("#CCCCCC"));
        canvas.drawRect(coord.getLeft(position), coord.getTop(position), coord.getRight(position), coord.getBottom(position), paint);
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
}

