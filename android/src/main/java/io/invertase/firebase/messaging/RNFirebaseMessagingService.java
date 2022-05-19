package io.invertase.firebase.messaging;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.ComponentName;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import com.facebook.react.HeadlessJsTaskService;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Map;

import io.invertase.firebase.Utils;

public class RNFirebaseMessagingService extends FirebaseMessagingService {
  private static final String TAG = "RNFMessagingService";

  public static final String MESSAGE_EVENT = "messaging-message";
  public static final String NEW_TOKEN_EVENT = "messaging-token-refresh";
  public static final String REMOTE_NOTIFICATION_EVENT = "notifications-remote-notification";
  public static final String MKT_NOTIFICATION_EVENT = "mkt-notifications-remote-notification"; // frankie
  private NotificationManager notifManager;

  @Override
  public void onNewToken(String token) {
    Log.d(TAG, "onNewToken event received");

    Intent newTokenEvent = new Intent(NEW_TOKEN_EVENT);
    LocalBroadcastManager
      .getInstance(this)
      .sendBroadcast(newTokenEvent);
  }

  // frankie
  private Bundle createMktMessage(
    String title,
    String body,
    String messageId,
    String sound,
    String clickAction,
    String color,
    String icon,
    String tag,
    Map<String, String> data
  ) {

    Bundle notificationMap = new Bundle();
    Bundle dataMap = new Bundle();

    // Cross platform notification properties
    if (body != null) {
      notificationMap.putString("body", body);
    }
    if (data != null) {
      for (Map.Entry<String, String> e : data
        .entrySet()) {
        dataMap.putString(e.getKey(), e.getValue());
      }
    }
    notificationMap.putBundle("data", dataMap);
    if (messageId != null) {
      notificationMap.putString("notificationId", messageId);
    }
    if (sound != null) {
      notificationMap.putString("sound", sound);
    }

    if (title != null) {
      notificationMap.putString("title", title);
    }

    // Android specific notification properties
    if (clickAction != null) {
      notificationMap.putString("clickAction", clickAction);
    }
    if (color != null) {
      notificationMap.putString("color", color);
    }
    if (icon != null) {
      notificationMap.putString("icon", icon);
    }
    if (tag != null) {
      notificationMap.putString("group", tag);
      notificationMap.putString("tag", tag);
    }

    return notificationMap;
  }


  // frankie
  private Class getMainActivityClass(Context context) {
    String packageName = context.getPackageName();
    Intent launchIntent = context
      .getPackageManager()
      .getLaunchIntentForPackage(packageName);

    try {
      return Class.forName(launchIntent.getComponent().getClassName());
    } catch (ClassNotFoundException e) {
      Log.e(TAG, "Failed to get main activity class", e);
      return null;
    }
  }

  // frankie
  public void createNotification(String title,
                                 String body,
                                 String image
  ) {
    final int NOTIFY_ID = 1002;

    String name = "MKT Channel";
    String id = "mkt-channel";
    String description = "MKT Cloud notification channel";

    Intent intent;
    PendingIntent pendingIntent;
    NotificationCompat.Builder builder;
    Bitmap remotePicture = null;
    NotificationCompat.BigPictureStyle notiStyle = new NotificationCompat.BigPictureStyle();

    try {
      remotePicture = BitmapFactory.decodeStream((InputStream) new URL(image).getContent());
    } catch (IOException e) {
      e.printStackTrace();
    }

    if(remotePicture != null)
      notiStyle.bigPicture(remotePicture);

    if (notifManager == null) {
      notifManager =
        (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      int importance = NotificationManager.IMPORTANCE_HIGH;
      NotificationChannel mChannel = notifManager.getNotificationChannel(id);
      if (mChannel == null) {
        mChannel = new NotificationChannel(id, name, importance);
        mChannel.setDescription(description);
        mChannel.enableVibration(true);
        mChannel.setLightColor(Color.GREEN);
        mChannel.setVibrationPattern(new long[]{100, 200, 300, 400, 500, 400, 300, 200, 400});
        notifManager.createNotificationChannel(mChannel);
      }

      builder = new NotificationCompat.Builder(getApplicationContext(), id);

      intent = new Intent(this, getMainActivityClass(getApplicationContext()));
      intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
      pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);


      builder.setContentTitle(title)  // required
        .setSmallIcon( getApplicationContext()
          .getResources()
          .getIdentifier("ic_notification", "drawable", getApplicationContext().getPackageName())
        ) // required
        .setContentText(body)  // required
        .setDefaults(Notification.DEFAULT_ALL)
        .setAutoCancel(true)
        .setContentIntent(pendingIntent)
        .setTicker(title)
        .setStyle(remotePicture == null ? null : notiStyle)
        .setVibrate(new long[]{100, 200, 300, 400, 500, 400, 300, 200, 400});
    } else {

      builder = new NotificationCompat.Builder(getApplicationContext(), id);

      intent = new Intent(this, getMainActivityClass(getApplicationContext()));
      intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
      pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);

      builder.setContentTitle(title)                           // required
        .setSmallIcon( getApplicationContext()
          .getResources()
          .getIdentifier("ic_notification", "drawable", getApplicationContext().getPackageName())
        ) // required
        .setContentText(body)  // required
        .setDefaults(Notification.DEFAULT_ALL)
        .setAutoCancel(true)
        .setContentIntent(pendingIntent)
        .setTicker(title)
        .setStyle(remotePicture == null ? null : notiStyle)
        .setVibrate(new long[]{100, 200, 300, 400, 500, 400, 300, 200, 400})
        .setPriority(Notification.PRIORITY_HIGH);
    }

