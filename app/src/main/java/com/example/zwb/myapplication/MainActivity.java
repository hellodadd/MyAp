package com.example.zwb.myapplication;

import android.content.ComponentName;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import java.lang.reflect.Method;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements UnreadMonitor.BadgeUpdateListener{

    private UnreadMonitor mMonitor;

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            Map<ComponentName, UnreadMonitor.BadgeCount> badge =
                    (Map<ComponentName, UnreadMonitor.BadgeCount>) msg.obj;
            for (Map.Entry<ComponentName, UnreadMonitor.BadgeCount> entry : badge.entrySet()) {
                ComponentName componentName = entry.getKey();
                int unreadNum = entry.getValue().getBadgeCount();
                Log.e("UnreadMonitor", " pacakgeName = " + componentName.getPackageName().toString()
                  + " unreadNum = " + unreadNum);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mMonitor = new UnreadMonitor(this.getApplicationContext(), null, this);

        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                try {
                    mMonitor.start();
                } catch (Exception e) {
                    //
                }
            }
        },100);

        TextView imei = (TextView)findViewById(R.id.imei);

        String imei1 = getProperty("persist.sys.freeme_imei1", "null");
        String imei2 = getProperty("persist.sys.freeme_imei2", "null");

        imei.setText(imei1 + " , " + imei2);
    }

    @Override
    public void onBadgeUpdated(Map<ComponentName, UnreadMonitor.BadgeCount> badge) {
        mHandler.obtainMessage(0, badge).sendToTarget();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mMonitor.stop();
    }

    private String getProperty(String key, String defaultValue) {
        String value = defaultValue;
        try {
            Class<?> c = Class.forName("android.os.SystemProperties");
            Method get = c.getMethod("get", String.class, String.class);
            value = (String)(get.invoke(c, key, "unknown" ));
        } catch (Exception e) {
            e.printStackTrace();
        }finally {
            return value;
        }
    }
}
