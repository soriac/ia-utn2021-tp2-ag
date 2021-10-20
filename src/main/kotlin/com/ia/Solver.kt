package com.ia

import io.jenetics.*
import io.jenetics.engine.Engine
import io.jenetics.engine.EvolutionResult
import io.jenetics.util.Factory
import io.jenetics.util.RandomRegistry
import java.io.File
import java.util.*
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.Future
import kotlin.collections.ArrayList
import kotlin.math.abs
import kotlin.math.ceil

const val runCount = 10
val experiments = listOf(
    Experiment("01", MonteCarloSelector(), GaussianMutator(0.2), UniformCrossover(0.2, 0.2)),
    Experiment("02", TournamentSelector(), GaussianMutator(0.2), UniformCrossover(0.2, 0.2)),
    Experiment("03", RouletteWheelSelector(), GaussianMutator(0.2), UniformCrossover(0.2, 0.2)),
    Experiment("04", LinearRankSelector(), GaussianMutator(0.2), UniformCrossover(0.2, 0.2)),
    Experiment("05", RouletteWheelSelector(), GaussianMutator(0.2), UniformCrossover(0.2, 0.5)),
    Experiment("06", RouletteWheelSelector(), GaussianMutator(0.2), UniformCrossover(0.2, 0.9)),
    Experiment("07", RouletteWheelSelector(), GaussianMutator(0.2), UniformCrossover(0.5, 0.5)),
    Experiment("08", RouletteWheelSelector(), GaussianMutator(0.2), UniformCrossover(0.1, 0.5)),
    Experiment("09", RouletteWheelSelector(), GaussianMutator(0.2), UniformCrossover(0.01, 0.5)),
    Experiment("10", RouletteWheelSelector(), GaussianMutator(0.5), UniformCrossover(0.2, 0.5)),
    Experiment("11", RouletteWheelSelector(), GaussianMutator(0.1), UniformCrossover(0.2, 0.5))
)

fun main() {
    val executor = Executors.newFixedThreadPool(8)

    val outputs = executor.invokeAll(
        experiments.map { e ->
            Callable {
                val outcome = experiment(e)
                "${e.name}\t${outcome.worstIterations}\t${outcome.averageIterations}\t${outcome.bestCount.toDouble() / runCount.toDouble() * 100.0}%\n"
            }
        }
    )

    File("output/final.txt").bufferedWriter().apply {
        outputs.forEach { write(it.get().toString()) }
        close()
    }

    executor.shutdown()
}


data class Experiment(
    val name: String,
    val selector: Selector<IntegerGene, Int>,
    val mutator: Mutator<IntegerGene, Int>,
    val crossover: Crossover<IntegerGene, Int>
)

data class ExperimentOutcome(
    val worstIterations: Long,
    val averageIterations: Double,
    val bestCount: Int
)

data class RunOutcome(var iterations: Long, var result: Pair<Genotype<IntegerGene>, MutableList<Int>>, var score: Int)

fun experiment(experiment: Experiment): ExperimentOutcome {
    val (name, selector, mutator, crossover) = experiment;
    val writer = File("output/$name.txt").bufferedWriter()

    println("$name - Starting experiment")
    val result = (1..runCount).map {
        println("$name - Starting run $it.")
        val random: ThreadLocal<Random> = object : ThreadLocal<Random>() {
            override fun initialValue(): Random {
                return Random(it.toLong())
            }
        }

        val result = run(random, selector, mutator, crossover)
        val score = eval(result.first)
        val iterations = result.second.size.toLong()

        writer.write("Resultado obtenido en $iterations iteraciones.\n")
        writer.write("Aptitud: $score\n")
        writer.write("Mejor genotipo: ${result.first}\n\n")

        RunOutcome(iterations, result, score)
    }

    val sortedBySpeed = result.sortedBy { it.iterations }
    val firstBest = sortedBySpeed.maxByOrNull { it.score }!!
    val best = sortedBySpeed.filter { it.score == firstBest.score }

    val averageIterations = sortedBySpeed.map { it.iterations }.average()
    val maxIterations = sortedBySpeed.maxByOrNull { it.iterations }!!.iterations

    writer.write("La cantidad promedio de iteraciones fueron $averageIterations y la máxima fue de $maxIterations\n")
    writer.write("Hubieron ${best.size} resultado(s) con el valor de aptitud ${firstBest.score}, que fue el mas alto.\n")
    writer.write("La menor cantidad de iteraciones para llegar al mejor resultado fue de ${firstBest.iterations}. El genotipo fue:\n")
    writer.write(firstBest.result.first.toString())
    writer.write("\nLos datos de aptitud por iteración son:\n")
    firstBest.result.second.forEach {
        writer.write("$it\n")
    }

    writer.close()

    return ExperimentOutcome(maxIterations, averageIterations, best.size)
}

val dispatchCenter = DispatchCenter.getDispatchCenter()
private fun eval(gt: Genotype<IntegerGene>): Int {
    var score = 0

    // convierto el genotipo a una lista de compañías
    val companies = Company.getCompanies(gt)

    for (i in 0 until companies[0].availability.size) {
        /* 0. penalizar muy fuertemente por cada día donde el centro de despacho tiene que trabajar mas horas que
            su máximo de horas disponibles */
        val dayLeftoverHours = dispatchCenter.leftoverHours(companies, i)
        if (dayLeftoverHours < 0) score -= ceil(dayLeftoverHours).toInt() * 10000
    }

    companies.forEach { company ->
        // 1. penalizar muy fuertemente por cada día que se asignó más cemento que su disponibilidad de transporte
        score -= company.invalidDayCount() * 100000

        // 2. penalizar fuertemente por cada paquete que no sea divisible por 5
        score -= company.indivisiblePackageAmount() * 100

        // 3. penalizar por la cantidad total de viajes realizados
        score -= company.tripCount() * 10

        // 4. penalizar por la diferencia entre la cantidad asignada y disponibilidad
        val difference = company.totalAssigned() - company.requested
        score -= abs(difference)
    }

    return score
}

fun run(
    random: ThreadLocal<Random>,
    selector: Selector<IntegerGene, Int>,
    mutator: Mutator<IntegerGene, Int>,
    crossover: Crossover<IntegerGene, Int>
): Pair<Genotype<IntegerGene>, MutableList<Int>> {

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
        .survivorsSelector(selector)
        .alterers(mutator, crossover)
        .maximizing()
        .build()

    val fitnesses = mutableListOf<Int>()

    val result = RandomRegistry.with(random) {
        engine.stream()
            .limit(SteadyAndMinFitnessLimit(10_000, -10_000))
            .peek { result -> fitnesses.add(result.bestFitness()) }
            .collect(EvolutionResult.toBestGenotype())
    }

    return Pair(result, fitnesses)
}
