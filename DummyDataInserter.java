package com.example.carbonfootprintcalculator;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.widget.Toast;

public class DummyDataInserter {

    private final Context context;
    private final DatabaseHelper dbHelper;

    public DummyDataInserter(Context context) {
        this.context = context;
        this.dbHelper = new DatabaseHelper(context);
    }

    public void insertDummyData() {
        new InsertDataTask().execute();
    }

    private class InsertDataTask extends AsyncTask<Void, Void, Boolean> {

        @Override
        protected Boolean doInBackground(Void... voids) {
            SQLiteDatabase db = dbHelper.getWritableDatabase();

            try {
                db.beginTransaction();

                // Insert dummy data
                dbHelper.insertDummyUsers(db);
                dbHelper.insertDummySettings(db);
                dbHelper.insertDummyActivities(db);

                db.setTransactionSuccessful();
                return true;
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            } finally {
                db.endTransaction();
                db.close();
            }
        }

        @Override
        protected void onPostExecute(Boolean success) {
            if (success) {
                Toast.makeText(context, "Dummy data inserted successfully.", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(context, "Failed to insert dummy data.", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
