package com.eso.socialmedia.notification;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;

import com.eso.socialmedia.R;
import com.eso.socialmedia.ui.activity.ChatActivity;
import com.eso.socialmedia.ui.activity.PostDetailsActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Random;

import static com.eso.socialmedia.utils.Common.HISUID;
import static com.eso.socialmedia.utils.Common.SP_USER;
import static com.eso.socialmedia.utils.Common.TOKENS;

public class FirebaseMessaging extends FirebaseMessagingService {

    private static final String ADMIN_CHANNEL_ID = "admin_channel";

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        SharedPreferences sp = getApplicationContext().getSharedPreferences(SP_USER, 0);
        String savedCurrentUser = sp.getString("Current_USERID","None");

        String notificationType = remoteMessage.getData().get("notificationType");
        if (notificationType.equals("PostNotification")){
            String sender = remoteMessage.getData().get("sender");
            String pId = remoteMessage.getData().get("pId");
            String pName = remoteMessage.getData().get("pName");
            String pDescription = remoteMessage.getData().get("pDescription");
            if (!sender.equals(savedCurrentUser))
                showPostNotification(""+pId,""+pName,""+pDescription);
        }else if (notificationType.equals("ChatNotification")){
            String sent = remoteMessage.getData().get("sent");
            String user = remoteMessage.getData().get("user");
            FirebaseUser fuser = FirebaseAuth.getInstance().getCurrentUser();
            if (fuser != null && sent.equals(fuser.getUid())){
                if (!savedCurrentUser.equals(user))
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                        sendONormalNotification(remoteMessage);
                    else
                        sendNormalNotification(remoteMessage);
            }
        }
    }

    private void showPostNotification(String pId,String pName, String pDescription) {
        NotificationManager notificationManager = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        int notificationID = new Random().nextInt(3000);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            setupPostNotificationChannel(notificationManager);

        Intent i = new Intent(this, PostDetailsActivity.class);
        i.putExtra("postId",pId);
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this,0 , i, PendingIntent.FLAG_ONE_SHOT);
        Bitmap largeIcon = BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher);
        Uri notificationSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this,""+ADMIN_CHANNEL_ID )
                .setSmallIcon(R.mipmap.ic_launcher)
                .setLargeIcon(largeIcon)
                .setContentTitle(pName)
                .setContentText(pDescription)
                .setSound(notificationSoundUri)
                .setContentIntent(pendingIntent);


        notificationManager.notify(notificationID,notificationBuilder.build());
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void setupPostNotificationChannel(NotificationManager notificationManager) {
        CharSequence channelName = "New Notification";
        String channelDescription = "Device to device post notification";
        NotificationChannel adminChannel = new NotificationChannel(ADMIN_CHANNEL_ID, channelName, NotificationManager.IMPORTANCE_HIGH);
        adminChannel.setDescription(channelDescription);
        adminChannel.enableLights(true);
        adminChannel.setLightColor(Color.RED);
        adminChannel.enableVibration(true);
        if (notificationManager != null)
            notificationManager.createNotificationChannel(adminChannel);
    }

    @TargetApi(Build.VERSION_CODES.O)
    private void sendONormalNotification(RemoteMessage remoteMessage) {
        String user = remoteMessage.getData().get("user");
        String icon = remoteMessage.getData().get("icon");
        String title = remoteMessage.getData().get("title");
        String body = remoteMessage.getData().get("body");

        int i  = Integer.parseInt(user.replaceAll("[\\D]",""));
        Intent intent = new Intent(this, ChatActivity.class);
        Bundle bundle = intent.getExtras();
        bundle.putString(HISUID,user);
        intent.putExtras(bundle);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pIntent = PendingIntent.getActivity(this,i,intent,PendingIntent.FLAG_ONE_SHOT);

        Uri defSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        OreoAndAbove oreoAndAbove = new OreoAndAbove(this);
        Notification.Builder builder = oreoAndAbove.getONotifications(title,body,pIntent,defSoundUri,icon);
        int j = 0;
        if (j >0)
            j = i;
        oreoAndAbove.getManager().notify(j,builder.build());
    }

    private void sendNormalNotification(RemoteMessage remoteMessage) {
        String user = remoteMessage.getData().get("user");
        String icon = remoteMessage.getData().get("icon");
        String title = remoteMessage.getData().get("title");
        String body = remoteMessage.getData().get("body");

        RemoteMessage.Notification notification = remoteMessage.getNotification();
        int i  = Integer.parseInt(user.replaceAll("[\\D]",""));
        Intent intent = new Intent(this, ChatActivity.class);
        intent.putExtra(HISUID,user);
        /*Bundle bundle = intent.getExtras();
        bundle.putString(HISUID,user);
        intent.putExtras(bundle);*/
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pIntent = PendingIntent.getActivity(this,i,intent,PendingIntent.FLAG_ONE_SHOT);

        Uri defSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this,""+ADMIN_CHANNEL_ID)
                .setSmallIcon(Integer.parseInt(icon))
                .setContentText(body)
                .setContentTitle(title)
                .setAutoCancel(true)
                .setSound(defSoundUri)
                .setContentIntent(pIntent);

        NotificationManager notificationManager = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        int j = 0;
        if (j >0)
            j = i;
        notificationManager.notify(j,builder.build());
    }

    @Override
    public void onNewToken(@NonNull String s) {
        super.onNewToken(s);
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null)
            updateToken(s);
    }

    private void updateToken(String s){
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference(TOKENS);
        Token token = new Token(s);
        ref.child(user.getUid()).setValue(token);
    }
}
