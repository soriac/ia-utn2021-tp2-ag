import io.jenetics.Genotype
import io.jenetics.IntegerChromosome
import io.jenetics.IntegerGene
import io.jenetics.engine.Engine
import io.jenetics.engine.EvolutionResult
import io.jenetics.engine.Limits
import io.jenetics.util.Factory
import kotlin.math.abs

private fun eval(gt: Genotype<IntegerGene>): Int {
    var score = 0

    val companies = Company.getCompanies(gt)
    val dispatchCenter = DispatchCenter.getDispatchCenter()

    companies.forEach { company ->
        val assigned = company.totalAssigned()
        score -= company.invalidDayCount() * 100
        score -= abs(assigned - company.requested)
        score -= company.tripCount() * 10
        score -= company.indivisiblePackageCount()
    }

    for (i in 0 until companies[0].availability.size) {
        val dayLeftoverHours = dispatchCenter.leftoverHours(companies, i)
        if (dayLeftoverHours < 0) score -= 100
//        score -= dayLeftoverHours.toInt()
    }

    return score
}

fun main(args: Array<String>) {
    val chromosomes = ArrayList<IntegerChromosome>()
    val baseCompanies = Company.getBaseCompanies()

    val dayCount = baseCompanies[0].availability.size

    // creo un cromosoma por empresa
    // cada cromosoma tiene la cantidad de dias del experimento
    baseCompanies.forEach {
        // se puede settear el valor inicial de los genes con este código
        // val genes = it.availability.mapIndexed { index, availability ->
        //     IntegerGene.of(availability, 0, it.requested)
        // }

        // chromosomes.add(IntegerChromosome.of(genes))
        chromosomes.add(IntegerChromosome.of(0, it.requested, dayCount))
    }

    val gtf: Factory<Genotype<IntegerGene>> = Genotype.of(chromosomes)

    val engine = Engine
        .builder({ obj -> eval(obj) }, gtf)
        .maximizing()
        .build()

    val result = engine.stream()
        .limit(Limits.bySteadyFitness(10000))
        .collect(EvolutionResult.toBestGenotype())

    println("$result")
    println(eval(result))
}

