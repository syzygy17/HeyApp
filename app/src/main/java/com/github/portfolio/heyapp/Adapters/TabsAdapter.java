package com.github.portfolio.heyapp.Adapters;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;

import com.github.portfolio.heyapp.Fragments.ChatsFragment;
import com.github.portfolio.heyapp.Fragments.ContactsFragment;
import com.github.portfolio.heyapp.Fragments.RequestsFragment;

public class TabsAdapter extends FragmentPagerAdapter {

    private final String CHATS_TITLE = "Chats";
    private final String CONTACTS_TITLE = "Contacts";
    private final String REQUESTS_TITLE = "Requests";

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
