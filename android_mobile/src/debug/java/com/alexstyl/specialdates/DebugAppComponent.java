package com.alexstyl.specialdates;

import com.alexstyl.resources.ResourcesModule;
import com.alexstyl.specialdates.contact.ContactsModule;
import com.alexstyl.specialdates.dailyreminder.DailyReminderModule;
import com.alexstyl.specialdates.date.DateModule;
import com.alexstyl.specialdates.debug.DebugFragment;
import com.alexstyl.specialdates.debug.DebugModule;
import com.alexstyl.specialdates.donate.DonateModule;
import com.alexstyl.specialdates.events.namedays.NamedayModule;
import com.alexstyl.specialdates.events.peopleevents.PeopleEventsModule;
import com.alexstyl.specialdates.images.ImageModule;

import javax.inject.Singleton;

import dagger.Component;

@Singleton
@Component(modules = {
        AndroidApplicationModule.class,
        NamedayModule.class,
        ContactsModule.class,
        PeopleEventsModule.class,
        DonateModule.class,
        DebugModule.class,
        DailyReminderModule.class,
        ResourcesModule.class,
        ImageModule.class,
        DateModule.class})
public interface DebugAppComponent {
    void inject(DebugFragment fragment);
}
