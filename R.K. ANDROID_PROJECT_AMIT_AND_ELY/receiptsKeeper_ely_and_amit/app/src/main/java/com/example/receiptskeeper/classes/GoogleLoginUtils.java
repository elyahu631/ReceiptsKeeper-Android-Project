package com.example.receiptskeeper.classes;
import android.app.Activity;
import android.content.Intent;
import android.util.Log;

import com.example.receiptskeeper.HomeActivity;
import com.example.receiptskeeper.R;
import com.example.receiptskeeper.utils.FirebaseHandler;
import com.example.receiptskeeper.utils.Utility;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;

import java.util.Objects;

public class GoogleLoginUtils {

    private final int googleRequestCode;
    private GoogleSignInClient client;
    private final FirebaseHandler firebaseHandler;

    public GoogleLoginUtils(Activity activity, FirebaseHandler firebaseHandler, int googleRequestCode) {
        this.googleRequestCode = googleRequestCode;
        this.firebaseHandler = firebaseHandler;
        initGoogle(activity);
    }

    private void initGoogle(Activity activity) {
        GoogleSignInOptions options = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(activity.getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        client = GoogleSignIn.getClient(activity, options);

        Log.d("GoogleSignInUtils", "Initialized GoogleSignInClient with requestIdToken: " + options.getServerClientId());
    }

    public void performGoogleSignIn(Activity activity) {
        // Log the Google sign-in attempt
        Log.d("GoogleSignInUtils", "Performing Google sign-in");

        Intent intent = client.getSignInIntent();
        activity.startActivityForResult(intent, googleRequestCode);
    }

    public void handleGoogleSignInResult(Activity activity, Intent data) {
        Log.d("GoogleSignInUtils", "Handling Google sign-in result");
        Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
        try {
            GoogleSignInAccount account = task.getResult(ApiException.class);

            Log.d("GoogleSignInUtils", "Successful sign-in: " + account.getEmail());

            firebaseHandler.signInWithGoogle(account, t -> {
                if (t.isSuccessful()) {
                    Utility.startActivity(activity, HomeActivity.class);
                } else {
                    Log.d("GoogleSignInUtils", "Failed sign-in with exception: " + t.getException());

                    Utility.showToast(activity, Objects.requireNonNull(t.getException()).getMessage());
                }
            });
        } catch (ApiException e) {
            e.printStackTrace();
            Log.d("GoogleSignInUtils", "Failed sign-in with ApiException: " + e.getStatusCode());
        }
    }

}
