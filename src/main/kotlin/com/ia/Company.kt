package com.ia

import io.jenetics.Genotype
import io.jenetics.IntegerGene
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import kotlin.math.min

const val minPackageSize = 5

@Serializable
class Company(
    val requested: Int,
    var availability: List<Int>,
    var assigned: List<Int>
) {
    fun invalidDayCount() = availability.zip(assigned).count { it.first < it.second }
    fun fixInvalidDays() {
        this.availability = availability.zip(assigned).map {
            min(it.first, it.second)
        }
    }
    fun indivisiblePackageCount() = assigned.count { it % minPackageSize != 0 }
    fun indivisiblePackageAmount() = assigned.fold(0) { acc, i ->  acc + (i % minPackageSize) }
    fun tripCount() = assigned.count { it > 0 }
    fun totalAssigned() = assigned.sum()

    companion object {
        private var baseCompanies: List<Company>? = null

        fun getBaseCompanies(): List<Company> {
            if (baseCompanies == null) readData()
            return baseCompanies!!
        }

        fun getCompanies(gt: Genotype<IntegerGene>): List<Company> {
            if (baseCompanies == null) readData()

            val companies = mutableListOf<Company>()

            gt.forEachIndexed { i, chromosome ->
                val base = baseCompanies!![i]
                val assigned = chromosome.map { it.allele() }
                companies.add(Company(base.requested, base.availability, assigned))
            }

            return companies
        }

        private fun readData() {
            File("company_data.json").bufferedReader().apply {
                val read = readText()
                baseCompanies = Json.decodeFromString(read)
                close()
            }
        }

        private fun generateData() {
            val companies = listOf(
                Company(50, listOf(10, 20, 30, 40, 50), listOf(0, 0, 0, 0, 0)),
                Company(100, listOf(20, 20, 20, 20, 20), listOf(0, 0, 0, 0, 0)),
                Company(75, listOf(0, 90, 0, 0, 90), listOf(0, 0, 0, 0, 0)),
                Company(50, listOf(10, 40, 10, 40, 10), listOf(0, 0, 0, 0, 0))
            )

            File("company_data.json").bufferedWriter().apply {
                val encoded = Json.encodeToString(companies)
                write(encoded)
                close()
            }
        }
    }
}
