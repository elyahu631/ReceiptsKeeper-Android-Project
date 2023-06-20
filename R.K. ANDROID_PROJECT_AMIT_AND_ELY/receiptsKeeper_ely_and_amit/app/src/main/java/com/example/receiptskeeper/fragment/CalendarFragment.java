package com.example.receiptskeeper.fragment;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.example.receiptskeeper.R;
import com.example.receiptskeeper.classes.DecorateDaysWithQueue;
import com.example.receiptskeeper.utils.FirebaseHandler;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.prolificinteractive.materialcalendarview.CalendarDay;
import com.prolificinteractive.materialcalendarview.MaterialCalendarView;

import java.util.Calendar;
import java.util.Objects;

public class CalendarFragment extends Fragment {

    private MaterialCalendarView calendarView;
    private TextInputLayout monthInput;
    private TextInputLayout yearInput;
    int currentMonth;
    int currentYear;
    private FirebaseHandler firebaseHandlerObject;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_calendar, container, false);
        initializeViews(view);
        setInitialDateValues();
        setListeners();
        isThereQueuesInDay();
        return view;
    }

    private void initializeViews(View view) {
        calendarView = view.findViewById(R.id.calendarView);

        CalendarDay currentDay = CalendarDay.today();
        calendarView.setCurrentDate(currentDay);
        calendarView.setSelectedDate(currentDay);

        monthInput = view.findViewById(R.id.monthInput);
        yearInput = view.findViewById(R.id.yearInput);
        firebaseHandlerObject = FirebaseHandler.getInstance();

    }

    private void setInitialDateValues() {
        Calendar currentDate = Calendar.getInstance();
        currentMonth = currentDate.get(Calendar.MONTH) + 1;
        currentYear = currentDate.get(Calendar.YEAR);
        Objects.requireNonNull(monthInput.getEditText()).setText(String.valueOf(currentMonth));
        Objects.requireNonNull(yearInput.getEditText()).setText(String.valueOf(currentYear));
    }

    private void setListeners() {
        TextWatcher textWatcher = createTextWatcher();
        Objects.requireNonNull(monthInput.getEditText()).addTextChangedListener(textWatcher);
        Objects.requireNonNull(yearInput.getEditText()).addTextChangedListener(textWatcher);

        calendarView.setOnDateChangedListener((widget, date, selected) -> changeFragment(date));
        calendarView.setOnMonthChangedListener((widget, date) -> {

            int selectedYear = date.getYear();
            int selectedMonth = date.getMonth() + 1;


            currentYear = selectedYear;
            currentMonth = selectedMonth;
            monthInput.getEditText().setText(String.valueOf(selectedMonth));
            yearInput.getEditText().setText(String.valueOf(selectedYear));
            isThereQueuesInDay();

        });
    }

    @NonNull
    private TextWatcher createTextWatcher() {
        return new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // This space intentionally left blank
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                updateCalendar();
            }
        };
    }

    private void changeFragment(@NonNull CalendarDay date) {
        int year = date.getYear();
        int month = date.getMonth();
        int dayOfMonth = date.getDay();

        Bundle bundle = new Bundle();
        bundle.putInt("keyYear", year);
        bundle.putInt("keyMonth", month);
        bundle.putInt("keyDay", dayOfMonth);

        DailyFragment dailyFragment = new DailyFragment();
        dailyFragment.setArguments(bundle);

        FragmentManager fragmentManager = requireActivity().getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.fragmentContainerView, dailyFragment);
        fragmentTransaction.addToBackStack(null);
        fragmentTransaction.commit();
    }

    private void updateCalendar() {
        try {
            currentMonth = Integer.parseInt(Objects.requireNonNull(monthInput.getEditText()).getText().toString());
            currentYear = Integer.parseInt(Objects.requireNonNull(yearInput.getEditText()).getText().toString());

            validateMonth(currentMonth);
            validateYear(currentYear);

            CalendarDay calendarDay = CalendarDay.from(currentYear, currentMonth - 1, 1);
            calendarView.setCurrentDate(calendarDay, true);
            isThereQueuesInDay();
        } catch (NumberFormatException e) {
            Log.e("CalendarFragment", "Error : " + e);
        }
    }

    private void validateMonth(int month) {
        if (month < 1 || month > 12) {
            monthInput.setError("חודש לא תקין");
        } else {
            monthInput.setError(null);
        }
    }

    private void validateYear(int year) {
        Calendar currentCalendar = Calendar.getInstance();
        int currentYear = currentCalendar.get(Calendar.YEAR);

        if (year < currentYear - 1 || year > currentYear + 1) {
            yearInput.setError("שנה לא תקינה");
        } else {
            yearInput.setError(null);
        }
    }

    public void isThereQueuesInDay() {
        //delete the decorators , if was in the month
        calendarView.removeDecorators();

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference yearRef = db.collection(firebaseHandlerObject.getCurrentUserId()).document(String.valueOf(currentYear));
        yearRef.collection(String.valueOf(currentMonth)).get().addOnCompleteListener(task -> handleMonthDataFetch(task, yearRef));
    }

    private void handleMonthDataFetch(Task<QuerySnapshot> task, DocumentReference yearRef) {
        if (task.isSuccessful()) {
            for (QueryDocumentSnapshot document : task.getResult()) {
                processEachDay(yearRef, document);
            }
        } else {
            Log.d("CalendarFragment", "Error getting documents: ", task.getException());
        }
    }

    private void processEachDay(DocumentReference yearRef, QueryDocumentSnapshot document) {
        int day = Integer.parseInt(document.getId());

        yearRef.collection(String.valueOf(currentMonth))
                .document(document.getId())
                .collection("DayQueues")
                .get()
                .addOnCompleteListener(task2 -> handleDayQueueDataFetch(task2, day));
    }

    private void handleDayQueueDataFetch(Task<QuerySnapshot> task2, int day) {
        if (task2.isSuccessful()) {
            QuerySnapshot result = task2.getResult();
            if (result != null && !result.isEmpty()) {
                decorateCalendarDay(day);
            }
        }
    }

    private void decorateCalendarDay(int day) {
        CalendarDay dateToDecorate = CalendarDay.from(currentYear, currentMonth - 1, day);
        DecorateDaysWithQueue newDayDecorator = new DecorateDaysWithQueue(dateToDecorate);
        calendarView.addDecorator(newDayDecorator);
    }
}
