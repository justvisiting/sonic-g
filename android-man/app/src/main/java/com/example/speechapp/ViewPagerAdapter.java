package com.example.speechapp;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

public class ViewPagerAdapter extends FragmentStateAdapter {
    private final Fragment chatFragment;
    private final Fragment debugFragment;

    public ViewPagerAdapter(FragmentActivity activity, Fragment chatFragment, Fragment debugFragment) {
        super(activity);
        this.chatFragment = chatFragment;
        this.debugFragment = debugFragment;
    }

    @Override
    public Fragment createFragment(int position) {
        return position == 0 ? chatFragment : debugFragment;
    }

    @Override
    public int getItemCount() {
        return 2;
    }
}
