package com.eso.socialmedia.adapter;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.text.format.DateFormat;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.eso.socialmedia.ui.activity.PostDetailsActivity;
import com.eso.socialmedia.R;
import com.eso.socialmedia.model.Post;
import com.eso.socialmedia.ui.activity.AddPostActivity;
import com.eso.socialmedia.ui.activity.PostLikedByActivity;
import com.eso.socialmedia.ui.activity.ThereProfileActivity;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
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
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import static com.eso.socialmedia.utils.Common.LIKES;
import static com.eso.socialmedia.utils.Common.NOTIFICATION;
import static com.eso.socialmedia.utils.Common.POSTS;
import static com.eso.socialmedia.utils.Common.UID;
import static com.eso.socialmedia.utils.Common.USERS;


public class PostAdapter extends RecyclerView.Adapter<PostAdapter.ViewHolder> {

    private List<Post> postList;
    private Context context;
    private String myUid;
    private DatabaseReference likesRef; //for likes database node
    private DatabaseReference postsRef; //reference of posts
    private boolean mProcessLike = false;
    public PostAdapter(Context context, List<Post> postList) {
        this.context = context;
        this.postList = postList;
        myUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        likesRef = FirebaseDatabase.getInstance().getReference().child(LIKES);
        postsRef = FirebaseDatabase.getInstance().getReference().child(POSTS);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.row_posts, parent, false));

    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int i) {
        Post post = postList.get(i);
        final String uid = post.getUid();
        String uName = postList.get(i).getuName();
        String uDp = post.getuDp();
        final String pId = post.getpId();
        String pDescription = post.getpDescription();
        final String pImage = post.getpImage();
        String pTimeStamp = post.getpTime();
        String pLikes = post.getpLikes();
        String pComments = post.getpComments();
        holder.mUNameTv.setText(uName);

        Calendar calendar = Calendar.getInstance(Locale.getDefault());
        calendar.setTimeInMillis(Long.parseLong(pTimeStamp));
        String time = DateFormat.format("hh:mm aa", calendar).toString();
        holder.mPTimeTv.setText(time);
        holder.mPDescription.setText(pDescription);
        holder.pLikesTv.setText(pLikes +" Likes"); //e.g. 100 Likes\
        holder.pCommentsTv.setText(pComments + " Comments");
        setLikes(holder,pId);
        Glide.with(context).load(uDp).placeholder(R.drawable.profile).into(holder.mUPictureIv);
        if (pImage.equals("noImage")) {
            holder.mPImageIv.setVisibility(View.GONE);
        } else {
            holder.mPImageIv.setVisibility(View.VISIBLE);
            Glide.with(context).load(pImage).into(holder.mPImageIv);
        }

        holder.mMoreBtn.setOnClickListener(v -> {
            showMoreOptions(holder.mMoreBtn,uid,myUid,pId,pImage);
        });

        holder.likeBtn.setOnClickListener(v -> {
            final int pLikess = Integer.parseInt(postList.get(i).getpLikes());
            mProcessLike = true;
            final String postIde = postList.get(i).getpId();
            likesRef.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    if (mProcessLike){
                        if (dataSnapshot.child(postIde).hasChild(myUid)){
                            postsRef.child(postIde).child("pLikes").setValue("" + (pLikess-1));
                            likesRef.child(postIde).child(myUid).removeValue();
                            mProcessLike = false;
                        }else {
                            postsRef.child(postIde).child("pLikes").setValue("" + (pLikess+1));
                            likesRef.child(postIde).child(myUid).setValue("Liked");
                            mProcessLike = false;
                            addToHisNotification(""+uid,""+pId,"Liked your post");
                        }
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });
        });

        holder.mCommentBtn.setOnClickListener(v -> {
            Intent intent = new Intent(context, PostDetailsActivity.class);
            intent.putExtra("postId",pId);
            context.startActivity(intent);
        });

        holder.shareBtn.setOnClickListener(v -> {
            BitmapDrawable bitmapDrawable = (BitmapDrawable)holder.mPImageIv.getDrawable();
            if (bitmapDrawable == null)
                shareTextOnly(pDescription);
            else{
                Bitmap bitmap = bitmapDrawable.getBitmap();
                shareImageAndText(pDescription,bitmap);
            }
        });

        holder.mProfileLay.setOnClickListener(v -> {
            Intent intent = new Intent(context, ThereProfileActivity.class);
            intent.putExtra(UID,uid);
            context.startActivity(intent);
        });

        holder.pLikesTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                context.startActivity(new Intent(context, PostLikedByActivity.class).putExtra("postId",pId));
            }
        });

    }

    private void addToHisNotification(String hisUid,String pId, String notification){
        String timestamp = ""+ System.currentTimeMillis();
        HashMap<String,Object> hashMap = new HashMap<>();
        hashMap.put("pId", pId);
        hashMap.put("timestamp", timestamp);
        hashMap.put("pUid", hisUid);
        hashMap.put("notification", notification);
        hashMap.put("sUid", myUid);
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference(USERS);
        reference.child(hisUid).child(NOTIFICATION).child(timestamp).setValue(hashMap).
                addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {

            }
        });
    }

    private void shareImageAndText(String pDescription, Bitmap bitmap) {
        String shareBody = pDescription;
        Uri uri = saveImageToShare(bitmap);
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.putExtra(Intent.EXTRA_STREAM,uri);
        intent.putExtra(Intent.EXTRA_TEXT,shareBody);
        intent.putExtra(Intent.EXTRA_SUBJECT,"Subject Here");
        intent.setType("image/png");
        context.startActivity(Intent.createChooser(intent,"Share Via"));
    }

    private Uri saveImageToShare(Bitmap bitmap) {
        File imageFolder = new File(context.getCacheDir(),"images");
        Uri uri = null;
        try {
            imageFolder.mkdirs();
            File file = new File(imageFolder,"shared_image.png");
            FileOutputStream stream = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.PNG,90,stream);
            stream.flush();
            stream.close();
            uri = FileProvider.getUriForFile(context,"com.eso.socialmedia.fileprovider",file);
        }catch (Exception e){
            Toast.makeText(context, e.getMessage(), Toast.LENGTH_SHORT).show();
        }
        return uri;
    }

    private void shareTextOnly(String pDescription) {
        String shareBody = pDescription;
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.putExtra(Intent.EXTRA_SUBJECT,"Subject Here");
        intent.putExtra(Intent.EXTRA_TEXT,shareBody);
        context.startActivity(Intent.createChooser(intent,"Share Via"));
    }

    private void setLikes(ViewHolder holder, String postKey) {
        likesRef.addValueEventListener(new ValueEventListener() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.child(postKey).hasChild(myUid)){
                        holder.likeBtn.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_thumb_up,0,0,0);
                        holder.likeBtn .setText("Liked");
                }else {
                    holder.likeBtn.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_thumb_up_black,0,0,0);
                    holder.likeBtn.setText("Like");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void showMoreOptions(ImageButton mMoreBtn, String uid, String myUid, String pId, String pImage) {
        PopupMenu popupMenu = new PopupMenu(context, mMoreBtn, Gravity.END);
        if (uid.equals(myUid)){
            popupMenu.getMenu().add(Menu.NONE, 0, 0, "Edit");
            popupMenu.getMenu().add(Menu.NONE, 1, 0, "Delete");
        }
        popupMenu.getMenu().add(Menu.NONE,2,0,"View Post");
        popupMenu.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == 0){
                Intent intent = new Intent(context, AddPostActivity.class);
                intent.putExtra("key","editPost");
                intent.putExtra("editPostId",pId);
                context.startActivity(intent);
            }else if (id == 1){
                beginDelete(pId,pImage);
            }else if (id == 2){
                Intent intent = new Intent(context, PostDetailsActivity.class);
                intent.putExtra("postId",pId);
                context.startActivity(intent);
            }
            return false;
        });
        popupMenu.show();
    }

    private void beginDelete(String pId, String pImage) {
        if (pImage.equals("noImage"))
            deleteWithoutImage(pId);
        else
            deleteWithImage(pId,pImage);
    }

    private void deleteWithImage(String pId, String pImage) {
        ProgressDialog pd = new ProgressDialog(context);
        pd.setMessage("Deleting...");
        pd.show();
        StorageReference picRef = FirebaseStorage.getInstance().getReferenceFromUrl(pImage);
        picRef.delete().addOnSuccessListener(aVoid -> {
            Query query = postsRef.orderByChild("pId").equalTo(pId);
            query.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    for (DataSnapshot ds : dataSnapshot.getChildren()){
                        ds.getRef().removeValue();
                    }
                    Toast.makeText(context, "Deleted successfully", Toast.LENGTH_SHORT).show();
                    pd.dismiss();
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    Toast.makeText(context, databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private void deleteWithoutImage(String pId) {
        ProgressDialog pd = new ProgressDialog(context);
        pd.setMessage("Deleting...");
        pd.show();
        Query query = FirebaseDatabase.getInstance().getReference(POSTS).orderByChild("pId").equalTo(pId);
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot ds : dataSnapshot.getChildren()){
                    ds.getRef().removeValue();
                }
                Toast.makeText(context, "Deleted successfully", Toast.LENGTH_SHORT).show();
                pd.dismiss();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(context, databaseError.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public int getItemCount() {
        if (postList == null) return 0;
        return postList.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {

        CircularImageView mUPictureIv;
        TextView mUNameTv, mPTimeTv, mPDescription, pLikesTv,pCommentsTv;
        ImageView mPImageIv;
        ImageButton mMoreBtn;
        Button likeBtn,mCommentBtn,shareBtn;
        LinearLayout mProfileLay;

        ViewHolder(@NonNull View itemView) {
            super(itemView);

            mUPictureIv = itemView.findViewById(R.id.uPictureIv);
            mUNameTv = itemView.findViewById(R.id.uNameTv);
            mPTimeTv = itemView.findViewById(R.id.pTimeTv);
            mPDescription = itemView.findViewById(R.id.pDescription);
            mPImageIv = itemView.findViewById(R.id.pImageIv);
            likeBtn = itemView.findViewById(R.id.likeBtn);
            pLikesTv = itemView.findViewById(R.id.pLike);
            mMoreBtn = itemView.findViewById(R.id.moreBtn);
            mCommentBtn = itemView.findViewById(R.id.commentBtn);
            mProfileLay = itemView.findViewById(R.id.profileLay);
            pCommentsTv = itemView.findViewById(R.id.pCommentTv);
            shareBtn= itemView.findViewById(R.id.shareBtn);
        }
    }
}
