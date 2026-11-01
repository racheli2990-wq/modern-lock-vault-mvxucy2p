package com.generated.app;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.hardware.camera2.CameraManager;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Vibrator;
import android.provider.MediaStore;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class LockScreenActivity extends Activity {

    private SharedPreferences prefs;
    private int securityType; // 0=PIN, 1=Pattern, 2=Swipe, 3=Bio
    private String savedPin;
    private final StringBuilder enteredPin = new StringBuilder();

    private RelativeLayout lockRootView;
    private TextView tvLockDate;
    private TextView tvLockTime;
    private TextView tvLockHint;
    private LinearLayout layoutNotifications;
    private LinearLayout layoutBiometric;
    private LinearLayout layoutKeypad;
    private PatternLockView patternLockView;
    private View dot1, dot2, dot3, dot4;
    private TextView tvPinError;
    private TextView btnTorch;
    private TextView btnCamera;
    private TextView tvUnlockedNotice;

    private boolean isTorchOn = false;
    private Handler timeHandler;
    private Runnable timeRunnable;

    private float touchStartY = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lock_screen);

        prefs = getSharedPreferences(MainActivity.PREFS_NAME, Context.MODE_PRIVATE);
        securityType = prefs.getInt(MainActivity.KEY_SECURITY_TYPE, 0);
        savedPin = prefs.getString(MainActivity.KEY_PIN_CODE, "1234");

        initViews();
        applyWallpaper();
        applyClockStyle();
        setupSecurityMode();
        setupBottomShortcuts();
        startTimeTicker();
    }

    private void initViews() {
        lockRootView = findViewById(R.id.lockRootView);
        tvLockDate = findViewById(R.id.tvLockDate);
        tvLockTime = findViewById(R.id.tvLockTime);
        tvLockHint = findViewById(R.id.tvLockHint);
        layoutNotifications = findViewById(R.id.layoutNotifications);
        layoutBiometric = findViewById(R.id.layoutBiometric);
        layoutKeypad = findViewById(R.id.layoutKeypad);
        patternLockView = findViewById(R.id.patternLockView);
        dot1 = findViewById(R.id.dot1);
        dot2 = findViewById(R.id.dot2);
        dot3 = findViewById(R.id.dot3);
        dot4 = findViewById(R.id.dot4);
        tvPinError = findViewById(R.id.tvPinError);
        btnTorch = findViewById(R.id.btnTorch);
        btnCamera = findViewById(R.id.btnCamera);
        tvUnlockedNotice = findViewById(R.id.tvUnlockedNotice);

        boolean showNotifications = prefs.getBoolean(MainActivity.KEY_NOTIFICATIONS, true);
        layoutNotifications.setVisibility(showNotifications ? View.VISIBLE : View.GONE);

        // Handle swipe up gesture on empty screen
        lockRootView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    touchStartY = event.getY();
                    return true;
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    float deltaY = touchStartY - event.getY();
                    if (deltaY > 120) {
                        handleSwipeUpAction();
                    }
                    return true;
                }
                return false;
            }
        });
    }

    private void handleSwipeUpAction() {
        if (securityType == 2) {
            // Simple swipe unlock
            triggerUnlockSuccess();
        } else if (securityType == 0) {
            // Show PIN keypad
            if (layoutKeypad.getVisibility() != View.VISIBLE) {
                layoutNotifications.setVisibility(View.GONE);
                layoutKeypad.setVisibility(View.VISIBLE);
                tvLockHint.setText(R.string.enter_pin);
            }
        } else if (securityType == 1) {
            // Show Pattern
            if (patternLockView.getVisibility() != View.VISIBLE) {
                layoutNotifications.setVisibility(View.GONE);
                patternLockView.setVisibility(View.VISIBLE);
                tvLockHint.setText(R.string.draw_pattern);
            }
        }
    }

    private void applyWallpaper() {
        int wpIndex = prefs.getInt(MainActivity.KEY_WALLPAPER, 0);
        GradientDrawable gradient = new GradientDrawable();
        gradient.setOrientation(GradientDrawable.Orientation.TOP_BOTTOM);

        switch (wpIndex) {
            case 0: // Midnight
                gradient.setColors(new int[]{Color.parseColor("#090D16"), Color.parseColor("#0F172A"), Color.parseColor("#1E1B4B")});
                break;
            case 1: // Neon
                gradient.setColors(new int[]{Color.parseColor("#0A192F"), Color.parseColor("#0F3460"), Color.parseColor("#E94560")});
                break;
            case 2: // Sunset
                gradient.setColors(new int[]{Color.parseColor("#2D142C"), Color.parseColor("#510A32"), Color.parseColor("#801336")});
                break;
            case 3: // Minimal
            default:
                gradient.setColors(new int[]{Color.parseColor("#050505"), Color.parseColor("#121212"), Color.parseColor("#1E1E1E")});
                break;
        }
        lockRootView.setBackground(gradient);
    }

    private void applyClockStyle() {
        int style = prefs.getInt(MainActivity.KEY_CLOCK_STYLE, 0);
        if (style == 1) {
            tvLockTime.setTextSize(90);
        } else if (style == 2) {
            tvLockTime.setTextSize(54);
        } else {
            tvLockTime.setTextSize(72);
        }
    }

    private void setupSecurityMode() {
        if (securityType == 0) {
            tvLockHint.setText(R.string.swipe_to_unlock);
            setupPinKeypad();
        } else if (securityType == 1) {
            tvLockHint.setText(R.string.swipe_to_unlock);
            setupPatternView();
        } else if (securityType == 2) {
            tvLockHint.setText(R.string.swipe_to_unlock);
        } else if (securityType == 3) {
            tvLockHint.setText(R.string.tap_fingerprint);
            layoutBiometric.setVisibility(View.VISIBLE);
            setupBiometric();
        }
    }

    private void setupBiometric() {
        findViewById(R.id.btnFingerprint).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                vibrateFeedback(40);
                triggerUnlockSuccess();
            }
        });
    }

    private void setupPinKeypad() {
        int[] numBtnIds = {
                R.id.btn0, R.id.btn1, R.id.btn2, R.id.btn3, R.id.btn4,
                R.id.btn5, R.id.btn6, R.id.btn7, R.id.btn8, R.id.btn9
        };

        View.OnClickListener numListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                vibrateFeedback(25);
                Button b = (Button) v;
                if (enteredPin.length() < 4) {
                    enteredPin.append(b.getText().toString());
                    updatePinDots();
                    if (enteredPin.length() == 4) {
                        checkPin();
                    }
                }
            }
        };

        for (int id : numBtnIds) {
            findViewById(id).setOnClickListener(numListener);
        }

        findViewById(R.id.btnDelete).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                vibrateFeedback(20);
                if (enteredPin.length() > 0) {
                    enteredPin.deleteCharAt(enteredPin.length() - 1);
                    updatePinDots();
                    tvPinError.setVisibility(View.INVISIBLE);
                }
            }
        });

        findViewById(R.id.btnEmergency).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                makeEmergencyCall();
            }
        });
    }

    private void updatePinDots() {
        int len = enteredPin.length();
        dot1.setBackgroundResource(len >= 1 ? R.drawable.pin_dot_filled : R.drawable.pin_dot_empty);
        dot2.setBackgroundResource(len >= 2 ? R.drawable.pin_dot_filled : R.drawable.pin_dot_empty);
        dot3.setBackgroundResource(len >= 3 ? R.drawable.pin_dot_filled : R.drawable.pin_dot_empty);
        dot4.setBackgroundResource(len >= 4 ? R.drawable.pin_dot_filled : R.drawable.pin_dot_empty);
    }

    private void checkPin() {
        if (enteredPin.toString().equals(savedPin)) {
            triggerUnlockSuccess();
        } else {
            vibrateError();
            tvPinError.setVisibility(View.VISIBLE);
            tvPinError.setText(R.string.wrong_pin);
            enteredPin.setLength(0);
            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                @Override
                public void run() {
                    updatePinDots();
                }
            }, 300);
        }
    }

    private void setupPatternView() {
        patternLockView.setOnPatternListener(new PatternLockView.OnPatternListener() {
            @Override
            public void onPatternDetected(List<Integer> pattern) {
                // Minimum 4 connected dots unlocks successfully
                if (pattern.size() >= 4) {
                    triggerUnlockSuccess();
                } else {
                    vibrateError();
                    Toast.makeText(LockScreenActivity.this, R.string.wrong_pattern, Toast.LENGTH_SHORT).show();
                    patternLockView.resetPattern();
                }
            }
        });
    }

    private void setupBottomShortcuts() {
        btnTorch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleTorch();
            }
        });

        btnCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openCamera();
            }
        });
    }

    private void toggleTorch() {
        vibrateFeedback(30);
        isTorchOn = !isTorchOn;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            CameraManager camManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
            if (camManager != null) {
                try {
                    String cameraId = camManager.getCameraIdList()[0];
                    camManager.setTorchMode(cameraId, isTorchOn);
                } catch (Exception ignored) {
                }
            }
        }

        btnTorch.setBackgroundColor(isTorchOn ? Color.parseColor("#99FBBF24") : Color.TRANSPARENT);
        btnTorch.setBackgroundResource(isTorchOn ? R.drawable.btn_primary_rounded : R.drawable.btn_shortcut);
        Toast.makeText(this, isTorchOn ? "פנס דלוק" : "פנס כבוי", Toast.LENGTH_SHORT).show();
    }

    private void openCamera() {
        vibrateFeedback(30);
        try {
            Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            startActivity(cameraIntent);
        } catch (Exception e) {
            Toast.makeText(this, "מצלמה אינה זמינה במכשיר", Toast.LENGTH_SHORT).show();
        }
    }

    private void makeEmergencyCall() {
        String emergencyNum = prefs.getString(MainActivity.KEY_EMERGENCY_NUM, "100");
        Intent intent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + emergencyNum));
        startActivity(intent);
    }

    private void triggerUnlockSuccess() {
        playUnlockSound();
        vibrateFeedback(50);

        tvUnlockedNotice.setVisibility(View.VISIBLE);
        lockRootView.animate().alpha(0.0f).setDuration(400).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                finish();
                overridePendingTransition(0, android.R.anim.fade_out);
            }
        });
    }

    private void playUnlockSound() {
        if (!prefs.getBoolean(MainActivity.KEY_SOUND, true)) return;
        try {
            ToneGenerator toneGen = new ToneGenerator(AudioManager.STREAM_SYSTEM, 70);
            toneGen.startTone(ToneGenerator.TONE_PROP_BEEP, 120);
        } catch (Exception ignored) {
        }
    }

    private void vibrateFeedback(long ms) {
        if (!prefs.getBoolean(MainActivity.KEY_HAPTICS, true)) return;
        try {
            Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            if (v != null && v.hasVibrator()) {
                v.vibrate(ms);
            }
        } catch (Exception ignored) {
        }
    }

    private void vibrateError() {
        if (!prefs.getBoolean(MainActivity.KEY_HAPTICS, true)) return;
        try {
            Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            if (v != null && v.hasVibrator()) {
                long[] pattern = {0, 60, 60, 60};
                v.vibrate(pattern, -1);
            }
        } catch (Exception ignored) {
        }
    }

    private void startTimeTicker() {
        timeHandler = new Handler(Looper.getMainLooper());
        final SimpleDateFormat timeFmt = new SimpleDateFormat("HH:mm", Locale.getDefault());
        final SimpleDateFormat dateFmt = new SimpleDateFormat("EEEE, d בMMMM", new Locale("he", "IL"));

        timeRunnable = new Runnable() {
            @Override
            public void run() {
                Date now = new Date();
                tvLockTime.setText(timeFmt.format(now));
                tvLockDate.setText(dateFmt.format(now));
                timeHandler.postDelayed(this, 1000);
            }
        };
        timeHandler.post(timeRunnable);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (timeHandler != null && timeRunnable != null) {
            timeHandler.removeCallbacks(timeRunnable);
        }
        if (isTorchOn && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                CameraManager camManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
                if (camManager != null) {
                    camManager.setTorchMode(camManager.getCameraIdList()[0], false);
                }
            } catch (Exception ignored) {
            }
        }
    }

    @Override
    public void onBackPressed() {
        // Prevent back press from accidentally closing lock screen without swipe/auth
        if (layoutKeypad.getVisibility() == View.VISIBLE || patternLockView.getVisibility() == View.VISIBLE) {
            layoutKeypad.setVisibility(View.GONE);
            patternLockView.setVisibility(View.GONE);
            if (prefs.getBoolean(MainActivity.KEY_NOTIFICATIONS, true)) {
                layoutNotifications.setVisibility(View.VISIBLE);
            }
            tvLockHint.setText(R.string.swipe_to_unlock);
            enteredPin.setLength(0);
            updatePinDots();
        } else {
            super.onBackPressed();
        }
    }
}
