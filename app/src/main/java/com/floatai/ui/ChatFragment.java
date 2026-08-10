package com.floatai.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Switch;
import android.widget.TextView;
import androidx.fragment.app.Fragment;

public class ChatFragment extends Fragment {
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_chat, container, false);
        Switch floatSwitch = v.findViewById(R.id.float_switch);
        floatSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) requireContext().startService(new android.content.Intent(requireContext(), FloatService.class));
            else requireContext().stopService(new android.content.Intent(requireContext(), FloatService.class));
        });
        return v;
    }
}
