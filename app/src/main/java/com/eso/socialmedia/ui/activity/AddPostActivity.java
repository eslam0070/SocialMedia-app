package com.eso.socialmedia.ui.activity;

import android.Manifest;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

import com.android.volley.AuthFailureError;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.bumptech.glide.Glide;
import com.eso.socialmedia.R;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
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
import com.google.firebase.storage.UploadTask;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;

import static com.eso.socialmedia.utils.Common.EMAIL;
import static com.eso.socialmedia.utils.Common.IMAGE;
import static com.eso.socialmedia.utils.Common.NAME;
import static com.eso.socialmedia.utils.Common.POSTS;
import static com.eso.socialmedia.utils.Common.USERS;

public class AddPostActivity extends AppCompatActivity {

    FirebaseAuth mAuth;
    DatabaseReference userDbRef;
    StorageReference storageReference;
    Toolbar toolbar;
    EditText mPDescriptionEt;
    ImageView mPImageIv;
    Button mPUploadBtn;
    static final int CAMERA_REQUEST_CODE = 100;
    static final int STORAGE_REQUEST_CODE = 200;
    static  final int IMAGE_GALLERY_CODE = 300;
    static  final int IMAGE_CAMERA_CODE = 400;
    String[] cameraPermission,storagePermission;
    Uri image_uri = null;
    String name,email,dp,uid,editDescription,editImage;

