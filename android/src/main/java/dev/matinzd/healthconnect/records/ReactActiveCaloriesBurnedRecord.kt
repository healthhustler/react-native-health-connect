package dev.matinzd.healthconnect.records

import androidx.health.connect.client.aggregate.AggregationResult
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.response.ReadRecordsResponse
import androidx.health.connect.client.time.TimeRangeFilter
import androidx.health.connect.client.units.Energy
import com.facebook.react.bridge.ReadableArray
import com.facebook.react.bridge.ReadableMap
import com.facebook.react.bridge.WritableNativeArray
import com.facebook.react.bridge.WritableNativeMap
import dev.matinzd.healthconnect.utils.InvalidEnergy
import dev.matinzd.healthconnect.utils.convertMetadataToJSMap
import dev.matinzd.healthconnect.utils.convertReactRequestOptionsFromJS
import dev.matinzd.healthconnect.utils.toMapList
import java.time.Instant

class ReactActiveCaloriesBurnedRecord : ReactHealthRecordImpl<ActiveCaloriesBurnedRecord> {
  override fun parseWriteRecord(records: ReadableArray): List<ActiveCaloriesBurnedRecord> {
    return records.toMapList().map {
      ActiveCaloriesBurnedRecord(
        startTime = Instant.parse(it.getString("startTime")),
        endTime = Instant.parse(it.getString("endTime")),
        energy = getEnergyFromJsMap(it.getMap("energy")),
        endZoneOffset = null,
        startZoneOffset = null
      )
    }
  }

  override fun parseReadRequest(options: ReadableMap): ReadRecordsRequest<ActiveCaloriesBurnedRecord> {
    return convertReactRequestOptionsFromJS(ActiveCaloriesBurnedRecord::class, options)
  }

    override fun parseReadResponse(response: ReadRecordsResponse<out ActiveCaloriesBurnedRecord>): WritableNativeArray {
    return WritableNativeArray().apply {
      for (record in response.records) {
        val reactMap = WritableNativeMap().apply {
          putString("startTime", record.startTime.toString())
          putString("endTime", record.endTime.toString())
          putMap("energy", energyToJsMap(record.energy))
          putMap("metadata", convertMetadataToJSMap(record.metadata))
        }
        pushMap(reactMap)
      }
    }
  }

  override fun getAggregateRequest(record: ReadableMap): AggregateRequest {
    return AggregateRequest(
      metrics = setOf(ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL),
      timeRangeFilter = TimeRangeFilter.between(
        Instant.parse(record.getString("startTime")),
        Instant.parse(record.getString("endTime"))
      )
    )
  }

  override fun parseAggregationResult(record: AggregationResult): WritableNativeMap {
    return WritableNativeMap().apply {
      putDouble("inCalories", record[ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL]!!.inCalories)
      putDouble("inKilojoules", record[ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL]!!.inKilojoules)
      putDouble("inKilocalories", record[ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL]!!.inKilocalories)
      putDouble("inJoules", record[ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL]!!.inJoules)
    }
  }

  private fun getEnergyFromJsMap(energyMap: ReadableMap?): Energy {
    if (energyMap == null) {
      throw InvalidEnergy()
    }

    val value = energyMap.getDouble("value")
    return when (energyMap.getString("unit")) {
      "kilojoules" -> Energy.kilocalories(value)
      "kilocalories" -> Energy.kilojoules(value)
      "joules" -> Energy.joules(value)
      "calories" -> Energy.calories(value)
      else -> Energy.calories(value)
    }
  }

  private fun energyToJsMap(energy: Energy): WritableNativeMap {
    return WritableNativeMap().apply {
      putDouble("inCalories", energy.inCalories)
      putDouble("inJoules", energy.inJoules)
      putDouble("inKilocalories", energy.inKilocalories)
      putDouble("inKilojoules", energy.inKilojoules)
    }
  }
}
