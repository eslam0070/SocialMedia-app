package com.eso.socialmedia.ui.fragment;

import android.Manifest;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.eso.socialmedia.R;
import com.eso.socialmedia.adapter.PostAdapter;
import com.eso.socialmedia.model.Post;
import com.eso.socialmedia.ui.activity.SettingsActivity;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static android.app.Activity.RESULT_OK;
import static com.eso.socialmedia.utils.Common.COMMENTS;
import static com.eso.socialmedia.utils.Common.COVER;
import static com.eso.socialmedia.utils.Common.EMAIL;
import static com.eso.socialmedia.utils.Common.IMAGE;
import static com.eso.socialmedia.utils.Common.NAME;
import static com.eso.socialmedia.utils.Common.PHONE;
import static com.eso.socialmedia.utils.Common.POSTS;
import static com.eso.socialmedia.utils.Common.UID;
import static com.eso.socialmedia.utils.Common.USERS;

/**
 * A simple {@link Fragment} subclass.
 */
public class ProfileFragment extends Fragment {

    private FirebaseUser user;
    private CircularImageView mAvatarIv;
    private ImageView image_header;
    private TextView mNameTv;
    private ProgressDialog pd;

    private DatabaseReference reference;
    private StorageReference storageReference;

    private NavController navController;
    private FirebaseAuth mAuth;
    private static final int CAMERA_REQUEST_CODE = 100;
    private static final int STORAGE_REQUEST_CODE = 200;
    private static final int IMAGE_GALLERY_CODE = 300;
    private static final int IMAGE_CAMERA_CODE = 400;

    private String[] cameraPermission, storagePermission;
    private Uri image_uri = null;
    private String storagePath;
    private RecyclerView postsRecyclerView;
    private List<Post> postList = new ArrayList<>();
    private PostAdapter postAdapter;
    private String uid;
    private String ProfileOrCoverPhoto;

