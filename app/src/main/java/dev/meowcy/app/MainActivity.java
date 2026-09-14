package dev.meowcy.app;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.VpnService;
import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Space;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
    private static final int VPN_REQUEST = 7001;

    private EditText host;
    private EditText port;
    private EditText username;
    private EditText password;
    private Button connect;
    private TextView status;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(createUi());
        loadConfig();

        if (Build.VERSION.SDK_INT >= 33) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 99);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateState();
    }

    private View createUi() {
        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(Color.rgb(13, 11, 18));

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(24), dp(42), dp(24), dp(32));
        scroll.addView(root, new ScrollView.LayoutParams(-1, -2));

        TextView cat = text("🐈", 44, Color.WHITE, Typeface.NORMAL);
        root.addView(cat);

        TextView title = text("Meowcy", 38, Color.WHITE, Typeface.BOLD);
        root.addView(title);

        TextView subtitle = text("Tiny Android VPN client", 16, Color.rgb(180, 174, 192), Typeface.NORMAL);
        subtitle.setPadding(0, dp(4), 0, dp(28));
        root.addView(subtitle);

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(18), dp(18), dp(18), dp(18));
        card.setBackground(roundRect(Color.rgb(27, 23, 36), 22));
        root.addView(card, new LinearLayout.LayoutParams(-1, -2));

        host = input("SOCKS5 host", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        port = input("Port", InputType.TYPE_CLASS_NUMBER);
        username = input("Username (optional)", InputType.TYPE_CLASS_TEXT);
        password = input("Password (optional)", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);

        card.addView(host);
        card.addView(port);
        card.addView(username);
        card.addView(password);

        Space gap = new Space(this);
        card.addView(gap, new LinearLayout.LayoutParams(1, dp(12)));

        connect = new Button(this);
        connect.setAllCaps(false);
        connect.setTextSize(18);
        connect.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        connect.setTextColor(Color.rgb(18, 12, 24));
        connect.setBackground(roundRect(Color.rgb(199, 155, 255), 18));
        connect.setOnClickListener(v -> onConnectPressed());
        card.addView(connect, new LinearLayout.LayoutParams(-1, dp(58)));

        status = text("Disconnected", 16, Color.rgb(180, 174, 192), Typeface.BOLD);
        status.setGravity(Gravity.CENTER_HORIZONTAL);
        status.setPadding(0, dp(22), 0, 0);
        root.addView(status, new LinearLayout.LayoutParams(-1, -2));

        TextView note = text(
                "MVP: system VpnService + SOCKS5 transport. Credentials stay in this app's private preferences.",
                13,
                Color.rgb(128, 122, 142),
                Typeface.NORMAL
        );
        note.setPadding(0, dp(28), 0, 0);
        root.addView(note);

        return scroll;
    }

    private void loadConfig() {
        ConfigStore.Config c = ConfigStore.load(this);
        host.setText(c.host());
        port.setText(Integer.toString(c.port()));
        username.setText(c.username());
        password.setText(c.password());
    }

    private void onConnectPressed() {
        if (MeowcyVpnService.isRunning()) {
            Intent stop = new Intent(this, MeowcyVpnService.class).setAction(MeowcyVpnService.ACTION_STOP);
            startService(stop);
            status.setText("Disconnecting…");
            connect.postDelayed(this::updateState, 350);
            return;
        }

        String h = host.getText().toString().trim();
        int p;
        try {
            p = Integer.parseInt(port.getText().toString().trim());
        } catch (NumberFormatException e) {
            toast("Port must be a number");
            return;
        }

        if (h.isEmpty() || p < 1 || p > 65535) {
            toast("Enter a valid host and port");
            return;
        }

        ConfigStore.save(this, new ConfigStore.Config(
                h,
                p,
                username.getText().toString(),
                password.getText().toString()
        ));

        Intent permission = VpnService.prepare(this);
        if (permission != null) {
            startActivityForResult(permission, VPN_REQUEST);
        } else {
            startVpn();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == VPN_REQUEST && resultCode == RESULT_OK) {
            startVpn();
        } else if (requestCode == VPN_REQUEST) {
            toast("VPN permission was not granted");
        }
    }

    private void startVpn() {
        Intent intent = new Intent(this, MeowcyVpnService.class).setAction(MeowcyVpnService.ACTION_START);
        if (Build.VERSION.SDK_INT >= 26) {
            startForegroundService(intent);
        } else {
            startService(intent);
        }
        status.setText("Connecting…");
        connect.postDelayed(this::updateState, 600);
    }

    private void updateState() {
        boolean running = MeowcyVpnService.isRunning();
        connect.setText(running ? "Disconnect" : "Connect");
        status.setText(running ? "Connected  •  nya~" : "Disconnected");
        status.setTextColor(running ? Color.rgb(184, 255, 201) : Color.rgb(180, 174, 192));
    }

    private EditText input(String hint, int inputType) {
        EditText edit = new EditText(this);
        edit.setHint(hint);
        edit.setHintTextColor(Color.rgb(125, 118, 140));
        edit.setTextColor(Color.WHITE);
        edit.setSingleLine(true);
        edit.setInputType(inputType);
        edit.setPadding(dp(14), 0, dp(14), 0);
        edit.setBackground(roundRect(Color.rgb(39, 34, 50), 14));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, dp(54));
        lp.bottomMargin = dp(10);
        edit.setLayoutParams(lp);
        return edit;
    }

    private TextView text(String s, float size, int color, int style) {
        TextView v = new TextView(this);
        v.setText(s);
        v.setTextSize(size);
        v.setTextColor(color);
        v.setTypeface(Typeface.DEFAULT, style);
        return v;
    }

    private GradientDrawable roundRect(int color, int radiusDp) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(radiusDp));
        return d;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private void toast(String text) {
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
    }
}
