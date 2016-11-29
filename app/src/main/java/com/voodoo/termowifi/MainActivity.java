package com.voodoo.termowifi;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.voodoo.termowifi.UDPProcessor.OnReceiveListener;

import java.net.InetAddress;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class MainActivity extends Activity implements OnReceiveListener
{

    TextView tvTime, inTemp, outTemp;;
    public static UDPProcessor udpProcessor ;
    Button btnGetdata, btnStat,setBtn;
    ProgressBar pbWait;

    com.voodoo.termowifi.plot inCanvas, outCanvas;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

       // tv = (TextView) findViewById(R.id.tvLabel);
        tvTime = (TextView) findViewById(R.id.response);

        ipMaster = null;

        udpProcessor = new UDPProcessor(7777);
        udpProcessor.start();
        udpProcessor.setOnReceiveListener(this);

        inTemp = (TextView) findViewById(R.id.inTemp);
        inTemp.setShadowLayer(5, 2, 2, Color.BLACK);

        outTemp = (TextView) findViewById(R.id.outTemp);
        outTemp.setShadowLayer(5, 2, 2, Color.BLACK);

        inCanvas  = (com.voodoo.termowifi.plot) findViewById(R.id.inCanvas);
        outCanvas = (com.voodoo.termowifi.plot) findViewById(R.id.outCanvas);

        pbWait = (ProgressBar) findViewById(R.id.progressBar);
        pbWait.setVisibility(View.VISIBLE);

        btnGetdata = (Button) findViewById(R.id.updtbtn);
        btnStat = (Button) findViewById(R.id.espbtn);
        setBtn = (Button) findViewById(R.id.setbtn);
        //==========================================================================================
        btnGetdata.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                extTemp = false;
                plotDataRequest();
            }
        });
        //==========================================================================================
        setBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                udpProcessor.stop();
                Intent intent = new Intent(MainActivity.this, settingsActivity.class);
                startActivity(intent);
            }
        });
    }
    //==============================================================================================
    boolean extTemp;
    //==============================================================================================
    void plotDataRequest()
    {
         pbWait.setVisibility(View.VISIBLE);
         byte [] pack = new  byte[8];

         SimpleDateFormat dateFormat = new SimpleDateFormat("yyMMddHHmmss", Locale.UK);
         Calendar cal = Calendar.getInstance();
         String timeString = dateFormat.format(cal.getTime());

         byte[] timeBuf = new byte[6];
         for(int i = 0; i < 6; i++)
             pack[i+2] = (byte)Integer.parseInt(timeString.substring(i*2,(i*2 +2)));

         pack[0] = 0x20;
         if(extTemp)    pack[1] = (byte)0x80;
         else           pack[1] = (byte)0x00;

         DataFrame df = new DataFrame(pack);
         udpProcessor.send(ipMaster,df);
    }
    //==============================================================================================
    //String _ipMaster  = "";
    public static InetAddress ipMaster = null;
    //==============================================================================================
    public void onFrameReceived(InetAddress ip, IDataFrame frame)
    {
        byte[] in = frame.getFrameData();
        String str = new String(in);

        switch(in[0])
        {
            case (byte)0x10: // BROADCAST_DATA
                str = str.substring(1, 9) + "    ";
                if(in.length > 9) {
                    if(ipMaster == null) {
                        setBtn.setVisibility(View.VISIBLE);
                        btnGetdata.setVisibility(View.VISIBLE);
//                        _ipMaster = ip.toString();
                        ipMaster = ip;

                        btnStat.setBackgroundResource(R.drawable.heater_icon);
                        Toast t = Toast.makeText(getApplicationContext(),
                                "Connected", Toast.LENGTH_SHORT);
                        t.setGravity(Gravity.BOTTOM, 0, 0);
                        t.show();
                        pbWait.setVisibility(View.INVISIBLE);
                        plotDataRequest();
                    }
                    tvTime.setText(ip.toString() + "/  " + in[11] + ":" + in[10] + ":" + in[9] + ", " + in[12] + "." + (in[13] + 1) + "." + in[14]);
                }


                if(str.charAt(0) != '0') {
                    String s = str.substring(0, 3) + "." + str.substring(3, 4);
                    if(s.charAt(1) == '0') s = s.substring(0,1) + s.substring(2);
                    inTemp.setText(s);
                }
                if(str.charAt(4) != '0') {
                    String s = (str.substring(4, 7) + "." + str.substring(7, 8));
                    if(s.charAt(1) == '0') s = s.substring(0,1) + s.substring(2);
                    outTemp.setText(s);
                }

                //tv.setText(rTmp1 + "  ...  " + rTmp2);
                break;

            case (byte) 0x21://#define PLOT_DATA_ANS
                pbWait.setVisibility(View.INVISIBLE);
                short[] pData = new short[24];

                for(int i = 0; i < 24; i++)
                    pData[i] = ((short)((in[1 + i*2] & 0xff) | ((in[1+ i*2 +1] & 0xff) << 8)));
                plot.aBuf = pData;
                String sign = "";
                if(pData[23] > 0) sign = "+";
                //tvTime.setText(ip.toString());
                if(extTemp) {
                    plot.aColor = new int[]{120, 255, 255, 0};
                    outCanvas.invalidate();
                    outTemp.setText(sign + String.valueOf((float) pData[23] / 10));
                }
                else
                {
                    plot.aColor = new int[]{150, 102, 204, 255};
                    inCanvas.invalidate();
                    inTemp.setText(sign + String.valueOf((float) pData[23] / 10));
                    extTemp = true;
                    plotDataRequest();
                }
                break;

            default: //case (byte) 0xAA: //OK_ANS
                tvTime.setText("SAVED!!!");
                break;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        udpProcessor.stop();
    }
}
