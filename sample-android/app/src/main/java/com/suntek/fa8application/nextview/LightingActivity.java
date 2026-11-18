package com.suntek.fa8application.nextview;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.sdkapi.api.SdkApi;
import com.suntek.fa8application.R;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;

public class LightingActivity extends AppCompatActivity {
    private static final String TAG = "SUNTEK_FA8";

    // UI Controls
    private Button btnSetBrightness, btnTurnOff, btnQueryStatus;
    private TextView tvLog;

    // Serial port related
    private SdkApi spix;
    private int serialHandle = -1; // Serial port handle, initialized to -1 indicates not opened
    private Thread readThread; // Thread for reading data
    private volatile boolean isReading = false; // Flag to control the reading thread
    private static final int REQUEST_SERIAL_PERMISSION = 1;

    // Service connection check
    private volatile boolean isCheckingService = true;
    private Handler serviceCheckHandler;
    private Runnable serviceCheckRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_lighting);


        // Add permission check
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                Log.w(TAG, "No permission");
                // If no permission, request permission
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        REQUEST_SERIAL_PERMISSION);
                // The result will be called back in onRequestPermissionsResult after requesting permission
                return; // Do not continue execution for now
            } else {
                Log.w(TAG, "have permission");
                initview();
                //SdkApi.newInstance(getApplicationContext());
                startServiceConnectionCheck();
            }
        }

        //initview(); // Initialize UI first
        SdkApi.newInstance(getApplicationContext());
        // startServiceConnectionCheck(); // Then start checking the service

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    // Add callback method for permission request result
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_SERIAL_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, continue initialization
                appendLog("Storage permission granted, continue initialization...");
                initview();
                //SdkApi.newInstance(getApplicationContext());
                startServiceConnectionCheck();
            } else {
                // Permission denied, prompt user
                appendLog("Error: Storage permission denied, unable to access serial port!");
                Log.e(TAG, "Permission denied!");
            }
        }
    }

    private void initview() {
        tvLog = findViewById(R.id.tv_log);

        // --- 1. HEX format - fill light ---
        findViewById(R.id.btn_hex_fill_light_100).setOnClickListener(v -> sendHexCommand(new byte[]{0x4C, (byte) 0x64, 0x23}, "HEX-fill light100%"));
        findViewById(R.id.btn_hex_fill_light_60).setOnClickListener(v -> sendHexCommand(new byte[]{0x4C, 0x3C, 0x23}, "HEX-fill light60%"));
        findViewById(R.id.btn_hex_fill_light_off).setOnClickListener(v -> sendHexCommand(new byte[]{0x4C, 0x00, 0x23}, "HEX-fill light off"));
        findViewById(R.id.btn_hex_fill_light_query).setOnClickListener(v -> sendHexCommand(new byte[]{0x4C, (byte) 0xFF, 0x23}, "HEX-fill light query"));

        // --- 2. HEX format - status light color ---
        findViewById(R.id.btn_hex_status_red).setOnClickListener(v -> sendHexCommand(new byte[]{(byte) 0xFF, 0x00, 0x00}, "HEX-status-red"));
        findViewById(R.id.btn_hex_status_green).setOnClickListener(v -> sendHexCommand(new byte[]{0x00, (byte) 0xFF, 0x00}, "HEX-status-green"));
        findViewById(R.id.btn_hex_status_blue).setOnClickListener(v -> sendHexCommand(new byte[]{0x00, 0x00, (byte) 0xFF}, "HEX-status-blue"));
        findViewById(R.id.btn_hex_status_white).setOnClickListener(v -> sendHexCommand(new byte[]{(byte) 0xFF, (byte) 0xFF, (byte) 0xFF}, "HEX-status-white"));
        findViewById(R.id.btn_hex_status_off).setOnClickListener(v -> sendHexCommand(new byte[]{0x00, 0x00, 0x00}, "HEX-status-off"));

        // --- 3. HEX format - status light blink ---
        findViewById(R.id.btn_hex_status_blink).setOnClickListener(v -> sendHexCommand(new byte[]{(byte) 0xFF, 0x00, 0x00, 0x3C, 0x28, (byte) 0x64, 0x00}, "HEX-status-red blink 100 times"));

        // --- 4. String format - fill light ---
        findViewById(R.id.btn_str_fill_light_100).setOnClickListener(v -> sendStringCommand("LED100#", "STR-fill light100%"));
        findViewById(R.id.btn_str_fill_light_50).setOnClickListener(v -> sendStringCommand("LED050#", "STR-fill light50%"));
        findViewById(R.id.btn_str_fill_light_off).setOnClickListener(v -> sendStringCommand("LED000#", "STR-fill light off"));
        findViewById(R.id.btn_str_fill_light_query).setOnClickListener(v -> sendStringCommand("LED#", "STR-fill light query"));

        // --- 5. String format - status light ---
        findViewById(R.id.btn_str_status_red).setOnClickListener(v -> sendStringCommand("L0001#", "STR-status-red"));
        findViewById(R.id.btn_str_status_green).setOnClickListener(v -> sendStringCommand("L0010#", "STR-status-green"));
        findViewById(R.id.btn_str_status_blue).setOnClickListener(v -> sendStringCommand("L0100#", "STR-status-blue"));
        findViewById(R.id.btn_str_status_white).setOnClickListener(v -> sendStringCommand("L0111#", "STR-status-white"));
        findViewById(R.id.btn_str_status_red_blink).setOnClickListener(v -> sendStringCommand("L0001S", "STR-status-red blink"));

        // --- 6. String format - full color control ---
        findViewById(R.id.btn_str_full_color_orange).setOnClickListener(v -> sendStringCommand("L992500#", "STR-full color-orange"));
        findViewById(R.id.btn_str_full_color_blink).setOnClickListener(v -> executeComplexBlinkSequence());
    }

    private void startServiceConnectionCheck() {
        serviceCheckHandler = new Handler(Looper.getMainLooper());
        serviceCheckRunnable = new Runnable() {
            @Override
            public void run() {
                if (SdkApi.getInstance().getServiceConnectionStatus()) {
                    Log.d(TAG, "Service connection successful, ready to initialize serial port");
                    initSerialPort(); // After service connection is successful, initialize serial port
                } else {
                    if (isCheckingService) {
                        appendLog("Waiting for service connection...");
                        serviceCheckHandler.postDelayed(this, 1000);
                    }
                }
            }
        };
        serviceCheckHandler.post(serviceCheckRunnable);
    }

    /**
     * Initialize serial port and start reading thread
     */
    private void initSerialPort() {
        spix = SdkApi.getInstance();
        serialHandle = spix.Hardware().openSerial("/dev/ttyS9", 9600);
        if (serialHandle < 0) {
            appendLog("Error: Failed to open serial port!");
            Log.e(TAG, "Failed to open serial port!");
            return;
        } else {
            Log.w(TAG, "Successfully opened serial port!");
            sendHexCommand(new byte[]{(byte) 0xFF, 0x00, 0x00}, "HEX-status-red");
        }
        appendLog("Success: Serial ttyS9 opened, handle: " + serialHandle);
        startReadThread(); // Start reading thread
    }

    /**
     * Start a thread to continuously read serial port data
     */
    private void startReadThread() {
        isReading = true;
        readThread = new Thread(() -> {
            byte[] buffer = new byte[1024]; // Buffer
            while (isReading && !Thread.currentThread().isInterrupted()) {
                // readSerial is a blocking method, continues when data is returned or an error occurs
                int bytesRead = spix.Hardware().readSerial(serialHandle, buffer, buffer.length);
                if (bytesRead > 0) {
                    // Convert received bytes to hexadecimal string for display
                    String receivedData = bytesToHex(buffer, bytesRead);
                    // Try to parse brightness value (if the return is a single brightness value)
                    try {
                        if (bytesRead == 1) {
                            int brightness = buffer[0] & 0xFF; // Convert to unsigned integer
                            runOnUiThread(() -> appendLog("Received: " + receivedData + " (Current brightness: " + brightness + ")"));
                        } else {
                            runOnUiThread(() -> appendLog("Received: " + receivedData));
                        }
                    } catch (Exception e) {
                        runOnUiThread(() -> appendLog("Received: " + receivedData + " (Parse failed)"));
                    }
                }
            }
            Log.d(TAG, "Read thread ended");
        });
        readThread.start();
    }

    /**
     * Unified method for sending commands
     */
    private void sendCommand(byte[] command, String actionName) {
        if (serialHandle < 0) {
            appendLog("Error: Serial port not opened, cannot send command!");
            return;
        }
        int writtenBytes = spix.Hardware().writeSerial(serialHandle, command, command.length);
        String commandHex = bytesToHex(command, command.length);
        appendLog("Send command (" + actionName + "): " + commandHex + " | Written bytes: " + writtenBytes);
    }

    /**
     * Send HEX format command
     */
    private void sendHexCommand(byte[] command, String actionName) {
        if (serialHandle < 0) {
            appendLog("Error: Serial port not opened, cannot send command!");
            return;
        }

        int writtenBytes = spix.Hardware().writeSerial(serialHandle, command, command.length);
        String commandHex = bytesToHex(command, command.length);
        appendLog("Send HEX command (" + actionName + "): " + commandHex + " | Written bytes: " + writtenBytes);
    }

    /**
     * Send string format command
     */
    private void sendStringCommand(String command, String actionName) {
        if (serialHandle < 0) {
            appendLog("Error: Serial port not opened, cannot send command!");
            return;
        }
        byte[] commandBytes = command.getBytes(StandardCharsets.US_ASCII); // String commands usually use ASCII encoding
        int writtenBytes = spix.Hardware().writeSerial(serialHandle, commandBytes, commandBytes.length);
        appendLog("Send string command (" + actionName + "): " + command + " | Written bytes: " + writtenBytes);
    }

    /**
     * Convert byte array to hexadecimal string
     */
    private String bytesToHex(byte[] bytes, int length) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append(String.format("%02X ", bytes[i]));
        }
        return sb.toString().trim();
    }

    /**
     * Execute complex combination instructions (flashing)
     */
    private void executeComplexBlinkSequence() {
        appendLog("Start executing complex flashing sequence...");
        // 1. Send command to turn on the light first
        sendStringCommand("L990000#", "Flashing - turn on red light");
        // 2. Send flashing command after a delay of 50ms
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            sendStringCommand("LS1000&0500&10&0#", "Flashing - turn off after 10 red lights");
        }, 50);
    }

    /**
     * Append text to the log TextView
     */
    private void appendLog(String message) {
        runOnUiThread(() -> {
            String currentTime = java.text.DateFormat.getTimeInstance().format(new java.util.Date());
            tvLog.append("[" + currentTime + "] " + message + "\n");
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 1. Stop service check
        isCheckingService = false;
        if (serviceCheckHandler != null) {
            serviceCheckHandler.removeCallbacks(serviceCheckRunnable);
        }

        // 2. Stop reading thread and close serial port
        isReading = false;
        if (readThread != null) {
            readThread.interrupt(); // Interrupt the thread, causing readSerial to throw an exception and exit the loop
        }
        if (serialHandle >= 0 && spix != null) {
            spix.Hardware().closeSerial(serialHandle);
            appendLog("The serial port is closed");
            Log.d(TAG, "Serial port is closed");
        }
        Log.d(TAG, "Activity destroyed, all resources cleaned up");
    }
}
