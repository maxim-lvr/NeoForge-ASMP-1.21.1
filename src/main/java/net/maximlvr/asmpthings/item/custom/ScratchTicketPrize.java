package net.maximlvr.asmpthings.item.custom;

import net.minecraft.util.RandomSource;

public final class ScratchTicketPrize {
    public static final int LOST = 0;
    public static final int TWO_COINS = 2;
    public static final int FIVE_COINS = 5;
    public static final int TEN_COINS = 10;
    public static final int CHIENGUE = 11;
    public static final int KOMBUCIAO = 12;
    public static final int BOUTEILLE = 13;
    public static final int DIAMOND_BLOCK = 21;
    public static final int IGNITIUM_BLOCK = 22;
    public static final int DISC = 23;
    public static final int BOUEE = 31;
    public static final int CARDS = 32;
    public static final int PELUCHE = 33;
    public static final int STACK_COINS = 64;

    private static final int TOTAL_WEIGHT = 10_000;

    private static final PrizeWeight[] PRIZE_WEIGHTS = {
            new PrizeWeight(LOST, 5_000),
            new PrizeWeight(STACK_COINS, 70),
            new PrizeWeight(TEN_COINS, 100),
            new PrizeWeight(BOUEE, 175),
            new PrizeWeight(CARDS, 175),
            new PrizeWeight(PELUCHE, 175),
            new PrizeWeight(FIVE_COINS, 250),
            new PrizeWeight(DIAMOND_BLOCK, 334),
            new PrizeWeight(IGNITIUM_BLOCK, 333),
            new PrizeWeight(DISC, 333),
            new PrizeWeight(CHIENGUE, 686),
            new PrizeWeight(KOMBUCIAO, 685),
            new PrizeWeight(TWO_COINS, 1_000),
            new PrizeWeight(BOUTEILLE, 685)
    };

    private ScratchTicketPrize() {
    }

    public static int generate(RandomSource random) {
        int roll = random.nextInt(TOTAL_WEIGHT);
        int cursor = 0;

        for (PrizeWeight prizeWeight : PRIZE_WEIGHTS) {
            cursor += prizeWeight.weight();

            if (roll < cursor) {
                return prizeWeight.prize();
            }
        }

        return LOST;
    }

    public static void simulate(RandomSource random, int generations) {
        int[] counts = new int[65];

        for (int i = 0; i < generations; i++) {
            int prize = generate(random);
            counts[prize]++;
        }

        printSimulationResult("Perdu", LOST, counts, generations);
        printSimulationResult("2 CrazyCoins", TWO_COINS, counts, generations);
        printSimulationResult("5 CrazyCoins", FIVE_COINS, counts, generations);
        printSimulationResult("10 CrazyCoins", TEN_COINS, counts, generations);
        printSimulationResult("Chiengue", CHIENGUE, counts, generations);
        printSimulationResult("Kombuciao", KOMBUCIAO, counts, generations);
        printSimulationResult("Bouteille", BOUTEILLE, counts, generations);
        printSimulationResult("Bloc de diamant", DIAMOND_BLOCK, counts, generations);
        printSimulationResult("Bloc d'Ignitium", IGNITIUM_BLOCK, counts, generations);
        printSimulationResult("Disque", DISC, counts, generations);
        printSimulationResult("Bouée", BOUEE, counts, generations);
        printSimulationResult("Cartes", CARDS, counts, generations);
        printSimulationResult("Peluche", PELUCHE, counts, generations);
        printSimulationResult("64 CrazyCoins", STACK_COINS, counts, generations);
    }

    private static void printSimulationResult(
            String name,
            int prize,
            int[] counts,
            int generations
    ) {
        int count = counts[prize];
        double percentage = count * 100.0 / generations;

        System.out.printf(
                "%-20s : %8d fois | %8.4f %% | environ 1 sur %.2f%n",
                name,
                count,
                percentage,
                count == 0 ? 0.0 : (double) generations / count
        );
    }

    private record PrizeWeight(int prize, int weight) {
    }
}
