package com.arose.barcodescanner;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.snackbar.Snackbar;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.rengwuxian.materialedittext.MaterialEditText;

import java.util.Objects;

import dmax.dialog.SpotsDialog;


public class LoginActivity extends AppCompatActivity {

    Button btnSign;
    Button btnRegister;
    ConstraintLayout rootLayout;
    android.app.AlertDialog waitingDialog;

    FirebaseAuth auth;
    FirebaseDatabase db;
    DatabaseReference users;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        //ist firebase
        auth = FirebaseAuth.getInstance();
        db = FirebaseDatabase.getInstance();
        users = db.getReference("user");

        //int view
        btnRegister = findViewById(R.id.btnRegister);
        btnSign = findViewById(R.id.btnSignIn);
        rootLayout = findViewById(R.id.rootLayout);

        //Event
        btnRegister.setOnClickListener(v -> showRegisterDialog());

        btnSign.setOnClickListener(v -> showLoginDialog());
    }

    private void showLoginDialog() {
        AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        dialog.setTitle("SIGN IN");
        dialog.setMessage("Please use email to sign in");

        LayoutInflater inflater = LayoutInflater.from(this);
        View login_layout = inflater.inflate(R.layout.sign_in,null);

        MaterialEditText editEmail = login_layout.findViewById(R.id.editEmail);
        MaterialEditText editPassword = login_layout.findViewById(R.id.editPassword);

        dialog.setView(login_layout);

        // set button
        dialog.setPositiveButton("Sign In", (dialog1, which) -> {
            dialog1.dismiss();

            //set disable Button Sign In if is processing
            btnSign.setEnabled(false);

            //check validation
            if(TextUtils.isEmpty(Objects.requireNonNull(editEmail.getText()).toString())){
                Snackbar.make(rootLayout, "please enter email address", Snackbar.LENGTH_SHORT)
                        .show();
                return;
            }

            if(TextUtils.isEmpty(Objects.requireNonNull(editPassword.getText()).toString())){
                Snackbar.make(rootLayout, "password is to short", Snackbar.LENGTH_SHORT)
                        .show();
                return;
            }

            //waitingDialog = new SpotsDialog(LoginActivity.this);
            waitingDialog = new SpotsDialog.Builder().setContext(LoginActivity.this).build();
            waitingDialog.show();

            //login
            auth.signInWithEmailAndPassword(editEmail.getText().toString(), editPassword.getText().toString())
                    .addOnSuccessListener(authResult -> {
                        waitingDialog.dismiss();
                        startActivity(new Intent(LoginActivity.this, MainActivity.class));
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        waitingDialog.dismiss();
                        Snackbar.make(rootLayout,"Failed "+e.getMessage(),Snackbar.LENGTH_SHORT)
                                .show();

                        //Activate Button
                        btnSign.setEnabled(true);
                    });
        });
        dialog.setNegativeButton("CANCEL", (dialog12, which) -> dialog12.dismiss());


        dialog.show();
    }

    private void showRegisterDialog() {
        AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        dialog.setTitle("REGISTER");
        dialog.setMessage("Please use email to register");

        LayoutInflater inflater = LayoutInflater.from(this);
        View register_layout = inflater.inflate(R.layout.register,null);

        MaterialEditText editEmail = register_layout.findViewById(R.id.editEmail);
        MaterialEditText editPassword = register_layout.findViewById(R.id.editPassword);
        MaterialEditText editPhone = register_layout.findViewById(R.id.editPhone);
        MaterialEditText editName = register_layout.findViewById(R.id.editName);

        dialog.setView(register_layout);

        // set button
        dialog.setPositiveButton("Register", (dialog1, which) -> {
            dialog1.dismiss();

            //check validation
            if(TextUtils.isEmpty(Objects.requireNonNull(editEmail.getText()).toString())){
                Snackbar.make(rootLayout, "please enter email address", Snackbar.LENGTH_SHORT)
                        .show();
                return;
            }
            if(TextUtils.isEmpty(Objects.requireNonNull(editPhone.getText()).toString())){
                Snackbar.make(rootLayout, "please enter phone number", Snackbar.LENGTH_SHORT)
                        .show();
                return;
            }
            if(Objects.requireNonNull(editName.getText()).toString().length() < 6){
                Snackbar.make(rootLayout, "please enter your name", Snackbar.LENGTH_SHORT)
                        .show();
                return;
            }
            if(TextUtils.isEmpty(Objects.requireNonNull(editPassword.getText()).toString())){
                Snackbar.make(rootLayout, "password is to short", Snackbar.LENGTH_SHORT)
                        .show();
                return;
            }

            //Register new user
            auth.createUserWithEmailAndPassword(editEmail.getText().toString(),editPassword.getText().toString())
                    .addOnSuccessListener(authResult -> {
                        // save users to db
                        User user = new User();
                        user.setEmail(editEmail.getText().toString());
                        user.setName(editName.getText().toString());
                        user.setPhone(editPhone.getText().toString());
                        user.setPassword(editPassword.getText().toString());

                        // use email to key
                        users.child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                                .setValue(user)
                                .addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void aVoid) {
                                        Snackbar.make(rootLayout, "RegisterSuccess Fully", Snackbar.LENGTH_SHORT)
                                                .show();
                                    }
                                })
                                .addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        Snackbar.make(rootLayout, "Failed "+e.getMessage(), Snackbar.LENGTH_SHORT)
                                                .show();
                                    }
                                });

                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Snackbar.make(rootLayout, "Failed "+e.getMessage(), Snackbar.LENGTH_SHORT)
                                    .show();
                        }
                    });
        });

        dialog.setNegativeButton("cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        dialog.show();

    }
}