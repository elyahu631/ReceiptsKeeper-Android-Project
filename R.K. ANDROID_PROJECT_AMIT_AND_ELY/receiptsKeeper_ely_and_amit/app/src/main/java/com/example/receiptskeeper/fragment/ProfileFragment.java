package com.example.receiptskeeper.fragment;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.receiptskeeper.R;
import com.example.receiptskeeper.classes.Business;
import com.example.receiptskeeper.classes.SharedViewModel;
import com.example.receiptskeeper.utils.FirebaseHandler;
import com.example.receiptskeeper.utils.Utility;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.squareup.picasso.Picasso;

public class ProfileFragment extends Fragment {

    private static final int PICK_IMAGE_REQUEST = 100;

    private DatabaseReference Database;
    private SharedViewModel sharedViewModel;
    private EditText ownerNameEditText, businessNameEditText;
    private Button submitButton, imageButton;
    private Uri imageUri;
    private ImageView businessImageView;
    private EditText emailEditText;
    private FirebaseUser currentUser;
    private FirebaseHandler firebaseHandlerObject;

    private final String TAG = "ProfileFragment";
    private String imageUrl = "";
    private boolean isImageChanged = false;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        sharedViewModel = new ViewModelProvider(requireActivity()).get(SharedViewModel.class);

        initializeFireBase();
        initializeViews(view);
        fetchEmailFromFirebaseAuth();
        setButtonListeners();
        populateFields(); // Load fields and image

