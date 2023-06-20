package com.example.receiptskeeper.classes;

import android.view.View;

import java.util.Map;
import java.util.WeakHashMap;

public abstract class DebouncedOnClickListener implements View.OnClickListener {

    private final long minimumInterval;
    private final Map<View, Long> lastClickMap;

    /**
     * Implement this in your subclass instead of onClick
     * @param v The view that was clicked
     */
    public abstract void onDebouncedClick(View v);

    /**
     * The one and only constructor
     * @param minimumIntervalMsec The minimum allowed time between clicks - any click sooner than this after a previous click will be rejected
     */

    public DebouncedOnClickListener(long minimumIntervalMsec) {
        this.minimumInterval = minimumIntervalMsec;
        this.lastClickMap = new WeakHashMap<>();
    }

    @Override
    public void onClick(View clickedView)  {
        Long previousClickTimestamp = lastClickMap.get(clickedView);
        long currentTimestamp = System.currentTimeMillis();

        if (previousClickTimestamp == null || (currentTimestamp - previousClickTimestamp > minimumInterval)) {
            lastClickMap.put(clickedView, currentTimestamp);
            onDebouncedClick(clickedView);
        }
    }
}
