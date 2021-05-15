package com.github.portfolio.heyapp.Adapters;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;

import com.github.portfolio.heyapp.App;
import com.github.portfolio.heyapp.Fragments.ChatsFragment;
import com.github.portfolio.heyapp.Fragments.ContactsFragment;
import com.github.portfolio.heyapp.Fragments.RequestsFragment;
import com.github.portfolio.heyapp.R;

public class TabsAdapter extends FragmentPagerAdapter {

    private Context context = App.getContext();

    private final String CHATS_TITLE = context.getResources().getString(R.string.chats_title);
    private final String CONTACTS_TITLE = context.getResources().getString(R.string.contacts_title);
    private final String REQUESTS_TITLE = context.getResources().getString(R.string.requests_title);

    public TabsAdapter(@NonNull FragmentManager fm) {
        super(fm);
    }

    @NonNull
    @Override
    public Fragment getItem(int position) {
        switch (position) {
            case 0:
                return new ChatsFragment();
            case 1:
                return new ContactsFragment();
            case 2:
                return new RequestsFragment();
            default:
                return null;
        }
    }

    @Override
    public int getCount() {
        return 3;
    }

    @Nullable
    @Override
    public CharSequence getPageTitle(int position) {
        switch (position) {
            case 0:
                return CHATS_TITLE;
            case 1:
                return CONTACTS_TITLE;
            case 2:
                return REQUESTS_TITLE;
            default:
                return null;
        }
    }
}
