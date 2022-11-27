package tk.kvakva.protodatastoreapplication

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import carBiasDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.util.concurrent.Executors

enum class LeftRight {
    LEFT,
    RIGHT
}

enum class StartedStopped {
    STARTED,
    STOPPED,
    PAUSED
}
var executor = Executors.newSingleThreadScheduledExecutor()



class MainActivityViewModel(val appl: Application) : AndroidViewModel(appl) {

    val startedStopped = MutableLiveData(StartedStopped.STOPPED)

    val lifes = MutableLiveData(3)

    val carBiasFlow: Flow<Float> = appl.carBiasDataStore.data
        .map { carbias ->
            // The exampleCounter property is generated from the proto schema.
            carbias.carBias
        }.stateIn(viewModelScope, SharingStarted.Eagerly, 0.5f)

    suspend fun sussetBias(lr: LeftRight, delta: Float) {
        appl.carBiasDataStore.updateData { currentSettings ->
            val builder = currentSettings.toBuilder()
            Log.v(TAG, "sussetBias lr=$lr,  delta=$delta")
            var bias = currentSettings.carBias
            when (lr) {
                LeftRight.LEFT -> {
                    if (bias > 0f) {
                        bias -= delta
                    }
                    if (bias < 0f) {
                        bias = 0f
                    }
                }
                LeftRight.RIGHT -> {
                    if (1f > bias) {
                        bias += delta
                    }
                    if (bias > 1f) {
                        bias = 1f
                    }
                }
            }
            builder.setCarBias(bias)
                .build()
        }
    }

    var top = 0f
    var bot = 0f
}

private const val TAG = "MainActivityViewModel"