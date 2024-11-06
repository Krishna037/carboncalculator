package com.example.carbonfootprintcalculator;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "CarbonFootprintCalculator.db";
    private static final int DATABASE_VERSION = 2;

    // User Table
    public static final String TABLE_USER = "User";
    public static final String COLUMN_USER_ID = "_id";
    public static final String COLUMN_USER_NAME = "name";
    public static final String COLUMN_USER_PHONE = "phone";
    public static final String COLUMN_USER_CITY = "city";
    public static final String COLUMN_USER_EMAIL = "email";
    public static final String COLUMN_USER_PASSWORD = "password";
    public static final String COLUMN_PROFILE_IMAGE = "profile_image";

    // Activity Table
    public static final String TABLE_ACTIVITIES = "Activities";
    public static final String COLUMN_ACTIVITY_NAME = "activity_name";
    public static final String COLUMN_ACTIVITY_DATE = "date";
    public static final String COLUMN_ACTIVITY_CARBON_FOOTPRINT = "carbon_footprint";
    public static final String COLUMN_BILL_IMAGE_PATH = "bill_image_path";
    public static final String COLUMN_FUEL_USED = "fuel_used"; // New column
    public static final String COLUMN_VEHICLE_MILEAGE = "vehicle_mileage";
    public static final String COLUMN_TRAVEL_DISTANCE = "travel_distance";
    public static final String COLUMN_TICKET_IMAGE_PATH = "ticket_image_path";

