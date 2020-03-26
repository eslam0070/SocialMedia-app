package com.eso.socialmedia.ui.fragment;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.eso.socialmedia.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;

import static com.eso.socialmedia.utils.Common.COVER;
import static com.eso.socialmedia.utils.Common.EMAIL;
import static com.eso.socialmedia.utils.Common.IMAGE;
import static com.eso.socialmedia.utils.Common.NAME;
import static com.eso.socialmedia.utils.Common.ONLINE_STATUS;
import static com.eso.socialmedia.utils.Common.PHONE;
import static com.eso.socialmedia.utils.Common.TYPING_TO;
import static com.eso.socialmedia.utils.Common.UID;
import static com.eso.socialmedia.utils.Common.USERS;

/**
 * A simple {@link Fragment} subclass.
 */
public class RegisterFragment extends Fragment {

    private TextInputLayout mRegEmail, mRegPassword;
    private Button mRegButton;
    private ProgressDialog progressDialog;
    private FirebaseAuth mAuth;
    private NavController navController;
    private TextView mRegIntentLogin;

    public RegisterFragment() {
        // Required empty public constructor

    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_register, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // Action Bar
        navController = Navigation.findNavController(view);
        mAuth = FirebaseAuth.getInstance();
        progressDialog = new ProgressDialog(getContext());
        progressDialog.setMessage("Registering User...");
        mRegEmail = view.findViewById(R.id.log_email);
        mRegPassword = view.findViewById(R.id.log_password);
        mRegButton = view.findViewById(R.id.reg_button);
        mRegButton.setOnClickListener(v -> {
            String email = mRegEmail.getEditText().getText().toString().trim();
            String password = mRegPassword.getEditText().getText().toString();
            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                mRegEmail.setError("Invalid Email");
                mRegEmail.setFocusable(true);
            } else if (password.length() < 6) {
                mRegPassword.setError("Password length at least 6 characters");
                mRegPassword.setFocusable(true);
            } else
                createAccount(email, password);
        });
        mRegIntentLogin = view.findViewById(R.id.intent_login);
        mRegIntentLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                navController.navigate(R.id.action_registerFragment_to_loginFragment);
            }
        });

    }

    private void createAccount(final String email, final String password) {
        progressDialog.show();
        mAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if (task.isSuccessful()) {
                    progressDialog.dismiss();
                    FirebaseUser user = mAuth.getCurrentUser();
                    HashMap<Object,String> hashMap = new HashMap<Object, String>();
                    hashMap.put(UID,user.getUid());
                    hashMap.put(EMAIL,email);
                    hashMap.put(NAME,"");
                    hashMap.put(ONLINE_STATUS,"online");
                    hashMap.put(TYPING_TO,"noOne");
                    hashMap.put(PHONE,"");
                    hashMap.put(IMAGE,"");
                    hashMap.put(COVER,"");
                    FirebaseDatabase fa = FirebaseDatabase.getInstance();
                    DatabaseReference reference = fa.getReference().child(USERS);
                    reference.child(user.getUid()).setValue(hashMap);
                    navController.navigate(R.id.action_registerFragment_to_loginFragment);
                }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                progressDialog.dismiss();
                Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
