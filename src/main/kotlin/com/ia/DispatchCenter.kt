package com.ia

import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

@Serializable
data class DispatchCenter(
    val stock: List<Int>,
    val dispatchOverheadFlat: Double = 0.5,
    val dispatchOverheadPerTon: Double = 0.02,
    val dailyWorkHours: Double = 100.0,
) {
    fun calculateTotalWorkedHours(companies: List<Company>): Double {
        return companies.fold(0.0) { acc, company ->
            acc + company.assigned.fold(0.0) { acc, it ->
                if (it > 0) acc + dispatchOverheadFlat + it * dispatchOverheadPerTon
                else acc
            }
        }
    }

    fun calculateWorkedHours(companies: List<Company>, day: Int): Double {
        return companies.fold(0.0) { acc, company ->
            val assignment = company.assigned[day]
            if (assignment > 0) acc + dispatchOverheadFlat + assignment * dispatchOverheadPerTon
            else acc
        }
    }

    fun leftoverHours(companies: List<Company>, day: Int): Double =
        dailyWorkHours - calculateWorkedHours(companies, day)

    companion object {
        private var baseDispatchCenter: DispatchCenter? = null

        fun getDispatchCenter(): DispatchCenter {
            if (baseDispatchCenter == null) readData()
            return baseDispatchCenter!!
        }

        fun readData() {
            File("dispatch_center_data.json").bufferedReader().apply {
                val read = readText()
                baseDispatchCenter = Json.decodeFromString(read)
                close()
            }
        }

        fun generateData() {
            File("dispatch_center_data.json").bufferedWriter().apply {
                val encoded = Json.encodeToString(baseDispatchCenter)
                write(encoded)
                close()
            }
        }
    }
}
