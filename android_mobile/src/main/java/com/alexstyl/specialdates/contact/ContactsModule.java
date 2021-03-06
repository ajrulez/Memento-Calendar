package com.alexstyl.specialdates.contact;

import android.content.ContentResolver;
import android.content.Context;

import com.alexstyl.specialdates.CrashAndErrorTracker;
import com.alexstyl.specialdates.events.database.EventSQLiteOpenHelper;
import com.alexstyl.specialdates.events.peopleevents.UpcomingEventsSettings;

import javax.inject.Singleton;
import java.util.HashMap;
import java.util.Map;

import dagger.Module;
import dagger.Provides;

import static com.alexstyl.specialdates.contact.ContactSource.SOURCE_DEVICE;
import static com.alexstyl.specialdates.contact.ContactSource.SOURCE_FACEBOOK;

@Module
@Singleton
public class ContactsModule {

    private static final int CACHE_SIZE = 1024;

    @Provides
    @Singleton
    ContactsProvider provider(Context context, CrashAndErrorTracker tracker, UpcomingEventsSettings upcomingEventsSettings) {
        Map<Integer, ContactsProviderSource> sources = new HashMap<>();
        sources.put(SOURCE_DEVICE, buildAndroidSource(context, tracker));
        sources.put(SOURCE_FACEBOOK, buildFacebookSource(new EventSQLiteOpenHelper(context)));
        return new ContactsProvider(sources);
    }

    private static ContactsProviderSource buildAndroidSource(Context context, CrashAndErrorTracker tracker) {
        ContentResolver contentResolver = context.getContentResolver();
        AndroidContactFactory factory = new AndroidContactFactory(contentResolver, tracker);
        ContactCache contactCache = new ContactCache(CACHE_SIZE);
        return new AndroidContactsProviderSource(contactCache, factory);
    }

    private static ContactsProviderSource buildFacebookSource(EventSQLiteOpenHelper eventSQLHelper) {
        ContactCache contactCache = new ContactCache(CACHE_SIZE);
        return new FacebookContactsSource(eventSQLHelper, contactCache);
    }
}
