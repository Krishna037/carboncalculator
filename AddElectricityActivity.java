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
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class AddElectricityActivity extends AppCompatActivity {
    private static final int PICK_IMAGE_REQUEST = 1;
    private static final int PERMISSION_REQUEST_CODE = 100;

    private TextInputEditText consumptionEditText;
    private ImageView billImageView;
    private MaterialButton calculateButton, saveButton, uploadButton;
    private DatabaseHelper dbHelper;
    private float calculatedCarbonFootprint;
    private Uri selectedImageUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_electricity);

        dbHelper = new DatabaseHelper(this);

        consumptionEditText = findViewById(R.id.editTextConsumption);
        calculateButton = findViewById(R.id.buttonCalculate);
        saveButton = findViewById(R.id.buttonSave);

        calculateButton.setOnClickListener(v -> calculateCarbonFootprint());

        saveButton.setOnClickListener(v -> saveElectricityData());
    }

    private void initializeViews() {
        consumptionEditText = findViewById(R.id.editTextConsumption);
        billImageView = findViewById(R.id.imageViewBill);
        calculateButton = findViewById(R.id.buttonCalculate);
        saveButton = findViewById(R.id.buttonSave);
        uploadButton = findViewById(R.id.buttonUpload);
    }

    private void setupClickListeners() {
        calculateButton.setOnClickListener(v -> calculateCarbonFootprint());
        saveButton.setOnClickListener(v -> saveElectricityData());
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
        startActivityForResult(Intent.createChooser(intent, "Select Bill Image"), PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {
            selectedImageUri = data.getData();
            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), selectedImageUri);
                billImageView.setImageBitmap(bitmap);
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private String saveImageToInternalStorage(Uri imageUri) {
        try {
            String fileName = "bill_" + System.currentTimeMillis() + ".jpg";
            File directory = new File(getFilesDir(), "bill_images");
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

    private void saveElectricityData() {
        String consumptionText = consumptionEditText.getText().toString().trim();

        if (consumptionText.isEmpty()) {
            Toast.makeText(this, "Please enter electricity consumption", Toast.LENGTH_SHORT).show();
            return;
        }

        String imagePath = null;
        if (selectedImageUri != null) {
            imagePath = saveImageToInternalStorage(selectedImageUri);
        }

        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COLUMN_ACTIVITY_NAME, "Electricity Usage");
        values.put(DatabaseHelper.COLUMN_ACTIVITY_DATE, System.currentTimeMillis());
        values.put(DatabaseHelper.COLUMN_ACTIVITY_CARBON_FOOTPRINT, calculatedCarbonFootprint);
        values.put(DatabaseHelper.COLUMN_BILL_IMAGE_PATH, imagePath);  // Add this column to your database

        long result = dbHelper.getWritableDatabase().insert(DatabaseHelper.TABLE_ACTIVITIES, null, values);
        if (result != -1) {
            Toast.makeText(this, "Electricity data saved successfully", Toast.LENGTH_SHORT).show();
            finish();
        } else {
            Toast.makeText(this, "Failed to save data", Toast.LENGTH_SHORT).show();
        }
    }


    private void calculateCarbonFootprint() {
        String consumptionText = consumptionEditText.getText().toString().trim();

        if (consumptionText.isEmpty()) {
            Toast.makeText(this, "Please enter electricity consumption", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            float consumption = Float.parseFloat(consumptionText);
            // Assuming an average carbon footprint of 0.38 kg CO₂ per kWh
            calculatedCarbonFootprint = consumption * 0.38f;

            Toast.makeText(this, String.format("Carbon Footprint: %.2f kg CO₂", calculatedCarbonFootprint), Toast.LENGTH_LONG).show();
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Invalid consumption value", Toast.LENGTH_SHORT).show();
        }
    }
}
