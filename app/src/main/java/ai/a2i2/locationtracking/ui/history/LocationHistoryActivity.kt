package ai.a2i2.locationtracking.ui.history

import ai.a2i2.locationtracking.R
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity

class LocationHistoryActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.location_history_activity)
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.container, LocationHistoryFragment.newInstance())
                .commitNow()
        }

        setTitle(R.string.your_locations)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            } else -> super.onOptionsItemSelected(item)
        }
    }
}