        return view;
    }

    private void initializeFireBase() {
        firebaseHandlerObject=FirebaseHandler.getInstance();
        currentUser = firebaseHandlerObject.getCurrentUser();
        Database = firebaseHandlerObject.getDatabaseReference();
    }

    private void initializeViews(View view) {
        emailEditText = view.findViewById(R.id.email_text);
        ownerNameEditText = view.findViewById(R.id.username);
        businessNameEditText = view.findViewById(R.id.business_name);
        submitButton = view.findViewById(R.id.submit_button);
        imageButton = view.findViewById(R.id.select_image_button);
        businessImageView = view.findViewById(R.id.business_image);
    }

    private void setButtonListeners() {
        imageButton.setOnClickListener(this::handleImageButtonClick);
        submitButton.setOnClickListener(this::handleSubmitButtonClick);
    }

    // Get the user email for firebase
    private void fetchEmailFromFirebaseAuth() {
        FirebaseUser user = firebaseHandlerObject.getCurrentUser();
        if (user != null) {
            String email = user.getEmail();
            emailEditText.setText(email);
        }
    }

    private void populateFields() {
        Business business = sharedViewModel.getBusiness().getValue();
        if (business != null) {
            setBusinessFields(business);
            handleImageLoading(business);
        }
        //loading the img with picasso to businessImageView
        loadImageWithPicasso();
    }

    private void setBusinessFields(Business business) {
        ownerNameEditText.setText(business.getOwnerName());
        businessNameEditText.setText(business.getBusinessName());
    }

    private void handleImageLoading(Business business) {
        // Check if the imageUrl has changed
        if (!business.getImageUrl().equals(imageUrl)) {
            imageUrl = business.getImageUrl();
            loadImageWithPicasso();
        } else {
            isImageChanged = false;
        }
    }

    private void loadImageWithPicasso() {
        if (imageUrl != null && !imageUrl.isEmpty()) {
            Picasso.get().load(imageUrl).into(businessImageView, new com.squareup.picasso.Callback() {
                @Override
                public void onSuccess() {
                    isImageChanged = false;
                }
                @Override
                public void onError(Exception e) {
                    Log.e(TAG, "Failed to load img:" + e);
                }
            });
        } else {
            Log.d(TAG, "no img to load");
        }
    }

    private void handleImageButtonClick(View view) {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    private void handleSubmitButtonClick(View view) {
        submitData();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d(TAG, "onActivityResult active");

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null && data.getData() != null) {
            imageUri = data.getData();
            imageUrl = imageUri.toString();
            businessImageView.setImageURI(imageUri);
            isImageChanged = true;
        } else {
            isImageChanged = false;
        }
    }

    private void submitData() {
        if (currentUser != null) {
            final String ownerName = ownerNameEditText.getText().toString().trim();
            final String businessName = businessNameEditText.getText().toString().trim();
            final String ownerEmail = emailEditText.getText().toString().trim();

            Business previousBusiness = sharedViewModel.getBusiness().getValue();

            boolean isInfoChanged = (previousBusiness == null || !ownerName.equals(previousBusiness.getOwnerName()) ||
                    !businessName.equals(previousBusiness.getBusinessName()));

            isImageChanged = (imageUri != null); // Set it to true if image is changed

            boolean isEmailChanged = !ownerEmail.equals(currentUser.getEmail());

            if (!isInfoChanged && !isImageChanged && !isEmailChanged) {
                Utility.showToast(getContext(), "לא עדכנת שום דבר");
                return;
            }

            if (isImageChanged) {
                uploadFileToStorage(ownerName, businessName);
                Log.d(TAG, "Image updated");
            }
            if (isInfoChanged) {
                Business updatedBusiness = new Business(ownerName, businessName, imageUrl);
                sharedViewModel.setBusiness(updatedBusiness);
                uploadDataToDatabase(ownerName, businessName, imageUrl);
                Log.d(TAG, "Info updated");
            }
            if (isEmailChanged) {
                updateEmail(ownerEmail);
            }
        }
    }

    private void uploadFileToStorage(final String ownerName, final String businessName) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            String userId = user.getUid();
            final StorageReference fileReference = FirebaseStorage.getInstance().getReference("uploads")
                    .child(userId + "." + getFileExtension(imageUri));
            uploadFile(fileReference, ownerName, businessName);
        }
    }

    private void uploadFile(StorageReference fileReference, final String ownerName, final String businessName) {
        fileReference.putFile(imageUri)
                .addOnSuccessListener(taskSnapshot -> {
                    Log.d(TAG, "Image Upload: successful");
                    handleSuccessfulUpload(fileReference, ownerName, businessName);
                })
                .addOnFailureListener(e -> {
                    Log.d(TAG, "Image Upload: failed", e);
                    Utility.showToast(getContext(), "נכשל בעדכון התמונה");
                });
    }

    private void updateEmail(String email) {
        currentUser.updateEmail(email)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "Email updated");
                    } else {
                        Log.d(TAG, "Email  Fail updated");
                    }
                });
    }

    private void handleSuccessfulUpload(StorageReference fileReference, String ownerName, String businessName) {
        fileReference.getDownloadUrl().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Log.d(TAG, "Download Url: " + task.getResult().toString());
                uploadDataToDatabase(ownerName, businessName, task.getResult().toString());
                isImageChanged = false; // Reset here if the image to false
            } else {
                Log.d(TAG, "Download Url retrieval: failed", task.getException());
                Toast.makeText(getContext(), "Failed to get download url", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void uploadDataToDatabase(String ownerName, String businessName, String imageUrl) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            String userId = user.getUid();
            Business business = new Business(ownerName, businessName, imageUrl);
            Database.child(userId).setValue(business)
                    .addOnSuccessListener(l -> {
                        if (getActivity() != null) {
                            Utility.showToast(getActivity().getApplicationContext(), "הפרופיל עודכן");
                            sharedViewModel.setBusiness(business);
                            updateSharedPreferences(userId, business);
                        }
                    })
                    .addOnFailureListener(e -> Utility.showToast(getContext(), "נכשל בעדכון המידע"));
        }
    }

    private void updateSharedPreferences(String userId, Business business) {
        SharedPreferences sharedPreferences = getActivity().getSharedPreferences("BusinessData" + userId, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("ownerName", business.getOwnerName());
        editor.putString("businessName", business.getBusinessName());
        editor.putString("imageUrl", business.getImageUrl());
        editor.apply();
        Log.d(TAG, "SharedPreferences updated");
    }

    private String getFileExtension(Uri uri) {
        String type = requireActivity().getContentResolver().getType(uri);
        String extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(type);
        Log.d(TAG, "MIME type: " + type + ", File extension: " + extension);
        if (type == null) {
            return null;
        }
        return extension;
    }

    // the image will be loaded or refreshed whenever the fragment or activity resumes or comes
    // into the foreground. This ensures that the image is always up to date and displayed correctly
    // to the user.
    @Override
    public void onResume() {
        super.onResume();
        loadImageWithPicasso(); // Load the image using Picasso
    }
}
