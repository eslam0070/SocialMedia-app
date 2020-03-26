package com.eso.socialmedia.ui.activity;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.SearchView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.eso.socialmedia.R;
import com.eso.socialmedia.adapter.PostAdapter;
import com.eso.socialmedia.model.Post;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.mikhaellopez.circularimageview.CircularImageView;

import java.util.ArrayList;
import java.util.List;

import static com.eso.socialmedia.utils.Common.COVER;
import static com.eso.socialmedia.utils.Common.EMAIL;
import static com.eso.socialmedia.utils.Common.IMAGE;
import static com.eso.socialmedia.utils.Common.NAME;
import static com.eso.socialmedia.utils.Common.PHONE;
import static com.eso.socialmedia.utils.Common.POSTS;
import static com.eso.socialmedia.utils.Common.UID;
import static com.eso.socialmedia.utils.Common.USERS;

public class ThereProfileActivity extends AppCompatActivity {

    private RecyclerView postsRecyclerView;
    private List<Post> postList = new ArrayList<>();
    PostAdapter postAdapter;
    String uid;
    FirebaseAuth mAuth;
    DatabaseReference reference;
    CircularImageView mAvatarIv;
    TextView mNameTv;
    ImageView image_header;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_there_profile);
        Toolbar toolbar = findViewById(R.id.toolbar_there_profile);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null){
            getSupportActionBar().setTitle("Profile");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        mAuth = FirebaseAuth.getInstance();
        Intent intent = getIntent();
        uid = intent.getStringExtra(UID);
        mAvatarIv = findViewById(R.id.avatarIv);
        mNameTv = findViewById(R.id.nameTv);
        postsRecyclerView = findViewById(R.id.recyclerView_posts);
        image_header = findViewById(R.id.image_cover);
        reference = FirebaseDatabase.getInstance().getReference().child(USERS);
        Query query = reference.orderByChild(UID).equalTo(uid);
        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot ds : dataSnapshot.getChildren()) {
                    // get Data
                    String name = ds.child(NAME).getValue(String.class);
                    String image = ds.child(IMAGE).getValue(String.class);
                    String cover = ds.child(COVER).getValue(String.class);
                    // set Data
                    mNameTv.setText(name);
                    Glide.with(ThereProfileActivity.this).load(image).placeholder(R.drawable.profile).into(mAvatarIv);
                    Glide.with(ThereProfileActivity.this).load(cover).into(image_header);
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
            }
        });
        loadHistPosts();

    }

    private void loadHistPosts() {
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setStackFromEnd(true);
        linearLayoutManager.setReverseLayout(true);
        postsRecyclerView.setLayoutManager(linearLayoutManager);
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference(POSTS);
        Query query = ref.orderByChild(UID).equalTo(uid);
        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot ds : dataSnapshot.getChildren()){
                    Post post = ds.getValue(Post.class);
                    postList.add(post);
                    postAdapter = new PostAdapter(ThereProfileActivity.this,postList);
                    postsRecyclerView.setAdapter(postAdapter);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void searchHistPosts(String search){
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setStackFromEnd(true);
        linearLayoutManager.setReverseLayout(true);
        postsRecyclerView.setLayoutManager(linearLayoutManager);
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference(POSTS);
        Query query = ref.orderByChild(UID).equalTo(uid);
        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot ds : dataSnapshot.getChildren()){
                    Post post = ds.getValue(Post.class);
                    if (post.getpDescription().toLowerCase().contains(search.toLowerCase()) ||
                            post.getpDescription().toLowerCase().contains(search.toLowerCase())){
                        postList.add(post);
                    }
                    postAdapter = new PostAdapter(ThereProfileActivity.this,postList);
                    postsRecyclerView.setAdapter(postAdapter);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(@NonNull Menu menu) {
        getMenuInflater().inflate(R.menu.menu,menu);
        MenuItem item = menu.findItem(R.id.action_search);
        SearchView searchView = (SearchView) item.getActionView();
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                if (!TextUtils.isEmpty(query.trim()))
                    searchHistPosts(query);
                else
                    loadHistPosts();
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (!TextUtils.isEmpty(newText.trim()))
                    searchHistPosts(newText);
                else
                    loadHistPosts();
                return false;
            }
        });
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_logout){
            mAuth.signOut();
            startActivity(new Intent(this,MainActivity.class));
            finish();
        }
        return true;
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return super.onSupportNavigateUp();
    }
}
