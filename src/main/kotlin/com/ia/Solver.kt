package com.ia

import io.jenetics.*
import io.jenetics.engine.Engine
import io.jenetics.engine.EvolutionResult
import io.jenetics.util.Factory
import kotlin.math.abs
import kotlin.math.ceil

const val runCount = 10

data class RunOutcome(var iterations: Long, var result: Pair<Genotype<IntegerGene>, MutableList<Int>>, var score: Int)

fun main() {
    val result = (1..runCount).map {
        val result = run()
        val score = eval(result.first)
        val iterations = result.second.size.toLong()

        println("Resultado obtenido en $iterations iteraciones.")
        println("Aptitud: $score")
        println("Mejor genotipo: ${result.first}\n")

        RunOutcome(iterations, result, score)
    }

    val sortedBySpeed = result.sortedBy { it.iterations }
    val firstBest = sortedBySpeed.maxByOrNull { it.score }!!
    val best = sortedBySpeed.filter { it.score == firstBest.score }

    println("La cantidad promedio de iteraciones fueron ${sortedBySpeed.map {it.iterations}.average()} y la máxima fue de ${sortedBySpeed.maxByOrNull { it.iterations }!!.iterations}")
    println("Hubieron ${best.size} resultado(s) con el valor de aptitud ${firstBest.score}, que fue el mas alto.")
    println("La menor cantidad de iteraciones para llegar al mejor resultado fue de ${firstBest.iterations}. El genotipo fue:")
    println(firstBest.result.first)
    println("\nLos datos de aptitud por iteración son:")
    println(firstBest.result.second)
}

val dispatchCenter = DispatchCenter.getDispatchCenter()
private fun eval(gt: Genotype<IntegerGene>): Int {
    var score = 0

    val companies = Company.getCompanies(gt)

    companies.forEach { company ->
        score -= company.invalidDayCount() * 100000

        val difference = company.totalAssigned() - company.requested
        score -= abs(difference)

        score -= company.indivisiblePackageAmount() * 100

        score -= company.tripCount() * 10
    }

    for (i in 0 until companies[0].availability.size) {
        val dayLeftoverHours = dispatchCenter.leftoverHours(companies, i)
        if (dayLeftoverHours < 0) score -= ceil(dayLeftoverHours).toInt() * 10000
    }

    return score
}

fun run(): Pair<Genotype<IntegerGene>, MutableList<Int>> {
    val chromosomes = ArrayList<IntegerChromosome>()
    val baseCompanies = Company.getBaseCompanies()


    // creo un cromosoma por empresa
    // cada cromosoma tiene la cantidad de dias del experimento
    baseCompanies.forEach {
        val max = it.availability.maxOrNull()!!
        val genes = it.availability.map { availability ->
            IntegerGene.of(availability, 0, max)
        }

        chromosomes.add(IntegerChromosome.of(genes))
    }

    val gtf: Factory<Genotype<IntegerGene>> = Genotype.of(chromosomes)

    val engine = Engine
        .builder(::eval, gtf)
        .executor(Runnable::run)
        .survivorsSelector(RouletteWheelSelector())
        .alterers(GaussianMutator(0.2), UniformCrossover(0.2, 0.5))
        .maximizing()
        .build()

    val fitnesses = mutableListOf<Int>()

    val result = engine.stream()
        .limit(SteadyAndMinFitnessLimit(10_000, -10_000))
        .peek { result -> fitnesses.add(result.bestFitness()) }
        .collect(EvolutionResult.toBestGenotype())

    return Pair(result, fitnesses)
}
