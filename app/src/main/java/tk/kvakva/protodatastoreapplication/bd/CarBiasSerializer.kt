import android.content.Context
import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataStore
import androidx.datastore.core.Serializer
import androidx.datastore.dataStore
import com.google.protobuf.InvalidProtocolBufferException
import tk.kvakva.protodatastoreapplication.CarBias
import java.io.InputStream
import java.io.OutputStream

object CarBiasSerializer : Serializer<CarBias> {
    override val defaultValue: CarBias = CarBias.getDefaultInstance()

    override suspend fun readFrom(input: InputStream): CarBias {
        try {
            return CarBias.parseFrom(input)
        } catch (exception: InvalidProtocolBufferException) {
            throw CorruptionException("Cannot read proto.", exception)
        }
    }

    override suspend fun writeTo(
        t: CarBias,
        output: OutputStream
    ) = t.writeTo(output)
}

val Context.carBiasDataStore: DataStore<CarBias> by dataStore(
    fileName = "carbias.pb",
    serializer = CarBiasSerializer
)
