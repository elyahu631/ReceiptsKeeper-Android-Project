package com.example.receiptskeeper.fragment;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;

import com.example.receiptskeeper.R;
import com.example.receiptskeeper.classes.DebouncedOnClickListener;
import com.example.receiptskeeper.utils.FirebaseHandler;
import com.example.receiptskeeper.utils.Utility;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class DetailsFragment extends Fragment {
    private static final int GALLERY_REQ_CODE = 50;
    private static final int GALLERY_PERMISSION_REQUEST_CODE = 100;

    private static final int CAMERA_REQ_CODE = 150;
    private static final int CAMERA_PERMISSION_REQUEST_CODE = 200;

    private boolean isImageChanged = false;

    private int year, month, day;
    private String customerKey;
    private Button openCamera, uploadImage;
    private ImageView image;
    private DocumentReference dbStoreRefWithDate;
    private StorageReference storageRef;
    private TextView timeTextView, clientNameTextView, dateTextView;
    private Button btnDeleteImg ,btnShareImage;
    private String name;
    private AlertDialog fullScreenImageDialog;
    private String existingHour;
    private FirebaseHandler firebaseHandlerObject;
    private DocumentReference storeDocRefWithDateAndCustomerKey;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_details, container, false);

        Bundle bundle = getArguments();
        initViews(view);
        extractCustomerDetails(bundle);
        initFirebase();
        loadExistingImage();
        setCameraClickListener();
        btnSaveImageClickListener();
        deleteBtn();
        fullImage();

        shareImage();

        return view;
    }

    private void initViews(View view) {
        image = view.findViewById(R.id.imageDisplay);
        clientNameTextView = view.findViewById(R.id.nameTextView);
        timeTextView = view.findViewById(R.id.timeTextView);
        dateTextView = view.findViewById(R.id.dateTextView);
        openCamera = view.findViewById(R.id.btnOpenCamera);
        uploadImage = view.findViewById(R.id.btnUploadImage);
        btnDeleteImg = view.findViewById(R.id.btnDeleteImg);
        btnShareImage = view.findViewById(R.id.btnShareImage);
        firebaseHandlerObject=FirebaseHandler.getInstance();
    }

    private void initFirebase() {
        dbStoreRefWithDate = firebaseHandlerObject.getStoreDocRefWithDate(year,month,day);
        storageRef = firebaseHandlerObject.getStorageRefWithDateAndCustomerKey(year,month,day,customerKey);
        storeDocRefWithDateAndCustomerKey= firebaseHandlerObject.getStoreDocRefWithDateAndCustomerKey(year,month,day,customerKey);
    }

    private void extractCustomerDetails(Bundle bundle) {
        customerKey = bundle.getString("customerKey");
        name = customerKey.substring(4);
        existingHour = customerKey.substring(0,5);
        existingHour = existingHour.substring(0, 2) + ":" + existingHour.substring(2, 4);
        clientNameTextView.setText("שם: "+ name);
        timeTextView.setText("שעה: "+ existingHour);
        year = bundle.getInt("keyYear", -1);
        month = bundle.getInt("keyMonth", -1);
        day = bundle.getInt("keyDay", -1);
        if (customerKey == null || customerKey.isEmpty()) {
            Toast.makeText(getContext(), "לקוח לא נמצא.", Toast.LENGTH_LONG).show();
        }

        if (year == -1 || month == -1 || day == -1) {
            Calendar c = Calendar.getInstance();
            day = c.get(Calendar.DAY_OF_MONTH);
            month = c.get(Calendar.MONTH);
            year = c.get(Calendar.YEAR);
        }

        dateTextView.setText(day+"/"+month+"/"+year);
    }

    // טעינת התמונה הקיימת מה firebase
    private void loadExistingImage() {
        dbStoreRefWithDate
                .collection("DayQueues")
                .document(customerKey)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String imageUrl = getImageUrlFromSnapshot(documentSnapshot);
                        if (imageUrl != null && !imageUrl.isEmpty()) {
                            loadAndDisplayImage(imageUrl);
                        }
                    }
                })
                .addOnFailureListener(e -> handleImageLoadingFailure());
    }

    private String getImageUrlFromSnapshot(DocumentSnapshot documentSnapshot) {
        if (documentSnapshot.contains("imageUrl")) {
            return documentSnapshot.getString("imageUrl");
        }
        return null;
    }

    private void loadAndDisplayImage(String imageUrl) {
        AlertDialog progressDialog = showProgressDialog();
        Picasso.get()
                .load(imageUrl)
                .into(image, new com.squareup.picasso.Callback() {
                    @Override
                    public void onSuccess() {
                        dismissProgressDialog(progressDialog);
                    }

                    @Override
                    public void onError(Exception e) {
                        handleImageLoadingFailure();
                        dismissProgressDialog(progressDialog);
                    }
                });
    }

    private AlertDialog showProgressDialog() {
        ProgressBar progressBar = new ProgressBar(getContext());
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setView(progressBar);
        builder.setMessage("טוען קבלה...");
        builder.setCancelable(false);
        AlertDialog progressDialog = builder.create();
        progressDialog.show();
        return progressDialog;
    }

    private void dismissProgressDialog(AlertDialog progressDialog) {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }

    private void handleImageLoadingFailure() {
        Utility.showToast(getContext(),"נכשל לעלות קבלה.");
    }

    private void setCameraClickListener() {
        openCamera.setOnClickListener(v -> showProgressDialogWithCameraOptions());
    }

    private void showProgressDialogWithCameraOptions() {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(getContext());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.progress_dialog, null);

        Button buttonCamera = dialogView.findViewById(R.id.buttonCamera);
        Button buttonGallery = dialogView.findViewById(R.id.buttonGallery);

        dialogBuilder.setView(dialogView);
        AlertDialog dialog = dialogBuilder.create();
        dialog.show();

        buttonCamera.setOnClickListener(v -> {
            dialog.dismiss();
            openCameraIntent();
        });

        buttonGallery.setOnClickListener(v -> {
            dialog.dismiss();
            openGalleryIntent();
        });
    }

    private void openCameraIntent() {
        if (checkCameraPermission()) {
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            if (intent.resolveActivity(getActivity().getPackageManager()) != null) {
                // Set image capture quality and resolution options
                intent.putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 1); // Set the video quality to high
                intent.putExtra(MediaStore.EXTRA_SIZE_LIMIT, Long.MAX_VALUE); // Set the maximum file size (10MB)
                // Start the camera intent
                startActivityForResult(intent, CAMERA_REQ_CODE);
            } else {
                Utility.showToast(getContext(),"מצלמה לא זמינה.");
            }
        } else {
            requestCameraPermission();
        }
    }

    private void openGalleryIntent() {
        if (checkGalleryPermission()) {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(intent, GALLERY_REQ_CODE);
        } else {
            requestGalleryPermission();
        }
    }

    private boolean checkCameraPermission() {
        return ContextCompat.checkSelfPermission(getContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestCameraPermission() {
        requestPermissions(new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_REQUEST_CODE);
    }

    private boolean checkGalleryPermission() {
        return ContextCompat.checkSelfPermission(getContext(), Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestGalleryPermission() {
        requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, GALLERY_PERMISSION_REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openCameraIntent();
            } else {
                Utility.showToast(getContext(),"Permission denied to use camera");
            }
        } else if (requestCode == GALLERY_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openGalleryIntent();
            } else {
                Utility.showToast(getContext(),"Permission denied to read external storage");
            }
        }
    }

    private void btnSaveImageClickListener() {
        uploadImage.setOnClickListener(v -> uploadImageToStorage());
    }

    private float calculateTextSize(Bitmap bitmap) {
        int height = bitmap.getHeight();
        int width = bitmap.getWidth();
        // Use the longer side of the image to calculate the text size
        int longerSide = Math.max(height, width);
        // Base the text size on a certain fraction of the longer side of the image
        // Here we're using 1/24th of the length.
        float textSize = longerSide / 24f;
        // Ensure a minimum text size
        if (textSize < 10f) textSize = 10f;
        return textSize;
    }

    private Bitmap drawTextOnPhoto(Bitmap bitmap, String text) {
        Bitmap mutableBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);
        Canvas canvas = new Canvas(mutableBitmap);

        Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(calculateTextSize(bitmap));
        textPaint.setStyle(Paint.Style.FILL);
        textPaint.setShadowLayer(2f, 2f, 2f, Color.BLACK);

        float x = (float) (mutableBitmap.getWidth() / 4); // Change X-coordinate
        float y = mutableBitmap.getHeight() / 6; // Change Y-coordinate

        canvas.drawText(text, x, y, textPaint);

        return mutableBitmap;
    }

    // it is overridden to handle the result of different types of requests.
    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        String infoOnImg = name + "   " + day + "/" + month + "/" + year + "   " +  existingHour;
        if (resultCode != Activity.RESULT_OK){
            return;
        }
        Bitmap img = null;
        if (requestCode == CAMERA_REQ_CODE) {
            img = (Bitmap) (data.getExtras().get("data"));
        }
        else if(requestCode == GALLERY_REQ_CODE) {
            Uri selectedImageUri = data.getData();
            try {
                InputStream imageStream = getActivity().getContentResolver().openInputStream(selectedImageUri);
                img = BitmapFactory.decodeStream(imageStream);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        img = drawTextOnPhoto(img, infoOnImg);
        image.setImageBitmap(img);
        isImageChanged = true;
    }

    // the first function to start the uplode
    private void uploadImageToStorage() {
        if (isImageChanged) {
            Bitmap bitmap = ((BitmapDrawable) image.getDrawable()).getBitmap();
            saveImageToStorage(bitmap);
            isImageChanged = false;
        }
        else {
            Toast.makeText(getContext(), "בבקשה בחר קבלה חדשה.", Toast.LENGTH_SHORT).show();
        }
    }

    private void saveImageToStorage(Bitmap bitmap) {
        if (bitmap != null) {
            byte[] imageData = convertBitmapToByteArray(bitmap);
            String fileName = "image_" + customerKey + ".jpg";
            StorageReference storageReference = storageRef.child(fileName);
            uploadImageToStorage(imageData, storageReference);
        } else {
            Toast.makeText(getContext(), "לא נבחרה קבלה.", Toast.LENGTH_SHORT).show();
        }
    }

    private byte[] convertBitmapToByteArray(Bitmap bitmap) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        return baos.toByteArray();
    }

    private void uploadImageToStorage(byte[] imageData, StorageReference storageReference) {
        UploadTask uploadTask = storageReference.putBytes(imageData);
        uploadTask.addOnSuccessListener(taskSnapshot -> getImageUrlFromStorage(storageReference)).addOnFailureListener(e -> {
            Log.e("TAG", "Upload failed", e);
            Toast.makeText(getContext(), "נכשל לטעון קבלה.", Toast.LENGTH_SHORT).show();
        });
    }

    private void getImageUrlFromStorage(StorageReference storageReference) {
        storageReference.getDownloadUrl().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Uri downloadUri = task.getResult();
                if (downloadUri != null) {
                    String imageUrl = downloadUri.toString();
                    saveImageFirestore(imageUrl);
                } else {
                    Toast.makeText(getContext(), "נכשל בקבלת כתובת התמונה.", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(getContext(), "נכשל בלטעון הקבלה.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void saveImageFirestore(String imageUrl) {
        if (customerKey != null && !customerKey.isEmpty())//is Customer Available
        {
            Map<String, Object> imageMap = createImageMap(imageUrl);
            updateImageFirestore(imageMap);
        } else {
            Toast.makeText(getContext(), "לקוח לא נמצא.", Toast.LENGTH_LONG).show();
        }
    }

    private Map<String, Object> createImageMap(String imageUrl) {
        Map<String, Object> imageMap = new HashMap<>();
        imageMap.put("imageUrl", imageUrl);
        return imageMap;
    }

    private void updateImageFirestore(Map<String, Object> imageMap) {
        if (getContext() != null) {
            storeDocRefWithDateAndCustomerKey
                    .update(imageMap)
                    .addOnSuccessListener(aVoid -> {
                        if (getContext() != null) {
                            Toast.makeText(getContext(), "קבלה נשמרה בהצלחה.", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(e -> {
                        if (getContext() != null) {
                            Toast.makeText(getContext(), "אירעה שגיאה בשמירת הקבלה.", Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }

    private void deleteBtn() {
        btnDeleteImg.setOnClickListener(v -> {
            if (image.getDrawable() != null) {
                showDeleteConfirmationDialog();
            } else {
                Toast.makeText(getContext(), "אין תמונה למחיקה.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showDeleteConfirmationDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("מחיקת תמונה");
        builder.setMessage("האם אתה בטוח שברצונך למחוק את התמונה?");
        builder.setPositiveButton("כן", (dialog, which) -> deleteImageFromFirestoreAndStorage());
        builder.setNegativeButton("לא", (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    private void deleteImageFromFirestoreAndStorage() {
        StorageReference imageRef = storageRef.child("image_" + customerKey + ".jpg");

        // Check if image exists in Firebase storage
        imageRef.getDownloadUrl().addOnSuccessListener(uri -> {
            // If image exists in Firebase storage, delete it from there and Firestore
            deleteImageFromStorage(imageRef, aVoid -> deleteImageFromFirestore());
        }).addOnFailureListener(e -> {
            // If image doesn't exist in Firebase storage, just remove it from the ImageView
            image.setImageDrawable(null);
            isImageChanged = false;
        });
    }

    private void deleteImageFromStorage(StorageReference imageRef, OnSuccessListener<Void> onSuccessListener) {
        imageRef.delete().addOnSuccessListener(onSuccessListener)
                .addOnFailureListener(exception -> Log.d("TAG", "onFailure: did not delete file"));
    }

    private void deleteImageFromFirestore() {
        storeDocRefWithDateAndCustomerKey
                .update("imageUrl", "")
                .addOnSuccessListener(aVoid -> onFirestoreDocumentUpdated())
                .addOnFailureListener(e -> Log.w("TAG", "Error deleting document", e));
    }

    private void onFirestoreDocumentUpdated() {
        Toast.makeText(getContext(), "תמונה נמחקה בהצלחה", Toast.LENGTH_SHORT).show();
        image.setImageDrawable(null);
        isImageChanged = false;
    }

    private void fullImage(){
        image.setOnClickListener(v -> openFullScreenImage());
    }

    private void openFullScreenImage() {
        if (image.getDrawable() != null) {
            Drawable drawable = image.getDrawable();
            Bitmap bitmap = ((BitmapDrawable) drawable).getBitmap();

            // Create a new dialog to display the full-screen image
            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(getContext());
            LayoutInflater inflater = LayoutInflater.from(getContext());
            View dialogView = inflater.inflate(R.layout.dialog_fullscreen_image, null);

            ImageView fullScreenImageView = dialogView.findViewById(R.id.fullScreenImageView);
            fullScreenImageView.setImageBitmap(bitmap);

            dialogBuilder.setView(dialogView);
            fullScreenImageDialog = dialogBuilder.create();

            // Set the dialog to use full screen
            if (fullScreenImageDialog.getWindow() != null) {
                fullScreenImageDialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            }

            fullScreenImageDialog.show();

            // Set a click listener to close the full-screen image when clicked
            fullScreenImageView.setOnClickListener(v -> closeFullScreenImage());
        }
    }

    private void closeFullScreenImage() {
        if (fullScreenImageDialog != null && fullScreenImageDialog.isShowing()) {
            fullScreenImageDialog.dismiss();
        }
    }

    private void shareImage() {
        btnShareImage.setOnClickListener(new DebouncedOnClickListener(1000) {
            public void onDebouncedClick(View v) {
                shareImageViaEmail();
            }
        });
    }

    private void shareImageViaEmail() {
        Drawable drawable = image.getDrawable();
        if (drawable instanceof BitmapDrawable) {
            BitmapDrawable bitmapDrawable = (BitmapDrawable) drawable;
            Bitmap bitmap = bitmapDrawable.getBitmap();
            try {
                Uri imageUri = getImageUri(bitmap);
                if (imageUri != null) {
                    Intent intent = new Intent(Intent.ACTION_SEND);
                    intent.setType("image/jpeg");
                    intent.putExtra(Intent.EXTRA_STREAM, imageUri);
                    startActivity(Intent.createChooser(intent, "שתף תמונה באמצעות"));
                } else {
                    Toast.makeText(getContext(), "אירעה שגיאה בשיתוף התמונה.", Toast.LENGTH_SHORT).show();
                }
            } catch (IOException e) {
                Toast.makeText(getContext(), "אירעה שגיאה בשיתוף התמונה.", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(getContext(), "לא נבחרה תמונה.", Toast.LENGTH_SHORT).show();
        }
    }

    private Uri getImageUri(Bitmap bitmap) throws IOException {
        File cachePath = new File(getContext().getCacheDir(), "images");
        cachePath.mkdirs();
        FileOutputStream stream = new FileOutputStream(cachePath + "/image.png"); // create a unique name for the file
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
        stream.close();
        return FileProvider.getUriForFile(getContext(), getContext().getPackageName() + ".fileprovider", new File(cachePath + "/image.png"));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        closeFullScreenImage();
    }

}