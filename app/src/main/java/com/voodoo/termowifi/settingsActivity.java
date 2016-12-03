package com.voodoo.termowifi;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.TimePicker;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class settingsActivity extends Activity implements UDPProcessor.OnReceiveListener {

    UDPProcessor udpProcessor;
    private ProgressBar pb;
    private TextView dayType, tvResp;
    TextView delta;

    private ListView lvMain;
    private List<String> aStrings = new ArrayList<>();
    private List<String> bStrings = new ArrayList<>();
    enum cMode {mWeek, mWork, mHolly, mHyst};

    Button bSave, bAdd;

    cMode curMode;
    int currentPeroid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        udpProcessor = new UDPProcessor(7777);
        udpProcessor.start();
        udpProcessor.setOnReceiveListener(this);

        dayType = (TextView) findViewById(R.id.dayType);
        lvMain = (ListView) findViewById(R.id.lvMain);

        pb = (ProgressBar)findViewById(R.id.pbConfig);
        //delta = (TextView)(findViewById(R.id.delta_value));

        bSave = (Button) findViewById(R.id.btnSave);
        bAdd  = (Button) findViewById(R.id.btnAdd);
        final Button bHyst  = (Button) findViewById(R.id.btnDelta);

        Button bLoad = (Button) findViewById(R.id.btnLoad);
        Button bLoadHolly = (Button) findViewById(R.id.btnLoadHolly);

        Button bWeek = (Button) findViewById(R.id.btnWeek);
        //================================================
        bWeek.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                curMode = cMode.mWeek;
                bAdd.setVisibility(View.INVISIBLE);
                bSave.setVisibility(View.INVISIBLE);
                pb.setVisibility(View.VISIBLE);
                dayType.setText("Неделя");

                byte[] udpRequestBuf = new byte[1];
                udpRequestBuf[0] = Protocol.READ_WEEK_CONFIGS;
                udpRequest(udpRequestBuf);
            }
        });
        //================================================
        bLoad.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                curMode = cMode.mWork;
                pb.setVisibility(View.VISIBLE);
                bSave.setVisibility(View.INVISIBLE);
                dayType.setText("Рабочий день");
                getHolly = false;
                requestDayCfg((byte)0x01);
         }
        });
        //================================================
        bLoadHolly.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                curMode = cMode.mHolly;
                pb.setVisibility(View.VISIBLE);
                bSave.setVisibility(View.INVISIBLE);
                dayType.setText("Выходной");
                getHolly = true;
                requestDayCfg((byte)0x81);
            }
        });
        //================================================
        bAdd.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if(time == null)
                {
                    time = new String[0];
                    temp = new String[0];
                }
                if(time.length < 9)
                {
                    List<String> tmpTime = new ArrayList<>();
                    List<String> tmpTemp = new ArrayList<>();
                    if(time != null)
                        for(int k = 0; k < time.length; k++)
                        {
                            tmpTime.add(time[k]);
                            tmpTemp.add(temp[k]);
                        }
                    tmpTime.add("23:59");
                    tmpTemp.add("19.0");

                    time = new String[tmpTime.size()];
                    temp = new String[tmpTemp.size()];

                    tmpTime.toArray(time);
                    tmpTemp.toArray(temp);
                    sortByTime();
                    updateListviewTemperature();
                }
                bSave.setVisibility(View.VISIBLE);
            }
        });
        //================================================
        bSave.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                switch(curMode)
                {
                    case mWeek:
                        pb.setVisibility(View.VISIBLE);
                        udpRequestBuf = new byte[8];
                        udpRequestBuf = new byte[8];
                        udpRequestBuf[0] = Protocol.SAVE_WEEK_CONFIGS;
                        for(int i = 0; i < 7; i++)
                            udpRequestBuf[i+1] = weekString.getBytes()[i];
                        udpRequest(udpRequestBuf);
                        break;

                    case mWork:
                    case mHolly:
                        pb.setVisibility(View.VISIBLE);
                        currentPeroid = 1;
                        udpRequestBuf = new byte[8];
                        udpRequestBuf[0] = Protocol.SAVE_DAY_CONFIGS;
                        formBuffer();
                        udpRequest(udpRequestBuf);
                        break;
                }
            }
        });
        //================================================
        bHyst.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                curMode = cMode.mHyst;
                udpRequestBuf = new byte[1];
                udpRequestBuf[0] = Protocol.READ_USTANOVKI;
                udpRequest(udpRequestBuf);
            }
        });
    }
    //==============================================================================================
    public void Dialog_period()
    {
        final AlertDialog.Builder popDialog = new AlertDialog.Builder(this);
        final LayoutInflater inflater = (LayoutInflater) this.getSystemService(LAYOUT_INFLATER_SERVICE);
        final View Viewlayout = inflater.inflate(R.layout.activity_peroid_config, (ViewGroup) findViewById(R.id.rl));

        final TextView pTime = (TextView)Viewlayout.findViewById(R.id.setTime);
        pTime.setText(time[selectedRow]);
        pTime.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Calendar mcurrentTime = Calendar.getInstance();
                int hour = Integer.valueOf(time[selectedRow].substring(0,2));
                int minute = Integer.valueOf(time[selectedRow].substring(3));
                TimePickerDialog mTimePicker;

                mTimePicker = new TimePickerDialog(settingsActivity.this, new TimePickerDialog.OnTimeSetListener() {
                    @Override
                    public void onTimeSet(TimePicker timePicker, int selectedHour, int selectedMinute) {
                        pTime.setText(String.format("%02d",selectedHour) + ":" + String.format("%02d",selectedMinute));
                        bSave.setVisibility(View.VISIBLE);
                    }
                }, hour, minute, true);//Yes 24 hour time
                mTimePicker.setTitle("Начало периода");
                mTimePicker.setIcon(R.drawable.timeicon2);
                mTimePicker.show();
            }
        });

        final TextView pTemp = (TextView)Viewlayout.findViewById(R.id.setTemp);
        pTemp.setText(temp[selectedRow]);

        popDialog.setIcon(R.drawable.timeicon2);
        popDialog.setTitle("Период " + (selectedRow + 1));
        popDialog.setView(Viewlayout);

        SeekBar seek = (SeekBar) Viewlayout.findViewById(R.id.seekBarTemp);
        seek.setProgress((int) (((Float.valueOf(pTemp.getText().toString())) - 19) * 10));
        seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser){
                String s = String.format("%2.1f", (0.1 * progress + 19));
                s = s.replace(',','.');
                pTemp.setText( s );
                bSave.setVisibility(View.VISIBLE);
            }
            public void onStartTrackingTouch(SeekBar arg0) {
            }
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
        popDialog.setPositiveButton("OK",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        time[selectedRow] = pTime.getText().toString();
                        temp[selectedRow] = pTemp.getText().toString();
                        sortByTime();
                        updateListviewTemperature();
                        dialog.dismiss();
                    }
                });
        popDialog.create();
        popDialog.show();
    }
    //==============================================================================================
    public void Dialog_delta()
    {
        final AlertDialog.Builder popDialog = new AlertDialog.Builder(this);
        final LayoutInflater inflater = (LayoutInflater) this.getSystemService(LAYOUT_INFLATER_SERVICE);
        final View Viewlayout = inflater.inflate(R.layout.ustanovki, (ViewGroup) findViewById(R.id.layout_ust));
        delta = (TextView)(findViewById(R.id.delta_value));
        popDialog.setIcon(R.drawable.plot2);
        popDialog.setTitle("Дельта");
        popDialog.setView(Viewlayout);

        final TextView delta = (TextView)Viewlayout.findViewById(R.id.delta_value);

        SeekBar seek = (SeekBar) Viewlayout.findViewById(R.id.seekBar);
        seek.setProgress(deltaValue);
        delta.setText(String.format("%2.1f", (0.025 * deltaValue)));
        seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser){
                String s = String.format("%2.1f", (0.025 * progress));
                s = s.replace(',','.');
                delta.setText( s );
            }
            public void onStartTrackingTouch(SeekBar arg0) {
            }
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
        popDialog.setPositiveButton("OK",
        new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                udpRequestBuf = new byte[3];
                udpRequestBuf[0] = Protocol.SAVE_USTANOVKI;
                udpRequestBuf[1] = Byte.valueOf(delta.getText().toString().replace(".",""));
//                if(swap.isChecked())REQUEST_ACTION[2] = 1;
//                else                REQUEST_ACTION[2] = 0;
                udpRequest(udpRequestBuf);
                pb.setVisibility(View.VISIBLE);
                dialog.dismiss();
            }
        });
        popDialog.create();
        popDialog.show();
    }
    //==============================================================================================
    private void formBuffer()
    {
        if (curMode == cMode.mHolly) udpRequestBuf[1] = (byte)0x80;
        else                         udpRequestBuf[1] = (byte)0x00;
        udpRequestBuf[2] = (byte) currentPeroid;
        udpRequestBuf[3] = (byte) time.length;
        udpRequestBuf[4] = Byte.valueOf(time[currentPeroid-1].substring(0,2));
        udpRequestBuf[5] = Byte.valueOf(time[currentPeroid-1].substring(3,5));

        short shrt = Short.valueOf(temp[currentPeroid-1].substring(0,2) + temp[currentPeroid-1].substring(3,4));
        udpRequestBuf[6] = (byte)(shrt & 0xff);
        udpRequestBuf[7] = (byte)((shrt >> 8) & 0xff);
    }
    //==============================================================================================
    private String[] time;
    private String[] temp;
    private final String ATTRIBUTE_NAME_REF = "ref";
    private final String ATTRIBUTE_NAME_TIME = "time";
    private final String ATTRIBUTE_NAME_TEMP = "temper";
    //==============================================================================================
    private final String wDays[] = {"Понедельник","Вторник","Среда","Четверг","Пятница","Суббота","Воскресенье"};
    private final String ATTRIBUTE_NAME_WDAY = "wday";
    private final String ATTRIBUTE_NAME_IMAGE = "image";
    private char[] day;
    //==============================================================================================
    private void updateListviewWeek(final String aStr)
    {
        int img;
        day = aStr.toCharArray();
        ArrayList<Map<String, Object>> data = new ArrayList<>(
                wDays.length);
        Map<String, Object> m;
        for (int i = 0; i < wDays.length; i++) {
            m = new HashMap<>();
            m.put(ATTRIBUTE_NAME_REF,  i + 1);
            if (day[i] == 'H') img = R.drawable.beer;
            else               img = R.drawable.shovel;
            m.put(ATTRIBUTE_NAME_IMAGE, img);
            m.put(ATTRIBUTE_NAME_WDAY, wDays[i]);
            data.add(m);
        }
        String[] from = {ATTRIBUTE_NAME_REF, ATTRIBUTE_NAME_IMAGE, ATTRIBUTE_NAME_WDAY};
        int[] to = {R.id.tvRefW, R.id.ivDay, R.id.chbDay};
        SimpleAdapter sAdapter = new SimpleAdapter(this, data, R.layout.itemweek, from, to);
        lvMain.setAdapter(sAdapter);
        //==========================================================
        lvMain.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {
                if(day[position] == 'H') day[position] = 'W';
                else                     day[position] = 'H';
                weekString = String.valueOf(day);
                updateListviewWeek(weekString);
                bSave.setVisibility(View.VISIBLE);
            }
        });
    }
    //==============================================================================================
    private void updateListviewTemperature()
    {
        try {
            ArrayList<Map<String, Object>> data = new ArrayList<>(
                    time.length);
            Map<String, Object> m;
            for (int i = 0; i < time.length; i++) {

                m = new HashMap<>();
                m.put(ATTRIBUTE_NAME_REF, i + 1);
                m.put(ATTRIBUTE_NAME_TIME, time[i]);
                m.put(ATTRIBUTE_NAME_TEMP, temp[i]);

                data.add(m);
            }
            String[] from = {ATTRIBUTE_NAME_REF, ATTRIBUTE_NAME_TIME, ATTRIBUTE_NAME_TEMP};
            int[] to = {R.id.tvRef, R.id.tvTime, R.id.tvTemp};
            SimpleAdapter sAdapter = new SimpleAdapter(this, data, R.layout.item, from, to);
            lvMain = (ListView) findViewById(R.id.lvMain);
            lvMain.setAdapter(sAdapter);
            //==========================================================
            lvMain.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                public void onItemClick(AdapterView<?> parent, View view,
                                        int position, long id) {
                    selectedRow = position;
                   // bSave.setVisibility(View.VISIBLE);
                    Dialog_period();
                }
            });
            //==========================================================
            lvMain.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
                public boolean onItemLongClick(AdapterView<?> parent, View view,
                                               final int position, long id) {
                    AlertDialog.Builder adb=new AlertDialog.Builder(settingsActivity.this);
                    adb.setTitle("Delete?");
                    adb.setMessage("Удалить период " + (position+1) + "?");
                    //final int positionToRemove = position;
                    adb.setNegativeButton("Cancel", null);
                    adb.setPositiveButton("Ok", new AlertDialog.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            if (time != null)
                            {
                                List<String> tmpTime = new ArrayList<>();
                                List<String> tmpTemp = new ArrayList<>();
                                for (int k = 0; k < position; k++) {
                                    tmpTime.add(time[k]);
                                    tmpTemp.add(temp[k]);
                                }
                                for (int k = position + 1; k < time.length; k++) {
                                    tmpTime.add(time[k]);
                                    tmpTemp.add(temp[k]);
                                }
                                time = new String[tmpTime.size()];
                                temp = new String[tmpTemp.size()];

                                tmpTime.toArray(time);
                                tmpTemp.toArray(temp);
                                sortByTime();
                                updateListviewTemperature();
                                if(time.length != 0) bSave.setVisibility(View.VISIBLE);
                                else  bSave.setVisibility(View.INVISIBLE);
                            }
                        }});
                    adb.show();
                    return true;
                }
            });
        }
        catch(Exception e){tvResp.setText("Error in data");};
    }
    //==============================================================================================
    int selectedRow;
    //==============================================================================================
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (data == null) {
            return;
        }

        time[selectedRow] =  data.getStringExtra("rTime");
        temp[selectedRow] =  data.getStringExtra("rTemp");
        sortByTime();
        updateListviewTemperature();
    }
    //==============================================================================================
    private void sortByTime()
    {
        try
        {
            for(int i = 0; i < time.length; i++)
            {
                for(int j = i+1; j < time.length; j++)
                {
                    String tStrA = time[i].replace(":","");
                    String tStrB = time[j].replace(":","");
                    if (Integer.valueOf(tStrA) > Integer.valueOf(tStrB))
                    {
                        String t = time[i];
                        time[i] = time[j];
                        time[j] = t;
                        String tt = temp[i];
                        temp[i] = temp[j];
                        temp[j]= tt;
                    }
                }
            }
        }
        catch(Exception e){}

    }
    //==============================================================================================
    boolean getHolly = false;
    byte [] udpRequestBuf;
    //==============================================================================================
    void udpRequest(byte [] aBuf)
    {
        DataFrame df = new DataFrame(aBuf);
        udpProcessor.send(MainActivity.ipMaster,df);
    }
    //==============================================================================================
    void requestDayCfg(byte aParam)
    {
        udpRequestBuf = new byte[2];
        udpRequestBuf[0] = (byte) Protocol.READ_DAY_CONFIGS;
        udpRequestBuf[1] = aParam;
        udpRequest(udpRequestBuf);
    }
    //==============================================================================================
    String weekString = "";
    int deltaValue;
    //==============================================================================================
    public void onFrameReceived(InetAddress ip, IDataFrame frame)
    {
        byte[] in = frame.getFrameData();
        switch (in[0]) {

            case Protocol.READ_USTANOVKI_ANS:
                deltaValue = in[1] * 4;
                Dialog_delta();
                break;

            case Protocol.READ_WEEK_CONFIGS_ANS:
                weekString = new String(in).substring(1,8);
                updateListviewWeek(weekString);
                pb.setVisibility(View.INVISIBLE);
                break;

            case Protocol.READ_DAY_CONFIGS_ANS:
                int msgNumb    = (int)in[1];
                int partsCount = (int)in[2];

                if(msgNumb >= partsCount || msgNumb >9)
                {
                    String a = "", b = "";
                    if(in[3] < 10)  a = "0";
                    if(in[4] < 10)  b = "0";

                    try {
                        aStrings.add(a + String.valueOf(in[3]) + ":" + b + String.valueOf(in[4]));
                        a = new String(((in[5] & 0xff) | (in[6] & 0xff) << 8) + "");
                        bStrings.add(a.substring(0, 2) + "." + a.substring(2, 3));

                        time = new String[aStrings.size()];
                        temp = new String[bStrings.size()];
                        time = aStrings.toArray(time);
                        temp = bStrings.toArray(temp);
                    }
                    catch (Exception e){}

                    updateListviewTemperature();
                    pb.setVisibility(View.INVISIBLE);
                    bAdd.setVisibility(View.VISIBLE);

                    aStrings.clear();
                    bStrings.clear();
                }
                else {
                    String a = "", b = "";
                    if (in[3] < 10) a = "0";
                    if (in[4] < 10) b = "0";

                    aStrings.add(a + String.valueOf(in[3]) + ":" + b + String.valueOf(in[4]));
                    a = new String(((in[5] & 0xff) | (in[6] & 0xff) << 8) + "");
                    bStrings.add(a.substring(0, 2) + "." + a.substring(2, 3));
                    byte param = (byte)(msgNumb + 1);
                    if(getHolly) param = (byte)(param | 0x80);
                    requestDayCfg(param);
                }
                break;

            case Protocol.OK_ANS:
                switch (curMode)
                {

                    case mHyst:
                    case mWeek:
                        pb.setVisibility(View.INVISIBLE);
                        bSave.setVisibility(View.INVISIBLE);
                        break;

                    case mWork:
                    case mHolly:
                        if(currentPeroid < time.length)
                        {
                            currentPeroid++;
                            formBuffer();
                            udpRequest(udpRequestBuf);
                        }
                        else
                        {
                            bSave.setVisibility(View.INVISIBLE);
                            pb.setVisibility(View.INVISIBLE);
                        }
                        break;
                }
                break;
        }
    }
    //==============================================================================================
    @Override
    protected void onDestroy() {
        super.onDestroy();
        udpProcessor.stop();
        MainActivity.udpProcessor.start();
    }
}
