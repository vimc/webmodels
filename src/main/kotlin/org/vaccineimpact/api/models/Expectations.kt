package org.vaccineimpact.api.models

import org.vaccineimpact.api.models.helpers.FlexibleProperty
import kotlin.coroutines.experimental.buildSequence

data class OutcomeExpectations(val id: Int,
                               val description: String,
                               val years: IntRange,
                               val ages: IntRange,
                               val cohorts: CohortRestriction,
                               val outcomes: List<String>)

data class Expectations(
        val id: Int,
        val description: String,
        val years: IntRange,
        val ages: IntRange,
        val cohorts: CohortRestriction,
        val countries: List<Country>,
        val outcomes: List<String>)
{
    private fun Int.withinCohortRange(): Boolean
    {
        return (cohorts.minimumBirthYear == null || this >= cohorts.minimumBirthYear)
                && (cohorts.maximumBirthYear == null || this <= cohorts.maximumBirthYear)
    }

    data class RowDeterminer(val year: Int, val age: Int, val country: Country)

    fun expectedCentralRows(disease: String) = expectedRows()
            .map { mapCentralRow(disease, it.year, it.age, it.country) }

    fun expectedStochasticRows(disease: String) = expectedRows()
            .map { mapStochasticRow(disease, it.year, it.age, it.country) }

    private fun expectedRows(): Sequence<RowDeterminer> = buildSequence {
        for (age in ages)
        {
            for (country in countries)
            {
                yieldAll(years
                        .filter { (it - age).withinCohortRange() }
                        .map { RowDeterminer(it, age, country) })

            }
        }
    }

    fun expectedRowLookup(): RowLookup
    {
        val map = RowLookup()
        for (country in countries)
        {
            val ageMap = AgeLookup()
            for (age in ages)
            {
                val yearMap = YearLookup()
                years.map { if ((it - age).withinCohortRange()) yearMap[it.toShort()] = false }
                ageMap[age.toShort()] = yearMap
            }
            map[country.id] = ageMap
        }
        return map
    }

    private fun outcomesMap() = outcomes.associateBy({ it }, { null })

    private fun mapCentralRow(disease: String, year: Int, age: Int, country: Country): ExpectedCentralRow
    {
        return ExpectedCentralRow(disease, year, age, country.id, country.name, null, outcomesMap())
    }

    private fun mapStochasticRow(disease: String, year: Int, age: Int, country: Country): ExpectedStochasticRow
    {
        return ExpectedStochasticRow(disease, null, year, age, country.id, country.name, null, outcomesMap())
    }

}

data class CohortRestriction(
        val minimumBirthYear: Short? = null,
        val maximumBirthYear: Short? = null
)

data class ExpectedCentralRow(
        val disease: String,
        val year: Int,
        val age: Int,
        val country: String,
        val countryName: String,
        val cohortSize: Float?,
        @FlexibleProperty
        val outcomes: Map<String, Float?>
) : ExpectedRow

data class ExpectedStochasticRow(
        val disease: String,
        val runId: Int?,
        val year: Int,
        val age: Int,
        val country: String,
        val countryName: String,
        val cohortSize: Float?,
        @FlexibleProperty
        val outcomes: Map<String, Float?>
) : ExpectedRow

interface ExpectedRow

typealias YearLookup = HashMap<Short, Boolean>
typealias Year = Map.Entry<Short, Boolean>
typealias AgeLookup = HashMap<Short, YearLookup>
typealias YearsForAge = Map.Entry<Short, YearLookup>
typealias RowLookup = HashMap<String, AgeLookup>
typealias AgesForCountry = Map.Entry<String, AgeLookup>

fun Year.isMissing(): Boolean
{
    return !this.value
}

fun YearsForAge.hasMissingYear(): Boolean
{
    return this.value.any { it.isMissing() }
}

fun AgesForCountry.hasMissingAges(): Boolean
{
    return this.value.any { it.hasMissingYear() }
}

fun AgeLookup.firstAgeWithMissingRows(): Short {
    return this.filter { it.hasMissingYear() }.keys.first()
}

fun YearLookup.firstMissingYear(): Short {
    return this.filter { it.isMissing() }.keys.first()
}
