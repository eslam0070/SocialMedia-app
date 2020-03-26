package com.eso.socialmedia.ui.fragment;

import android.content.Context;
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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.eso.socialmedia.R;
import com.eso.socialmedia.adapter.PostAdapter;
import com.eso.socialmedia.model.Post;
import com.eso.socialmedia.ui.activity.AddPostActivity;
import com.eso.socialmedia.ui.activity.SettingsActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

import static com.eso.socialmedia.utils.Common.POSTS;

/**
 * A simple {@link Fragment} subclass.
 */
public class HomeFragment extends Fragment {

    private FirebaseAuth mAuth;
    private NavController navController;
    private RecyclerView mPostsRecyclerView;
    private List<Post> postList = new ArrayList<>();
    private PostAdapter postAdapter;
    public HomeFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setHasOptionsMenu(true);
        navController = Navigation.findNavController(view);
        Toolbar toolbar = view.findViewById(R.id.toolbar_home);
        ((AppCompatActivity) getActivity()).setSupportActionBar(toolbar);
        if (((AppCompatActivity) getActivity()).getSupportActionBar() != null) {
            ((AppCompatActivity) getActivity()).getSupportActionBar().setTitle("Home");
        }
        mAuth = FirebaseAuth.getInstance();

        mPostsRecyclerView = view.findViewById(R.id.postsRecyclerView);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getContext());
        linearLayoutManager.setStackFromEnd(true);
        linearLayoutManager.setReverseLayout(true);
        mPostsRecyclerView.setLayoutManager(linearLayoutManager);
        mPostsRecyclerView.setItemAnimator(new DefaultItemAnimator());
        loadPosts();
    }

    private void loadPosts() {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference(POSTS);
        ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                postList.clear();
                for (DataSnapshot ds : dataSnapshot.getChildren()){
                    Post post = ds.getValue(Post.class);
                    postList.add(post);

                    postAdapter = new PostAdapter(getContext(),postList);
                    mPostsRecyclerView.setAdapter(postAdapter);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void searchPosts(String s){
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference(POSTS);
        ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                postList.clear();
                for (DataSnapshot ds : dataSnapshot.getChildren()){
                    Post post = ds.getValue(Post.class);
                    if (post.getpDescription().toLowerCase().contains(s.toLowerCase()) ||
                    post.getpDescription().toLowerCase().contains(s.toLowerCase())){
                        postList.add(post);
                    }

                    postAdapter = new PostAdapter(getContext(),postList);
                    mPostsRecyclerView.setAdapter(postAdapter);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.menu, menu);
        MenuItem item = menu.findItem(R.id.action_search);
        SearchView searchView = (SearchView) item.getActionView();
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                if (!TextUtils.isEmpty(query))
                    searchPosts(query);
                else
                    loadPosts();
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (!TextUtils.isEmpty(newText))
                    searchPosts(newText);
                else
                    loadPosts();
                return false;
            }
        });
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_logout) {
            mAuth.signOut();
            navController.navigate(R.id.action_dashboardFragment_to_loginFragment);
        } else if (item.getItemId() == R.id.action_add_post) {
            startActivity(new Intent(getContext(), AddPostActivity.class));
        }else if (item.getItemId() == R.id.action_search){

        }else if (item.getItemId() == R.id.action_setting) {
            getActivity().startActivity(new Intent(getContext(), SettingsActivity.class));
        }
        return true;
    }
}
