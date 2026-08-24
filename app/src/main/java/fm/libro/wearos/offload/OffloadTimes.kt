package fm.libro.wearos.offload

import java.text.NumberFormat

data class OffloadTimes(
    val enabled: Long = 0L,
    val disabled: Long = 0L,
    val notPlaying: Long = 0L,
    val isPlaying: Boolean = false,
    val updated: Long = System.currentTimeMillis(),
) {
    val shortDescription: String
        get() = "$enabled/$disabled/$isPlaying"

    val percent: String
        get() {
            val value = enabled.toFloat() / (enabled + disabled)
            return if (value.isNaN()) "--%" else PercentFormat.format(value)
        }

    fun timesToNow(sleepingForOffload: Boolean, updatedIsPlaying: Boolean): OffloadTimes {
        val time = System.currentTimeMillis()
        val extra = time - updated

        return if (isPlaying) {
            copy(
                enabled = enabled + (if (sleepingForOffload) extra else 0),
                disabled = disabled + (if (sleepingForOffload) 0 else extra),
                updated = time,
                isPlaying = updatedIsPlaying,
            )
        } else {
            copy(
                notPlaying = notPlaying + extra,
                updated = time,
                isPlaying = updatedIsPlaying,
            )
        }
    }

    private companion object {
        val PercentFormat: NumberFormat = NumberFormat.getPercentInstance()
    }
}