//    setting table
    public static final String TABLE_SETTINGS = "Settings";
    public static final String COLUMN_SETTING_NAME = "setting_name";
    public static final String COLUMN_SETTING_VALUE = "setting_value";

    private static final String CREATE_SETTINGS_TABLE = "CREATE TABLE " + TABLE_SETTINGS + "(" +
            COLUMN_USER_ID + " INTEGER, " +
            COLUMN_SETTING_NAME + " TEXT, " +
            COLUMN_SETTING_VALUE + " TEXT, " +
            "FOREIGN KEY(" + COLUMN_USER_ID + ") REFERENCES " + TABLE_USER + "(" + COLUMN_USER_ID + "), " +
            "UNIQUE(" + COLUMN_USER_ID + ", " + COLUMN_SETTING_NAME + "));";

    private static final String CREATE_USER_TABLE = "CREATE TABLE " + TABLE_USER + "(" +
            COLUMN_USER_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
            COLUMN_USER_NAME + " TEXT, " +
            COLUMN_USER_PHONE + " TEXT, " +
            COLUMN_USER_CITY + " TEXT, " +
            COLUMN_USER_EMAIL + " TEXT UNIQUE, " +
            COLUMN_USER_PASSWORD + " TEXT," + COLUMN_PROFILE_IMAGE +  "TEXT)";

    private static final String CREATE_ACTIVITIES_TABLE = "CREATE TABLE " + TABLE_ACTIVITIES + " (" +
            COLUMN_USER_ID + " INTEGER, " +
            COLUMN_ACTIVITY_NAME + " TEXT, " +
            COLUMN_ACTIVITY_DATE + " TEXT, " +
            COLUMN_ACTIVITY_CARBON_FOOTPRINT + " REAL, " +
            COLUMN_TICKET_IMAGE_PATH + " TEXT, " +
            COLUMN_FUEL_USED + " REAL, " +
            COLUMN_VEHICLE_MILEAGE + " REAL, " +
            COLUMN_TRAVEL_DISTANCE + " REAL, " +
            "FOREIGN KEY(" + COLUMN_USER_ID + ") REFERENCES " + TABLE_USER + "(" + COLUMN_USER_ID + "));";


    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_USER_TABLE);
        db.execSQL(CREATE_ACTIVITIES_TABLE);
        db.execSQL(CREATE_SETTINGS_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            // Add new columns without dropping tables
            db.execSQL("ALTER TABLE " + TABLE_ACTIVITIES + " ADD COLUMN " + COLUMN_FUEL_USED + " REAL");
            db.execSQL("ALTER TABLE " + TABLE_ACTIVITIES + " ADD COLUMN " + COLUMN_VEHICLE_MILEAGE + " REAL");
            db.execSQL("ALTER TABLE " + TABLE_ACTIVITIES + " ADD COLUMN " + COLUMN_TRAVEL_DISTANCE + " REAL");

            // Rename existing column
            db.execSQL("ALTER TABLE " + TABLE_ACTIVITIES + " RENAME COLUMN " + COLUMN_BILL_IMAGE_PATH +
                    " TO " + COLUMN_TICKET_IMAGE_PATH);
        }
    }

    public boolean addTransportActivity(int userId, float distance, float mileage, float fuelUsed,
                                        float carbonFootprint, String ticketImagePath) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();

        values.put(COLUMN_USER_ID, userId);
        values.put(COLUMN_ACTIVITY_NAME, "Transport Usage");
        values.put(COLUMN_ACTIVITY_DATE, System.currentTimeMillis());
        values.put(COLUMN_TRAVEL_DISTANCE, distance);
        values.put(COLUMN_VEHICLE_MILEAGE, mileage);
        values.put(COLUMN_FUEL_USED, fuelUsed);
        values.put(COLUMN_ACTIVITY_CARBON_FOOTPRINT, carbonFootprint);
        values.put(COLUMN_TICKET_IMAGE_PATH, ticketImagePath);

        long result = db.insert(TABLE_ACTIVITIES, null, values);
        db.close();
        return result != -1;
    }

    // Method to retrieve user ID by email
    public int getUserIdByEmail(String email) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_USER, new String[]{COLUMN_USER_ID},
                COLUMN_USER_EMAIL + "=?", new String[]{email}, null, null, null);
        int userId = -1;
        if (cursor.moveToFirst()) {
            userId = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_USER_ID));
        }
        cursor.close();
        return userId;
    }
    public boolean addUser(String name, String phone, String city, String email, String password) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_USER_NAME, name);
        values.put(COLUMN_USER_PHONE, phone);
        values.put(COLUMN_USER_CITY, city);
        values.put(COLUMN_USER_EMAIL, email);
        values.put(COLUMN_USER_PASSWORD, encryptPassword(password));
        values.put(COLUMN_PROFILE_IMAGE, "default_image_path"); // Set a default profile image path or placeholder

        // Insert the user and get the generated user ID
        long userId = db.insert(TABLE_USER, null, values);

        if (userId != -1) {
            // Initialize settings and activities with default values for this user
            initializeUserDefaults((int) userId, db);
        }
        db.close();
        return userId != -1; // Return true if insert was successful
    }

    // Method to initialize default values for a new user in settings and activities
    private void initializeUserDefaults(int userId, SQLiteDatabase db) {
        // Set default settings, e.g., theme, notifications enabled
        ContentValues settingValues = new ContentValues();
        settingValues.put(COLUMN_USER_ID, userId);
        settingValues.put(COLUMN_SETTING_NAME, "theme");
        settingValues.put(COLUMN_SETTING_VALUE, "light"); // Default theme value
        db.insert(TABLE_SETTINGS, null, settingValues);

        settingValues.clear();
        settingValues.put(COLUMN_USER_ID, userId);
        settingValues.put(COLUMN_SETTING_NAME, "notifications");
        settingValues.put(COLUMN_SETTING_VALUE, "enabled"); // Default notifications value
        db.insert(TABLE_SETTINGS, null, settingValues);

        // Set default activity values
        ContentValues activityValues = new ContentValues();
        activityValues.put(COLUMN_USER_ID, userId);
        activityValues.put(COLUMN_ACTIVITY_NAME, "Welcome Activity"); // Meaningful default name
        activityValues.put(COLUMN_ACTIVITY_DATE, "2024-01-01"); // Default date for activity
        activityValues.put(COLUMN_ACTIVITY_CARBON_FOOTPRINT, 1.0f); // Default carbon footprint value
        db.insert(TABLE_ACTIVITIES, null, activityValues);
    }


    // Check if an email is already registered
    public boolean isEmailRegistered(String email) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_USER, new String[]{COLUMN_USER_ID},
                COLUMN_USER_EMAIL + "=?", new String[]{email},
                null, null, null);
        boolean exists = cursor.getCount() > 0;
        cursor.close();
        db.close();
        return exists;
    }

    // Validate user login
    public boolean isUserValid(String email, String password) {
        SQLiteDatabase db = this.getReadableDatabase();
        String encryptedPassword = encryptPassword(password);

        Cursor cursor = db.query(TABLE_USER, new String[]{COLUMN_USER_ID},
                COLUMN_USER_EMAIL + "=? AND " + COLUMN_USER_PASSWORD + "=?",
                new String[]{email, encryptedPassword},
                null, null, null);
        boolean isValid = cursor.getCount() > 0;
        cursor.close();
        db.close();
        return isValid;
    }

    // Retrieve user details by email
    public Cursor getUserDetails(String email) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.query(TABLE_USER, null, COLUMN_USER_EMAIL + "=?", new String[]{email},
                null, null, null);
    }

    // Update user information
    public boolean updateUser(String name, String phone, String city, String email) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_USER_NAME, name);
        values.put(COLUMN_USER_PHONE, phone);
        values.put(COLUMN_USER_CITY, city);

        int rowsAffected = db.update(TABLE_USER, values, COLUMN_USER_EMAIL + "=?", new String[]{email});
        db.close();
        return rowsAffected > 0;
    }

    // Delete user
    public boolean deleteUser(String email) {
        SQLiteDatabase db = this.getWritableDatabase();
        int rowsDeleted = db.delete(TABLE_USER, COLUMN_USER_EMAIL + "=?", new String[]{email});
        db.close();
        return rowsDeleted > 0;
    }

    // Add an activity log entry for a user
    public boolean addActivity(int userId, String activityName, String date, float carbonFootprint) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_USER_ID, userId);
        values.put(COLUMN_ACTIVITY_NAME, activityName);
        values.put(COLUMN_ACTIVITY_DATE, date);
        values.put(COLUMN_ACTIVITY_CARBON_FOOTPRINT, carbonFootprint);

        long result = db.insert(TABLE_ACTIVITIES, null, values);
        db.close();
        return result != -1;
    }

    // Retrieve all activity logs for a specific user
    public Cursor getUserActivities(int userId) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.query(TABLE_ACTIVITIES, null, COLUMN_USER_ID + "=?", new String[]{String.valueOf(userId)},
                null, null, COLUMN_ACTIVITY_DATE + " DESC");
    }

    // Encrypt password using SHA-256
    private String encryptPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes("UTF-8"));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException | java.io.UnsupportedEncodingException e) {
            e.printStackTrace();
            return null;
        }
    }

    public boolean updateProfileImage(String email, String imagePath) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_PROFILE_IMAGE, imagePath);

        int rowsAffected = db.update(TABLE_USER, values, COLUMN_USER_EMAIL + " = ?",
                new String[]{email});
        return rowsAffected > 0;
    }

    // Method to save a setting
    public boolean saveSetting(int userId, String settingName, String settingValue) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_USER_ID, userId);
        values.put(COLUMN_SETTING_NAME, settingName);
        values.put(COLUMN_SETTING_VALUE, settingValue);

        try {
            db.insertWithOnConflict(TABLE_SETTINGS, null, values,
                    SQLiteDatabase.CONFLICT_REPLACE);
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        } finally {
            db.close();
        }
    }

    // Method to get a setting
    public String getSetting(int userId, String settingName, String defaultValue) {
        SQLiteDatabase db = this.getReadableDatabase();
        String value = defaultValue;

        Cursor cursor = db.query(TABLE_SETTINGS,
                new String[]{COLUMN_SETTING_VALUE},
                COLUMN_USER_ID + "=? AND " + COLUMN_SETTING_NAME + "=?",
                new String[]{String.valueOf(userId), settingName},
                null, null, null);

        if (cursor.moveToFirst()) {
            value = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_SETTING_VALUE));
        }
        cursor.close();
        db.close();
        return value;
    }

    // Method to get all settings for a user
    public Map<String, String> getAllSettings(int userId) {
        Map<String, String> settings = new HashMap<>();
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.query(TABLE_SETTINGS,
                new String[]{COLUMN_SETTING_NAME, COLUMN_SETTING_VALUE},
                COLUMN_USER_ID + "=?",
                new String[]{String.valueOf(userId)},
                null, null, null);

        while (cursor.moveToNext()) {
            String name = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_SETTING_NAME));
            String value = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_SETTING_VALUE));
            settings.put(name, value);
        }
        cursor.close();
        db.close();
        return settings;
    }

    public void insertDummyUsers(SQLiteDatabase db) {
        ContentValues values = new ContentValues();

        // User 1
        values.put(COLUMN_USER_NAME, "Alice Johnson");
        values.put(COLUMN_USER_PHONE, "1234567890");
        values.put(COLUMN_USER_CITY, "New York");
        values.put(COLUMN_USER_EMAIL, "alice@example.com");
        values.put(COLUMN_USER_PASSWORD, encryptPassword("password123"));
        values.put(COLUMN_PROFILE_IMAGE, "default_image_path");
        db.insert(TABLE_USER, null, values);

        // User 2
        values.clear();
        values.put(COLUMN_USER_NAME, "Bob Smith");
        values.put(COLUMN_USER_PHONE, "0987654321");
        values.put(COLUMN_USER_CITY, "Los Angeles");
        values.put(COLUMN_USER_EMAIL, "bob@example.com");
        values.put(COLUMN_USER_PASSWORD, encryptPassword("password123"));
        values.put(COLUMN_PROFILE_IMAGE, "default_image_path");
        db.insert(TABLE_USER, null, values);

        // User 3
        values.clear();
        values.put(COLUMN_USER_NAME, "Charlie Brown");
        values.put(COLUMN_USER_PHONE, "5551234567");
        values.put(COLUMN_USER_CITY, "Chicago");
        values.put(COLUMN_USER_EMAIL, "charlie@example.com");
        values.put(COLUMN_USER_PASSWORD, encryptPassword("password123"));
        values.put(COLUMN_PROFILE_IMAGE, "default_image_path");
        db.insert(TABLE_USER, null, values);

        // User 4 (New User: Krishna)
        values.clear();
        values.put(COLUMN_USER_NAME, "Krishna");
        values.put(COLUMN_USER_PHONE, "7778889990");
        values.put(COLUMN_USER_CITY, "Mumbai");
        values.put(COLUMN_USER_EMAIL, "krishna.college.learn@gmail.com");
        values.put(COLUMN_USER_PASSWORD, encryptPassword("159874236"));
        values.put(COLUMN_PROFILE_IMAGE, "default_image_path");
        db.insert(TABLE_USER, null, values);
    }

    public void insertDummySettings(SQLiteDatabase db) {
        ContentValues values = new ContentValues();

        // Settings for User 1
        values.put(COLUMN_USER_ID, 1);
        values.put(COLUMN_SETTING_NAME, "theme");
        values.put(COLUMN_SETTING_VALUE, "light");
        db.insert(TABLE_SETTINGS, null, values);

        values.clear();
        values.put(COLUMN_USER_ID, 1);
        values.put(COLUMN_SETTING_NAME, "notifications");
        values.put(COLUMN_SETTING_VALUE, "enabled");
        db.insert(TABLE_SETTINGS, null, values);

        // Settings for User 2
        values.clear();
        values.put(COLUMN_USER_ID, 2);
        values.put(COLUMN_SETTING_NAME, "theme");
        values.put(COLUMN_SETTING_VALUE, "dark");
        db.insert(TABLE_SETTINGS, null, values);

        values.clear();
        values.put(COLUMN_USER_ID, 2);
        values.put(COLUMN_SETTING_NAME, "notifications");
        values.put(COLUMN_SETTING_VALUE, "disabled");
        db.insert(TABLE_SETTINGS, null, values);

        // Settings for User 3
        values.clear();
        values.put(COLUMN_USER_ID, 3);
        values.put(COLUMN_SETTING_NAME, "theme");
        values.put(COLUMN_SETTING_VALUE, "light");
        db.insert(TABLE_SETTINGS, null, values);

        values.clear();
        values.put(COLUMN_USER_ID, 3);
        values.put(COLUMN_SETTING_NAME, "notifications");
        values.put(COLUMN_SETTING_VALUE, "enabled");
        db.insert(TABLE_SETTINGS, null, values);

        // Settings for User 4 (Krishna)
        values.clear();
        values.put(COLUMN_USER_ID, 4);
        values.put(COLUMN_SETTING_NAME, "theme");
        values.put(COLUMN_SETTING_VALUE, "dark");
        db.insert(TABLE_SETTINGS, null, values);

        values.clear();
        values.put(COLUMN_USER_ID, 4);
        values.put(COLUMN_SETTING_NAME, "notifications");
        values.put(COLUMN_SETTING_VALUE, "enabled");
        db.insert(TABLE_SETTINGS, null, values);
    }
    public void insertDummyActivities(SQLiteDatabase db) {
        ContentValues values = new ContentValues();

        // Activity for User 1
        values.put(COLUMN_USER_ID, 1); // Assuming Alice Johnson's user ID is 1
        values.put(COLUMN_ACTIVITY_NAME, "Commute to Work");
        values.put(COLUMN_ACTIVITY_DATE, "2024-01-15");
        values.put(COLUMN_ACTIVITY_CARBON_FOOTPRINT, 4.5f);
        values.put(COLUMN_TRAVEL_DISTANCE, 10.0f);
        values.put(COLUMN_VEHICLE_MILEAGE, 25.0f);
        values.put(COLUMN_FUEL_USED, 0.4f);
        values.put(COLUMN_TICKET_IMAGE_PATH, "path/to/ticket_image.jpg");
        db.insert(TABLE_ACTIVITIES, null, values);

        // Activity for User 2
        values.clear();
        values.put(COLUMN_USER_ID, 2); // Assuming Bob Smith's user ID is 2
        values.put(COLUMN_ACTIVITY_NAME, "Road Trip");
        values.put(COLUMN_ACTIVITY_DATE, "2024-01-20");
        values.put(COLUMN_ACTIVITY_CARBON_FOOTPRINT, 10.0f);
        values.put(COLUMN_TRAVEL_DISTANCE, 150.0f);
        values.put(COLUMN_VEHICLE_MILEAGE, 20.0f);
        values.put(COLUMN_FUEL_USED, 7.5f);
        values.put(COLUMN_TICKET_IMAGE_PATH, "path/to/ticket_image_2.jpg");
        db.insert(TABLE_ACTIVITIES, null, values);

        // Activity for User 3
        values.clear();
        values.put(COLUMN_USER_ID, 3); // Assuming Charlie Brown's user ID is 3
        values.put(COLUMN_ACTIVITY_NAME, "Public Transport");
        values.put(COLUMN_ACTIVITY_DATE, "2024-01-22");
        values.put(COLUMN_ACTIVITY_CARBON_FOOTPRINT, 2.0f);
        values.put(COLUMN_TRAVEL_DISTANCE, 5.0f);
        values.put(COLUMN_VEHICLE_MILEAGE, 0.0f); // Not applicable
        values.put(COLUMN_FUEL_USED, 0.0f); // Not applicable
        values.put(COLUMN_TICKET_IMAGE_PATH, "path/to/ticket_image_3.jpg");
        db.insert(TABLE_ACTIVITIES, null, values);

        // Activity for User 4 (Krishna)
        values.clear();
        values.put(COLUMN_USER_ID, 4); // Assuming Krishna's user ID is 4
        values.put(COLUMN_ACTIVITY_NAME, "Commute to University");
        values.put(COLUMN_ACTIVITY_DATE, "2024-01-25");
        values.put(COLUMN_ACTIVITY_CARBON_FOOTPRINT, 5.5f);
        values.put(COLUMN_TRAVEL_DISTANCE, 20.0f);
        values.put(COLUMN_VEHICLE_MILEAGE, 22.0f);
        values.put(COLUMN_FUEL_USED, 0.9f);
        values.put(COLUMN_TICKET_IMAGE_PATH, "path/to/ticket_image_4.jpg");
        db.insert(TABLE_ACTIVITIES, null, values);
    }


}
