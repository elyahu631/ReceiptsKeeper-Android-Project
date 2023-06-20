package com.example.receiptskeeper.fragment;

import android.os.Bundle;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TimePicker;
import com.airbnb.lottie.LottieAnimationView;
import com.example.receiptskeeper.R;
import com.example.receiptskeeper.adapters.QueueAdapter;
import com.example.receiptskeeper.classes.Customer;
import com.example.receiptskeeper.utils.FirebaseHandler;
import com.example.receiptskeeper.utils.Utility;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class DailyFragment extends Fragment {
    private String currentUserId;
    private EditText dialog_name;
    private final List<Customer> CustomersList = new ArrayList<>();
    private final List<Boolean> isHavingReceiptsList = new ArrayList<>();
    private TextView title;
    private int year, month, day;
    private RecyclerView queueRecyclerView;
    private AlertDialog dialog;
    private DocumentReference docRef;
    private FirebaseFirestore dbStore;
    private com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton btnAdd;
    private LottieAnimationView lottieAnimationView;
    private ListenerRegistration registration;
    private FirebaseHandler firebaseHandlerObject;

    public DailyFragment() {}

    //LayoutInflater object is used to convert an XML layout file into its corresponding View object,
    // enabling you to work with the UI elements defined in that layout file within your code.
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_daily, container, false);


        initializeFirebase();
        processBundleArguments();
        initViews(view);
        assignButtonHandlers(view);
        fetchQueueData();

        return view;
    }

    // initialize Firebase related fields
    private void initializeFirebase() {
        firebaseHandlerObject= FirebaseHandler.getInstance();
        currentUserId = firebaseHandlerObject.getCurrentUserId();
        dbStore = FirebaseFirestore.getInstance();
    }

    // process Bundle Arguments
    private void processBundleArguments() {
        Bundle bundle = getArguments();
        assert bundle != null;
        year = bundle.getInt("keyYear", -1);
        month = bundle.getInt("keyMonth", -1);
        day = bundle.getInt("keyDay", -1);

        // Default to current date if no valid date provided
        if (year == -1 || month == -1 || day == -1) {
            Calendar c = Calendar.getInstance();
            day = c.get(Calendar.DAY_OF_MONTH);
            month = c.get(Calendar.MONTH);
            year = c.get(Calendar.YEAR);
        }
    }

    // assign view elements
    private void initViews(View view) {
        title = view.findViewById(R.id.title);
        String date = day + "/" + (month + 1) + "/" + year;
        title.setText("תאריך: " + date);

        queueRecyclerView = view.findViewById(R.id.queueRecyclerView);
        queueRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        lottieAnimationView = view.findViewById(R.id.lottieAnimationView);
    }

    // assign button handlers
    private void assignButtonHandlers(View view) {
        Button btnPreviousDay = view.findViewById(R.id.btnPreviousDay);
        Button btnNextDay = view.findViewById(R.id.btnNextDay);
        btnAdd = view.findViewById(R.id.btnAdd);

        btnAdd.setOnClickListener(v -> showDialogToAddQueue());
        btnPreviousDay.setOnClickListener(v -> updateDay(-1));
        btnNextDay.setOnClickListener(v -> updateDay(1));
    }

    private void updateDay(int offset) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(year, month, day);
        calendar.add(Calendar.DAY_OF_MONTH, offset);
        year = calendar.get(Calendar.YEAR);
        month = calendar.get(Calendar.MONTH);
        day = calendar.get(Calendar.DAY_OF_MONTH);
        title.setText("תאריך: " + day + "/" + (month + 1) + "/" + year);
        fetchQueueData();
    }

    private void fetchQueueData() {
        docRef = firebaseHandlerObject.getStoreDocRefWithDate(year,month+1,day);

        registration = docRef
                .collection("DayQueues")
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.w("dailyFragment", "Listen failed.", error);
                        return;
                    }
                    CustomersList.clear();
                    isHavingReceiptsList.clear();

                    if (value != null) {
                        for (QueryDocumentSnapshot document : value) {
                            String name = document.getString("name");
                            String hour = document.getString("hour");
                            String isHavingReceipt = document.getString("imageUrl");
                            CustomersList.add(new Customer(name, hour, day, month + 1, year));
                            isHavingReceiptsList.add(isHavingReceipt!=null&&!isHavingReceipt.isEmpty());
                        }
                        if (CustomersList.isEmpty()) {
                            // Data is empty, show animation
                            lottieAnimationView.setAnimation(R.raw.add_animation);
                            lottieAnimationView.playAnimation();
                            lottieAnimationView.setVisibility(View.VISIBLE);
                        } else {
                            // Data exists, hide animation
                            lottieAnimationView.setVisibility(View.GONE);
                        }

                        //checks if the fragment has a valid context and, if so, creates an
                        // instance of a custom adapter (OurAdapter) with the context, a layout resource,
                        // and a list of customers. Finally, it sets this adapter to the queueListView to display the
                        // data in the ListView.
                        if (getContext() != null) {

                            QueueAdapter ourAdapter = new QueueAdapter(getContext(), CustomersList,isHavingReceiptsList);
                            queueRecyclerView.setAdapter(ourAdapter);
                        }
                    }
                });
    }

    private void createDocumentIfNotExist(DocumentReference refToCheck) {
        refToCheck.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                DocumentSnapshot document = task.getResult();
                if (document.exists()) {
                    return;
                }
            }});
        refToCheck.set(new HashMap<>())
                .addOnSuccessListener(unused -> Log.e("dailyFragment", "created ref"))
                .addOnFailureListener(e -> Log.e("dailyFragment", "Error creating document", e));
    }

    public void showDialogToAddQueue() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("הכנס שם לקוח ושעה");
        View dialogView = createDialogView();
        builder.setView(dialogView);
        Button submit = dialogView.findViewById(R.id.btn_custom_dialog);
        submit.setOnClickListener(createSubmitButtonListener(dialogView));
        dialog = builder.create();
        dialog.show();
    }

    private View createDialogView() {
        View dialogView = getLayoutInflater()
                .inflate(R.layout.custom_dialog_add_queue, null, false);
        dialog_name = dialogView.findViewById(R.id.custom_dialog_name);
        TimePicker dialog_hour = dialogView.findViewById(R.id.custom_dialog_hour);
        //use the 24-hour format for selecting the hour.
        dialog_hour.setIs24HourView(true);
        return dialogView;
    }

    interface IsQueueExistsCallback {
        //existingQueue parameter of type DocumentSnapshot provides access to the data of the
        // existing queue document, allowing you to interact with and utilize the document's
        // information within the callback method.
        void isQueueExists(boolean exists, DocumentSnapshot existingQueue);
    }

    private View.OnClickListener createSubmitButtonListener(View dialogView) {
        return v -> {
            String name = dialog_name.getText().toString();
            if (name.equals("")) {
                Utility.showToast(getContext(), "בבקשה מלא את כל הפרטים");
                return;
            }
            TimePicker dialog_hour = dialogView.findViewById(R.id.custom_dialog_hour);
            String queueId = getQueueId(name, dialog_hour.getHour(), dialog_hour.getMinute());
            //implementation of the isQueueExists() method defined in the IsQueueExistsCallback interface.
            checkIfQueueExists(queueId, (exists, existingQueue) -> {
                if (exists) {
                    showDialogQueueExists(name, dialog_hour, queueId);
                } else {
                    addQueueToFirestore(queueId, createQueueMap(name, dialog_hour));
                }
            });
        };
    }

    private void checkIfQueueExists(String queueId, IsQueueExistsCallback callback) {
        docRef.collection("DayQueues").document(queueId).get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                DocumentSnapshot document = task.getResult();
                boolean exists = document.exists();
                if (exists) {
                    callback.isQueueExists(true, document);
                } else {
                    callback.isQueueExists(false, null);
                }
            } else {
                callback.isQueueExists(false, null);
            }
        });
    }

    private void showDialogQueueExists(String name, TimePicker dialog_hour, String queueId) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("תור קיים");
        builder.setMessage("תור עם השם והשעה שהוכנסו כבר קיים. האם ברצונך לדרוס אותו?");
        builder.setPositiveButton("כן", (dialogInterface, i) -> addQueueToFirestore(queueId,
                createQueueMap(name, dialog_hour)));
        builder.setNegativeButton("לא", (dialogInterface, i) -> dialogInterface.dismiss());
        builder.show();
    }

    private Map<String, Object> createQueueMap(String name, TimePicker dialog_hour) {
        Map<String, Object> queue = new HashMap<>();
        queue.put("name", name);
        String strHour = createFormat(dialog_hour.getHour(),dialog_hour.getMinute());
        queue.put("hour", strHour);
        return queue;
    }

    private String createFormat(int hour, int minute){
        String formattedHour = String.format("%02d", hour);
        String formattedMinute = String.format("%02d", minute);
        return formattedHour + ":" + formattedMinute;
    }

    private String   getQueueId(String name, int hour, int minute) {
        return createFormat(hour,minute).replace(":","") + name;
    }

    private void addQueueToFirestore(String queueId, Map<String, Object> queue) {
        DocumentReference refToCheck = dbStore.collection(currentUserId).document(year +"");
        createDocumentIfNotExist(refToCheck);
        createDocumentIfNotExist(docRef);

        docRef.collection("DayQueues").document(queueId)
                .set(queue)
                .addOnSuccessListener(unused -> {
                    Utility.showToast(getContext(), "נוסף תור");
                    dialog.dismiss();
                })
                .addOnFailureListener(e -> Utility.showToast(getContext(), "נכשל להוסיף תור"));
    }

    @Override
    public void onStart() {
        super.onStart();
        fetchQueueData();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (registration != null) {
            registration.remove();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (registration != null) {
            registration.remove();
        }
    }
}
