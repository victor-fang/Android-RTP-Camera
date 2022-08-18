package com.vfang.android_rtp_camera;

import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.google.android.material.snackbar.Snackbar;
import com.vfang.android_rtp_camera.databinding.ActivityMainBinding;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MjpegView";
    InetAddress mInetAddress;
    private AppBarConfiguration appBarConfiguration;
    private ActivityMainBinding binding;
    private boolean isStreaming = false;
    private DatagramSocket sockCmd;
    private ExecutorService mExecutorService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);

        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        appBarConfiguration = new AppBarConfiguration.Builder(navController.getGraph()).build();
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);

        binding.fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String strCmd;
                if (isStreaming) {
                    isStreaming = false;
                    strCmd = "Stop";
                } else {
                    isStreaming = true;
                    strCmd = "Start 5004";
                }

                sendCommand(strCmd);

                Snackbar.make(view, "COMMAND: " + strCmd, Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        try {
            mInetAddress = InetAddress.getByName("192.168.88.1");
            sockCmd = new DatagramSocket();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendCommand(String strCmd) {

        Log.d(TAG, "COMMAND: " + strCmd);
        DatagramPacket datagramPacket = new DatagramPacket(strCmd.getBytes(), strCmd.getBytes().length, mInetAddress, 5000);

        mExecutorService = Executors.newCachedThreadPool();
        mExecutorService.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    sockCmd.send(datagramPacket);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        mExecutorService.shutdown();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        return NavigationUI.navigateUp(navController, appBarConfiguration)
                || super.onSupportNavigateUp();
    }
}
