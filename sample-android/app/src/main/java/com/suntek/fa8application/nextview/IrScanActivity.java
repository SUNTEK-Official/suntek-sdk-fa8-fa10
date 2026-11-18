package com.suntek.fa8application.nextview;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.KeyEvent;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.suntek.fa8application.R;

public class IrScanActivity extends AppCompatActivity {

    private String keyContent = ""; // Simulated keyboard output content
    private boolean fGet = true; // Flag for input handling

    // UI Views
    private TextView irTitle;
    private TextView irScanPrompt;
    private TextView irSubPrompt;
    private TextView irInfoText;
    private TextView irSuccessText;
    private ImageView irIcon;
    private ImageView successCheckmark;
    private LinearLayout scanPromptLayout;
    private LinearLayout successLayout;

    private Handler mainHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ir_scan);

        mainHandler = new Handler(Looper.getMainLooper());

        initViews();

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void initViews() {
        irTitle = findViewById(R.id.ir_title);
        irScanPrompt = findViewById(R.id.ir_scan_prompt);
        irSubPrompt = findViewById(R.id.ir_sub_prompt);
        irInfoText = findViewById(R.id.ir_info_text);
        irSuccessText = findViewById(R.id.ir_success_text);
        irIcon = findViewById(R.id.ir_icon);
        successCheckmark = findViewById(R.id.success_checkmark);
        scanPromptLayout = findViewById(R.id.scan_prompt_layout);
        successLayout = findViewById(R.id.success_layout);

        // Add pulse animation to IR scan icon
        startPulseAnimation(irIcon);
    }

    /**
     * Start pulse animation effect
     */
    private void startPulseAnimation(ImageView imageView) {
        ScaleAnimation scaleAnimation = new ScaleAnimation(
            1.0f, 1.1f, 1.0f, 1.1f,
            Animation.RELATIVE_TO_SELF, 0.5f,
            Animation.RELATIVE_TO_SELF, 0.5f
        );
        scaleAnimation.setDuration(1000);
        scaleAnimation.setRepeatCount(Animation.INFINITE);
        scaleAnimation.setRepeatMode(Animation.REVERSE);
        imageView.startAnimation(scaleAnimation);
    }

    /**
     * Show success state
     */
    private void showSuccessState(String scanData) {
        mainHandler.post(() -> {
            // Hide scan prompt
            scanPromptLayout.setVisibility(LinearLayout.GONE);

            // Show success state
            successLayout.setVisibility(LinearLayout.VISIBLE);

            // Display scanned barcode content
            irInfoText.setText("Barcode: " + keyContent);

            // Checkmark animation
            ScaleAnimation scaleAnimation = new ScaleAnimation(
                0.0f, 1.0f, 0.0f, 1.0f,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f
            );
            scaleAnimation.setDuration(500);
            successCheckmark.startAnimation(scaleAnimation);
        });
    }

    /**
     * Reset to scan state
     */
    private void resetToScanState() {
        mainHandler.post(() -> {
            keyContent = "";
            scanPromptLayout.setVisibility(LinearLayout.VISIBLE);
            successLayout.setVisibility(LinearLayout.GONE);
            irIcon.clearAnimation();
            startPulseAnimation(irIcon);
        });
    }

    // Convert key code to ASCII character
    public char GetCharByKeyCode(int keyCode) {
        char outChar = 0;

        if (keyCode > 6 && keyCode < 17) // Numbers
            outChar = (char) ((keyCode - 7) + 0x30);

        if (keyCode > 28 && keyCode < 55) // Letters
            outChar = (char) ((keyCode - 29) + 0x41);

        return outChar;
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) { // Monitor keyboard output
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            if (event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
                if (keyContent.length() > 0) {
                    // Show success state
                    showSuccessState(keyContent);

                    // Show Toast notification
                    Toast.makeText(getApplicationContext(), "IR scan success: " + keyContent, Toast.LENGTH_SHORT).show();

                    // Auto reset to scan state after 3 seconds (for demo)
                    mainHandler.postDelayed(this::resetToScanState, 3000);
                }
                keyContent = "";
            } else {
                int keyCode = event.getKeyCode();
                char keyChar = GetCharByKeyCode(keyCode);

                if (keyChar != 0) {
                    if (fGet) {
                        keyContent += keyChar;
                        fGet = false;
                    } else {
                        fGet = true;
                    }
                }
            }
        }
        return super.dispatchKeyEvent(event);
    }
}
