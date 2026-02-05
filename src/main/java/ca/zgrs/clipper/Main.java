package ca.zgrs.clipper;

import android.Manifest;
import android.app.Activity;
import android.content.ClipboardManager;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

public class Main extends Activity {
    private static final String TAG = "ClipperMain";
    private static final int PERMISSION_REQUEST_CODE = 1;

    public static final String ACTION_GET = "ca.zgrs.clipper.GET";
    public static final String ACTION_SET = "ca.zgrs.clipper.SET";

    private Intent pendingIntent = null;
    private boolean hasFocus = false;
    private Handler handler = new Handler(Looper.getMainLooper());

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        // Store intent for later processing when we have focus
        pendingIntent = getIntent();

        // Request notification permission on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(
                    new String[]{Manifest.permission.POST_NOTIFICATIONS},
                    PERMISSION_REQUEST_CODE
                );
            } else {
                startClipboardService();
            }
        } else {
            startClipboardService();
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        Log.d(TAG, "onNewIntent: " + intent.getAction());
        pendingIntent = intent;
        if (hasFocus) {
            processPendingIntent();
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        this.hasFocus = hasFocus;
        Log.d(TAG, "onWindowFocusChanged: " + hasFocus);
        if (hasFocus && pendingIntent != null) {
            // Delay to ensure focus is fully established
            handler.postDelayed(this::processPendingIntent, 500);
        }
    }

    private void processPendingIntent() {
        if (pendingIntent == null) return;
        Intent intent = pendingIntent;
        pendingIntent = null;
        handleClipboardIntent(intent);
    }

    private void handleClipboardIntent(Intent intent) {
        if (intent == null) return;

        String action = intent.getAction();
        if (action == null) return;

        ClipboardManager cb = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);

        if (ACTION_SET.equals(action) || "clipper.set".equals(action)) {
            String text = intent.getStringExtra("text");
            if (text != null) {
                Log.d(TAG, "Setting clipboard: " + text);
                ClipData clip = ClipData.newPlainText("", text);
                cb.setPrimaryClip(clip);
                Log.d(TAG, "Clipboard set complete");
                Toast.makeText(this, "Copied: " + text, Toast.LENGTH_SHORT).show();
            }
        } else if (ACTION_GET.equals(action) || "clipper.get".equals(action)) {
            Log.d(TAG, "Getting clipboard");
            ClipData clipData = cb.getPrimaryClip();
            if (clipData != null && clipData.getItemCount() > 0) {
                CharSequence text = clipData.getItemAt(0).getText();
                if (text != null) {
                    Log.d(TAG, "Clipboard: " + text);
                    Toast.makeText(this, "Clipboard: " + text, Toast.LENGTH_LONG).show();
                } else {
                    Log.d(TAG, "Clipboard text is null");
                    Toast.makeText(this, "Clipboard is empty", Toast.LENGTH_SHORT).show();
                }
            } else {
                Log.d(TAG, "No clipboard data");
                Toast.makeText(this, "Clipboard is empty", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == PERMISSION_REQUEST_CODE) {
            startClipboardService();
        }
    }

    private void startClipboardService() {
        Intent serviceIntent = new Intent(this, ClipboardService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }
    }
}
