package org.gnucash.android.ui.syncable

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle

import org.gnucash.android.R

class SyncableLinkActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_syncable_link)
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.container, SyncableLinkFragment.newInstance())
                .commitNow()
        }
    }

}