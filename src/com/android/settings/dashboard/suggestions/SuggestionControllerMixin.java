/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.dashboard.suggestions;

import android.app.LoaderManager;
import android.content.Context;
import android.content.Loader;
import android.os.Bundle;
import android.service.settings.suggestions.Suggestion;
import android.util.Log;

import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnStart;
import com.android.settingslib.core.lifecycle.events.OnStop;

import java.util.List;

/**
 * Manages IPC communication to SettingsIntelligence for suggestion related services.
 */
public class SuggestionControllerMixin implements SuggestionController.ServiceConnectionListener,
        LifecycleObserver, OnStart, OnStop, LoaderManager.LoaderCallbacks<List<Suggestion>> {

    public interface SuggestionControllerHost {
        /**
         * Called when suggestion data fetching is ready.
         */
        void onSuggestionReady(List<Suggestion> data);

        /**
         * Returns {@link LoaderManager} associated with the host.
         */
        LoaderManager getLoaderManager();
    }

    private static final String TAG = "SuggestionCtrlMixin";
    private static final boolean DEBUG = false;

    private final Context mContext;
    private final SuggestionController mSuggestionController;
    private final SuggestionControllerHost mHost;

    public SuggestionControllerMixin(Context context, SuggestionControllerHost host,
            Lifecycle lifecycle) {
        mContext = context.getApplicationContext();
        mHost = host;
        mSuggestionController = new SuggestionController(mContext,
                FeatureFactory.getFactory(mContext)
                        .getSuggestionFeatureProvider(mContext)
                        .getSuggestionServiceComponent(),
                this /* serviceConnectionListener */);
        if (lifecycle != null) {
            lifecycle.addObserver(this);
        }
    }

    @Override
    public void onStart() {
        mSuggestionController.start();
    }

    @Override
    public void onStop() {
        mSuggestionController.stop();
    }

    @Override
    public void onServiceConnected() {
        mHost.getLoaderManager().restartLoader(SuggestionLoader.LOADER_ID_SUGGESTIONS,
                null /* args */, this /* callback */);
    }

    @Override
    public void onServiceDisconnected() {
        if (DEBUG) {
            Log.d(TAG, "SuggestionService disconnected");
        }
        mHost.getLoaderManager().destroyLoader(SuggestionLoader.LOADER_ID_SUGGESTIONS);
    }

    @Override
    public Loader<List<Suggestion>> onCreateLoader(int id, Bundle args) {
        if (id == SuggestionLoader.LOADER_ID_SUGGESTIONS) {
            return new SuggestionLoader(mContext, mSuggestionController);
        }
        throw new IllegalArgumentException("This loader id is not supported " + id);
    }

    @Override
    public void onLoadFinished(Loader<List<Suggestion>> loader, List<Suggestion> data) {
        mHost.onSuggestionReady(data);
    }

    @Override
    public void onLoaderReset(Loader<List<Suggestion>> loader) {

    }
}
