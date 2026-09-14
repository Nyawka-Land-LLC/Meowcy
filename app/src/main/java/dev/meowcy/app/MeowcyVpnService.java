package dev.meowcy.app;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.net.VpnService;
import android.os.Build;
import android.os.IBinder;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;

import cc.hev.socks5.tunnel.HevSocks5Tunnel;
import cc.hev.socks5.tunnel.TunnelConfig;
import cc.hev.socks5.tunnel.TunnelException;

public class MeowcyVpnService extends VpnService {
    public static final String ACTION_START = "dev.meowcy.app.START";
    public static final String ACTION_STOP = "dev.meowcy.app.STOP";

    private static final String TAG = "MeowcyVpn";
    private static final String CHANNEL = "meowcy_vpn";
    private static final int NOTIFICATION_ID = 42;

    private static volatile boolean running;

    private ParcelFileDescriptor tun;
    private HevSocks5Tunnel tunnel;

    public static boolean isRunning() {
        return running;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent == null ? ACTION_START : intent.getAction();
        if (ACTION_STOP.equals(action)) {
            stopTunnel();
            stopSelf();
            return START_NOT_STICKY;
        }

        startForeground(NOTIFICATION_ID, notification("Connecting…"));
        if (!running) {
            new Thread(this::startTunnel, "MeowcyTunnelStart").start();
        }
        return START_STICKY;
    }

    private void startTunnel() {
        ConfigStore.Config c = ConfigStore.load(this);
        try {
            Inet4Address proxyIp = resolveIPv4(c.host());

            Builder builder = new Builder()
                    .setSession("Meowcy")
                    .setMtu(8500)
                    .addAddress("198.18.0.1", 30)
                    .addDnsServer("1.1.1.1");

            // Route everything through the VPN except the SOCKS5 server itself.
            // This prevents the tunnel's own upstream connection from looping back into TUN.
            IPv4Routes.addAllExcept(builder, proxyIp);

            tun = builder.establish();
            if (tun == null) {
                throw new IllegalStateException("VpnService.Builder.establish() returned null");
            }

            TunnelConfig.Builder tunnelBuilder = new TunnelConfig.Builder()
                    .setSocks5Address(proxyIp.getHostAddress())
                    .setSocks5Port(c.port())
                    .setTunName("tun0")
                    .setTunMtu(8500)
                    .setTunIPv4Address("198.18.0.1")
                    .setTunIPv4Gateway("198.18.0.2")
                    .addDnsServer("1.1.1.1");

            if (!c.username().isEmpty()) {
                tunnelBuilder.setSocks5Username(c.username());
            }
            if (!c.password().isEmpty()) {
                tunnelBuilder.setSocks5Password(c.password());
            }

            tunnel = new HevSocks5Tunnel();
            if (!HevSocks5Tunnel.isLibraryLoaded()) {
                Throwable cause = HevSocks5Tunnel.getLibraryLoadError();
                throw new IllegalStateException("HEV native library failed to load", cause);
            }

            tunnel.startAsync(tunnelBuilder.build(), tun.getFileDescriptor());
            running = true;
            updateNotification("Connected to " + proxyIp.getHostAddress() + ":" + c.port());
        } catch (TunnelException | IOException | RuntimeException e) {
            Log.e(TAG, "Failed to start VPN", e);
            updateNotification("Connection failed");
            stopTunnel();
            stopSelf();
        }
    }

    private Inet4Address resolveIPv4(String host) throws IOException {
        for (InetAddress address : InetAddress.getAllByName(host)) {
            if (address instanceof Inet4Address v4) {
                return v4;
            }
        }
        throw new IOException("Meowcy MVP requires an IPv4 SOCKS5 endpoint");
    }

    private synchronized void stopTunnel() {
        running = false;

        if (tunnel != null) {
            try {
                tunnel.stop();
            } catch (RuntimeException e) {
                Log.w(TAG, "Error stopping tunnel", e);
            }
            tunnel = null;
        }

        if (tun != null) {
            try {
                tun.close();
            } catch (IOException e) {
                Log.w(TAG, "Error closing TUN", e);
            }
            tun = null;
        }

        stopForeground(STOP_FOREGROUND_REMOVE);
    }

    @Override
    public void onRevoke() {
        stopTunnel();
        stopSelf();
        super.onRevoke();
    }

    @Override
    public void onDestroy() {
        stopTunnel();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return super.onBind(intent);
    }

    private Notification notification(String text) {
        Intent open = new Intent(this, MainActivity.class);
        PendingIntent openPending = PendingIntent.getActivity(
                this,
                1,
                open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        Intent stop = new Intent(this, MeowcyVpnService.class).setAction(ACTION_STOP);
        PendingIntent stopPending = PendingIntent.getService(
                this,
                2,
                stop,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        return new Notification.Builder(this, CHANNEL)
                .setSmallIcon(android.R.drawable.stat_sys_warning)
                .setContentTitle("Meowcy")
                .setContentText(text)
                .setContentIntent(openPending)
                .setOngoing(true)
                .addAction(new Notification.Action.Builder(null, "Disconnect", stopPending).build())
                .build();
    }

    private void updateNotification(String text) {
        NotificationManager nm = getSystemService(NotificationManager.class);
        nm.notify(NOTIFICATION_ID, notification(text));
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationManager nm = getSystemService(NotificationManager.class);
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL,
                    "Meowcy VPN",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("VPN connection status");
            nm.createNotificationChannel(channel);
        }
    }
}
