import io.jenetics.*;
import io.jenetics.engine.Engine;
import io.jenetics.engine.EvolutionResult;
import io.jenetics.engine.Limits;
import io.jenetics.util.Factory;

import java.util.ArrayList;

// FALTA CONTEMPLAR QUE NO LE ASIGNE MAS CEMENTO QUE LO QUE COMPRO
public class HelloWorld {
    static Integer[] availableCementKg = {
            100, 110, 100, 110, 100,
    };

    static Integer[][] companyAvailabilities = {
            {10, 20, 30, 40, 50},
            {20, 20, 20, 20, 20},
            { 0, 90,  0,  0, 90},
            {10, 40, 10, 40, 10}
    };

    private static Integer eval(Genotype<IntegerGene> gt) {
        int total = 0;

        for (double amount : availableCementKg) {
            total += amount;
        }

        for (int i = 0; i < gt.length(); i++) {
            Chromosome<IntegerGene> chromosome = gt.get(i);
            double available = availableCementKg[i];

            // estar lo mas cerca posible al total de ese centro de despacho
            double sum = 0;
            for (IntegerGene gene : chromosome) {
                sum += gene.allele();
            }

            if (sum > available) total -= 999999.0;
            total += (sum - available);

            // estar lo mas cerca posible al maximo de cada empresa
            for (int j = 0; j < chromosome.length(); j++) {
                IntegerGene gene = chromosome.get(j);

                if (gene.allele() > companyAvailabilities[j][i]) {
                    total -= 9999.0;
                    total += (gene.allele() - companyAvailabilities[j][i]);
                }
            }
        }

        return total;
    }

    public static void main(String[] args) {
        ArrayList<IntegerChromosome> chromosomes = new ArrayList<>();

        for (int i = 0; i < 5; i++) {
            chromosomes.add(IntegerChromosome.of(0, 110, companyAvailabilities.length));
        }

        Factory<Genotype<IntegerGene>> gtf =
                Genotype.of(chromosomes);

        Engine<IntegerGene, Integer> engine = Engine
                .builder(HelloWorld::eval, gtf)
                .maximizing()
                .build();

        Genotype<IntegerGene> result = engine.stream()
                .limit(Limits.bySteadyFitness(10000))
                .collect(EvolutionResult.toBestGenotype());

        System.out.println("Hello World:\n" + result);
        System.out.println(eval(result));
    }
}
