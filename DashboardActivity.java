package com.example.carbonfootprintcalculator;

import android.animation.ValueAnimator;
import android.content.Intent;
import android.database.Cursor;

import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Handler;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.widget.Toolbar;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.tabs.TabLayout;
import androidx.appcompat.app.ActionBar;
import androidx.drawerlayout.widget.DrawerLayout;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DashboardActivity extends AppCompatActivity {

    private DatabaseHelper dbHelper;
    private DrawerLayout drawerLayout;
    private SwipeRefreshLayout swipeRefreshLayout;
    private BarChart comparisonChart;
    private PieChart pieChart;
    private TextView carbonScoreText, totalEmissionsText;
    private ProgressBar goalProgressBar;
    private RecyclerView actionItemsRecycler;
    private ExtendedFloatingActionButton fabQuickAdd;
    private ActionItemsAdapter actionItemsAdapter;
    private List<ActionItem> actionItems;
    private Map<String, Float> emissionCategories;
    private TabLayout timeframeTabLayout;
    private float monthlyGoal = 100f;
    private String userEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        // Initialize the Toolbar and set it as the ActionBar
        Toolbar toolbar = findViewById(R.id.toolbar); // Ensure this ID matches the toolbar in XML
        setSupportActionBar(toolbar);

        // Now get the ActionBar and set DisplayHomeAsUpEnabled
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        // Initialize DatabaseHelper and other variables
        dbHelper = new DatabaseHelper(this);
        userEmail = getIntent().getStringExtra("USER_EMAIL");

        // Initialize views and setup navigation drawer
        initializeViews();
        setupNavigationDrawer();

        // Load user data if available
        if (userEmail != null) {
            loadUserData();
            loadActivityData();
        } else {
            Toast.makeText(this, "User data could not be loaded.", Toast.LENGTH_SHORT).show();
        }

        // Additional setup
        setupCharts();
        setupTimeframeTabs();
        setupSwipeRefresh();
        setupFloatingActionButton();
        updateCarbonScore();
        updateGoalProgress();
    }

    private void initializeViews() {
        drawerLayout = findViewById(R.id.drawer_layout);
        swipeRefreshLayout = findViewById(R.id.swipe_refresh_layout);
        comparisonChart = findViewById(R.id.comparison_chart);
        pieChart = findViewById(R.id.pie_chart);
        carbonScoreText = findViewById(R.id.carbon_score);
        totalEmissionsText = findViewById(R.id.total_emissions_text);
        goalProgressBar = findViewById(R.id.goal_progress);
        actionItemsRecycler = findViewById(R.id.action_items_recycler);
        fabQuickAdd = findViewById(R.id.fab_quick_add);

        ImageButton refreshButton = findViewById(R.id.refresh_button);
        refreshButton.setOnClickListener(v -> refreshDashboard());

        actionItems = new ArrayList<>();
        actionItemsAdapter = new ActionItemsAdapter((ArrayList<ActionItem>) actionItems, this::onActionItemCompleted);
        actionItemsRecycler.setLayoutManager(new LinearLayoutManager(this));
        actionItemsRecycler.setAdapter(actionItemsAdapter);
    }

    private void loadUserData() {
        Cursor cursor = dbHelper.getUserDetails(userEmail);
        if (cursor != null) {
            try {
                if (cursor.moveToFirst()) {
                    // Use exact column names as defined in DatabaseHelper
                    String name = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_USER_NAME));
                    String city = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_USER_CITY));
                    setTitle("Welcome, " + name);
                    Toast.makeText(this, "Location: " + city, Toast.LENGTH_SHORT).show();
                }
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(this, "Failed to load user data", Toast.LENGTH_SHORT).show();
            } finally {
                cursor.close();
            }
        } else {
            Toast.makeText(this, "User data not found", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadActivityData() {
        int userId = dbHelper.getUserIdByEmail(userEmail);
        if (userId != -1) { // Ensure valid userId
            Cursor cursor = dbHelper.getUserActivities(userId);
            if (cursor != null) { // Check if cursor is not null
                try {
                    if (cursor.moveToFirst()) {
                        actionItems.clear();
                        do {
                            String activityName = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_ACTIVITY_NAME));
                            String date = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_ACTIVITY_DATE));
                            float carbonFootprint = cursor.getFloat(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_ACTIVITY_CARBON_FOOTPRINT));
                            actionItems.add(new ActionItem(activityName, date, carbonFootprint));
                        } while (cursor.moveToNext());
                        actionItemsAdapter.notifyDataSetChanged();
                        updateEmissionCategories();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(this, "Failed to load activity data", Toast.LENGTH_SHORT).show();
                } finally {
                    cursor.close();
                }
            } else {
                Toast.makeText(this, "No activities found for this user", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "Invalid user ID", Toast.LENGTH_SHORT).show();
        }
    }


    private void setupSwipeRefresh() {
        swipeRefreshLayout.setOnRefreshListener(() -> {
            refreshDashboard();
            swipeRefreshLayout.setRefreshing(false);
        });
    }

    private void onActionItemCompleted(int position) {
        ActionItem item = actionItems.get(position);
        item.setCompleted(!item.isCompleted());
        actionItemsAdapter.notifyItemChanged(position);
        updateCarbonScore();
    }

    private void setupComparisonChart() {
        ArrayList<BarEntry> entries = new ArrayList<>();
        entries.add(new BarEntry(0, calculateTotalEmissions()));
        entries.add(new BarEntry(1, 150f));
        entries.add(new BarEntry(2, 180f));

        BarDataSet dataSet = new BarDataSet(entries, "Emissions Comparison (kg CO₂)");
        dataSet.setColors(ColorTemplate.MATERIAL_COLORS);

        BarData barData = new BarData(dataSet);
        comparisonChart.setData(barData);

        String[] labels = new String[]{"You", "City Avg", "National Avg"};
        XAxis xAxis = comparisonChart.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
        ((XAxis) xAxis).setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);

        comparisonChart.getDescription().setEnabled(false);
        comparisonChart.animateY(1000);
        comparisonChart.invalidate();
    }

    private void updateCarbonScore() {
        float totalEmissions = calculateTotalEmissions();
        float score = Math.max(0, 100 - (totalEmissions / monthlyGoal) * 100);

        ValueAnimator animator = ValueAnimator.ofFloat(0, score);
        animator.setDuration(1000);
        animator.addUpdateListener(animation -> {
            float animatedValue = (float) animation.getAnimatedValue();
            carbonScoreText.setText(String.format("%.1f", animatedValue));

        });
        animator.start();
    }

    private void updateGoalProgress() {
        float progress = (calculateTotalEmissions() / monthlyGoal) * 100;
        goalProgressBar.setProgress((int) progress);
    }

    private void setupFloatingActionButton() {
        fabQuickAdd.setOnClickListener(view -> showQuickAddBottomSheet());
    }

    private void showQuickAddBottomSheet() {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
        View bottomSheetView = getLayoutInflater().inflate(R.layout.bottom_sheet_quick_add, null);

        bottomSheetView.findViewById(R.id.transport_option).setOnClickListener(v -> {
            startActivity(new Intent(this, AddTransportActivity.class));
            bottomSheetDialog.dismiss();
        });

        bottomSheetView.findViewById(R.id.electricity_option).setOnClickListener(v -> {
            startActivity(new Intent(this, AddElectricityActivity.class));
            bottomSheetDialog.dismiss();
        });

        bottomSheetDialog.setContentView(bottomSheetView);
        bottomSheetDialog.show();
    }

    private void refreshDashboard() {
        swipeRefreshLayout.setRefreshing(true);
        new Handler().postDelayed(() -> {
            loadActivityData();
            setupCharts();
            updateCarbonScore();
            updateGoalProgress();
            showSuggestions();
            swipeRefreshLayout.setRefreshing(false);
        }, 1000);
    }

    private void updateEmissionCategories() {
        float totalEmissions = calculateTotalEmissions();
        if (totalEmissions == 0) return;

        emissionCategories.clear();

        for (ActionItem item : actionItems) {
            String category = item.getCategory();
            emissionCategories.put(category, emissionCategories.getOrDefault(category, 0f) + item.getCarbonFootprint());
        }
    }


    private float calculateTotalEmissions() {
        float total = 0;
        for (ActionItem item : actionItems) {
            total += item.getCarbonFootprint();
        }
        totalEmissionsText.setText(String.format("Total Emissions: %.1f kg CO₂", total));
        return total;
    }

    private void showSuggestions() {
        StringBuilder suggestions = new StringBuilder("Recommendations:\n\n");
        for (Map.Entry<String, Float> entry : emissionCategories.entrySet()) {
            String category = entry.getKey();
            float value = entry.getValue();
            float percentage = (value / calculateTotalEmissions()) * 100;

            if (percentage > 30) {
                suggestions.append("🔴 High ").append(category).append(" emissions. Reduce usage.\n");
            } else if (percentage > 15) {
                suggestions.append("🟡 Moderate ").append(category).append(" emissions.\n");
            } else {
                suggestions.append("🟢 Good ").append(category).append(" emissions.\n");
            }
        }
        Toast.makeText(this, suggestions.toString(), Toast.LENGTH_LONG).show();
    }

    private void setupNavigationDrawer() {
        DrawerLayout drawerLayout = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawerLayout, R.string.open, R.string.close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        navigationView.setNavigationItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.nav_profile) {
                startActivity(new Intent(this, ProfileActivity.class));
            } else if (itemId == R.id.nav_settings) {
                startActivity(new Intent(this, SettingsActivity.class));
            } else if (itemId == R.id.nav_logout) {
                startActivity(new Intent(this, LoginActivity.class));
                finish();
            } else {
                drawerLayout.closeDrawer(GravityCompat.START);
                return false;
            }

            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        });
    }


    private void setupCharts() {

        setupPieChart();
        setupComparisonChart();
    }

    private void setupPieChart() {
        ArrayList<PieEntry> entries = new ArrayList<>();
        for (Map.Entry<String, Float> entry : emissionCategories.entrySet()) {
            entries.add(new PieEntry(entry.getValue(), entry.getKey()));
        }

        PieDataSet dataSet = new PieDataSet(entries, "Emissions by Category");
        dataSet.setColors(ColorTemplate.MATERIAL_COLORS);
        PieData data = new PieData(dataSet);
        pieChart.setData(data);
        pieChart.setDrawHoleEnabled(true);
        pieChart.animateY(1000);
        pieChart.invalidate();
    }

    private void setupTimeframeTabs() {
        timeframeTabLayout = findViewById(R.id.timeframe_tabs);
        timeframeTabLayout.addTab(timeframeTabLayout.newTab().setText("Weekly"));
        timeframeTabLayout.addTab(timeframeTabLayout.newTab().setText("Monthly"));
        timeframeTabLayout.addTab(timeframeTabLayout.newTab().setText("Yearly"));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        dbHelper.close();
    }
}
