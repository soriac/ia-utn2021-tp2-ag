package com.ia

import io.jenetics.engine.EvolutionResult
import java.util.function.Predicate

// copiado de SteadyFitnessLimit.java, es package local y no se puede extender
// (ademas el user guide te da el ejemplo y te dice que lo podes copiar)
class SteadyAndMinFitnessLimit<C : Comparable<C>?>(generations: Int, minimumFitness: C) :
    Predicate<EvolutionResult<*, C>> {
    private val _generations: Int
    private var _proceed = true
    private var _stable = 0
    private var _fitness: C? = null
    private val _minimumFitness: C
    override fun test(result: EvolutionResult<*, C>): Boolean {
        if (!_proceed) return false
        if (_fitness == null) {
            _fitness = result.bestFitness()
            _stable = 1
        } else {
            val opt = result.optimize()
            if (opt.compare(_fitness!!, result.bestFitness()) >= 0) {
                _proceed = (++_stable <= _generations) || (_fitness!! < _minimumFitness)
            } else {
                _fitness = result.bestFitness()
                _stable = 1
            }
        }
        return _proceed
    }

    init {
        require(generations >= 1) { "Generations < 1: $generations" }
        _generations = generations
        _minimumFitness = minimumFitness
    }
}
