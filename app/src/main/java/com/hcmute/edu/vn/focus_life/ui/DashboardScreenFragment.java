package com.hcmute.edu.vn.focus_life.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.hcmute.edu.vn.focus_life.R;

public class DashboardScreenFragment extends Fragment {

    private static final String ARG_LAYOUT_RES = "arg_layout_res";

    public static DashboardScreenFragment newInstance(int layoutRes) {
        DashboardScreenFragment fragment = new DashboardScreenFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_LAYOUT_RES, layoutRes);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        int layoutRes = R.layout.activity_widget_preview;
        if (getArguments() != null) {
            layoutRes = getArguments().getInt(ARG_LAYOUT_RES, R.layout.activity_widget_preview);
        }
        return inflater.inflate(layoutRes, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        View bottomNav = view.findViewById(R.id.bottomNav);
        if (bottomNav != null) {
            bottomNav.setVisibility(View.GONE);
        }
    }
}
