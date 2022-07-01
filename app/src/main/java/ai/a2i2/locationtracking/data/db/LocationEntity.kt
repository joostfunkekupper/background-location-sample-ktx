package ai.a2i2.locationtracking.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.text.DateFormat
import java.util.*

@Entity(tableName = "location")
data class LocationEntity(
    @PrimaryKey var id: UUID = UUID.randomUUID(),
    var latitude: Double = 0.0,
    var longitude: Double = 0.0,
    var foreground: Boolean = true,
    var recordedAt: Date = Date()
) {
    override fun toString(): String {
        val appState = if (foreground) {
            "in app"
        } else {
            "in BG"
        }

        return "$latitude, $longitude $appState on " +
                "${DateFormat.getDateTimeInstance().format(recordedAt)}.\n"
    }
}