    Notification notification = builder.build();
    notifManager.notify(NOTIFY_ID, notification);
  }

  @Override
  public void onMessageReceived(RemoteMessage message) {
    Log.d(TAG, "onMessageReceived event received");

    @NonNull Map<String, String> messageMkt = message.getData(); // frankie

    // frankie
    if("SFMC".equalsIgnoreCase(messageMkt.get("_sid"))){
      // it's a marketing cloud notification
      Bundle notificationBundle = createMktMessage(
        messageMkt.get("title"),
        messageMkt.get("alert"),
        message.getMessageId(),
        messageMkt.get("sound"),
        null,
        null,
        null,
        null,
        message.getData()
      );

      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        createNotification(messageMkt.get("title"), messageMkt.get("alert"), messageMkt.get("_mediaUrl"));
      }

      Intent notificationEvent = new Intent(MKT_NOTIFICATION_EVENT);
      notificationEvent.putExtra("notification", notificationBundle);

      // Broadcast it to the (foreground) RN Application
      LocalBroadcastManager
        .getInstance(this)
        .sendBroadcast(notificationEvent);

        if (Utils.isAppInForeground(this.getApplicationContext())) {
          Intent messagingEvent = new Intent(MESSAGE_EVENT);
          messagingEvent.putExtra("message", message);
          // Broadcast it so it is only available to the RN Application
          LocalBroadcastManager
            .getInstance(this)
            .sendBroadcast(messagingEvent);
        } else {
          try {
            // If the app is in the background we send it to the Headless JS Service
            Intent headlessIntent = new Intent(
              this.getApplicationContext(),
              RNFirebaseBackgroundMessagingService.class
            );
            headlessIntent.putExtra("message", message);
            ComponentName name = this.getApplicationContext().startService(headlessIntent);
            if (name != null) {
              HeadlessJsTaskService.acquireWakeLockNow(this.getApplicationContext());
            }
          } catch (IllegalStateException ex) {
            Log.e(
              TAG,
              "Background messages will only work if the message priority is set to 'high'",
              ex
            );
          }
        }


    } else if (message.getNotification() != null) {
      // It's a notification, pass to the Notifications module
      Intent notificationEvent = new Intent(REMOTE_NOTIFICATION_EVENT);
      notificationEvent.putExtra("notification", message);

      // Broadcast it to the (foreground) RN Application
      LocalBroadcastManager
        .getInstance(this)
        .sendBroadcast(notificationEvent);
    } else {
      // It's a data message
      // If the app is in the foreground we send it to the Messaging module
      if (Utils.isAppInForeground(this.getApplicationContext())) {
        Intent messagingEvent = new Intent(MESSAGE_EVENT);
        messagingEvent.putExtra("message", message);
        // Broadcast it so it is only available to the RN Application
        LocalBroadcastManager
          .getInstance(this)
          .sendBroadcast(messagingEvent);
      } else {
        try {
          // If the app is in the background we send it to the Headless JS Service
          Intent headlessIntent = new Intent(
            this.getApplicationContext(),
            RNFirebaseBackgroundMessagingService.class
          );
          headlessIntent.putExtra("message", message);
          ComponentName name = this.getApplicationContext().startService(headlessIntent);
          if (name != null) {
            HeadlessJsTaskService.acquireWakeLockNow(this.getApplicationContext());
          }
        } catch (IllegalStateException ex) {
          Log.e(
            TAG,
            "Background messages will only work if the message priority is set to 'high'",
            ex
          );
        }
      }
    }
  }
}
