package com.eso.socialmedia.ui.fragment;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;

import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.eso.socialmedia.R;
import com.eso.socialmedia.notification.Token;
import com.eso.socialmedia.ui.activity.MainActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.iid.FirebaseInstanceId;

import static com.eso.socialmedia.utils.Common.SP_USER;
import static com.eso.socialmedia.utils.Common.TOKENS;

/**
 * A simple {@link Fragment} subclass.
 */
public class DashboardFragment extends Fragment implements BottomNavigationView.OnNavigationItemSelectedListener {

    private FirebaseAuth mAuth;
    private String mUID;

    public DashboardFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_dashboard, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mAuth = FirebaseAuth.getInstance();
        BottomNavigationView bottomNavigationView = view.findViewById(R.id.navigation);
        bottomNavigationView.setOnNavigationItemSelectedListener(this);
        replaceFragment(new HomeFragment());
        checkUserStatus();
        updateToken(FirebaseInstanceId.getInstance().getToken());
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.nav_home:
                replaceFragment(new HomeFragment());
                return true;
            case R.id.nav_profile:
                replaceFragment(new ProfileFragment());
                return true;
            case R.id.nav_users:
                replaceFragment(new UsersFragment());
                return true;
            case R.id.nav_chats:
                replaceFragment(new ChatListFragment());
                return true;
            case R.id.nav_notification:
                replaceFragment(new NotificationsFragment());
                return true;
        }
        return false;
    }

    private void replaceFragment(Fragment fragment) {
        getFragmentManager().beginTransaction().replace(R.id.fragmentLayout, fragment).commit();
    }

    private void updateToken(String token) {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference(TOKENS);
        Token mToken = new Token(token);
        ref.child(mAuth.getCurrentUser().getUid()).setValue(mToken);
    }

    @Override
    public void onStart() {
        checkUserStatus();
        super.onStart();
    }

    private void checkUserStatus() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null){
            mUID = user.getUid();
            SharedPreferences sp;
            sp = getActivity().getSharedPreferences(SP_USER,0);
            SharedPreferences.Editor editor = sp.edit();
            editor.putString("Current_USERID",mUID);
            editor.apply();
        }else {
            startActivity(new Intent(getActivity(), MainActivity.class));
            getActivity().finish();
        }
    }
}
