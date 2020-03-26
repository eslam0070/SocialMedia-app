package com.eso.socialmedia.ui.activity;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.RecyclerView;

import com.eso.socialmedia.R;
import com.eso.socialmedia.adapter.UsersAdapter;
import com.eso.socialmedia.model.Users;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

import static com.eso.socialmedia.utils.Common.LIKES;
import static com.eso.socialmedia.utils.Common.USERS;

public class PostLikedByActivity extends AppCompatActivity {

    private Toolbar toolbar;
    private String postId;
    private RecyclerView mRecyclerViewPostLiked;
    private List<Users> usersList = new ArrayList<>();
    private UsersAdapter usersAdapter;
    private FirebaseAuth firebaseAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post_liked_by);
        firebaseAuth = FirebaseAuth.getInstance();
        toolbar = findViewById(R.id.toolbar_post_details);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setSubtitle(firebaseAuth.getCurrentUser().getEmail());
            getSupportActionBar().setTitle("Post Detail");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        postId = getIntent().getStringExtra("postId");
        mRecyclerViewPostLiked = findViewById(R.id.recyclerView_post_liked);
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference(LIKES);
        ref.child(postId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                usersList.clear();
                for (DataSnapshot ds : dataSnapshot.getChildren()){
                    String hisUid = ds.getRef().getKey();
                    getUsers(hisUid);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void getUsers(String hisUid) {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference(USERS);
        ref.orderByChild("uid").equalTo(hisUid).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot ds : dataSnapshot.getChildren()){
                    Users users = ds.getValue(Users.class);
                    usersList.add(users);
                }
                usersAdapter = new UsersAdapter(PostLikedByActivity.this,usersList);
                mRecyclerViewPostLiked.setAdapter(usersAdapter);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return super.onSupportNavigateUp();
    }
}
