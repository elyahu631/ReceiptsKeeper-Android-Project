package com.example.receiptskeeper;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.ImageButton;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProvider;
import com.example.receiptskeeper.classes.Business;
import com.example.receiptskeeper.classes.SharedViewModel;
import com.example.receiptskeeper.fragment.CalendarFragment;
import com.example.receiptskeeper.fragment.DailyFragment;
import com.example.receiptskeeper.fragment.HomeFragment;
import com.example.receiptskeeper.fragment.ProfileFragment;
import com.example.receiptskeeper.fragment.WeeklyFragment;
import com.example.receiptskeeper.utils.FirebaseHandler;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseUser;

public class HomeActivity extends AppCompatActivity{

    private SharedViewModel sharedViewModel;
    private SharedPreferences sharedPreferences;

    private static final int GALLERY_PERMISSION_REQUEST_CODE = 100;
    private static final int CAMERA_PERMISSION_REQUEST_CODE = 200;

    private Business business;
    private ImageButton btnProfile;
    private FragmentManager fragmentManager;
    private FirebaseHandler firebaseHandler;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        initFirebase();
        initSharedViewModel();
        fetchBusinessData(sharedViewModel);
        initAndCheckPermissions();
        initFragmentManager();
        initViews();
        moveToProfile();
    }

    private void initFirebase() {
        firebaseHandler = FirebaseHandler.getInstance();
    }

    private void initViews() {
        btnProfile = findViewById(R.id.btnProfile);
        ImageView navToHome = findViewById(R.id.app_home);
        btnNavToHome(navToHome);
        BottomNavigationView bottomNavigationView = findViewById(R.id.bnv_Main);
        bottomNavigationView.setOnItemSelectedListener(this::handleBottomNavigationItemClick);
        bottomNavigationView.setSelectedItemId(R.id.btnHome);
    }

    private void initSharedViewModel() {
        sharedViewModel = new ViewModelProvider(this).get(SharedViewModel.class);
    }

    private void initAndCheckPermissions() {
        requestPermissions();
    }

    private void initFragmentManager() {
        fragmentManager = getSupportFragmentManager();
    }

    private void fetchBusinessData(SharedViewModel sharedViewModel) {
        FirebaseUser currentUser = firebaseHandler.getCurrentUser();
        if(currentUser == null){
            Log.e("HomeActivityData", "currentUser is null");
            return;
        }

        String userId = currentUser.getUid();

         sharedPreferences = getSharedPreferences("BusinessData" + userId, Context.MODE_PRIVATE);
        // Try to fetch business data from SharedPreferences
        Business loadedBusiness = loadBusinessFromPreferences();

        if (loadedBusiness != null) {
            // Business data exists in SharedPreferences, use this
            business = loadedBusiness;
            sharedViewModel.setBusiness(business);
            Log.d("HomeActivityData", "Data loaded from Shared Preferences");
        } else {
            // Business data does not exist in SharedPreferences, fetch from Firebase
            firebaseHandler.fetchBusinessData(userId, sharedPreferences, new FirebaseHandler.OnDataFetchedListener() {
                @Override
                public void onDataFetched(Business fetchedBusiness) {
                    business = fetchedBusiness;
                    sharedViewModel.setBusiness(business);
                    Log.d("HomeActivityData", "Data loaded from Firebase");
                }

                @Override
                public void onError(String errorMessage) {
                    Log.e("HomeActivityData", "Firebase error: " + errorMessage);
                }
            });
        }
    }

    private Business loadBusinessFromPreferences() {
        String ownerName = sharedPreferences.getString("ownerName", null);
        String businessName = sharedPreferences.getString("businessName", null);
        String storedImageUrl = sharedPreferences.getString("imageUrl", null);

        if (ownerName != null && businessName != null && storedImageUrl != null) {
            return new Business(ownerName, businessName, storedImageUrl);
        } else {
            return null;
        }
    }

    private void requestPermissions() {
        checkAndRequestPermission(Manifest.permission.READ_EXTERNAL_STORAGE, GALLERY_PERMISSION_REQUEST_CODE);
        checkAndRequestPermission(Manifest.permission.CAMERA, CAMERA_PERMISSION_REQUEST_CODE);
    }

    private void checkAndRequestPermission(String permission, int requestCode) {
        if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{permission}, requestCode);
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == GALLERY_PERMISSION_REQUEST_CODE){
            checkAndRequestPermission(Manifest.permission.CAMERA, CAMERA_PERMISSION_REQUEST_CODE);
        }
    }

    private void handleHomeClick() {
        HomeFragment homeFragment = new HomeFragment();
        if (business != null) {
            Bundle bundle = new Bundle();
            bundle.putSerializable("BusinessData", business);
            homeFragment.setArguments(bundle);
        }
        fragmentManager.beginTransaction()
                .replace(R.id.fragmentContainerView, homeFragment, "HomeFragment")
                .setReorderingAllowed(true)
                .addToBackStack("HomeFragment")
                .commit();
        setTitle("בית");
        btnProfile.setImageResource(R.drawable.icon_profile);
    }

    private void handleQueuesClick() {
        Bundle bundle = new Bundle();
        DailyFragment dailyFragment = new DailyFragment();
        dailyFragment.setArguments(bundle);
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.fragmentContainerView, dailyFragment, "DailyFragment");
        fragmentTransaction.addToBackStack("DailyFragment");
        fragmentTransaction.commit();
    }

    private void handleCalendarClick() {
        CalendarFragment calendarFragment = new CalendarFragment();
        fragmentManager.beginTransaction()
                .replace(R.id.fragmentContainerView, calendarFragment, "CalendarFragment")
                .setReorderingAllowed(true)
                .addToBackStack("CalendarFragment")
                .commit();
        setTitle("Calendar");
    }

    private void handleWeeklyClick() {
        WeeklyFragment weeklyFragment = new WeeklyFragment();
        fragmentManager.beginTransaction()
                .replace(R.id.fragmentContainerView, weeklyFragment, "WeeklyFragment")
                .setReorderingAllowed(true)
                .addToBackStack("WeeklyFragment")
                .commit();
        setTitle("שבועי");
    }

    private void handleProfileClick() {
        ProfileFragment profileFragment = new ProfileFragment();
        fragmentManager.beginTransaction()
                .replace(R.id.fragmentContainerView, profileFragment, "ProfileFragment")
                .setReorderingAllowed(true)
                .addToBackStack("name")
                .commit();
        setTitle("פרופיל");
    }

    private void moveToProfile() {
        btnProfile.setOnClickListener(v -> {
            Fragment profileFragment = fragmentManager.findFragmentByTag("ProfileFragment");
            if (profileFragment == null || !profileFragment.isVisible()) {
                handleProfileClick();
                btnProfile.setImageResource(R.drawable.icon_arrow_circle_right);
            } else {
                onBackPressed();
                btnProfile.setImageResource(R.drawable.icon_profile);
            }
        });
    }

    private void btnNavToHome(ImageView navToHome){
        navToHome.setOnClickListener(v -> {
            Fragment homeFragment = fragmentManager.findFragmentByTag("HomeFragment");
            if (homeFragment == null || !homeFragment.isVisible()) {
                handleHomeClick();
            }
        });
    }

    private boolean isFragmentOpen(Fragment fragment){
        return fragment == null || !fragment.isVisible();
    }

    private boolean handleBottomNavigationItemClick(MenuItem item) {
        btnProfile.setImageResource(R.drawable.icon_profile);
        int itemId = item.getItemId();

        if (itemId == R.id.btnHome) {
            Fragment homeFragment = fragmentManager.findFragmentByTag("HomeFragment");
            if (isFragmentOpen(homeFragment)) {
                handleHomeClick();
            }
        } else if (itemId == R.id.btnQueues) {
            handleQueuesClick();
        } else if (itemId == R.id.btnCalendar) {
            Fragment calendarFragment = fragmentManager.findFragmentByTag("CalendarFragment");
            if (isFragmentOpen(calendarFragment)) {
                handleCalendarClick();
            }
        } else if (itemId == R.id.btnWeekly) {
            Fragment weeklyFragment = fragmentManager.findFragmentByTag("WeeklyFragment");
            if (isFragmentOpen(weeklyFragment)) {
                handleWeeklyClick();
            }
        }
        return true;
    }

    @Override
    public void onBackPressed() {
        Fragment profileFragment = fragmentManager.findFragmentByTag("ProfileFragment");
        if (profileFragment != null && profileFragment.isVisible()) {
            btnProfile.setImageResource(R.drawable.icon_profile);
        }
        super.onBackPressed();
    }
}