package com.lyman.video;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;

public class Main2Activity extends AppCompatActivity {
    private static final String TAG = "Main2Activity";
    private MyInterface myInterface;
    public interface MyInterface{
        void callBack();
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);

        myInterface = new MyInterface() {
            @Override
            public void callBack() {
                Log.d(TAG, "callBack: "+Thread.currentThread());
            }
        };
        Log.d(TAG, "onCreate: "+Thread.currentThread());
        new Thread(new Runnable() {
            @Override
            public void run() {
                myInterface.callBack();
            }
        }).start();
    }

}
