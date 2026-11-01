package com.generated.app;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

    public static final String PREFS_NAME = "SmartLockPrefs";
    public static final String KEY_SECURITY_TYPE = "security_type"; // 0: PIN, 1: Pattern, 2: Swipe, 3: Biometric
    public static final String KEY_PIN_CODE = "pin_code";
    public static final String KEY_CLOCK_STYLE = "clock_style";
    public static final String KEY_WALLPAPER = "wallpaper";
    public static final String KEY_NOTIFICATIONS = "show_notifications";
    public static final String KEY_HAPTICS = "haptics";
    public static final String KEY_SOUND = "sound";
    public static final String KEY_EMERGENCY_NUM = "emergency_num";

    private SharedPreferences prefs;

    private RadioGroup radioGroupSecurity;
    private RadioButton rbPin, rbPattern, rbSwipe, rbBio;
    private LinearLayout layoutPinConfig;
    private EditText etPinCode;
    private Button btnSavePin;
    private Spinner spinnerClock;
    private Spinner spinnerWallpaper;
    private Switch switchNotifications;
    private Switch switchHaptics;
    private Switch switchSound;
    private EditText etEmergency;
    private Button btnLaunchLock;
    private TextView tvSavedIndicator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        initViews();
        loadPreferences();
        setupListeners();
    }

    private void initViews() {
        radioGroupSecurity = findViewById(R.id.radioGroupSecurity);
        rbPin = findViewById(R.id.rbPin);
        rbPattern = findViewById(R.id.rbPattern);
        rbSwipe = findViewById(R.id.rbSwipe);
        rbBio = findViewById(R.id.rbBio);
        layoutPinConfig = findViewById(R.id.layoutPinConfig);
        etPinCode = findViewById(R.id.etPinCode);
        btnSavePin = findViewById(R.id.btnSavePin);
        spinnerClock = findViewById(R.id.spinnerClock);
        spinnerWallpaper = findViewById(R.id.spinnerWallpaper);
        switchNotifications = findViewById(R.id.switchNotifications);
        switchHaptics = findViewById(R.id.switchHaptics);
        switchSound = findViewById(R.id.switchSound);
        etEmergency = findViewById(R.id.etEmergency);
        btnLaunchLock = findViewById(R.id.btnLaunchLock);
        tvSavedIndicator = findViewById(R.id.tvSavedIndicator);

        // Setup Spinners
        String[] clockStyles = {
                getString(R.string.clock_modern),
                getString(R.string.clock_digital),
                getString(R.string.clock_compact)
        };
        ArrayAdapter<String> clockAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, clockStyles);
        spinnerClock.setAdapter(clockAdapter);

        String[] wallpapers = {
                getString(R.string.wp_midnight),
                getString(R.string.wp_neon),
                getString(R.string.wp_sunset),
                getString(R.string.wp_minimal)
        };
        ArrayAdapter<String> wpAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, wallpapers);
        spinnerWallpaper.setAdapter(wpAdapter);
    }

    private void loadPreferences() {
        int secType = prefs.getInt(KEY_SECURITY_TYPE, 0);
        if (secType == 0) {
            rbPin.setChecked(true);
            layoutPinConfig.setVisibility(View.VISIBLE);
        } else if (secType == 1) {
            rbPattern.setChecked(true);
            layoutPinConfig.setVisibility(View.GONE);
        } else if (secType == 2) {
            rbSwipe.setChecked(true);
            layoutPinConfig.setVisibility(View.GONE);
        } else {
            rbBio.setChecked(true);
            layoutPinConfig.setVisibility(View.GONE);
        }

        String pin = prefs.getString(KEY_PIN_CODE, "1234");
        etPinCode.setText(pin);

        spinnerClock.setSelection(prefs.getInt(KEY_CLOCK_STYLE, 0));
        spinnerWallpaper.setSelection(prefs.getInt(KEY_WALLPAPER, 0));

        switchNotifications.setChecked(prefs.getBoolean(KEY_NOTIFICATIONS, true));
        switchHaptics.setChecked(prefs.getBoolean(KEY_HAPTICS, true));
        switchSound.setChecked(prefs.getBoolean(KEY_SOUND, true));

        etEmergency.setText(prefs.getString(KEY_EMERGENCY_NUM, "100"));
    }

    private void setupListeners() {
        radioGroupSecurity.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                int secType = 0;
                if (checkedId == R.id.rbPin) {
                    secType = 0;
                    layoutPinConfig.setVisibility(View.VISIBLE);
                } else if (checkedId == R.id.rbPattern) {
                    secType = 1;
                    layoutPinConfig.setVisibility(View.GONE);
                } else if (checkedId == R.id.rbSwipe) {
                    secType = 2;
                    layoutPinConfig.setVisibility(View.GONE);
                } else if (checkedId == R.id.rbBio) {
                    secType = 3;
                    layoutPinConfig.setVisibility(View.GONE);
                }
                prefs.edit().putInt(KEY_SECURITY_TYPE, secType).apply();
                showSavedToast("סוג האבטחה עודכן בהצלחה");
            }
        });

        btnSavePin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String pin = etPinCode.getText().toString().trim();
                if (pin.length() != 4) {
                    Toast.makeText(MainActivity.this, "נא להזין קוד בעל 4 ספרות בדיוק", Toast.LENGTH_SHORT).show();
                    return;
                }
                prefs.edit().putString(KEY_PIN_CODE, pin).apply();
                showSavedToast("קוד הסודי נשמר בהצלחה!");
            }
        });

        spinnerClock.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                prefs.edit().putInt(KEY_CLOCK_STYLE, position).apply();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        spinnerWallpaper.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                prefs.edit().putInt(KEY_WALLPAPER, position).apply();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        CompoundButton.OnCheckedChangeListener switchListener = new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                prefs.edit()
                        .putBoolean(KEY_NOTIFICATIONS, switchNotifications.isChecked())
                        .putBoolean(KEY_HAPTICS, switchHaptics.isChecked())
                        .putBoolean(KEY_SOUND, switchSound.isChecked())
                        .apply();
            }
        };

        switchNotifications.setOnCheckedChangeListener(switchListener);
        switchHaptics.setOnCheckedChangeListener(switchListener);
        switchSound.setOnCheckedChangeListener(switchListener);

        btnLaunchLock.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Save emergency phone
                String emNum = etEmergency.getText().toString().trim();
                if (!emNum.isEmpty()) {
                    prefs.edit().putString(KEY_EMERGENCY_NUM, emNum).apply();
                }

                Intent intent = new Intent(MainActivity.this, LockScreenActivity.class);
                startActivity(intent);
            }
        });
    }

    private void showSavedToast(String msg) {
        tvSavedIndicator.setText(msg);
        tvSavedIndicator.setVisibility(View.VISIBLE);
        tvSavedIndicator.postDelayed(new Runnable() {
            @Override
            public void run() {
                tvSavedIndicator.setVisibility(View.GONE);
            }
        }, 2200);
    }
}
