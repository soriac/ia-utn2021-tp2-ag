package com.ia

import io.jenetics.*
import io.jenetics.engine.Engine
import io.jenetics.engine.EvolutionResult
import io.jenetics.engine.Limits
import io.jenetics.util.Factory
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
        println("Mejor genotipo: ${result.first}")

        RunOutcome(iterations, result, score)
    }

    val sortedBySpeed = result.sortedBy { it.iterations }
    val firstBest = sortedBySpeed.maxByOrNull { it.score }!!
    val best = sortedBySpeed.filter { it.score == firstBest.score }

    println("Hubieron ${best.size} resultado(s) con el valor de aptitud ${firstBest.score}, que fue el mas alto.")
    println("El resultado con con menor cantidad de iteraciones fue de ${firstBest.iterations}. El genotipo fue:")
    println(firstBest.result.first)
    println("Los datos de aptitud por iteración son:")
    println(firstBest.result.second)
}

val dispatchCenter = DispatchCenter.getDispatchCenter()
private fun eval(gt: Genotype<IntegerGene>): Int {
    var score = 0

    val companies = Company.getCompanies(gt)

    companies.forEach { company ->
        score -= company.invalidDayCount() * 10000

        val difference = company.totalAssigned() - company.requested
        score -= if (difference > 0) difference else -difference * 2

        score -= company.indivisiblePackageAmount() * 100

        score -= company.tripCount() * 10
    }

    for (i in 0 until companies[0].availability.size) {
        val dayLeftoverHours = dispatchCenter.leftoverHours(companies, i)
        if (dayLeftoverHours < 0) score -= ceil(dayLeftoverHours).toInt() * 1000
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
        .survivorsSelector(TournamentSelector())
        .alterers(GaussianMutator(), UniformCrossover())
        .maximizing()
        .build()

    val fitnesses = mutableListOf<Int>()

    val result = engine.stream()
        .limit(Limits.bySteadyFitness(10_000))
        .peek { result -> fitnesses.add(result.bestFitness()) }
        .collect(EvolutionResult.toBestGenotype())

    return Pair(result, fitnesses)
}

