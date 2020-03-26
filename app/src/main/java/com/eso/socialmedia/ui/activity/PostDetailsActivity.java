package com.eso.socialmedia.ui.activity;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.eso.socialmedia.R;
import com.eso.socialmedia.adapter.CommentAdapter;
import com.eso.socialmedia.model.Comment;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.mikhaellopez.circularimageview.CircularImageView;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import static com.eso.socialmedia.utils.Common.COMMENTS;
import static com.eso.socialmedia.utils.Common.IMAGE;
import static com.eso.socialmedia.utils.Common.LIKES;
import static com.eso.socialmedia.utils.Common.NAME;
import static com.eso.socialmedia.utils.Common.NOTIFICATION;
import static com.eso.socialmedia.utils.Common.POSTS;
import static com.eso.socialmedia.utils.Common.UID;
import static com.eso.socialmedia.utils.Common.USERS;

public class PostDetailsActivity extends AppCompatActivity {

    FirebaseAuth mAuth;
    ProgressDialog pd;
    CircularImageView mUPictureIv, mCAvatarIv;
    TextView mUNameTv, mPTimeTv, mPLike, mPDescription, pCommentTv,mLikeTv;
    ImageButton mMoreBtn, mSendbtn;
    LinearLayout mProfileLay;
    ImageView mPImageIv;
    Button mLinkBtn, mCommentBtn,shareBtn;
    EditText mCommentEt;
    Toolbar toolbar;
    String postId, myUid,myId, myEmail, myName, myDp, pLikes, hisDp, hisName, hisUid, pImage;
    boolean mProcessComment = false;
    boolean mProcessLike = false;
    DatabaseReference likesRef;
    RecyclerView mRecyclerViewComment;
    CommentAdapter commentAdapter;
    List<Comment> commentList = new ArrayList<>();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post_details);
        pd = new ProgressDialog(this);
        mAuth = FirebaseAuth.getInstance();
        myId = mAuth.getUid();
        Intent intent = getIntent();
        postId = intent.getStringExtra("postId");
        toolbar = findViewById(R.id.toolbar_post_details);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Post Detail");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        likesRef = FirebaseDatabase.getInstance().getReference().child(LIKES);
        shareBtn = findViewById(R.id.shareBtn);
        shareBtn.setOnClickListener(v -> {
            String pDescr = mPDescription.getText().toString().trim();

            BitmapDrawable bitmapDrawable = (BitmapDrawable)mPImageIv.getDrawable();
            if (bitmapDrawable == null)
                shareTextOnly(pDescr);
            else{
                Bitmap bitmap = bitmapDrawable.getBitmap();
                shareImageAndText(pDescr,bitmap);
            }
        });
        mUPictureIv = findViewById(R.id.uPictureIv);
        mUNameTv = findViewById(R.id.uNameTv);
        mPTimeTv = findViewById(R.id.pTimeTv);
        mMoreBtn = findViewById(R.id.moreBtn);
        mProfileLay = findViewById(R.id.profileLay);
        mPDescription = findViewById(R.id.pDescription);
        mPImageIv = findViewById(R.id.pImageIv);
        mLinkBtn = findViewById(R.id.likeBtn);
        mPLike = findViewById(R.id.mLikeTv);
        mCommentBtn = findViewById(R.id.commentBtn);
        mCAvatarIv = findViewById(R.id.cAvatarIv);
        mSendbtn = findViewById(R.id.sendbtn);
        mCommentEt = findViewById(R.id.commentEt);
        pCommentTv = findViewById(R.id.pCommentTv);
        mLikeTv = findViewById(R.id.mLikeTv);
        checkUserStatus();
        loadPostInfo();
        loadUserInfo();
        setLikes();

        getSupportActionBar().setSubtitle("SignedIn as: " + myEmail);
        mSendbtn.setOnClickListener(v -> postComment());
        mLinkBtn.setOnClickListener(v ->{
            likePost();
        });
        mMoreBtn.setOnClickListener(v -> {
            showMoreOptions();
        });

        mRecyclerViewComment = findViewById(R.id.recyclerView_comment);
        loadComments();
        mLikeTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(
                        new Intent(PostDetailsActivity.this, PostLikedByActivity.class)
                                .putExtra("postId",postId));
            }
        });
    }


    private void shareTextOnly(String pDescription) {
        String shareBody = pDescription;
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.putExtra(Intent.EXTRA_SUBJECT,"Subject Here");
        intent.putExtra(Intent.EXTRA_TEXT,shareBody);
        startActivity(Intent.createChooser(intent,"Share Via"));
    }

    private void shareImageAndText(String pDescription, Bitmap bitmap) {
        String shareBody = pDescription;
        Uri uri = saveImageToShare(bitmap);
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.putExtra(Intent.EXTRA_STREAM,uri);
        intent.putExtra(Intent.EXTRA_TEXT,shareBody);
        intent.putExtra(Intent.EXTRA_SUBJECT,"Subject Here");
        intent.setType("image/png");
        startActivity(Intent.createChooser(intent,"Share Via"));
    }

    private Uri saveImageToShare(Bitmap bitmap) {
        File imageFolder = new File(getCacheDir(),"images");
        Uri uri = null;
        try {
            imageFolder.mkdirs();
            File file = new File(imageFolder,"shared_image.png");
            FileOutputStream stream = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.PNG,90,stream);
            stream.flush();
            stream.close();
            uri = FileProvider.getUriForFile(this,"com.eso.socialmedia.fileprovider",file);
        }catch (Exception e){
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
        }
        return uri;
    }
    private void loadComments() {
        mRecyclerViewComment.setLayoutManager(new LinearLayoutManager(this));
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference(POSTS).child(postId).child(COMMENTS);
        ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                commentList.clear();
                for (DataSnapshot ds : dataSnapshot.getChildren()){
                    Comment comment = ds.getValue(Comment.class);
                    commentList.add(comment);
                    commentAdapter = new CommentAdapter(PostDetailsActivity.this,commentList,myUid,postId);
                    mRecyclerViewComment.setAdapter(commentAdapter);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void showMoreOptions() {
        PopupMenu popupMenu = new PopupMenu(this, mMoreBtn, Gravity.END);
        if (hisUid.equals(myUid)) {
            popupMenu.getMenu().add(Menu.NONE, 0, 0, "Edit");
            popupMenu.getMenu().add(Menu.NONE, 1, 0, "Delete");
        }
        popupMenu.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == 0) {
                Intent intent = new Intent(this, AddPostActivity.class);
                intent.putExtra("key", "editPost");
                intent.putExtra("editPostId", postId);
                this.startActivity(intent);
            } else if (id == 1) {
                beginDelete();
            }
            return false;
        });
        popupMenu.show();
    }

    private void beginDelete() {
        if (pImage.equals("noImage"))
            deleteWithoutImage();
        else
            deleteWithImage();
    }

    private void deleteWithImage() {
        ProgressDialog pd = new ProgressDialog(this);
        pd.setMessage("Deleting...");
        pd.show();
        StorageReference picRef = FirebaseStorage.getInstance().getReferenceFromUrl(pImage);
        picRef.delete().addOnSuccessListener(aVoid -> {
            Query query = FirebaseDatabase.getInstance().getReference(POSTS).orderByChild("pId").equalTo(postId);
            query.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    for (DataSnapshot ds : dataSnapshot.getChildren()) {
                        ds.getRef().removeValue();
                    }
                    Toast.makeText(PostDetailsActivity.this, "Deleted successfully", Toast.LENGTH_SHORT).show();
                    pd.dismiss();
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    pd.dismiss();
                    Toast.makeText(PostDetailsActivity.this, databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private void deleteWithoutImage() {
        ProgressDialog pd = new ProgressDialog(this);
        pd.setMessage("Deleting...");
        pd.show();
        Query query = FirebaseDatabase.getInstance().getReference(POSTS).orderByChild("pId").equalTo(postId);
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot ds : dataSnapshot.getChildren()) {
                    ds.getRef().removeValue();
                }
                Toast.makeText(PostDetailsActivity.this, "Deleted successfully", Toast.LENGTH_SHORT).show();
                pd.dismiss();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                pd.dismiss();
                Toast.makeText(PostDetailsActivity.this, databaseError.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void likePost() {
        mProcessLike = true;
        DatabaseReference postsRef = FirebaseDatabase.getInstance().getReference().child(POSTS);
        likesRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (mProcessLike) {
                    if (dataSnapshot.child(postId).hasChild(myUid)) {
                        postsRef.child(postId).child("pLikes").setValue("" + (Integer.parseInt(pLikes) - 1));
                        likesRef.child(postId).child(myUid).removeValue();
                        mProcessLike = false;
                        mLinkBtn.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.ic_thumb_up_black, 0, 0, 0);
                        mLinkBtn.setText("Like");
                    } else {
                        postsRef.child(postId).child("pLikes").setValue("" + (Integer.parseInt(pLikes) + 1));
                        likesRef.child(postId).child(myUid).setValue("Liked");
                        mProcessLike = false;
                        mLinkBtn.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.ic_thumb_up, 0, 0, 0);
                        mLinkBtn.setText("Liked");
                        addToHisNotifications(""+hisUid,""+postId,"Liked your post");
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void setLikes() {
        likesRef.addValueEventListener(new ValueEventListener() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.child(postId).hasChild(myUid)) {
                    mLinkBtn.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_thumb_up, 0, 0, 0);
                    mLinkBtn.setText("Liked");
                } else {
                    mLinkBtn.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_thumb_up_black, 0, 0, 0);
                    mLinkBtn.setText("Like");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void postComment() {
        pd.setMessage("Adding comment...");
        String comment = mCommentEt.getText().toString().trim();
        String timeStamp = String.valueOf(System.currentTimeMillis());
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference(POSTS).child(postId).child(COMMENTS);
        HashMap<String, Object> hashMap = new HashMap<>();
        hashMap.put("cId", timeStamp);
        hashMap.put("comment", comment);
        hashMap.put("timestamp", timeStamp);
        hashMap.put("uid", myUid);
        hashMap.put("uEmail", myEmail);
        hashMap.put("uDp", myDp);
        hashMap.put("uName", myName);
        reference.child(timeStamp).setValue(hashMap).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                pd.dismiss();
                mCommentEt.setText("");
                updateCommentCount();
                addToHisNotifications(""+hisUid,""+postId,"Commented on your post");
            }
        });
    }

    private void updateCommentCount() {
        mProcessComment = true;
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference(POSTS).child(postId);
        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (mProcessComment) {
                    String comments = dataSnapshot.child("pComments").getValue(String.class);
                    int newCommentVal = Integer.parseInt(comments) + 1;
                    ref.child("pComments").setValue("" + newCommentVal);
                    mProcessComment = false;
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void loadUserInfo() {
        Query ref = FirebaseDatabase.getInstance().getReference(USERS);
        ref.orderByChild(UID).equalTo(myUid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot ds : dataSnapshot.getChildren()) {
                    myName = ds.child(NAME).getValue(String.class);
                    myDp = ds.child(IMAGE).getValue(String.class);

                    Glide.with(PostDetailsActivity.this).load(myDp).into(mCAvatarIv);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void loadPostInfo() {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference(POSTS);
        Query query = ref.orderByChild("pId").equalTo(postId);
        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot ds : dataSnapshot.getChildren()) {
                    String pDescr = ds.child("pDescription").getValue(String.class);
                    pLikes = ds.child("pLikes").getValue(String.class);
                    String pTimeStamp = ds.child("pTime").getValue(String.class);
                    pImage = ds.child("pImage").getValue(String.class);
                    hisDp = ds.child("uDp").getValue(String.class);
                    hisUid = ds.child("uid").getValue(String.class);
                    String uEmail = ds.child("uEmail").getValue(String.class);
                    hisName = ds.child("uName").getValue(String.class);
                    String commentCount = ds.child("pComments").getValue(String.class);

                    Calendar calendar = Calendar.getInstance(Locale.getDefault());
                    calendar.setTimeInMillis(Long.parseLong(pTimeStamp));
                    String pTime = DateFormat.format("hh:mm aa", calendar).toString();

                    mPDescription.setText(pDescr);
                    mPLike.setText(pLikes + " Likes");
                    mPTimeTv.setText(pTime);
                    mUNameTv.setText(hisName);
                    pCommentTv.setText(commentCount + " Comments");
                    if (pImage.equals("noImage")) {
                        mPImageIv.setVisibility(View.GONE);
                    } else {
                        mPImageIv.setVisibility(View.VISIBLE);
                        Glide.with(PostDetailsActivity.this).load(pImage).into(mPImageIv);
                    }
                    Glide.with(PostDetailsActivity.this).load(hisDp).into(mUPictureIv);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void addToHisNotifications(String hisUid, String pId, String notification){
        String timestamp = ""+System.currentTimeMillis();

        HashMap<Object, String> hashMap = new HashMap<>();
        hashMap.put("pId", pId);
        hashMap.put("timestamp", timestamp);
        hashMap.put("pUid", hisUid);
        hashMap.put("notification", notification);
        hashMap.put("sUid", myUid);

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users");
        ref.child(hisUid).child("Notifications").child(timestamp).setValue(hashMap)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {

                    }
                }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {

            }
        });
    }
    @Override
    protected void onStart() {
        checkUserStatus();
        super.onStart();
    }

    private void checkUserStatus() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            myEmail = user.getEmail();
            myUid = user.getUid();
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return super.onSupportNavigateUp();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);
        menu.findItem(R.id.action_search).setVisible(true);
        menu.findItem(R.id.action_add_post).setVisible(true);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_logout) {
            mAuth.signOut();
            checkUserStatus();
        }
        return super.onOptionsItemSelected(item);
    }
}
