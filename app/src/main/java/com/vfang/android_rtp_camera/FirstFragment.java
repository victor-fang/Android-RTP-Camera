package com.vfang.android_rtp_camera;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.jobson.mjpegview.DisplayMode;
import com.vfang.android_rtp_camera.databinding.FragmentFirstBinding;

import java.io.IOException;
import java.net.DatagramSocket;

public class FirstFragment extends Fragment {

    private static final String TAG = "MjpegView";

    private FragmentFirstBinding binding;

    private DatagramSocket mIn = null;

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {

        binding = FragmentFirstBinding.inflate(inflater, container, false);
        return binding.getRoot();

    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.buttonFirst.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                NavHostFragment.findNavController(FirstFragment.this)
                        .navigate(R.id.action_FirstFragment_to_SecondFragment);
            }
        });

        try {
            mIn = new DatagramSocket(5004);
        } catch (IOException e) {
	        e.printStackTrace();
        }

		binding.mjpegview.showFps(true);
        binding.mjpegview.setDisplayMode(DisplayMode.BEST_FIT);
        binding.mjpegview.setSource(mIn);
    }

    @Override
    public void onDestroyView() {
        if (mIn != null)
            mIn.close();
        super.onDestroyView();
        binding = null;
		
		Log.i(TAG, "onDestroyView");
    }

}
