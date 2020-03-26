package com.eso.socialmedia.ui.activity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.format.DateFormat;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.eso.socialmedia.R;
import com.eso.socialmedia.adapter.ChatAdapter;
import com.eso.socialmedia.adapter.UsersAdapter;
import com.eso.socialmedia.model.Chat;
import com.eso.socialmedia.model.Users;
import com.eso.socialmedia.notification.APIService;
import com.eso.socialmedia.notification.Client;
import com.eso.socialmedia.notification.Data;
import com.eso.socialmedia.notification.Response;
import com.eso.socialmedia.notification.Sender;
import com.eso.socialmedia.notification.Token;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.mikhaellopez.circularimageview.CircularImageView;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;

import static com.eso.socialmedia.utils.Common.BLOCKED_USERS;
import static com.eso.socialmedia.utils.Common.CHATLIST;
import static com.eso.socialmedia.utils.Common.CHATS;
import static com.eso.socialmedia.utils.Common.HISUID;
import static com.eso.socialmedia.utils.Common.IMAGE;
import static com.eso.socialmedia.utils.Common.NAME;
import static com.eso.socialmedia.utils.Common.ONLINE_STATUS;
import static com.eso.socialmedia.utils.Common.TOKENS;
import static com.eso.socialmedia.utils.Common.TYPING_TO;
import static com.eso.socialmedia.utils.Common.UID;
import static com.eso.socialmedia.utils.Common.USERS;

@RequiresApi(api = Build.VERSION_CODES.M)
public class ChatActivity extends AppCompatActivity {

    FirebaseAuth mAuth;
    TextView mNameTv, mUserStatusTv;
    Toolbar mToolbarChat;
    RecyclerView mChatRecyclerView;
    EditText mMessageEt;
    ImageView blockIv;
    ImageButton mSendBtn,mAttachBtn;

