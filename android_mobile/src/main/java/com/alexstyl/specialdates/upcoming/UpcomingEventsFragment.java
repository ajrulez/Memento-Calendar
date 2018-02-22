package com.alexstyl.specialdates.upcoming;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.transition.TransitionManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.alexstyl.specialdates.AppComponent;
import com.alexstyl.specialdates.CrashAndErrorTracker;
import com.alexstyl.specialdates.MementoApplication;
import com.alexstyl.specialdates.PeopleEventsView;
import com.alexstyl.specialdates.R;
import com.alexstyl.specialdates.analytics.Analytics;
import com.alexstyl.specialdates.contact.Contact;
import com.alexstyl.specialdates.date.Date;
import com.alexstyl.specialdates.events.PeopleEventsMonitor;
import com.alexstyl.specialdates.events.peopleevents.EventPreferences;
import com.alexstyl.specialdates.events.peopleevents.PeopleEventsViewRefresher;
import com.alexstyl.specialdates.home.HomeNavigator;
import com.alexstyl.specialdates.home.ViewPagerAware;
import com.alexstyl.specialdates.images.ImageLoader;
import com.alexstyl.specialdates.permissions.ContactPermissionRequest;
import com.alexstyl.specialdates.permissions.ContactPermissionRequest.PermissionCallbacks;
import com.alexstyl.specialdates.permissions.AndroidPermissionChecker;
import com.alexstyl.specialdates.permissions.MementoPermissionsChecker;
import com.alexstyl.specialdates.permissions.PermissionNavigator;
import com.alexstyl.specialdates.support.AskForSupport;
import com.alexstyl.specialdates.ui.base.MementoFragment;
import com.alexstyl.specialdates.upcoming.view.OnUpcomingEventClickedListener;
import com.novoda.notils.caster.Views;

import javax.inject.Inject;
import java.util.List;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

public class UpcomingEventsFragment extends MementoFragment implements UpcomingListMVPView, ViewPagerAware {

    private ViewGroup root;
    private ProgressBar progressBar;
    private TextView emptyView;
    private RecyclerView upcomingList;
    private FloatingActionButton addEventView;

    private UpcomingEventsPresenter presenter;
    private UpcomingEventsAdapter adapter;
    private ContactPermissionRequest permissions;
    private AskForSupport askForSupport;

    @Inject HomeNavigator navigator;
    @Inject Analytics analytics;
    @Inject ImageLoader imageLoader;
    @Inject UpcomingEventsProvider provider;
    @Inject PeopleEventsViewRefresher refresher;
    @Inject PeopleEventsMonitor eventsMonitor;
    @Inject EventPreferences eventPreferences;
    @Inject MementoPermissionsChecker permissionsChecker;
    @Inject CrashAndErrorTracker tracker;

    private final PeopleEventsView listener = new PeopleEventsView() {
        @Override
        public void onEventsUpdated() {
            presenter.refreshEvents();
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        AppComponent applicationModule = ((MementoApplication) getActivity().getApplication()).getApplicationModule();
        applicationModule.inject(this);

        askForSupport = new AskForSupport(getActivity());

        permissions = new ContactPermissionRequest(
                new PermissionNavigator(getActivity(), analytics),
                new AndroidPermissionChecker(tracker, getActivity()), permissionCallbacks
        );

        presenter = new UpcomingEventsPresenter(
                Date.Companion.today(),
                permissionsChecker,
                provider,
                Schedulers.io(),
                AndroidSchedulers.mainThread()
        );
        refresher.addView(listener);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_upcoming_events, container, false);
        root = Views.findById(view, R.id.root);
        progressBar = Views.findById(view, R.id.upcoming_events_progress);
        emptyView = Views.findById(view, R.id.upcoming_events_emptyview);
        addEventView = Views.findById(view, R.id.upcoming_events_add_event);
        addEventView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                navigator.toAddEvent(getActivity());
            }
        });

        upcomingList = Views.findById(view, R.id.upcoming_events_list);
        upcomingList.setHasFixedSize(true);
        upcomingList.setLayoutManager(new LinearLayoutManager(getActivity(), LinearLayoutManager.VERTICAL, false));
        upcomingList.addItemDecoration(
                new UpcomingEventsDecorator(
                        getResources().getDimensionPixelSize(R.dimen.upcoming_event_header_vertical_spacing),
                        getResources().getDimensionPixelSize(R.dimen.upcoming_event_vertical_spacing)
                ));

        adapter = new UpcomingEventsAdapter(
                new UpcomingViewHolderFactory(inflater, imageLoader),
                new OnUpcomingEventClickedListener() {

                    @Override
                    public void onContactClicked(Contact contact, int position) {
                        ContactEventViewHolder viewHolder = (ContactEventViewHolder) upcomingList.findViewHolderForAdapterPosition(position);
                        navigator.toContactDetails(contact, getActivity(), viewHolder.getImageView());
                    }

                    @Override
                    public void onNamedayClicked(Date date) {
                        navigator.toNamedayDate(date, getActivity());
                    }
                }
        );
        upcomingList.setAdapter(adapter);
        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        presenter.startPresentingInto(this);
    }

    @Override
    public void showLoading() {
        progressBar.setVisibility(View.VISIBLE);
        upcomingList.setVisibility(View.GONE);
        emptyView.setVisibility(View.GONE);
    }

    @Override
    public void display(List<UpcomingRowViewModel> events) {
        TransitionManager.beginDelayedTransition(root);

        progressBar.setVisibility(View.GONE);
        adapter.displayUpcomingEvents(events);

        if (events.size() > 0) {
            upcomingList.setVisibility(View.VISIBLE);
            emptyView.setVisibility(View.GONE);
        } else {
            upcomingList.setVisibility(View.GONE);
            emptyView.setVisibility(View.VISIBLE);
        }

        if (askForSupport.shouldAskForRating()) {
            askForSupport.askForRatingFromUser(getActivity());
        }
    }

    @Override
    public boolean isEmpty() {
        return upcomingList.getChildCount() == 0;
    }

    @Override
    public void askForContactPermission() {
        permissions.requestForPermission();
    }

    @Override
    public void onStop() {
        super.onStop();
        presenter.stopPresenting();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        refresher.removeView(listener);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        permissions.onActivityResult(requestCode, resultCode, data);
    }

    private final PermissionCallbacks permissionCallbacks = new PermissionCallbacks() {
        @Override
        public void onPermissionGranted() {
            eventsMonitor.updateEvents();
            eventPreferences.markEventsAsInitialised();
        }

        @Override
        public void onPermissionDenied() {
            Activity activity = getActivity();
            if (activity != null) {
                activity.finishAffinity();
            }
        }
    };

    @Override
    public void onPagerIdled() {
        addEventView.show();
    }

    @Override
    public void onPagerScrolled() {
        addEventView.hide();
    }
}
