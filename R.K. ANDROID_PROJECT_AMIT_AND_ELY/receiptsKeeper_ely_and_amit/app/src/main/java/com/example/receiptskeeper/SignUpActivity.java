package com.example.receiptskeeper;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.example.receiptskeeper.classes.GoogleLoginUtils;
import com.example.receiptskeeper.utils.FirebaseHandler;
import com.example.receiptskeeper.utils.Utility;

import java.util.Objects;


public class SignUpActivity extends AppCompatActivity implements View.OnClickListener {

    private GoogleLoginUtils googleLoginUtils;
    final private int googleRequestCode = 1234;
    private TextView alreadyHaveAccount;
    private EditText userEmail,userPass,userRepass;
    private Button btnSignUp,btnGoogle;
    private FirebaseHandler firebaseHandler;
    private String email ;
    private String password ;
    private String confirmPassword ;
    private String emailPattern;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        initViews();
        initFireBase();
        initButtons();
        navToLogin();
        googleLogin();
    }

    private void initFireBase() {
        firebaseHandler = FirebaseHandler.getInstance();  // Get instance of FirebaseHandler
        googleLoginUtils = new GoogleLoginUtils(this, firebaseHandler, googleRequestCode);

    }

    private void initButtons() {
        btnSignUp.setOnClickListener(this);
    }

    private void initViews() {
        userEmail = findViewById(R.id.inputEmail);
        userPass = findViewById(R.id.inputPassword);
        userRepass = findViewById(R.id.inputConfirmPassword);
        btnSignUp = findViewById(R.id.btnSignUp);
        btnGoogle = findViewById(R.id.btn_google);
        alreadyHaveAccount = findViewById(R.id.haveAccount);
    }

    private void navToLogin() {
        alreadyHaveAccount.setOnClickListener(v ->
                Utility.startActivity(SignUpActivity.this, MainActivity.class));
    }

    private void googleLogin() {
        btnGoogle.setOnClickListener(v -> googleLoginUtils.performGoogleSignIn(this));
    }

    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == googleRequestCode) {
            googleLoginUtils.handleGoogleSignInResult(SignUpActivity.this, data);
        }
    }

    @Override
    public void onClick(View v) {
        email = userEmail.getText().toString();
        password = userPass.getText().toString();
        confirmPassword = userRepass.getText().toString();
        emailPattern = "[a-zA-Za-z0-9._-]+@[a-z]+\\.+[a-z]+";
        if (v.getId() == R.id.btnSignUp) {
            signUpAction();
        }
    }

    private void signUpAction() {
        if (isInputCorrect()) {
            firebaseHandler.createUserWithEmail(userEmail.getText().toString(),
                    userPass.getText().toString(),
                    task -> {
                        if (task.isSuccessful()) {
                            Utility.startActivity(SignUpActivity.this, MainActivity.class);
                        } else {
                            Utility.showToast(SignUpActivity.this, Objects.requireNonNull(task.getException()).toString());
                        }
                    });
        } else {
            Utility.showToast(SignUpActivity.this, getString(R.string.signUpNoDetails));
        }
    }

    public boolean isInputCorrect(){
        if(!email.matches(emailPattern)){
            userEmail.setError(getString(R.string.mailError));
            return false;
        }else if(password.isEmpty() || password.length() < 6){
            userPass.setError(getString(R.string.PasswordError));
            return false;
        }else if(!password.equals(confirmPassword)){
            userRepass.setError(getString(R.string.confirmPasswordError));
            return false;
        }
        return true;
    }
}