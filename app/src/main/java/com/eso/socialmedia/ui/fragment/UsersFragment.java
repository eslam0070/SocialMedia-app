package com.eso.socialmedia.ui.fragment;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SearchView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.eso.socialmedia.R;
import com.eso.socialmedia.adapter.UsersAdapter;
import com.eso.socialmedia.model.Users;
import com.eso.socialmedia.ui.activity.ChatActivity;
import com.eso.socialmedia.ui.activity.SettingsActivity;
import com.eso.socialmedia.ui.activity.ThereProfileActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

import static com.eso.socialmedia.utils.Common.HISUID;
import static com.eso.socialmedia.utils.Common.USERS;

/**
 * A simple {@link Fragment} subclass.
 */
public class UsersFragment extends Fragment {

    private RecyclerView mUserRecyclerView;
    private UsersAdapter usersAdapter;
    private List<Users> usersList = new ArrayList<>();
    private DatabaseReference ref;
    private FirebaseAuth mAuth;
    private NavController navController;
    public UsersFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_users, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setHasOptionsMenu(true);
        navController = Navigation.findNavController(view);
        mAuth = FirebaseAuth.getInstance();
        Toolbar toolbar = view.findViewById(R.id.toolbar);
        ((AppCompatActivity)getActivity()).setSupportActionBar(toolbar);
        if (((AppCompatActivity) getActivity()).getSupportActionBar() != null){
            ((AppCompatActivity) getActivity()).getSupportActionBar().setTitle("Users");
        }
        mUserRecyclerView = view.findViewById(R.id.user_RecyclerView);
        mUserRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        mUserRecyclerView.setHasFixedSize(true);

        getAllUsers();

    }

    private void getAllUsers() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        ref = FirebaseDatabase.getInstance().getReference(USERS);
        ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                usersList.clear();
                for (DataSnapshot ds :dataSnapshot.getChildren()){
                    Users users = ds.getValue(Users.class);
                    if (!users.getUid().equals(user.getUid()))
                        usersList.add(users);

                    usersAdapter = new UsersAdapter(getContext(),usersList);
                    mUserRecyclerView.setAdapter(usersAdapter);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.menu,menu);

        MenuItem item = menu.findItem(R.id.action_search);
        SearchView searchView = (SearchView) item.getActionView();
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                if (!TextUtils.isEmpty(query.trim()))
                    searchUser(query);
                else
                    getAllUsers();
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (!TextUtils.isEmpty(newText.trim()))
                    searchUser(newText);
                else
                    getAllUsers();
                return false;
            }
        });
        super.onCreateOptionsMenu(menu, inflater);
    }

    private void searchUser(String query) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        ref = FirebaseDatabase.getInstance().getReference(USERS);
        ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                usersList.clear();
                for (DataSnapshot ds :dataSnapshot.getChildren()){
                    Users users = ds.getValue(Users.class);
                    if (!users.getUid().equals(user.getUid())){
                        if (users.getName().toLowerCase().contains(query.toLowerCase()) ||
                                users.getEmail().toLowerCase().contains(query.toLowerCase()))
                            usersList.add(users);
                    }

                    usersAdapter = new UsersAdapter(getContext(),usersList);
                    usersAdapter.notifyDataSetChanged();
                    mUserRecyclerView.setAdapter(usersAdapter);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_logout){
            mAuth.signOut();
            navController.navigate(R.id.action_dashboardFragment_to_loginFragment);
        }else if (item.getItemId() == R.id.action_setting) {
            getActivity().startActivity(new Intent(getContext(), SettingsActivity.class));
        }
        return true;
    }
}
