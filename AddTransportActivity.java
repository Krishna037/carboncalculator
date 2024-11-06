package com.example.carbonfootprintcalculator;

import android.Manifest;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class AddTransportActivity extends AppCompatActivity {
    private static final int PICK_IMAGE_REQUEST = 2;
    private static final int PERMISSION_REQUEST_CODE = 100;
    // CO2 emissions per liter of gasoline (kg CO2/L)
    private static final float CO2_PER_LITER = 2.31f;

    private TextInputEditText distanceEditText;
    private TextInputEditText mileageEditText;
    private ImageView ticketImageView;
    private MaterialButton calculateButton, saveButton, uploadButton;
    private CardView resultsCardView;
    private TextView fuelUsedTextView;
    private TextView carbonFootprintTextView;
    private DatabaseHelper dbHelper;
    private float calculatedCarbonFootprint;
    private float calculatedFuelUsed;
    private Uri selectedImageUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_transport);

        dbHelper = new DatabaseHelper(this);

        initializeViews();
        setupClickListeners();
    }

    private void initializeViews() {
        distanceEditText = findViewById(R.id.editTextDistance);
        mileageEditText = findViewById(R.id.editTextMileage);
        ticketImageView = findViewById(R.id.imageViewTicket);
        calculateButton = findViewById(R.id.buttonCalculate);
        saveButton = findViewById(R.id.buttonSave);
        uploadButton = findViewById(R.id.buttonUpload);
        resultsCardView = findViewById(R.id.cardViewResults);
        fuelUsedTextView = findViewById(R.id.textFuelUsed);
        carbonFootprintTextView = findViewById(R.id.textCarbonFootprint);
    }

    private void setupClickListeners() {
        calculateButton.setOnClickListener(v -> calculateCarbonFootprint());
        saveButton.setOnClickListener(v -> saveTransportData());
        uploadButton.setOnClickListener(v -> checkPermissionAndPickImage());
    }

    private void checkPermissionAndPickImage() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    PERMISSION_REQUEST_CODE);
        } else {
            openImagePicker();
        }
    }

    private void openImagePicker() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Ticket Image"), PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {
            selectedImageUri = data.getData();
            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), selectedImageUri);
                ticketImageView.setImageBitmap(bitmap);
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private String saveImageToInternalStorage(Uri imageUri) {
        try {
            String fileName = "ticket_" + System.currentTimeMillis() + ".jpg";
            File directory = new File(getFilesDir(), "ticket_images");
            if (!directory.exists()) {
                directory.mkdirs();
            }

            File file = new File(directory, fileName);
            InputStream inputStream = getContentResolver().openInputStream(imageUri);
            OutputStream outputStream = new FileOutputStream(file);

            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
            }

            inputStream.close();
            outputStream.close();
            return file.getAbsolutePath();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private void saveTransportData() {
        String distanceText = distanceEditText.getText().toString().trim();
        String mileageText = mileageEditText.getText().toString().trim();

        if (distanceText.isEmpty() || mileageText.isEmpty()) {
            Toast.makeText(this, "Please enter both distance and mileage", Toast.LENGTH_SHORT).show();
            return;
        }

        String imagePath = null;
        if (selectedImageUri != null) {
            imagePath = saveImageToInternalStorage(selectedImageUri);
        }

        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COLUMN_ACTIVITY_NAME, "Transport Usage");
        values.put(DatabaseHelper.COLUMN_ACTIVITY_DATE, System.currentTimeMillis());
        values.put(DatabaseHelper.COLUMN_ACTIVITY_CARBON_FOOTPRINT, calculatedCarbonFootprint);
        values.put(DatabaseHelper.COLUMN_FUEL_USED, calculatedFuelUsed);
        values.put(DatabaseHelper.COLUMN_TICKET_IMAGE_PATH, imagePath);

        long result = dbHelper.getWritableDatabase().insert(DatabaseHelper.TABLE_ACTIVITIES, null, values);
        if (result != -1) {
            Toast.makeText(this, "Transport data saved successfully", Toast.LENGTH_SHORT).show();
            finish();
        } else {
            Toast.makeText(this, "Failed to save data", Toast.LENGTH_SHORT).show();
        }
    }

    private void calculateCarbonFootprint() {
        String distanceText = distanceEditText.getText().toString().trim();
        String mileageText = mileageEditText.getText().toString().trim();

        if (distanceText.isEmpty() || mileageText.isEmpty()) {
            Toast.makeText(this, "Please enter both distance and mileage", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            float distance = Float.parseFloat(distanceText);
            float mileage = Float.parseFloat(mileageText);

            // Calculate fuel used in liters
            calculatedFuelUsed = distance / mileage;

            // Calculate CO2 emissions
            calculatedCarbonFootprint = calculatedFuelUsed * CO2_PER_LITER;

            // Update UI
            resultsCardView.setVisibility(View.VISIBLE);
            fuelUsedTextView.setText(String.format("Fuel Used: %.2f L", calculatedFuelUsed));
            carbonFootprintTextView.setText(String.format("Carbon Footprint: %.2f kg CO₂", calculatedCarbonFootprint));

        } catch (NumberFormatException e) {
            Toast.makeText(this, "Invalid distance or mileage value", Toast.LENGTH_SHORT).show();
        }
    }
}