    public ProfileFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setHasOptionsMenu(true);
        navController = Navigation.findNavController(view);
        mAuth = FirebaseAuth.getInstance();
        uid = mAuth.getCurrentUser().getUid();
        Toolbar toolbar = view.findViewById(R.id.toolbar);
        ((AppCompatActivity) getActivity()).setSupportActionBar(toolbar);
        if (((AppCompatActivity) getActivity()).getSupportActionBar() != null) {
            ((AppCompatActivity) getActivity()).getSupportActionBar().setTitle("");
        }
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        user = mAuth.getCurrentUser();
        storagePath = "Users_Profile_Cover_Imgs/";
        storageReference = FirebaseStorage.getInstance().getReference();
        cameraPermission = new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE};
        storagePermission = new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE};
        pd = new ProgressDialog(getContext());
        mAvatarIv = view.findViewById(R.id.image);
        mNameTv = view.findViewById(R.id.nameTv);
        image_header = view.findViewById(R.id.image_header);
        reference = FirebaseDatabase.getInstance().getReference().child(USERS);
        Query query = reference.orderByChild(EMAIL).equalTo(user.getEmail());
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
                    if (isAdded()) {
                        Glide.with(getContext()).load(image).placeholder(R.drawable.profile).into(mAvatarIv);
                        Glide.with(getContext()).load(cover).into(image_header);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
            }
        });
        FloatingActionButton mFab = view.findViewById(R.id.fab);
        mFab.setOnClickListener(v -> showEditProfile());
        postsRecyclerView = view.findViewById(R.id.recyclerView);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getContext());
        linearLayoutManager.setStackFromEnd(true);
        linearLayoutManager.setReverseLayout(true);
        postsRecyclerView.setLayoutManager(linearLayoutManager);
        loadMyPosts();
    }

    private void loadMyPosts() {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference(POSTS);
        Query query = ref.orderByChild(UID).equalTo(uid);
        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot ds : dataSnapshot.getChildren()) {
                    Post post = ds.getValue(Post.class);
                    postList.add(post);
                    postAdapter = new PostAdapter(getContext(), postList);
                    postsRecyclerView.setAdapter(postAdapter);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void searchMyPosts(String searchQuery) {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference(POSTS);
        Query query = ref.orderByChild(UID).equalTo(uid);
        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot ds : dataSnapshot.getChildren()) {
                    Post post = ds.getValue(Post.class);
                    if (post.getpDescription().toLowerCase().contains(searchQuery.toLowerCase()) ||
                            post.getpDescription().toLowerCase().contains(searchQuery.toLowerCase())) {
                        postList.add(post);
                    }
                    postAdapter = new PostAdapter(getContext(), postList);
                    postsRecyclerView.setAdapter(postAdapter);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void showEditProfile() {
        String[] options = {"Edit Photo Profile", "Edit Photo Cover", "Edit Name"};
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Select One Action");
        builder.setItems(options, (dialog, i) -> {
            if (i == 0) {
                pd.setMessage("Updating Photo Profile");
                ProfileOrCoverPhoto = "image";
                showImagePicDialog();
            } else if (i == 1) {
                pd.setMessage("Updating Photo Cover");
                ProfileOrCoverPhoto = "cover";
                showImagePicDialog();
            } else if (i == 2) {
                pd.setMessage("Updating Name Profile");
                showNomeUpdateDialog("name");
            }
        });
        builder.create().show();
    }

    private void showNomeUpdateDialog(final String key) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Update " + key);
        LinearLayout linearLayout = new LinearLayout(getActivity());
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        linearLayout.setPadding(10, 10, 10, 10);
        final EditText editText = new EditText(getActivity());
        editText.setHint("Enter your " + key);
        linearLayout.addView(editText);
        builder.setView(linearLayout);
        builder.setPositiveButton("Update", (dialog, i) -> {
            String value = editText.getText().toString().trim();
            if (!TextUtils.isEmpty(value)) {
                pd.show();
                HashMap<String, Object> result = new HashMap<>();
                result.put(key, value);
                reference.child(user.getUid()).updateChildren(result).addOnSuccessListener(aVoid -> {
                    pd.dismiss();
                    Toast.makeText(getActivity(), "Updated...", Toast.LENGTH_SHORT).show();
                }).addOnFailureListener(e -> {
                    pd.dismiss();
                    Toast.makeText(getActivity(), "" + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
                if (key.equals(NAME)) {
                    DatabaseReference ref = FirebaseDatabase.getInstance().getReference(POSTS);
                    Query query = ref.orderByChild(UID).equalTo(uid);
                    query.addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            for (DataSnapshot ds : dataSnapshot.getChildren()) {
                                String child = ds.getKey();
                                dataSnapshot.getRef().child(child).child("uName").setValue(value);
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {

                        }
                    });
                    ref.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            for (DataSnapshot ds : dataSnapshot.getChildren()) {
                                String child = ds.getKey();
                                if (dataSnapshot.child(child).hasChild(COMMENTS)) {
                                    String child1 = dataSnapshot.child(child).getKey();
                                    Query child2 = FirebaseDatabase.getInstance().getReference(POSTS).child(child1).child(COMMENTS).orderByChild(UID).equalTo(uid);
                                    child2.addValueEventListener(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                            for(DataSnapshot ds : dataSnapshot.getChildren()){
                                                String child = ds.getKey();
                                                dataSnapshot.getRef().child(child).child("uName").setValue(value);
                                            }
                                        }

                                        @Override
                                        public void onCancelled(@NonNull DatabaseError databaseError) {

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
            }
        });
        builder.setNegativeButton("Cancel", (dialogInterface, i) -> {
            dialogInterface.dismiss();
            pd.dismiss();
        });
        builder.show();
    }

    private void showImagePicDialog() {
        String[] options = {"Camera", "Gallery"};

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Select");
        builder.setItems(options, (dialogInterface, i) -> {
            if (i == 0) {
                if (!checkCameraPermission()) {
                    requestCameraPermission();
                } else {
                    takeFotoCamera();
                }
            } else if (i == 1) {
                if (!checkStoragePermission()) {
                    requestStoragePermission();
                } else {
                    getPhotoGallery();
                }
            }
        });

        builder.create().show();
    }

    private void getPhotoGallery() {
        Intent galleryIntent = new Intent(Intent.ACTION_PICK);
        galleryIntent.setType("image/*");
        startActivityForResult(galleryIntent, IMAGE_GALLERY_CODE);
    }

    private void takeFotoCamera() {
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.TITLE, "Temp Pic");
        values.put(MediaStore.Images.Media.DESCRIPTION, "Temp Description");

        image_uri = getActivity().getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, image_uri);
        startActivityForResult(cameraIntent, IMAGE_CAMERA_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        switch (requestCode) {
            case CAMERA_REQUEST_CODE: {
                if (grantResults.length > 0) {
                    boolean camera = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                    boolean writeStorage = grantResults[1] == PackageManager.PERMISSION_GRANTED;
                    if (camera && writeStorage) {
                        takeFotoCamera();
                    } else {
                        Toast.makeText(getActivity(), "Please allow access to Camera", Toast.LENGTH_SHORT).show();
                    }
                }
            }
            break;
            case STORAGE_REQUEST_CODE: {
                if (grantResults.length > 0) {
                    boolean writeStorage = grantResults[1] == PackageManager.PERMISSION_GRANTED;
                    if (writeStorage) {
                        getPhotoGallery();
                    } else {
                        Toast.makeText(getActivity(), "Please allow access to Gallery", Toast.LENGTH_SHORT).show();
                    }
                }

            }
            break;
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            if (requestCode == IMAGE_GALLERY_CODE) {
                image_uri = data.getData();
                uploadPhotoProfileCover(image_uri);
            }
            if (requestCode == IMAGE_CAMERA_CODE) {
                uploadPhotoProfileCover(image_uri);
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void uploadPhotoProfileCover(final Uri uri) {
        String filePathAndName = storagePath + "" + ProfileOrCoverPhoto + "_" + user.getUid();
        StorageReference storageReference1 = storageReference.child(filePathAndName);
        storageReference1.putFile(uri)
                .addOnSuccessListener(taskSnapshot -> {
                    Task<Uri> uriTask = taskSnapshot.getStorage().getDownloadUrl();
                    while (!uriTask.isSuccessful()) ;
                    Uri downloadUri = uriTask.getResult();
                    if (uriTask.isSuccessful()) {
                        HashMap<String, Object> result = new HashMap<>();
                        result.put(ProfileOrCoverPhoto, downloadUri.toString());
                        reference.child(user.getUid()).updateChildren(result)
                                .addOnSuccessListener(aVoid -> {
                                    pd.dismiss();
                                    Toast.makeText(getContext(), "Updated Image...", Toast.LENGTH_SHORT).show();
                                }).addOnFailureListener(e -> {
                            pd.dismiss();
                            Toast.makeText(getContext(), "Error updating image...", Toast.LENGTH_SHORT).show();
                        });
                        if (ProfileOrCoverPhoto.equals(NAME)) {
                            DatabaseReference ref = FirebaseDatabase.getInstance().getReference(POSTS);
                            Query query = ref.orderByChild(UID).equalTo(uid);
                            query.addValueEventListener(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                    for (DataSnapshot ds : dataSnapshot.getChildren()) {
                                        String child = ds.getKey();
                                        dataSnapshot.getRef().child(child).child("uImage").setValue(downloadUri.toString());
                                    }
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError databaseError) {

                                }
                            });
                            ref.addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                    for (DataSnapshot ds : dataSnapshot.getChildren()) {
                                        String child = ds.getKey();
                                        if (dataSnapshot.child(child).hasChild(COMMENTS)) {
                                            String child1 = dataSnapshot.child(child).getKey();
                                            Query child2 = FirebaseDatabase.getInstance().getReference(POSTS).child(child1).child(COMMENTS).orderByChild(UID).equalTo(uid);
                                            child2.addValueEventListener(new ValueEventListener() {
                                                @Override
                                                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                                    for(DataSnapshot ds : dataSnapshot.getChildren()){
                                                        String child = ds.getKey();
                                                        dataSnapshot.getRef().child(child).child("uDp").setValue(downloadUri.toString());
                                                    }
                                                }

                                                @Override
                                                public void onCancelled(@NonNull DatabaseError databaseError) {

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
                    }
                }).addOnFailureListener(e -> {
            pd.dismiss();
            Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }

    private boolean checkStoragePermission() {
        return ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == (PackageManager.PERMISSION_GRANTED);
    }

    private void requestStoragePermission() {
        requestPermissions(storagePermission, STORAGE_REQUEST_CODE);
    }

    private boolean checkCameraPermission() {
        boolean result = ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.CAMERA)
                == (PackageManager.PERMISSION_GRANTED);

        boolean result1;
        result1 = ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == (PackageManager.PERMISSION_GRANTED);

        return result && result1;
    }

    private void requestCameraPermission() {
        requestPermissions(cameraPermission, CAMERA_REQUEST_CODE);
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.menu, menu);
        MenuItem item = menu.findItem(R.id.action_search);
        SearchView searchView = (SearchView) item.getActionView();
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                if (!TextUtils.isEmpty(query.trim()))
                    searchMyPosts(query);
                else
                    loadMyPosts();
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (!TextUtils.isEmpty(newText.trim()))
                    searchMyPosts(newText);
                else
                    loadMyPosts();
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
        }else if (item.getItemId() == R.id.action_setting) {
            getActivity().startActivity(new Intent(getContext(), SettingsActivity.class));
        }
        return true;
    }
}


