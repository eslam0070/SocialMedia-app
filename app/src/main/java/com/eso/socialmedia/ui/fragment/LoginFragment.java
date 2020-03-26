package com.eso.socialmedia.ui.fragment;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.eso.socialmedia.R;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;

import static com.eso.socialmedia.utils.Common.COVER;
import static com.eso.socialmedia.utils.Common.ONLINE_STATUS;
import static com.eso.socialmedia.utils.Common.TYPING_TO;
import static com.eso.socialmedia.utils.Common.USERS;
import static com.eso.socialmedia.utils.Common.EMAIL;
import static com.eso.socialmedia.utils.Common.IMAGE;
import static com.eso.socialmedia.utils.Common.NAME;
import static com.eso.socialmedia.utils.Common.PHONE;
import static com.eso.socialmedia.utils.Common.UID;

/**
 * A simple {@link Fragment} subclass.
 */
public class LoginFragment extends Fragment {

    private FirebaseAuth mAuth;
    private TextInputLayout mLogEmail;
    private TextInputLayout mLogPassword;
    private Button mLogButton;
    private TextView mRegIntentLogin;
    private NavController navController;
    private ProgressDialog progressDialog;
    private TextView mLogForgetPassword;
    private ImageButton mGoogleLogin;
    private static final int RC_SIGN_IN = 100;
    private GoogleSignInClient googleSignInClient;
    public LoginFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_login, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail().build();
        googleSignInClient = GoogleSignIn.getClient(getContext(),gso);
        mAuth = FirebaseAuth.getInstance();
        progressDialog = new ProgressDialog(getContext());
        progressDialog.setMessage("Login User....");
        navController = Navigation.findNavController(view);
        mLogEmail = view.findViewById(R.id.log_email);
        mLogPassword = view.findViewById(R.id.log_password);
        mLogButton = view.findViewById(R.id.reg_button);
        mLogButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = mLogEmail.getEditText().getText().toString().trim();
                String password = mLogPassword.getEditText().getText().toString();
                if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    mLogEmail.setError("Invalid Email");
                    mLogEmail.setFocusable(true);
                } else if (password.length() < 6) {
                    mLogPassword.setError("Password length at least 6 characters");
                    mLogPassword.setFocusable(true);
                } else
                    loginAccount(email, password);
            }
        });
        mRegIntentLogin = view.findViewById(R.id.intent_login);
        mRegIntentLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                navController.navigate(R.id.action_loginFragment_to_registerFragment);
            }
        });
        mLogForgetPassword = view.findViewById(R.id.log_forget_password);
        mLogForgetPassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showRecoverPasswordDialog();
            }
        });
        mGoogleLogin = view.findViewById(R.id.googleLogin);
        mGoogleLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent signIntent = googleSignInClient.getSignInIntent();
                startActivityForResult(signIntent,RC_SIGN_IN);
            }
        });
    }

    private void showRecoverPasswordDialog() {
        // AlertDialog
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Recover Password");

        // set Layout linear layout
        LinearLayout linearLayout = new LinearLayout(getContext());
        // Views to set in dialog
        final EditText emailEt = new EditText(getContext());
        emailEt.setHint("Email");
        emailEt.setInputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        emailEt.setMinEms(16);
        linearLayout.addView(emailEt);
        linearLayout.setPadding(10,10,10,10);
        builder.setView(linearLayout);

        //buttons recover
        builder.setPositiveButton("Recover", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // input email
                String email = emailEt.getText().toString().trim();
                beginRecover(email);
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        builder.show();
    }

    private void beginRecover(String email) {
        progressDialog.setMessage("Sending email...");
        progressDialog.show();
        mAuth.sendPasswordResetEmail(email).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful()){
                    progressDialog.dismiss();
                    Toast.makeText(getContext(), "Email Sent", Toast.LENGTH_SHORT).show();
                }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                progressDialog.dismiss();
                Toast.makeText(getContext(), e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loginAccount(String email, String password) {
        progressDialog.show();
        mAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if (task.isSuccessful()) {
                    progressDialog.dismiss();
                    navController.navigate(R.id.action_loginFragment_to_dashboardFragment);
                }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                progressDialog.dismiss();
                Toast.makeText(getContext(), e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN){
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                firebaseAuthWithGoogle(account);
            }catch (ApiException e){
                Toast.makeText(getContext(), e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void firebaseAuthWithGoogle(GoogleSignInAccount account) {
        AuthCredential credential = GoogleAuthProvider.getCredential(account.getIdToken(), null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()){
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (task.getResult().getAdditionalUserInfo().isNewUser()){
                            HashMap<Object,String> hashMap = new HashMap<Object, String>();
                            hashMap.put(UID,user.getUid());
                            hashMap.put(EMAIL,user.getEmail());
                            hashMap.put(NAME,"");
                            hashMap.put(ONLINE_STATUS,"online");
                            hashMap.put(TYPING_TO,"noOne");
                            hashMap.put(PHONE,"");
                            hashMap.put(IMAGE,"");
                            hashMap.put(COVER,"");
                            DatabaseReference reference = FirebaseDatabase.getInstance().getReference().child(USERS);
                            reference.child(user.getUid()).setValue(hashMap);
                        }
                        navController.navigate(R.id.action_loginFragment_to_dashboardFragment);
                    }
                }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(getContext(), e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }


    @Override
    public void onStart() {
        checkUserStatus();
        super.onStart();
    }

    private void checkUserStatus(){
        // Get current user
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null){
            // user is signed is stay here
            navController.navigate(R.id.action_loginFragment_to_dashboardFragment);
        }
    }
}
