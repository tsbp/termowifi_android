package com.voodoo.termowifi;

import android.app.Activity;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.TimePicker;
import java.util.Calendar;


public class PeroidConfig extends Activity /*implements OnClickListener*/ {


    SeekBar  tmpSeekBar;
    TextView etTime;
    TextView etTemp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_peroid_config);



        etTime = (TextView)findViewById(R.id.setTime);
        etTemp = (TextView)findViewById(R.id.setTemp);


        tmpSeekBar = (SeekBar) findViewById(R.id.seekBar);

        Intent intent = getIntent();

        String time = intent.getStringExtra("pTime");
        String temp = intent.getStringExtra("pTemp");

        etTime.setText(time);
        etTemp.setText(temp);

        tmpSeekBar.setProgress((int) (((Float.valueOf(etTemp.getText().toString())) - 19) * 10));

        etTime.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                Calendar mcurrentTime = Calendar.getInstance();
                int hour = mcurrentTime.get(Calendar.HOUR_OF_DAY);
                int minute = mcurrentTime.get(Calendar.MINUTE);
                TimePickerDialog mTimePicker;
                mTimePicker = new TimePickerDialog(PeroidConfig.this, new TimePickerDialog.OnTimeSetListener() {
                    @Override
                    public void onTimeSet(TimePicker timePicker, int selectedHour, int selectedMinute) {
                        etTime.setText(String.format("%02d",selectedHour) + ":" + String.format("%02d",selectedMinute));
                    }
                }, hour, minute, true);//Yes 24 hour time
                mTimePicker.setTitle("Select Time");
                mTimePicker.show();
            }
        });

        tmpSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                String s = String.format("%2.1f", (0.1 * progress + 19));
                s = s.replace(',','.');
                etTemp.setText( s );
            }
        });

//        Button okBtn = (Button) findViewById(R.id.okBtnPeriod);
//        okBtn.setOnClickListener(new View.OnClickListener() {
//            public void onClick(View v) {
//
//                Intent intent = new Intent();
//                intent.putExtra("rTime", etTime.getText().toString());
//                intent.putExtra("rTemp", etTemp.getText().toString());
//                setResult(RESULT_OK, intent);
//                finish();
//
//            }
//        });
    }



}