    ProgressDialog progressDialog;
    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_post);
        progressDialog = new ProgressDialog(this);
        toolbar = findViewById(R.id.toolbar_add_post);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Add New Post");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setSubtitle(email);
        }
        mPDescriptionEt = findViewById(R.id.pDescriptionEt);
        mPImageIv = findViewById(R.id.pImageIv);
        mPImageIv.setOnClickListener(v -> showImagePickDialog());
        mPUploadBtn = findViewById(R.id.pUploadBtn);
        Intent intent = getIntent();
        String action = intent.getAction();
        String type = intent.getType();
        if(Intent.ACTION_SEND.equals(action) && type != null){
            if ("text/plain".equals(type))
                handleSendText(intent);
            else if (type.startsWith("image"))
                handleSendImage(intent);
        }
        String isUpdateKey = "" +intent.getStringExtra("key");
        String editPostId = "" +intent.getStringExtra("editPostId");
        if (isUpdateKey.equals("editPost")){
            getSupportActionBar().setTitle("Update Post");
            mPUploadBtn.setText("Update");
            loadPostData(editPostId);
        }else {
            getSupportActionBar().setTitle("Add New Post");
            mPUploadBtn.setText("Update");
        }
        mAuth = FirebaseAuth.getInstance();
        uid = mAuth.getCurrentUser().getUid();

        checkUserStatus();

        userDbRef = FirebaseDatabase.getInstance().getReference(USERS);
        Query query = userDbRef.orderByChild("email").equalTo(email);
        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot ds : dataSnapshot.getChildren()){
                    name = ds.child(NAME).getValue(String.class);
                    email = ds.child(EMAIL).getValue(String.class);
                    dp = ds.child(IMAGE).getValue(String.class);

                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
        storageReference = FirebaseStorage.getInstance().getReference();
        cameraPermission = new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE};
        storagePermission = new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE};

        mPUploadBtn.setOnClickListener(v -> {
            String description = mPDescriptionEt.getText().toString().trim();
            if (isUpdateKey.equals("editPost"))
                beginUpdate(description,editPostId);
            else
                uploadData(description);


        });
    }

    private void handleSendText(Intent intent) {
        String sharedText = intent.getStringExtra(Intent.EXTRA_TEXT);
        if (sharedText != null)
            mPDescriptionEt.setText(sharedText);
    }

    private void handleSendImage(Intent intent) {
        Uri imageUrl = intent.getParcelableExtra(Intent.EXTRA_STREAM);
        if (imageUrl != null){
            image_uri= imageUrl;
            mPImageIv.setImageURI(image_uri);
        }
    }

    private void beginUpdate(String description, String editPostId) {
        progressDialog.setMessage("Updating Post...");
        progressDialog.show();
        if (!editImage.equals("noImage"))
            updateWasWithImage(description,editPostId);
        else if (mPImageIv.getDrawable() != null)
            updateWithNowImage(description,editPostId);
        else
            updateWithoutImage(description,editPostId);
    }

    private void updateWithoutImage(String description, String editPostId) {
        HashMap<String, Object> hashMap = new HashMap<>();
        hashMap.put("uid",uid);
        hashMap.put("uName",name);
        hashMap.put("uEmail",email);
        hashMap.put("uDp",dp);
        hashMap.put("pDescription",description);
        hashMap.put("pImage","noImage");
        DatabaseReference ref1 = FirebaseDatabase.getInstance().getReference(POSTS);
        ref1.child(editPostId).updateChildren(hashMap).addOnSuccessListener(aVoid1 -> {
            progressDialog.dismiss();
            Toast.makeText(this, "Updated....", Toast.LENGTH_SHORT).show();
        });
    }

    private void updateWithNowImage(String description, String editPostId) {
        String timeStamp = String.valueOf(System.currentTimeMillis());
        String filePathAndName = "Posts/" + "post_"+timeStamp;
        Bitmap bitmap = ((BitmapDrawable)mPImageIv.getDrawable()).getBitmap();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG,100,baos);
        byte[] data = baos.toByteArray();
        StorageReference ref = FirebaseStorage.getInstance().getReference().child(filePathAndName);
        ref.putBytes(data).addOnSuccessListener(taskSnapshot -> {
            Task<Uri> uriTask = taskSnapshot.getStorage().getDownloadUrl();
            while (!uriTask.isSuccessful());
            String downloadUrl = uriTask.getResult().toString();
            if (uriTask.isSuccessful()){
                HashMap<String, Object> hashMap = new HashMap<>();
                hashMap.put("uid",uid);
                hashMap.put("uName",name);
                hashMap.put("uEmail",email);
                hashMap.put("uDp",dp);
                hashMap.put("pDescription",description);
                hashMap.put("pImage",downloadUrl);
                DatabaseReference ref1 = FirebaseDatabase.getInstance().getReference(POSTS);
                ref1.child(editPostId).updateChildren(hashMap).addOnSuccessListener(aVoid1 -> {
                    progressDialog.dismiss();
                    Toast.makeText(this, "Updated....", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void updateWasWithImage(String description, String editPostId) {
        StorageReference mPictureRef = FirebaseStorage.getInstance().getReferenceFromUrl(editImage);
        mPictureRef.delete().addOnSuccessListener(aVoid -> {
            String timeStamp = String.valueOf(System.currentTimeMillis());
            String filePathAndName = "Posts/" + "post_"+timeStamp;
            Bitmap bitmap = ((BitmapDrawable)mPImageIv.getDrawable()).getBitmap();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.PNG,100,baos);
            byte[] data = baos.toByteArray();
            StorageReference ref = FirebaseStorage.getInstance().getReference().child(filePathAndName);
            ref.putBytes(data).addOnSuccessListener(taskSnapshot -> {
                Task<Uri> uriTask = taskSnapshot.getStorage().getDownloadUrl();
                while (!uriTask.isSuccessful());
                String downloadUrl = uriTask.getResult().toString();
                if (uriTask.isSuccessful()){
                    HashMap<String, Object> hashMap = new HashMap<>();
                    hashMap.put("uid",uid);
                    hashMap.put("uName",name);
                    hashMap.put("uEmail",email);
                    hashMap.put("uDp",dp);
                    hashMap.put("pDescription",description);
                    hashMap.put("pImage",downloadUrl);
                    DatabaseReference ref1 = FirebaseDatabase.getInstance().getReference(POSTS);
                    ref1.child(editPostId).updateChildren(hashMap).addOnSuccessListener(aVoid1 -> {
                        progressDialog.dismiss();
                        Toast.makeText(this, "Updated....", Toast.LENGTH_SHORT).show();
                    });
                }
            });
        });
    }

    private void loadPostData(String editPostId) {
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference(POSTS);
        Query query = reference.orderByChild("pId").equalTo(editPostId);
        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot ds : dataSnapshot.getChildren()){
                    editDescription = ds.child("pDescription").getValue(String.class);
                    editImage = ds.child("pImage").getValue(String.class);
                    mPDescriptionEt.setText(editDescription);
                    if (!editImage.equals("noImage")){
                        Glide.with(AddPostActivity.this).load(editImage).into(mPImageIv);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void uploadData(String description) {
        progressDialog.setMessage("Publishing Post...");
        progressDialog.show();
        String timeStamp = String.valueOf(System.currentTimeMillis());
        String filePathAndName = "Posts/" + "post_" + timeStamp;

        if (mPImageIv.getDrawable() != null){
            Bitmap bitmap = ((BitmapDrawable)mPImageIv.getDrawable()).getBitmap();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.PNG,100,baos);
            byte[] data = baos.toByteArray();
            StorageReference ref = FirebaseStorage.getInstance().getReference().child(filePathAndName);
            ref.putBytes(data).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                    Task<Uri> uriTask = taskSnapshot.getStorage().getDownloadUrl();
                    while (!uriTask.isSuccessful());
                    String downloadUri = uriTask.getResult().toString();
                    if (uriTask.isSuccessful()){
                        HashMap<String, Object> hashMap = new HashMap<>();
                        hashMap.put("uid",uid);
                        hashMap.put("uName",name);
                        hashMap.put("uEmail",email);
                        hashMap.put("uDp",dp);
                        hashMap.put("pId",timeStamp);
                        hashMap.put("pDescription",description);
                        hashMap.put("pImage",downloadUri);
                        hashMap.put("pTime",timeStamp);
                        hashMap.put("pLikes","0");
                        hashMap.put("pComments","0");

                        DatabaseReference ref = FirebaseDatabase.getInstance().getReference(POSTS);
                        ref.child(timeStamp).setValue(hashMap).addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                progressDialog.dismiss();
                                Toast.makeText(AddPostActivity.this, "Done..", Toast.LENGTH_SHORT).show();
                                mPDescriptionEt.setText("");
                                mPImageIv.setImageResource(R.drawable.add_btn);
                                image_uri = null;
                                prepareNotification(
                                        ""+timeStamp,
                                        ""+name,
                                        ""+"\n"+"Add New Post",
                                        "PostNotification",
                                        "POST");
                            }
                        });
                    }
                }
            });
        }else {
            HashMap<String, Object> hashMap = new HashMap<>();
            hashMap.put("uid",uid);
            hashMap.put("uName",name);
            hashMap.put("uEmail",email);
            hashMap.put("uDp",dp);
            hashMap.put("pId",timeStamp);
            hashMap.put("pDescription",description);
            hashMap.put("pImage","noImage");
            hashMap.put("pTime",timeStamp);
            hashMap.put("pLikes","0");
            hashMap.put("pComments","0");
            DatabaseReference ref = FirebaseDatabase.getInstance().getReference(POSTS);
            ref.child(timeStamp).setValue(hashMap).addOnSuccessListener(new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void aVoid) {
                    progressDialog.dismiss();
                    Toast.makeText(AddPostActivity.this, "Done..", Toast.LENGTH_SHORT).show();
                    mPDescriptionEt.setText("");
                    mPImageIv.setImageResource(R.drawable.add_btn);
                    image_uri = null;
                    prepareNotification(
                            ""+timeStamp,
                            ""+name,
                            ""+"\n"+"Add New Post",
                            "PostNotification",
                            "POST");
                }
            });
        }
    }

    private void prepareNotification(String pId,String name,String description,String notificationType,String notificationTopic){
        String NOTIFICATION_TOPIC = "/topics/" + notificationTopic;
        String NOTIFICATION_NAME = name;
        String NOTIFICATION_DESCRIPTION = description;
        String NOTIFICATION_TYPE = notificationType;

        JSONObject notificationJo = new JSONObject();
        JSONObject notificationBodyJo = new JSONObject();
        try {
            notificationBodyJo.put("notificationType",NOTIFICATION_TYPE);
            notificationBodyJo.put("sender",uid);
            notificationBodyJo.put("pId",pId);
            notificationBodyJo.put("pName",NOTIFICATION_NAME);
            notificationBodyJo.put("pDescription",NOTIFICATION_DESCRIPTION);
            notificationJo.put("to",NOTIFICATION_TOPIC);
            notificationJo.put("data",notificationBodyJo);
        }catch (JSONException e){
            Toast.makeText(this, e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
        }
        sendPostNotification(notificationJo);
    }

    private void sendPostNotification(JSONObject notificationJo) {
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest("https://fcm.googleapis.com/fcm/send", notificationJo, response -> Log.d("FCM_RESPONSE","onResponse "+ response.toString()), new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Toast.makeText(AddPostActivity.this, ""+error.toString(), Toast.LENGTH_SHORT).show();
            }
        }){
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("Content-Type","application/json");
                headers.put("Authorization","key=AAAAMVNs_pQ:APA91bGEU4Dadr9NR0RVq9QqKW-NBvr5lJ9JR6EkMH8jaz_uXusNxCaYQyrRVDMalJWHl8FLylv6XPS4uOsIGj-FOqlFHHK839GcgUnqEHTTJ0Mc6oHU9VJp7sOxDKq0ZWVbT_am-2fa");
                return headers;
            }
        };
        Volley.newRequestQueue(this).add(jsonObjectRequest);
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
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

    @RequiresApi(api = Build.VERSION_CODES.M)
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
    @RequiresApi(api = Build.VERSION_CODES.M)
    private void requestCameraPermission(){
        requestPermissions(cameraPermission, CAMERA_REQUEST_CODE);
    }
    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return super.onSupportNavigateUp();
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
                mPImageIv.setImageURI(image_uri);
            }
            if (requestCode == IMAGE_CAMERA_CODE){
                mPImageIv.setImageURI(image_uri);
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onStart() {
        checkUserStatus();
        super.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkUserStatus();

    }

    private void checkUserStatus() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            email = user.getEmail();
            uid = user.getUid();
        } else {
            startActivity(new Intent(this, MainActivity.class));
            finish();
        }
    }
}
