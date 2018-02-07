package com.mitac.sar;

//import java.math.*;
import java.text.DecimalFormat;
import java.io.File;
import java.io.FileOutputStream;

import android.content.ContentResolver;
import android.content.Context;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.os.IPowerManager;
//import android.os.Parcel;
//import android.os.Parcelable;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.ServiceManager;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Button;
import android.os.SystemProperties;

import android.os.UEventObserver;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.SystemSensorManager;

import android.view.View;
import android.nfc.NfcAdapter;
import android.content.Intent;

public class SarDetectorActivity extends Activity {

	public LinearLayout linearLayout;
	private TextView mPSensor = null;
        //private TextView mBrightness = null;
    private Button mCloseNfc = null;
    private Button mOpenNfc = null;
    private NfcAdapter mNfcAdapter;
    private boolean bSarExist = false;
    // The sensor manager.
    private SensorManager mSensorManager;

    // The light sensor, or null if not available or needed.
    private Sensor mLightSensor;

    // Light sensor event rate in milliseconds.
    private static final int LIGHT_SENSOR_RATE_MILLIS = 1000;
    private float lux;
    // If true, enables the use of the screen auto-brightness adjustment setting.
    //private /*static final*/ boolean USE_SCREEN_AUTO_BRIGHTNESS_ADJUSTMENT =
    //        PowerManager.useScreenAutoBrightnessAdjustmentFeature();

    private  int mScreenBrightnessMinimum;
    private  int mScreenBrightnessMaximum;

    private static final int REFRESH = 0x1234;
    private Handler hRefresh = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            switch(msg.what){
                case REFRESH:
                if(bSarExist) {
                    if(mPSensor!=null) mPSensor.setText("Person is detected!!!");
                    linearLayout.setBackgroundColor(Color.RED);
                    SystemProperties.set("ril.psensor.event.active", "1");
                } else {
                    if(mPSensor!=null) mPSensor.setText("Person is out!!!");
                    linearLayout.setBackgroundColor(Color.BLACK);
                    SystemProperties.set("ril.psensor.event.active", "0");
                }
/*
if(mPSensor!=null) {
    DecimalFormat fnum = new DecimalFormat("##0.00"); 
    String dd=fnum.format(lux); 
    mPSensor.setText("Lux: "+dd);
    mPSensor.setTextSize(200);
}
*/
                break;
                default: break;
            }
        }
    };
	
    private UEventObserver mWwanObserver = new UEventObserver() {
        @Override
        public void onUEvent(UEventObserver.UEvent event) { //wakesource=sar
            boolean waked = "sar".equals(event.get("wakesource")) ? true : false;
            if (waked) {
                if("detect".equals(event.get("status")))
                    bSarExist = true;
                else
                    bSarExist = false;
            }
            hRefresh.sendEmptyMessage(REFRESH);
        }
    };
/*
    private final SensorEventListener mLightSensorListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
             lux = event.values[0];
             hRefresh.sendEmptyMessage(REFRESH);
       }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
            // Not used.
        }
    };
*/
    public void SetData(String fileName, String message)
    {
	try{
	    File file = new File(fileName);
	    FileOutputStream fout = new FileOutputStream(file);
	    byte[] bytes = message.getBytes();
	    fout.write(bytes);
	    fout.close();
	} catch (Exception e){
	    e.printStackTrace();
	}
    }

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        mNfcAdapter = NfcAdapter.getDefaultAdapter(this);

        mPSensor = (TextView)findViewById(R.id.info);
        //mBrightness = (TextView)findViewById(R.id.brightness);
        mOpenNfc = (Button)findViewById(R.id.open_nfc);
        mCloseNfc = (Button)findViewById(R.id.close_nfc);
        if(null!=mOpenNfc && null!=mCloseNfc && null!=mNfcAdapter) {
            if(mNfcAdapter.isEnabled()) {
                mOpenNfc.setEnabled(false);
                mCloseNfc.setEnabled(true);
            } else {
                mOpenNfc.setEnabled(true);
                mCloseNfc.setEnabled(false);
            }
        }

        linearLayout=(LinearLayout)findViewById(R.id.main); 
        mWwanObserver.startObserving("SUBSYSTEM=platform");

        //SystemProperties.set("ril.psensor.event.pop", "1");
        //SystemProperties.set("ril.psensor.event.active", "0");

        //mSensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
        //mLightSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
/*
        PowerManager pm = (PowerManager)getSystemService(Context.POWER_SERVICE);
        mScreenBrightnessMinimum = pm.getMinimumScreenBrightnessSetting();
        mScreenBrightnessMaximum = pm.getMaximumScreenBrightnessSetting();
*/
        //SetData("/sys/module/lte_power/parameters/lte_enable","1");
        SetData("/sys/module/lte_power/parameters/psensor_event_pop","1");
    }

        @Override
        public void onResume() {
                super.onResume();
              //mSensorManager.registerListener(mLightSensorListener, mLightSensor,
              //          LIGHT_SENSOR_RATE_MILLIS * 1000, hRefresh);

        }

        @Override
        public void onPause() {
                super.onPause();

                //mSensorManager.unregisterListener(mLightSensorListener);
        }

    public boolean turnOnNfc(boolean desiredState) {
        // Turn NFC on/off
        if(mNfcAdapter != null) {
            if (desiredState) {
                mNfcAdapter.enable();
            } else {
                mNfcAdapter.disable();
            }
        }
        return true;
    }

    public void onCloseNfc(View view) {
        turnOnNfc(false);
        //reboot the device after a while
        Intent intent=new Intent(Intent.ACTION_REBOOT);
        intent.putExtra("nowait", 1);
        intent.putExtra("interval", 1);
        intent.putExtra("window", 0);
        sendBroadcast(intent);
    }

    public void onOpenNfc(View view) {
        turnOnNfc(true);
    }

}