    DatabaseReference usersRef,userRefForSeen;
    ValueEventListener seenListener;
    List<Chat> chatList = new ArrayList<>();
    ChatAdapter chatAdapter;
    String hisUid, myUid;
    APIService apiService;
    boolean notify = false;
    boolean isBlocked = false;
    static final int CAMERA_REQUEST_CODE = 100;
    static final int STORAGE_REQUEST_CODE = 200;
    static  final int IMAGE_GALLERY_CODE = 300;
    static  final int IMAGE_CAMERA_CODE = 400;
    String[] cameraPermission,storagePermission;
    Uri image_uri = null;

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        apiService = Client.getRetrofit("https://fcm.googleapis.com/").create(APIService.class);
        mAuth = FirebaseAuth.getInstance();
        hisUid = getIntent().getStringExtra(HISUID);
        myUid = mAuth.getCurrentUser().getUid();
        mToolbarChat = findViewById(R.id.toolbar_chat);
        setSupportActionBar(mToolbarChat);
        if(getSupportActionBar() != null){
            getSupportActionBar().setTitle("");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        mToolbarChat.setTitle("");
        mNameTv = findViewById(R.id.nameTv);
        mUserStatusTv = findViewById(R.id.userStatusTv);
        mChatRecyclerView = findViewById(R.id.chat_recyclerView);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setStackFromEnd(true);
        mChatRecyclerView.setLayoutManager(linearLayoutManager);
        mChatRecyclerView.setHasFixedSize(true);
        mMessageEt = findViewById(R.id.messageEt);
        mSendBtn = findViewById(R.id.sendBtn);

        usersRef = FirebaseDatabase.getInstance().getReference(USERS);
        Query userQuery = usersRef.orderByChild(UID).equalTo(hisUid);
        userQuery.addValueEventListener(new ValueEventListener() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot ds : dataSnapshot.getChildren()) {
                    String name = ds.child(NAME).getValue(String.class);
                    mNameTv.setText(name);

                    String typingStatus = ds.child(TYPING_TO).getValue(String.class);
                    if (typingStatus.equals(myUid))
                        mUserStatusTv.setText("typing...");
                    else {
                        String onlineStatus = ds.child(ONLINE_STATUS).getValue(String.class);
                        if (onlineStatus.equals("online"))
                            mUserStatusTv.setText(onlineStatus);
                        else{
                            Calendar cal = Calendar.getInstance(Locale.ENGLISH);
                            cal.setTimeInMillis(Long.parseLong(onlineStatus));
                            String dateTime = DateFormat.format("dd/MM/yyyy hh:mm aa",cal).toString();
                            mUserStatusTv.setText("Last seen at: " + dateTime);
                        }
                    }

                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
        mSendBtn.setOnClickListener(v -> {
            notify = true;
            String message = mMessageEt.getText().toString().trim();
            if (TextUtils.isEmpty(message))
                Toast.makeText(ChatActivity.this, "Cannot send the empty message", Toast.LENGTH_SHORT).show();
            else
                sendMessage(message);

            mMessageEt.setText("");
        });
        mMessageEt.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.toString().trim().length() == 0)
                    checkTypingStatus("noOne");
                else
                    checkTypingStatus(hisUid);
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
        cameraPermission = new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE};
        storagePermission = new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE};
        mAttachBtn = findViewById(R.id.attachBtn);
        mAttachBtn.setOnClickListener(v -> {
            showImagePickDialog();
        });
        blockIv = findViewById(R.id.blockIv);
        blockIv.setOnClickListener(v -> {
            if (isBlocked)
                unBlockUser();
            else
                blockUser();
        });
        readMessages();
        checkIsBlocked();
        seenMessage();
    }

    private void seenMessage() {
        userRefForSeen = FirebaseDatabase.getInstance().getReference(CHATS);
        seenListener = userRefForSeen.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot ds : dataSnapshot.getChildren()){
                    Chat chat = ds.getValue(Chat.class);
                    if (chat.getReceiver().equals(myUid) && chat.getSender().equals(hisUid)){
                        HashMap<String, Object> hashSeenHashMap = new HashMap<>();
                        hashSeenHashMap.put("isSeen",true);
                        ds.getRef().updateChildren(hashSeenHashMap);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void readMessages() {
        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference(CHATS);
        dbRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                chatList.clear();
                for (DataSnapshot ds : dataSnapshot.getChildren()){
                    Chat chat = ds.getValue(Chat.class);
                    if(chat.getReceiver().equals(myUid) && chat.getSender().equals(hisUid) ||
                    chat.getReceiver().equals(hisUid) && chat.getSender().equals(myUid)){
                        chatList.add(chat);
                    }
                    chatAdapter = new ChatAdapter(ChatActivity.this,chatList);
                    chatAdapter.notifyDataSetChanged();
                    mChatRecyclerView.setAdapter(chatAdapter);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void sendMessage(String message) {
        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference();
        String timestamp = String.valueOf(System.currentTimeMillis());
        HashMap<String, Object> hashMap = new HashMap<>();
        hashMap.put("sender", myUid);
        hashMap.put("receiver", hisUid);
        hashMap.put("message", message);
        hashMap.put("timestamp",timestamp);
        hashMap.put("isSeen",false);
        hashMap.put("type","text");
        databaseReference.child(CHATS).push().setValue(hashMap);

        DatabaseReference database = FirebaseDatabase.getInstance().getReference(USERS).child(myUid);
        database.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Users users = dataSnapshot.getValue(Users.class);
                if (notify)
                    sendNotification(hisUid,users.getName(),message);

                notify = false;
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
            }
        });
        DatabaseReference chatRef1 = FirebaseDatabase.getInstance().getReference(CHATLIST)
                .child(myUid).child(hisUid);
        chatRef1.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (!dataSnapshot.exists()){
                    chatRef1.child("id").setValue(hisUid);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
        DatabaseReference chatRef2 = FirebaseDatabase.getInstance().getReference(CHATLIST)
                .child(hisUid).child(myUid);
        chatRef2.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (!dataSnapshot.exists()){
                    chatRef2.child("id").setValue(myUid);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void sendNotification(String hisUid, String name, String message) {
        DatabaseReference allTokens = FirebaseDatabase.getInstance().getReference(TOKENS);
        Query query = allTokens.orderByKey().equalTo(hisUid);
        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot ds : dataSnapshot.getChildren()){
                    Token token = ds.getValue(Token.class);
                    Data data = new Data(
                            ""+myUid,
                            ""+name+":"+message,
                            "New Message",
                            ""+hisUid,
                            R.mipmap.ic_launcher,"ChatNotification");
                    Sender sender = new Sender(data,token.getToken());
                    apiService.sendNotification(sender).enqueue(new Callback<Response>() {
                        @Override
                        public void onResponse(@NonNull Call<Response> call,@NonNull retrofit2.Response<Response> response) {
                        }

                        @Override
                        public void onFailure(@NonNull Call<Response> call,@NonNull Throwable t) {
                        }
                    });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void checkOnlineStatus(String status){
        DatabaseReference dpRef = FirebaseDatabase.getInstance().getReference(USERS).child(myUid);
        HashMap<String, Object> hashMap = new HashMap<>();
        hashMap.put(ONLINE_STATUS,status);
        dpRef.updateChildren(hashMap);
    }

    private void checkTypingStatus(String typing){
        DatabaseReference dpRef = FirebaseDatabase.getInstance().getReference(USERS).child(myUid);
        HashMap<String, Object> hashMap = new HashMap<>();
        hashMap.put(TYPING_TO,typing);
        dpRef.updateChildren(hashMap);
    }
    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return super.onSupportNavigateUp();
    }

    @Override
    protected void onStart() {
        checkOnlineStatus("online");
        super.onStart();
    }

    @Override
    protected void onPause() {
        super.onPause();
        String timestamp = String.valueOf(System.currentTimeMillis());
        checkOnlineStatus(timestamp);
        checkTypingStatus("noOne");
        userRefForSeen.removeEventListener(seenListener);
    }

    @Override
    protected void onResume() {
        checkOnlineStatus("online");
        super.onResume();
    }

    private void showImagePickDialog() {
        String[] options = {"Camera","Gallery"};
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Choose Image From");
        builder.setItems(options, (dialog, which) -> {
            if (which == 0){
                if (!checkCameraPermission())
                    requestCameraPermission();
                else
                    pickFromCamera();
            }else if (which == 1){
                if (!checkStoragePermission())
                    requestStoragePermission();
                else
                    pickFromGallery();
            }
        });
        builder.show();
    }

    private void pickFromGallery() {
        Intent galleryIntent = new Intent(Intent.ACTION_PICK);
        galleryIntent.setType("image/*");
        startActivityForResult(galleryIntent, IMAGE_GALLERY_CODE);
    }

    private void pickFromCamera() {
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.TITLE, "Temp Pic");
        values.put(MediaStore.Images.Media.DESCRIPTION, "Temp Description");

        image_uri = this.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, image_uri);
        startActivityForResult(cameraIntent, IMAGE_CAMERA_CODE);
    }

    private boolean checkStoragePermission(){
        return ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == (PackageManager.PERMISSION_GRANTED);
    }

    private void requestStoragePermission(){
        requestPermissions( storagePermission, STORAGE_REQUEST_CODE);
    }

    private boolean checkCameraPermission(){
        boolean result = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == (PackageManager.PERMISSION_GRANTED);

        boolean result1;
        result1 = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == (PackageManager.PERMISSION_GRANTED);

        return result && result1;
    }
    private void requestCameraPermission(){
        requestPermissions(cameraPermission, CAMERA_REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        switch (requestCode){
            case CAMERA_REQUEST_CODE:{
                if (grantResults.length > 0){
                    boolean camera = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                    boolean writeStorage = grantResults[1] == PackageManager.PERMISSION_GRANTED;
                    if (camera && writeStorage){
                        pickFromCamera();
                    }else {
                        Toast.makeText(this, "Please allow access to Camera",  Toast.LENGTH_SHORT).show();
                    }
                }
            }
            break;
            case STORAGE_REQUEST_CODE:{
                if (grantResults.length > 0){
                    boolean writeStorage = grantResults[1] == PackageManager.PERMISSION_GRANTED;
                    if (writeStorage){
                        pickFromGallery();
                    }else {
                        Toast.makeText(this, "Please allow access to Gallery",  Toast.LENGTH_SHORT).show();
                    }
                }

            }
            break;
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK){
            if (requestCode == IMAGE_GALLERY_CODE){
                image_uri = data.getData();
                sendImageMessage(image_uri);
            }
            if (requestCode == IMAGE_CAMERA_CODE){
                sendImageMessage(image_uri);
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void sendImageMessage(Uri image_uri) {
        notify = true;
        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Sending image...");
        progressDialog.show();

        String timestamp = "" + System.currentTimeMillis();
        String fileNameAndPath = "ChatImage/"+"post_"+timestamp;
        try {
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(),image_uri);
            ByteArrayOutputStream boas = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.PNG,100,boas);
            byte[] data = boas.toByteArray();
            StorageReference ref = FirebaseStorage.getInstance().getReference().child(fileNameAndPath);
            ref.putBytes(data).addOnSuccessListener(taskSnapshot -> {
                Task<Uri> uriTask = taskSnapshot.getStorage().getDownloadUrl();
                while (!uriTask.isSuccessful());
                String downloadUri = uriTask.getResult().toString();
                if (uriTask.isSuccessful()){
                    DatabaseReference reference = FirebaseDatabase.getInstance().getReference();
                    HashMap<String, Object> hashMap = new HashMap<>();
                    hashMap.put("sender",myUid);
                    hashMap.put("receiver",hisUid);
                    hashMap.put("message",downloadUri);
                    hashMap.put("timestamp",timestamp);
                    hashMap.put("type","image");
                    hashMap.put("isSeen",false);
                    reference.child(CHATS).push().setValue(hashMap);
                    DatabaseReference database = FirebaseDatabase.getInstance().getReference(USERS).child(myUid);
                    database.addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            Users users = dataSnapshot.getValue(Users.class);
                            if (notify)
                                sendNotification(hisUid,users.getName(),"Send you a photo....");
                            notify = false;
                            progressDialog.dismiss();
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {
                            progressDialog.dismiss();
                        }
                    });
                    DatabaseReference chatRef1 = FirebaseDatabase.getInstance().getReference(CHATLIST)
                            .child(myUid).child(hisUid);
                    chatRef1.addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            if (!dataSnapshot.exists()){
                                chatRef1.child("id").setValue(hisUid);
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {

                        }
                    });
                    DatabaseReference chatRef2 = FirebaseDatabase.getInstance().getReference(CHATLIST)
                            .child(hisUid).child(myUid);
                    chatRef2.addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            if (!dataSnapshot.exists()){
                                chatRef2.child("id").setValue(myUid);
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {

                        }
                    });
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private void checkIsBlocked() {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference(USERS);
        ref.child(myUid).child(BLOCKED_USERS).orderByChild("uid").equalTo(hisUid)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        for (DataSnapshot ds : dataSnapshot.getChildren()){
                            if (ds.exists()){
                                blockIv.setImageResource(R.drawable.ic_block_black_24dp);
                                isBlocked = true;
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
    }

    private void unBlockUser() {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference(USERS);
        ref.child(myUid).child(BLOCKED_USERS).orderByChild("uid").equalTo(hisUid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot ds : dataSnapshot.getChildren()){
                    if (ds.exists()){
                        ds.getRef().removeValue().addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                blockIv.setImageResource(R.drawable.ic_check_circle_black_24dp);
                                Toast.makeText(getApplicationContext(), "Unblocked Successfully", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void blockUser() {
        HashMap<String, Object> hashMap = new HashMap<>();
        hashMap.put("uid",hisUid);
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference(USERS);
        ref.child(myUid).child(BLOCKED_USERS).child(hisUid).setValue(hashMap).addOnSuccessListener(aVoid -> {
            blockIv.setImageResource(R.drawable.ic_block_black_24dp);
            Toast.makeText(getApplicationContext(), "Blocked Successfully", Toast.LENGTH_SHORT).show();
        });
    }
}
