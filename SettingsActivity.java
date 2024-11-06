package com.example.carbonfootprintcalculator;

import android.content.Context;
import android.Manifest;

import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.google.android.material.slider.Slider;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.firebase.messaging.FirebaseMessaging;

import java.io.IOException;
import java.util.Map;
import okhttp3.*;
public class SettingsActivity extends AppCompatActivity {
    private static final String BASE_URL ="" ;
    private DatabaseHelper dbHelper;
    private int currentUserId;

    // Settings keys
    private static final String SETTING_NOTIFICATIONS = "notifications";
    private static final String SETTING_EMAIL_NOTIFICATIONS = "email_notifications";
    private static final String SETTING_DARK_MODE = "dark_mode";
    private static final String SETTING_TEXT_SIZE = "text_size";
    private static final String SETTING_DATA_COLLECTION = "data_collection";
    private static final String SETTING_LANGUAGE = "language";

    // UI Components
    private SwitchMaterial notificationSwitch;
    private SwitchMaterial emailNotificationSwitch;
    private SwitchMaterial darkModeSwitch;
    private SwitchMaterial dataCollectionSwitch;
    private Slider textSizeSlider;
    private AutoCompleteTextView languageSpinner;

    // Permission request codes
    private static final int NOTIFICATION_PERMISSION_REQUEST_CODE = 1001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        dbHelper = new DatabaseHelper(this);
        String userEmail = getIntent().getStringExtra("USER_EMAIL");
        currentUserId = dbHelper.getUserIdByEmail(userEmail);

