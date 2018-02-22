package com.alexstyl.specialdates.upcoming.view;

import com.alexstyl.specialdates.contact.Contact;
import com.alexstyl.specialdates.date.Date;

public interface OnUpcomingEventClickedListener {
    void onContactClicked(Contact contact, int position);

    void onNamedayClicked(Date date);
}
