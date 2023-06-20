package com.example.receiptskeeper.fragment;


import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.graphics.pdf.PdfDocument.Page;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;


import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.airbnb.lottie.LottieAnimationView;
import com.example.receiptskeeper.R;

import com.example.receiptskeeper.adapters.ImageAdapter;
import com.example.receiptskeeper.classes.DebouncedOnClickListener;
import com.example.receiptskeeper.utils.Utility;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldPath;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import com.squareup.picasso.Picasso;


import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class ReportsFragment extends Fragment {

    private Button shareImages;
    private List<String> imageUrls ;
    private RecyclerView recyclerView;
    private ImageAdapter imageAdapter;
    private TextInputLayout monthInput ,yearInput;
    private int year,month;
    private final FirebaseAuth mAuth= FirebaseAuth.getInstance();
    private String currentUserId ;
    LottieAnimationView animationView;

    private static final String DIRECTORY_NAME = "pdfDirectory";
    private static final String FILE_PROVIDER = "com.example.receiptskeeper.fileprovider";
    private static final String TAG = "ReportsFragment";
    private boolean isSharingInProgress = false;


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        currentUserId =  mAuth.getUid();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_reports, container, false);
        animationView = view.findViewById(R.id.loadingAnimation);
        // Extract year and month details
        extractDetails(getArguments());
        initializeVariables(view);
        setupShareListenerBtn();
        setupRecyclerView();
        fetchImageUrls();
        setListeners();
        return view;
    }

    private void initializeVariables(View view) {
        imageUrls = new ArrayList<>();
        recyclerView = view.findViewById(R.id.recyclerView);
        monthInput = view.findViewById(R.id.monthInput);
        yearInput = view.findViewById(R.id.yearInput);
        shareImages = view.findViewById(R.id.shareImages);

        setInitialDateValues();
    }

    private void setupRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        imageAdapter = new ImageAdapter();
        recyclerView.setAdapter(imageAdapter);
    }

    private void setInitialDateValues() {
        Objects.requireNonNull(monthInput.getEditText()).setText(String.valueOf(month));
        Objects.requireNonNull(yearInput.getEditText()).setText(String.valueOf(year));
    }

    private void extractDetails(Bundle bundle) {
        if(bundle != null){
            year = bundle.getInt("year");
            month = bundle.getInt("month");
        }
    }

    private void fetchImageUrls() {

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference yearRef = db.collection(currentUserId).document(String.valueOf(year));
        imageUrls.clear();
        imageAdapter.setImageUrls(imageUrls);
        // Start the animation
        animationView.setVisibility(View.VISIBLE);
        animationView.playAnimation();
        yearRef.collection(String.valueOf(month)).get()
                .addOnSuccessListener(querySnapshot -> {
                    for (QueryDocumentSnapshot document : querySnapshot) {
                        fetchImageUrlsForDay(yearRef, document.getId());
                    }

                })
                .addOnFailureListener(e -> Log.d(TAG, "Error getting documents: ", e));

    }

    private void fetchImageUrlsForDay(DocumentReference yearRef, String dayId) {
        yearRef.collection(String.valueOf(month))
                .document(dayId)
                .collection("DayQueues")
                .orderBy(FieldPath.documentId())
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    for (QueryDocumentSnapshot document : querySnapshot) {
                        String imageUrl = document.getString("imageUrl");
                        // Check if the imageUrl is valid before adding it to the list
                        if (imageUrl != null && !imageUrl.trim().isEmpty()) {
                            Log.d(TAG, imageUrl + " added");
                            imageUrls.add(imageUrl);
                            animationView.cancelAnimation();
                            animationView.setVisibility(View.GONE);
                        } else {
                            Log.d(TAG, "Invalid image URL skipped");
                        }
                    }
                    imageAdapter.setImageUrls(imageUrls);
                })
                .addOnFailureListener(e -> Log.d(TAG, "Error getting documents: ", e));
    }

    private void validateInputs() {
        try {
            int month = Integer.parseInt(Objects.requireNonNull(monthInput.getEditText()).getText().toString());
            int year = Integer.parseInt(Objects.requireNonNull(yearInput.getEditText()).getText().toString());

            validateMonth(month);
            validateYear(year);
        } catch (NumberFormatException e) {
            Log.d(TAG, "Invalid input");
        }
    }

    private void validateMonth(int month) {
        if (month < 1 || month > 12) {
            monthInput.setError("Invalid month");
            imageAdapter.setImageUrls(imageUrls);
        } else {
            monthInput.setError(null);
            this.month = month;
        }
    }

    private void validateYear(int year) {
        Calendar currentCalendar = Calendar.getInstance();
        int currentYear = currentCalendar.get(Calendar.YEAR);

        if (year < currentYear - 1 || year > currentYear + 1) {
            yearInput.setError("Invalid year");
            imageAdapter.setImageUrls(imageUrls);
        } else {
            yearInput.setError(null);
            this.year = year;
            fetchImageUrls();
        }
    }

    private void setListeners() {
        TextWatcher textWatcher = createTextWatcher();
        Objects.requireNonNull(monthInput.getEditText()).addTextChangedListener(textWatcher);
        Objects.requireNonNull(yearInput.getEditText()).addTextChangedListener(textWatcher);
    }

    @NonNull
    private TextWatcher createTextWatcher() {
        return new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) { }

            @Override
            public void afterTextChanged(Editable s) {
                validateInputs();
            }
        };
    }

    private void setupShareListenerBtn() {
        shareImages.setOnClickListener(new DebouncedOnClickListener(1000) { // 1000 is the debounce time in millis
            public void onDebouncedClick(View v) {
                Log.d(TAG, "Share Images button clicked");
                if (imageUrls.isEmpty()) {
                    Utility.showToast(requireContext(),"אין קבלות לשתף!");
                    return;
                }

                if (isSharingInProgress) {
                    return;
                }

                try {
                    isSharingInProgress = true;
                    createAndSharePdf();
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.e(TAG, "Error while creating or sharing PDF", e);
                    isSharingInProgress = false;
                }
            }
        });
    }

    private void createAndSharePdf() {
        // new thread to handle the potentially time consuming operations
        new Thread(() -> {
            // Creating a new PdfDocument object which will eventually contain the pages for our PDF
            PdfDocument document = new PdfDocument();
            FileOutputStream fileOutputStream = null;
            try {
                // Downloading images and convert them into a PDF document
                downloadImagesAndCreatePdf(document);
                // Making sure the necessary directory exists for storing the PDF
                File directory = createDirectoryIfNotExists();
                //Creating the actual PDF file in the directory
                File pdfFile = createPdfFile(document, directory);
                // Attempting to share the created PDF file
                sharePdfFile(pdfFile);
            } catch (Exception e) {
                e.printStackTrace();
                Log.e(TAG, "Error while creating or sharing PDF", e);
                requireActivity().runOnUiThread(() ->  Utility.showToast(requireContext(),"אירעה שגיאה בעת יצירת ה-PDF"));
                isSharingInProgress = false;
            } finally {
                //Making sure to close the PdfDocument and the FileOutputStream if they're open
                if (document != null) {
                    document.close();
                }

                if (fileOutputStream != null) {
                    try {
                        fileOutputStream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                requireActivity().runOnUiThread(() -> isSharingInProgress = false);
            }
        }).start();
    }

    private void downloadImagesAndCreatePdf(PdfDocument document) throws IOException {
        for (int i = 0; i < imageUrls.size(); i++) {
            String imageUrl = imageUrls.get(i);

            // Skip this iteration if imageUrl is null or empty
            if (imageUrl == null || imageUrl.isEmpty()) {
                Log.d(TAG, "Skipping an empty or null image URL");
                continue;
            }

            Log.d(TAG, "Loading image: " + imageUrl);
            Bitmap bitmap = Picasso.get().load(imageUrl).get();
            Log.d(TAG, "Image loaded");
            PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(bitmap.getWidth(), bitmap.getHeight(), i + 1).create();
            Page page = document.startPage(pageInfo);
            Canvas canvas = page.getCanvas();
            Paint paint = new Paint();
            canvas.drawPaint(paint);
            paint.setColor(Color.WHITE);
            canvas.drawBitmap(bitmap, 0, 0, null);
            document.finishPage(page);
        }
    }

    private File createDirectoryIfNotExists() {
        File directory = new File(requireContext().getExternalFilesDir(null), DIRECTORY_NAME);
        if (!directory.exists()) {
            directory.mkdir();
        }
        return directory;
    }

    private File createPdfFile(PdfDocument document, File directory) throws IOException {
        String fileName = String.format(Locale.US, "report_%d_%d.pdf", year, month);
        File pdfFile = new File(directory, fileName);
        Log.d(TAG, "File path: " + pdfFile.getAbsolutePath());

        try (FileOutputStream fileOutputStream = new FileOutputStream(pdfFile)) {
            document.writeTo(fileOutputStream);
        }
        return pdfFile;
    }

    private void sharePdfFile(File pdfFile) {
        Uri fileUri = FileProvider.getUriForFile(requireContext(), FILE_PROVIDER, pdfFile);
        Log.d(TAG, "File URI: " + fileUri.toString());
        Intent shareIntent = new Intent();
        shareIntent.setAction(Intent.ACTION_SEND);
        shareIntent.setType("application/pdf");
        shareIntent.putExtra(Intent.EXTRA_STREAM, fileUri);
        startActivity(Intent.createChooser(shareIntent, "שתף קבלות"));
    }

}
