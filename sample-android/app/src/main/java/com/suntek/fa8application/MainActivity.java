package com.suntek.fa8application;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.sdkapi.api.SdkApi;
import com.suntek.fa8application.nextview.LightingActivity;
import com.suntek.fa8application.nextview.NfcActivity;
import com.suntek.fa8application.nextview.IrScanActivity;

public class MainActivity extends AppCompatActivity {
    SdkApi instance;
    private static final String TAG = "SUNTEK_FA8";
    TextView textView;
    Button button , light, wiegand, irScan;
    private boolean isServiceConnected = false;
    private Handler mainHandler;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        initViews();
        SdkApi.newInstance(this);
        
      mainHandler = new Handler(Looper.getMainLooper());


        initializeAndConnectService();

    }

    private void initViews() {
        textView = findViewById(R.id.sn);
        button = findViewById(R.id.nfc);
        light = findViewById(R.id.light);
        irScan = findViewById(R.id.ir_scan);
//        wiegand = findViewById(R.id.wiegand);
        toNfc();
        tolight();
        toIrScan();
//        towiegand();
    }

//    private void towiegand() {
//        wiegand.setOnClickListener(v -> {
//            Intent intent = new Intent(MainActivity.this, WiegendActivity.class);
//            startActivity(intent);
//        });
//    }

    private void tolight() {
        light.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, LightingActivity.class);
            startActivity(intent);
        });
    }

    private void toIrScan() {
        irScan.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, IrScanActivity.class);
            startActivity(intent);
        });
    }

    void toNfc(){
       button.setOnClickListener(v -> {
           Intent intent = new Intent(MainActivity.this, NfcActivity.class);
           startActivity(intent);
       });
    }
    private void initializeAndConnectService() {
        mainHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
 // aar
                if (isAarServiceConnected()) {
                    isServiceConnected = true;
 // aar
                    onServiceConnected();
                } else {

                    if (!isServiceConnected) {
                        initializeAndConnectService();
                    }
                }
            }
        },1000);
    }
 // aar
    private boolean isAarServiceConnected() {
        try {
 // aar
 // aar API
            boolean serviceConnectionStatus = SdkApi.getInstance().getServiceConnectionStatus();

            return serviceConnectionStatus;

        } catch (Exception e) {
            Log.e(TAG, "检查服务连接失败: " + e.getMessage());
            return false;
        }
    }

    private void onServiceConnected() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "AAR服务连接成功，可以使用库功能");

 // aar
                useAarLibraryFeatures();



            }
        });
    }
 // aar
    private void useAarLibraryFeatures() {
        try {
 // aar

             instance = SdkApi.getInstance();
            String serialNumber = instance.DeviceInfo().getSerialNumber();
            textView.setText(serialNumber);

        } catch (Exception e) {
            Log.e(TAG, "使用AAR库功能失败: " + e.getMessage());
        }
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (mainHandler != null) {
            mainHandler.removeCallbacksAndMessages(null);
        }
    }
}