        initializeViews();
        setupLanguageSpinner();
        loadSettings();
        setupListeners();
    }

    private void initializeViews() {
        notificationSwitch = findViewById(R.id.switch_notifications);
        emailNotificationSwitch = findViewById(R.id.switch_email_notifications);
        darkModeSwitch = findViewById(R.id.switch_dark_mode);
        dataCollectionSwitch = findViewById(R.id.switch_data_collection);
        textSizeSlider = findViewById(R.id.slider_text_size);
        languageSpinner = findViewById(R.id.spinner_language);
    }

    private void setupLanguageSpinner() {
        String[] languages = new String[]{"English", "Spanish", "French", "German"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, languages);
        languageSpinner.setAdapter(adapter);
    }

    private void loadSettings() {
        Map<String, String> settings = dbHelper.getAllSettings(currentUserId);

        notificationSwitch.setChecked(Boolean.parseBoolean(settings.getOrDefault(SETTING_NOTIFICATIONS, "true")));
        emailNotificationSwitch.setChecked(Boolean.parseBoolean(settings.getOrDefault(SETTING_EMAIL_NOTIFICATIONS, "false")));
        darkModeSwitch.setChecked(Boolean.parseBoolean(settings.getOrDefault(SETTING_DARK_MODE, "false")));
        dataCollectionSwitch.setChecked(Boolean.parseBoolean(settings.getOrDefault(SETTING_DATA_COLLECTION, "true")));

        float textSize = Float.parseFloat(settings.getOrDefault(SETTING_TEXT_SIZE, "16.0"));
        textSizeSlider.setValue(textSize);

        String language = settings.getOrDefault(SETTING_LANGUAGE, "English");
        languageSpinner.setText(language, false);

        applySettings();
    }

    private void setupListeners() {
        CompoundButton.OnCheckedChangeListener switchListener = (buttonView, isChecked) -> {
            String settingName = null;

            if (buttonView.getId() == R.id.switch_notifications) {
                settingName = SETTING_NOTIFICATIONS;
                updateNotificationSettings(isChecked);
            } else if (buttonView.getId() == R.id.switch_email_notifications) {
                settingName = SETTING_EMAIL_NOTIFICATIONS;
            } else if (buttonView.getId() == R.id.switch_dark_mode) {
                settingName = SETTING_DARK_MODE;
                updateTheme(isChecked);
            } else if (buttonView.getId() == R.id.switch_data_collection) {
                settingName = SETTING_DATA_COLLECTION;
            }

            if (settingName != null) {
                dbHelper.saveSetting(currentUserId, settingName, String.valueOf(isChecked));
            }
        };

        notificationSwitch.setOnCheckedChangeListener(switchListener);
        emailNotificationSwitch.setOnCheckedChangeListener(switchListener);
        darkModeSwitch.setOnCheckedChangeListener(switchListener);
        dataCollectionSwitch.setOnCheckedChangeListener(switchListener);

        textSizeSlider.addOnChangeListener((slider, value, fromUser) -> {
            if (fromUser) {
                dbHelper.saveSetting(currentUserId, SETTING_TEXT_SIZE, String.valueOf(value));
                updateTextSize(value);
            }
        });

        languageSpinner.setOnItemClickListener((parent, view, position, id) -> {
            String selectedLanguage = parent.getItemAtPosition(position).toString();
            dbHelper.saveSetting(currentUserId, SETTING_LANGUAGE, selectedLanguage);
            updateLanguage(selectedLanguage);
        });
    }

    private void updateNotificationSettings(boolean enabled) {
        if (enabled) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, NOTIFICATION_PERMISSION_REQUEST_CODE);
            }
            registerForPushNotifications();
        } else {
            unregisterFromPushNotifications();
        }
    }

    private void updateTheme(boolean darkMode) {
        if (darkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }
        recreate();
    }

    private void updateTextSize(float size) {
        applyTextSize(size);
    }

    private void updateLanguage(String languageCode) {
        Context updatedContext = LocaleHelper.setLocale(this, languageCode);
        Resources resources = updatedContext.getResources();

        // Update the configuration
        Configuration configuration = resources.getConfiguration();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            getApplicationContext().createConfigurationContext(configuration);
        } else {
            resources.updateConfiguration(configuration, resources.getDisplayMetrics());
        }

        // Save the setting and recreate the activity
        dbHelper.saveSetting(currentUserId, SETTING_LANGUAGE, languageCode);
        recreate();
    }

    private void applySettings() {
        boolean darkMode = Boolean.parseBoolean(dbHelper.getSetting(currentUserId, SETTING_DARK_MODE, "false"));
        float textSize = Float.parseFloat(dbHelper.getSetting(currentUserId, SETTING_TEXT_SIZE, "16.0"));
        String language = dbHelper.getSetting(currentUserId, SETTING_LANGUAGE, "English");
        updateLanguage(language);
        updateTheme(darkMode);
        updateTextSize(textSize);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == NOTIFICATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                notificationSwitch.setChecked(false);
                dbHelper.saveSetting(currentUserId, SETTING_NOTIFICATIONS, "false");
                Toast.makeText(this, "Notification permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void registerForPushNotifications() {
        FirebaseMessaging.getInstance().getToken().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                updateFCMTokenOnServer(task.getResult());
            }
        });
    }

    private void unregisterFromPushNotifications() {
        FirebaseMessaging.getInstance().deleteToken().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                removeFCMTokenFromServer();
            }
        });
    }

    private void applyTextSize(float size) {
        ViewGroup root = findViewById(android.R.id.content);
        updateTextSizeRecursive(root, size);
    }

    private void updateTextSizeRecursive(ViewGroup parent, float size) {
        for (int i = 0; i < parent.getChildCount(); i++) {
            View child = parent.getChildAt(i);
            if (child instanceof TextView) {
                ((TextView) child).setTextSize(TypedValue.COMPLEX_UNIT_SP, size);
            } else if (child instanceof ViewGroup) {
                updateTextSizeRecursive((ViewGroup) child, size);
            }
        }
    }

    private void updateFCMTokenOnServer(String token) {
        final String BASE_URL = "https://yourapi.com";
        OkHttpClient client = new OkHttpClient();
        String url = BASE_URL + "/update_token";

        RequestBody formBody = new FormBody.Builder()
                .add("token", token)  // Replace "token" with your server’s expected parameter name
                .build();

        Request request = new Request.Builder()
                .url(url)
                .post(formBody)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                // Handle the error
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    // Handle successful response
                    System.out.println("Token updated successfully on server");
                } else {
                    // Handle server errors
                    System.err.println("Failed to update token on server: " + response.message());
                }
            }
        });
    }
    private String getToken() {
        SharedPreferences preferences = getSharedPreferences("FCM_PREFS", MODE_PRIVATE);
        return preferences.getString("fcm_token", null);
    }

    private void removeFCMTokenFromServer() {
        OkHttpClient client = new OkHttpClient();
        String url = BASE_URL + "/remove_token";

        // Add token as a parameter in the request body if required
        RequestBody formBody = new FormBody.Builder()
                .add("token", getToken()) // Replace with a method to retrieve the FCM token if needed
                .build();

        Request request = new Request.Builder()
                .url(url)
                .delete(formBody) // Use DELETE method here
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                // Handle the error
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    // Handle successful response
                    System.out.println("Token removed successfully from server");
                } else {
                    // Handle server errors
                    System.err.println("Failed to remove token from server: " + response.message());
                }
            }
        });
    }
}
