package com.example.receiptskeeper;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.receiptskeeper.classes.GoogleLoginUtils;
import com.example.receiptskeeper.utils.FirebaseHandler;
import com.example.receiptskeeper.utils.Utility;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {
    private GoogleLoginUtils googleLoginUtils;
    final private int GOOGLE_REQUEST_CODE = 1234;

    private EditText userPass, userEmail;
    private Button btnLogin, btnGoogle;
    private FirebaseHandler firebaseHandler;
    private TextView forgetPass;
    private String email;
    private String password;
    private EditText emailEditText;
    private CheckBox checkBoxRemember;
    private boolean loginInProgress = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initFirebase();
        initViews();
        initButtons();
        buttonsAction();
        populateLoginDetails();
    }

    private void initFirebase() {
        firebaseHandler = FirebaseHandler.getInstance();
        firebaseHandler.signOut();
        googleLoginUtils = new GoogleLoginUtils(this, firebaseHandler, GOOGLE_REQUEST_CODE);

    }

    private void initButtons() {
        initLoginButton();
        initGoogleButton();
    }


    private void initLoginButton() {
        btnLogin = findViewById(R.id.btnLogin);
        btnLogin.setOnClickListener(view -> {
            if (!loginInProgress) {
                handleLoginButtonClick();
            }
        });
    }

    private void handleLoginButtonClick() {
        inputEmailAndPassword();
        if (isInputCorrect()) {
            disableUI();
            loginInProgress = true;
            firebaseHandler.signInWithEmail(userEmail.getText().toString(), userPass.getText().toString(), task -> {
                if (task.isSuccessful()) {
                    performBackgroundOperation();
                } else {
                    Utility.showToast(MainActivity.this,"משתמש או סיסמה שגויים");
                    enableUI();
                    loginInProgress = false;
                }
            });
        }
    }

    private void performBackgroundOperation() {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());

        executor.execute(() -> {
            handleLoginDetails();

            handler.post(() -> {
                startHomeActivity();
                enableUI();
                loginInProgress = false;
            });
        });
        executor.shutdown();
    }

    private void handleLoginDetails() {
        if (checkBoxRemember.isChecked()) {
            saveLoginDetails(email, password);
        } else {
            clearLoginDetails();
        }
    }

    private void startHomeActivity() {
        Utility.startActivity(getApplicationContext(), HomeActivity.class);
    }

    private void saveLoginDetails(String email, String password) {
        SharedPreferences sharedPreferences = getSharedPreferences("LoginDetails", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("Email", email);
        editor.putString("Password", password);
        editor.apply();
    }

    private void clearLoginDetails() {
        SharedPreferences sharedPreferences = getSharedPreferences("LoginDetails", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove("Email");
        editor.remove("Password");
        editor.apply();
    }

    private void populateLoginDetails() {
        SharedPreferences sharedPreferences = getSharedPreferences("LoginDetails", Context.MODE_PRIVATE);
        String savedEmail = sharedPreferences.getString("Email", null);
        String savedPassword = sharedPreferences.getString("Password", null);
        if (savedEmail != null && savedPassword != null) {
            userEmail.setText(savedEmail);
            userPass.setText(savedPassword);
            checkBoxRemember.setChecked(true);
        }
    }

    //opens the Google Sign-In intent
    private void initGoogleButton() {
        btnGoogle = findViewById(R.id.btnGoogle);
        btnGoogle.setOnClickListener(v -> {
            if (!loginInProgress) {
                 googleLoginUtils.performGoogleSignIn(this);
            }
        });
    }

    //retrieves the GoogleSignInAccount from the data intent
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == GOOGLE_REQUEST_CODE) {
            googleLoginUtils.handleGoogleSignInResult(MainActivity.this, data);
        }
    }

    public boolean isInputCorrect() {
        if (email.isEmpty()) {
            userEmail.setError(getString(R.string.mailError));
        } else if (password.isEmpty()) {
            userPass.setError(getString(R.string.PasswordError));
            return false;
        }
        return true;
    }

    private void showRecoverPasswordDialog() {
        AlertDialog.Builder builder = buildPasswordRecoveryDialog();
        builder.create().show();
    }

    private AlertDialog.Builder buildPasswordRecoveryDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("אפס סיסמה");
        LinearLayout linearLayout = createEmailInputLayout();
        builder.setView(linearLayout);
        builder.setPositiveButton("אפס", createPositiveButtonClickListener());
        builder.setNegativeButton("ביטול", (dialog, which) -> dialog.dismiss());

        return builder;
    }

    private LinearLayout createEmailInputLayout() {
        LinearLayout linearLayout = new LinearLayout(this);
        emailEditText = new EditText(this);
        emailEditText.setHint("דואר אלקטרוני");
        emailEditText.setMinEms(16);
        emailEditText.setInputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        linearLayout.addView(emailEditText);
        linearLayout.setPadding(10, 10, 10, 10);
        return linearLayout;
    }

    private DialogInterface.OnClickListener createPositiveButtonClickListener() {
        return (dialog, which) -> {
            String emailRes = emailEditText.getText().toString().trim();
            try {
                firebaseHandler.sendPasswordResetEmail(emailRes, MainActivity.this);
            } catch (Exception e) {
                Utility.showToast(MainActivity.this, "לא הכנסת אימייל");
            }
        };
    }

    private void buttonsAction() {
        forgetPass.setOnClickListener(v -> {
            if (!loginInProgress) {
                showRecoverPasswordDialog();
            }
        });

        findViewById(R.id.createNewAccount).setOnClickListener(v -> {
            if (!loginInProgress) {
                startActivity(new Intent(MainActivity.this, SignUpActivity.class));
            }
        });
    }

    private void inputEmailAndPassword() {
        email = userEmail.getText().toString();
        password = userPass.getText().toString();
    }

    private void initViews() {
        userEmail = findViewById(R.id.inputEmail);
        userPass = findViewById(R.id.inputPassword);
        btnLogin = findViewById(R.id.btnLogin);
        btnGoogle = findViewById(R.id.btnGoogle);
        forgetPass = findViewById(R.id.forgotPassword);
        checkBoxRemember = findViewById(R.id.checkBoxRemember);
    }

    @Override
    public void onBackPressed() {
        Intent a = new Intent(Intent.ACTION_MAIN);
        a.addCategory(Intent.CATEGORY_HOME);
        a.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(a);
    }

    private void disableUI() {
        btnLogin.setEnabled(false);
        btnGoogle.setEnabled(false);
        userEmail.setEnabled(false);
        userPass.setEnabled(false);
        forgetPass.setEnabled(false);
        checkBoxRemember.setEnabled(false);
    }

    private void enableUI() {
        btnLogin.setEnabled(true);
        btnGoogle.setEnabled(true);
        userEmail.setEnabled(true);
        userPass.setEnabled(true);
        forgetPass.setEnabled(true);
        checkBoxRemember.setEnabled(true);
    }
}
