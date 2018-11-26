package com.vulog.kickscooter.ui.scooterinfo;

import android.arch.lifecycle.ViewModelProviders;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.vulog.kickscooter.R;

public class ScooterInfoFragment extends Fragment {

    private ScooterInfoViewModel mViewModel;

    public static ScooterInfoFragment newInstance() {
        return new ScooterInfoFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.scooter_info_fragment, container, false);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mViewModel = ViewModelProviders.of(this).get(ScooterInfoViewModel.class);
        // TODO: Use the ViewModel
    }

}
