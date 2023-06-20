package com.example.receiptskeeper.utils;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;


import androidx.annotation.NonNull;

import com.example.receiptskeeper.classes.Business;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.storage.StorageReference;

public class FirebaseHandler {
    private static FirebaseHandler firebaseHandlerInstance = null;
    private FirebaseAuth mAuth;
    private  FirebaseFirestore db;
    private  FirebaseStorage storage;
    private FirebaseDatabase database;

    // Private constructor to prevent instantiation
    private FirebaseHandler() {
        mAuth = FirebaseAuth.getInstance();
        this.db = FirebaseFirestore.getInstance();
        this.storage = FirebaseStorage.getInstance();
        this.database = FirebaseDatabase.getInstance();
    }

    public DatabaseReference getDatabaseReference() {
        return database.getReference();
    }

    public String getCurrentUserId() {
      return mAuth.getUid();
    }

    public DocumentReference getStoreDocRefWithDate(int year,int month,int day) {
        String yearString = year + "";
        String monthString=month + "";
        String dayString=day + "";
        return db.collection(mAuth.getUid())
                .document(yearString).collection(monthString).document(dayString);
    }

    public DocumentReference getStoreDocRefWithDateAndCustomerKey(int year,int month,int day, String customerKey) {
        String yearString = year + "";
        String monthString=month + "";
        String dayString=day + "";
        return db.collection(mAuth.getUid())
                .document(yearString).collection(monthString).document(dayString).collection("DayQueues").document(customerKey);
    }

    public StorageReference getStorageRefWithDateAndCustomerKey(int year, int month, int day, String customerKey) {

        return storage.getReference().child(mAuth.getUid()).child("" + year).child("" + month).
                child("" + day).child("DayQueues").child(customerKey);
    }

    public static synchronized FirebaseHandler getInstance() {
        if (firebaseHandlerInstance == null) {
            firebaseHandlerInstance = new FirebaseHandler();
        }
        return firebaseHandlerInstance;
    }

    public FirebaseUser getCurrentUser() {
        return mAuth.getCurrentUser();
    }

    public DatabaseReference getUserDatabaseReference(String userId) {
        return database.getReference(userId);
    }

    public void signInWithEmail(String email, String password, OnCompleteListener<AuthResult> onCompleteListener) {
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(onCompleteListener);
    }

    public void signInWithGoogle(GoogleSignInAccount account, OnCompleteListener<AuthResult> onCompleteListener) {
        AuthCredential credential = GoogleAuthProvider.getCredential(account.getIdToken(), null);
        mAuth.signInWithCredential(credential).addOnCompleteListener(onCompleteListener);
    }

    public void sendPasswordResetEmail(String email, Context context) {
        mAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Utility.showToast(context, "אימייל נשלח בהצלחה");
                    } else {
                        Utility.showToast(context, "אירעה שגיאה");
                    }
                })
                .addOnFailureListener(e -> Utility.showToast(context, "נכשל"));
    }

    public void createUserWithEmail(String email, String password, OnCompleteListener<AuthResult> onCompleteListener) {
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(onCompleteListener);
    }

    public interface OnDataFetchedListener {

        // defines two callback methods: onDataFetched() and onError()
        void onDataFetched(Business business);
        void onError(String errorMessage);
    }

    public void fetchBusinessData(String userId, SharedPreferences sharedPreferences, OnDataFetchedListener onDataFetchedListener) {
        DatabaseReference businessRef = getUserDatabaseReference(userId);
        businessRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Business business = dataSnapshot.getValue(Business.class);
                onDataFetchedListener.onDataFetched(business);
                // Save data to Shared Preferences
                if (business != null) {
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putString("OwnerName", business.getOwnerName());
                    editor.putString("BusinessName", business.getBusinessName());
                    editor.putString("ImageUrl", business.getImageUrl());
                    editor.apply();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                onDataFetchedListener.onError(databaseError.getMessage());
            }
        });
    }

    public void signOut() {
        mAuth.signOut();
    }
}
