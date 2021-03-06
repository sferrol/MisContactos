package mx.vainiyasoft.agenda.util;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.provider.Settings;
import android.widget.RemoteViews;

import mx.vainiyasoft.agenda.MainActivity;
import mx.vainiyasoft.agenda.R;

/**
 * Created by alejandro on 6/16/14.
 */
public class NotificationController {

    private static Context context = ApplicationContextProvider.getContext();

    public static void notify(String title, String message, int notifID, int currentProgress, int maxProgress) {
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        RemoteViews contentView = createContentView(title, message, currentProgress, maxProgress);
        Intent notificationIntent = new Intent(context, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, notificationIntent, 0);
        Notification.Builder builder = new Notification.Builder(context)
                .setContent(contentView)
                .setSmallIcon(mx.vainiyasoft.agenda.R.drawable.ic_stat_agenda)
                .setOngoing(currentProgress < maxProgress)
                .setOnlyAlertOnce(true)
                .setTicker(message)
                .setContentIntent(pendingIntent)
                .setSound(Settings.System.DEFAULT_NOTIFICATION_URI);
        Notification notification = builder.build();
        manager.notify(notifID, notification);
    }

    public static void notify(String title, String message, int notifID, boolean onGoing) {
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        RemoteViews contentView = createContentView(title, message, 1, 1);
        Intent notificationIntent = new Intent(context, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, notificationIntent, 0);
        Notification.Builder builder = new Notification.Builder(context)
                .setContent(contentView)
                .setSmallIcon(mx.vainiyasoft.agenda.R.drawable.ic_stat_agenda)
                .setOngoing(onGoing)
                .setTicker(message)
                .setAutoCancel(!onGoing)
                .setContentIntent(pendingIntent)
                .setSound(Settings.System.DEFAULT_NOTIFICATION_URI);
        Notification notification = builder.build();
        manager.notify(notifID, notification);
    }

    public static void notify(String title, String message, int notifID) {
        notify(title, message, notifID, false);
    }

    public static void clearNotification(int notifID) {
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        manager.cancel(notifID);
    }

    private static RemoteViews createContentView(String title, String message, int currentProgress, int maxProgress) {
        RemoteViews contentView = new RemoteViews(context.getPackageName(), mx.vainiyasoft.agenda.R.layout.sync_notification);
        contentView.setImageViewResource(mx.vainiyasoft.agenda.R.id.notification_image, mx.vainiyasoft.agenda.R.drawable.ic_stat_agenda);
        contentView.setTextViewText(mx.vainiyasoft.agenda.R.id.notification_title, title);
        contentView.setTextViewText(mx.vainiyasoft.agenda.R.id.notification_text, message);
        contentView.setProgressBar(mx.vainiyasoft.agenda.R.id.notification_progress, maxProgress, currentProgress, false);
        if (currentProgress == maxProgress)
            contentView.setImageViewResource(mx.vainiyasoft.agenda.R.id.notification_check, R.drawable.ic_menu_accept);
        return contentView;
    }

}
