package com.example.receiptskeeper.fragment;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProvider;

import com.airbnb.lottie.LottieAnimationView;
import com.example.receiptskeeper.MainActivity;
import com.example.receiptskeeper.R;
import com.example.receiptskeeper.classes.Business;
import com.example.receiptskeeper.classes.SharedViewModel;
import com.example.receiptskeeper.utils.FirebaseHandler;
import com.squareup.picasso.Picasso;
import java.util.Calendar;

import cn.pedant.SweetAlert.SweetAlertDialog;

public class HomeFragment extends Fragment {

    private FirebaseHandler firebaseHandler;
    private TextView businessOwnerNameTextView, businessNameTextView;
    private ImageView businessImageView;
    private Business business;
    private Button btnDaily ,btn_report;

    private SharedPreferences sharedPreferences;

    private boolean isDialogShown = false;
//    private boolean isBusinessInfoDialogShown = false;

    public HomeFragment() {
        firebaseHandler = FirebaseHandler.getInstance();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sharedPreferences = getActivity().getSharedPreferences("com.example.receiptskeeper", Context.MODE_PRIVATE);
        if (getArguments() != null) {
            // from bundle at home activity
            business = (Business) getArguments().getSerializable("BusinessData");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        SharedViewModel sharedViewModel = new ViewModelProvider(requireActivity()).get(SharedViewModel.class);
        sharedViewModel.getBusiness().observe(getViewLifecycleOwner(), this::displayBusinessData);

        initializeFireBase();
        initializeViews(view);
        moveToDailyFragment();
        moveToReport();
        displayBusinessData(business);
        setupBackPressedHandling(view);
        setupLottieAnimation(view);


        return view;
    }

    private void initializeFireBase() {
        firebaseHandler = FirebaseHandler.getInstance();
    }

    private void initializeViews(View view) {
        businessOwnerNameTextView = view.findViewById(R.id.business_owner_name);
        businessNameTextView = view.findViewById(R.id.business_name);
        businessImageView = view.findViewById(R.id.business_image);
        btnDaily = view.findViewById(R.id.btnToday);
        btn_report = view.findViewById(R.id.btn_report);
    }

    private void moveToDailyFragment(){
        btnDaily.setOnClickListener(v -> {
            Bundle bundle = new Bundle();
            DailyFragment dailyFragment = new DailyFragment();
            dailyFragment.setArguments(bundle);
            FragmentManager fragmentManager1 = requireActivity().getSupportFragmentManager();
            FragmentTransaction fragmentTransaction = fragmentManager1.beginTransaction();
            fragmentTransaction.replace(R.id.fragmentContainerView, dailyFragment, "DailyFragment");
            fragmentTransaction.addToBackStack("DailyFragment"); // This line has changed
            fragmentTransaction.commit();
        });
    }

    private void moveToReport(){
        btn_report.setOnClickListener(v -> {
            //send to report
            ReportsFragment newFragment = new ReportsFragment();
            int currentYear = Calendar.getInstance().get(Calendar.YEAR);
            int currentMonth = Calendar.getInstance().get(Calendar.MONTH);
            Bundle bundle = new Bundle();
            bundle.putInt("year", currentYear);
            bundle.putInt("month", currentMonth+1);
            newFragment.setArguments(bundle);
            FragmentManager fragmentManager = requireActivity().getSupportFragmentManager();
            FragmentTransaction transaction = fragmentManager.beginTransaction();
            transaction.replace(R.id.fragmentContainerView, newFragment); // Replace 'fragment_container' with the ID of the container in your layout
            transaction.addToBackStack(null); // Add the transaction to the back stack
            transaction.commit(); // Commit the transaction
        });
    }

    private void displayBusinessData(Business business) {
        if(business != null){

            businessOwnerNameTextView.setText("ברוך הבא: " + business.getOwnerName() + " ל - ReceiptsKeeper");
            businessNameTextView.setText(business.getBusinessName());

            String imageUrl = business.getImageUrl();
            if (imageUrl != null && !imageUrl.isEmpty()) {
                loadBusinessImage(imageUrl);
            }
        }
    }

    private void loadBusinessImage(String imageUrl) {
        Picasso.get()
                .load(imageUrl)
                .placeholder(R.mipmap.icon_app_foreground)
                .into(businessImageView);
    }

    private void setupBackPressedHandling(View view) {
        view.setFocusableInTouchMode(true);
        view.requestFocus();
        view.setOnKeyListener((v, keyCode, event) -> {
            if (keyCode == KeyEvent.KEYCODE_BACK) {
                showSignOutDialog();
                return true;
            }
            return false;
        });
    }

    private void setupLottieAnimation(View view) {
        LottieAnimationView lottieAnimationView = view.findViewById(R.id.lottieAnimationView);
        lottieAnimationView.setAnimation(R.raw.home_animetion);
        lottieAnimationView.playAnimation();

        // New code: Stop animation after 3 seconds
        new Handler().postDelayed(() -> {
            if (lottieAnimationView != null) {
                lottieAnimationView.pauseAnimation();
            }
        }, 3000);  // time in milliseconds
    }

    private void showSignOutDialog() {
        if (!isDialogShown) {
            isDialogShown = true;
            SweetAlertDialog dialog = new SweetAlertDialog(requireActivity(), SweetAlertDialog.WARNING_TYPE)
                    .setTitleText("יציאה")
                    .setContentText("האם אתה רוצה לצאת מהאפליקציה?")
                    .setConfirmText("כן")
                    .setCancelText("לא")
                    .setConfirmClickListener(sDialog -> {
                        sDialog.dismissWithAnimation();
                        firebaseHandler.signOut();
                        startActivity(new Intent(getActivity(), MainActivity.class));
                    });

            dialog.showCancelButton(true);
            dialog.setCancelClickListener(sDialog -> {
                sDialog.dismissWithAnimation();
                isDialogShown = false;  // reset the flag when dialog is dismissed
            });
            dialog.setOnDismissListener(dialogInterface -> isDialogShown = false);  // reset the flag when dialog is dismissed for any reason
            dialog.show();
        }
    }

}