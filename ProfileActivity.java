package com.example.carbonfootprintcalculator;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class ProfileActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 123;
    private DatabaseHelper dbHelper;
    private EditText nameEditText, phoneEditText, cityEditText, emailEditText;
    private MaterialButton updateButton, logoutButton;
    private ImageView profileImageView;
    private ImageButton editProfileImageButton;
    private String userEmail;
    private Uri selectedImageUri;

    // Activity Result Launcher for getting image from gallery
    private final ActivityResultLauncher<Intent> galleryLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri imageUri = result.getData().getData();
                    handleSelectedImage(imageUri);
                }
            });

    // Activity Result Launcher for capturing image from camera
    private final ActivityResultLauncher<Intent> cameraLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Bundle extras = result.getData().getExtras();
                    if (extras != null) {
                        Bitmap imageBitmap = (Bitmap) extras.get("data");
                        handleCapturedImage(imageBitmap);
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        initializeViews();
        setupClickListeners();
        loadUserData();
    }

    private void initializeViews() {
        dbHelper = new DatabaseHelper(this);
        userEmail = getIntent().getStringExtra("USER_EMAIL");

        nameEditText = findViewById(R.id.editTextName);
        phoneEditText = findViewById(R.id.editTextPhone);
        cityEditText = findViewById(R.id.editTextCity);
        emailEditText = findViewById(R.id.editTextEmail);
        updateButton = findViewById(R.id.buttonUpdate);
        logoutButton = findViewById(R.id.buttonLogout);
        profileImageView = findViewById(R.id.profileImage);
        editProfileImageButton = findViewById(R.id.editProfileImage);
    }

    private void setupClickListeners() {
        updateButton.setOnClickListener(v -> updateUserProfile());

        logoutButton.setOnClickListener(v -> {
            Intent intent = new Intent(ProfileActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
        });

        editProfileImageButton.setOnClickListener(v -> showImagePickerOptions());
    }

    private void showImagePickerOptions() {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
        bottomSheetDialog.setContentView(R.layout.bottom_sheet_imagepicker);

        MaterialButton btnCamera = bottomSheetDialog.findViewById(R.id.btnCamera);
        MaterialButton btnGallery = bottomSheetDialog.findViewById(R.id.btnGallery);

        btnCamera.setOnClickListener(v -> {
            if (checkCameraPermission()) {
                openCamera();
            }
            bottomSheetDialog.dismiss();
        });

        btnGallery.setOnClickListener(v -> {
            if (checkStoragePermission()) {
                openGallery();
            }
            bottomSheetDialog.dismiss();
        });

        bottomSheetDialog.show();
    }

    private boolean checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA},
                    PERMISSION_REQUEST_CODE);
            return false;
        }
        return true;
    }

    private boolean checkStoragePermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    PERMISSION_REQUEST_CODE);
            return false;
        }
        return true;
    }

    private void openCamera() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            cameraLauncher.launch(takePictureIntent);
        }
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        galleryLauncher.launch(intent);
    }

    private void handleSelectedImage(Uri imageUri) {
        try {
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
            profileImageView.setImageBitmap(bitmap);
            selectedImageUri = imageUri;
            saveProfileImage(bitmap);
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show();
        }
    }

    private void handleCapturedImage(Bitmap bitmap) {
        profileImageView.setImageBitmap(bitmap);
        saveProfileImage(bitmap);
    }

    private void saveProfileImage(Bitmap bitmap) {
        // Create a file to save the image
        File filesDir = getApplicationContext().getFilesDir();
        File imageFile = new File(filesDir, "profile_" + userEmail + ".jpg");

        try {
            // Compress the bitmap and save it to the file
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bos);
            byte[] bitmapData = bos.toByteArray();

            FileOutputStream fos = new FileOutputStream(imageFile);
            fos.write(bitmapData);
            fos.flush();
            fos.close();

            // Update the image path in the database
            dbHelper.updateProfileImage(userEmail, imageFile.getAbsolutePath());
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to save image", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadUserData() {
        Cursor cursor = dbHelper.getUserDetails(userEmail);
        if (cursor != null && cursor.moveToFirst()) {
            try {
                String name = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_USER_NAME));
                String phone = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_USER_PHONE));
                String city = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_USER_CITY));
                String email = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_USER_EMAIL));
                String imagePath = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_PROFILE_IMAGE));

                nameEditText.setText(name);
                phoneEditText.setText(phone);
                cityEditText.setText(city);
                emailEditText.setText(email);
                emailEditText.setEnabled(false);

                // Load profile image if exists
                if (imagePath != null && !imagePath.isEmpty()) {
                    File imageFile = new File(imagePath);
                    if (imageFile.exists()) {
                        Bitmap bitmap = BitmapFactory.decodeFile(imagePath);
                        profileImageView.setImageBitmap(bitmap);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(this, "Failed to load profile data", Toast.LENGTH_SHORT).show();
            } finally {
                cursor.close();
            }
        } else {
            Toast.makeText(this, "User data not found", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateUserProfile() {
        String name = nameEditText.getText().toString().trim();
        String phone = phoneEditText.getText().toString().trim();
        String city = cityEditText.getText().toString().trim();

        if (dbHelper.updateUser(name, phone, city, userEmail)) {
            Toast.makeText(this, "Profile updated successfully", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Profile update failed", Toast.LENGTH_SHORT).show();
        }
    }
}