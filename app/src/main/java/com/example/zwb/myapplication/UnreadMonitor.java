package com.example.zwb.myapplication;

import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.util.Log;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by zwb on 17-12-22.
 */

public class UnreadMonitor extends ContentObserver {
    private static final String TAG = "UnreadMonitor";

    public static class BadgeCount {
        private int mCount;

        public BadgeCount(int count) {
            mCount = count;
        }

        public int getBadgeCount() {
            return mCount;
        }
    }

    public static final Uri AUTHORITY_URI = Uri.parse("content://com.freeme.badge");

    public static final Uri BADGE_URI
            = Uri.withAppendedPath(AUTHORITY_URI, "apps");
    public static final Uri BADGE_INTERNAL_URI
            = Uri.withAppendedPath(AUTHORITY_URI, "internal");
    private static final String[] BADGE_COLUMNS = {
            "package",
            "class",
            "badgecount"
    };

    private static final int INITIAL_BADGE_CAPACITY = 20;
    private final Map<ComponentName, BadgeCount> mBadges = new HashMap<>(INITIAL_BADGE_CAPACITY);

    private final ContentResolver mResolver;
    private boolean mRegistered;

    private final BadgeUpdateListener mBadgeListener;

    public UnreadMonitor(Context context, Handler handler, BadgeUpdateListener listener) {
        super(handler);
        mResolver = context.getContentResolver();
        mBadgeListener = listener;
    }

    public interface BadgeUpdateListener {
        void onBadgeUpdated(Map<ComponentName, BadgeCount> badge);
    }

    @Override
    public void onChange(boolean selfChange) {
        onChange(selfChange, null);
    }

    @Override
    public void onChange(boolean selfChange, Uri uri) {
        Map<ComponentName, BadgeCount> badge = updateBadgeCount();
        if (mBadgeListener != null) {
            mBadgeListener.onBadgeUpdated(badge);
        }
    }

    public UnreadMonitor start() {
        if (!mRegistered) {
            final Uri[] uri = new Uri[] { BADGE_URI, BADGE_INTERNAL_URI };
            final int N= uri.length;
            for (int i = 0; i < N; i++) {
                mResolver.registerContentObserver(uri[i], true, this);
            }
            mRegistered = true;
        }
        onChange(false, null);
        return this;
    }

    public void stop() {
        if (mRegistered) {
            mResolver.unregisterContentObserver(this);
            mRegistered = false;
        }
    }

    public Map<ComponentName, BadgeCount> getBadgeCount() {
        return Collections.unmodifiableMap(mBadges);
    }

    public int getBadgeCount(ComponentName cn) {
        BadgeCount b = mBadges.get(cn);
        return b != null ? b.getBadgeCount() : 0;
    }

    public Map<ComponentName, BadgeCount> updateBadgeCount() {
        Cursor c = mResolver.query(BADGE_URI, BADGE_COLUMNS, null, null, null);
        if (c != null) try {
            while (c.moveToNext()) {
                String packageName = c.getString(0);
                String className = c.getString(1);
                int count = c.getInt(2);
                if (packageName != null && className != null) {
                    ComponentName cn = new ComponentName(packageName, className);
                    mBadges.put(cn, new BadgeCount(count));
                    Log.v(TAG, "updateBadgeCount, app[" + cn + "] have " + count + " badges");
                }
            }
        } finally {
            c.close();
        }
        return Collections.unmodifiableMap(mBadges);
    }

}
