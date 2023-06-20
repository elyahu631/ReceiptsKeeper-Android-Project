package com.example.receiptskeeper.fragment;

import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import android.os.Bundle;
import android.util.Pair;
import android.view.View;
import android.view.ViewGroup;
import android.view.LayoutInflater;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.example.receiptskeeper.R;
import com.example.receiptskeeper.utils.FirebaseHandler;
import com.example.receiptskeeper.utils.Utility;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class WeeklyFragment extends Fragment {
    private FirebaseHandler firebaseHandlerObject;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_weekly, container, false);
        initializeFirebase();
        initializeViews(view);
        return view;
    }

    private void initializeFirebase() {
        firebaseHandlerObject = FirebaseHandler.getInstance();
    }

    private void initializeViews(View view) {
        LocalDate today = LocalDate.now();

        List<LocalDate> days = Arrays.asList(
                today.minusDays(3),
                today.minusDays(2),
                today.minusDays(1),
                today,
                today.plusDays(1),
                today.plusDays(2),
                today.plusDays(3)
        );

        List<Integer> receiptCountsIds = Arrays.asList(
                R.id.textView_day_minus3_count_receipts,
                R.id.textView_day_minus2_count_receipts,
                R.id.textView_day_minus1_count_receipts,
                R.id.textView_today_count_receipts,
                R.id.textView_day_plus1_count_receipts,
                R.id.textView_day_plus2_count_receipts,
                R.id.textView_day_plus3_count_receipts
        );

        List<Integer> dateIds = Arrays.asList(
                R.id.textView_day_minus3_date,
                R.id.textView_day_minus2_date,
                R.id.textView_day_minus1_date,
                R.id.textView_today_date,
                R.id.textView_day_plus1_date,
                R.id.textView_day_plus2_date,
                R.id.textView_day_plus3_date
        );

        List<Integer> dayIds = Arrays.asList(
                R.id.textView_day_minus3,
                R.id.textView_day_minus2,
                R.id.textView_day_minus1,
                R.id.textView_today,
                R.id.textView_day_plus1,
                R.id.textView_day_plus2,
                R.id.textView_day_plus3
        );

        List<Integer> countIds = Arrays.asList(
                R.id.textView_day_minus3_count,
                R.id.textView_day_minus2_count,
                R.id.textView_day_minus1_count,
                R.id.textView_today_count,
                R.id.textView_day_plus1_count,
                R.id.textView_day_plus2_count,
                R.id.textView_day_plus3_count
        );

        for (int i = 0; i < days.size(); i++) {
            TextView dateTextView = view.findViewById(dateIds.get(i));
            TextView dayTextView = view.findViewById(dayIds.get(i));
            TextView countTextView = view.findViewById(countIds.get(i));
            TextView receiptsCountTextView = view.findViewById(receiptCountsIds.get(i));

            fetchDataForDay(days.get(i), dateTextView, countTextView, receiptsCountTextView);
            setupDayTextView(dayTextView, days.get(i));
            setDayTextViewClickListener(dayTextView, days.get(i));
        }
    }

    private void fetchDataForDay(LocalDate date, TextView dateTextView, TextView countTextView, TextView receiptsCountTextView) {
        CollectionReference collectionRef = firebaseHandlerObject.getStoreDocRefWithDate(date.getYear(), date.getMonthOfYear(), date.getDayOfMonth()).collection("DayQueues");

        collectionRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                Pair<Integer, Integer> documentCounts = calculateDocumentCounts(task.getResult());
                updateUIWithCounts(documentCounts, date, dateTextView, countTextView, receiptsCountTextView);
            } else {
                Utility.showToast(getContext(), "Counting failed: " + task.getException());
            }
        });
    }

    private Pair<Integer, Integer> calculateDocumentCounts(QuerySnapshot result) {
        int documentCount = result.size();
        int documentCountWithReceipt = 0;
        for (QueryDocumentSnapshot document : result) {
            if (document.contains("imageUrl") && !Objects.requireNonNull(document.getString("imageUrl")).isEmpty()) {
                documentCountWithReceipt++;
            }
        }
        return new Pair<>(documentCount, documentCountWithReceipt);
    }

    private void updateUIWithCounts(Pair<Integer, Integer> counts, LocalDate date, TextView dateTextView, TextView countTextView, TextView receiptsCountTextView) {
        setupDateTextView(dateTextView, date);
        receiptsCountTextView.setText(String.valueOf(counts.second));
        countTextView.setText(String.valueOf(counts.first));
    }

    private void setupDayTextView(TextView textView, LocalDate date) {
        String dayName = getHebrewDayName(date);
        textView.setText(dayName);
    }

    private void setupDateTextView(TextView textView, LocalDate date) {
        String dayOfMonth = String.valueOf(date.getDayOfMonth());
        String month = getHebrewMonthName(date);
        String text = month + "," +dayOfMonth ;
        textView.setText(text);
    }

    private void setDayTextViewClickListener(TextView textView, LocalDate date) {
        textView.setOnClickListener(v -> navigateToDayQ(date));
    }

    public String getHebrewDayName(LocalDate date) {
        DateTimeFormatter fmt = DateTimeFormat.forPattern("EEEE").withLocale(new Locale("iw"));
        return fmt.print(date);
    }

    public String getHebrewMonthName(LocalDate date) {
        DateTimeFormatter fmt = DateTimeFormat.forPattern("MMMM").withLocale(new Locale("iw"));
        return fmt.print(date);
    }

    private void navigateToDayQ(LocalDate date) {
        Fragment dayQFragment = new DailyFragment();
        Bundle bundle = new Bundle();
        bundle.putInt("keyYear", date.getYear());
        bundle.putInt("keyMonth", date.getMonthOfYear() - 1);
        bundle.putInt("keyDay", date.getDayOfMonth());
        dayQFragment.setArguments(bundle);

        FragmentTransaction transaction = getParentFragmentManager().beginTransaction();
        transaction.replace(R.id.fragmentContainerView, dayQFragment);
        transaction.addToBackStack(null);
        transaction.commit();
    }
}
