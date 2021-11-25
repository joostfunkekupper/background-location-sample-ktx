package ai.a2i2.locationtracking.ui

import ai.a2i2.locationtracking.R
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

/**
 * This activity is (currently) not accessible from anywhere in the app. Instead it is accessed
 * through the Privacy Dashboard in Android 12. Each location list item has an information icon
 * which can be pressed to open this Activity.
 * See https://developer.android.com/training/permissions/explaining-access#privacy-dashboard
 */
class LocationRationaleActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_location_rationale)

        setTitle(R.string.location_rationale_long_title)
    }
}