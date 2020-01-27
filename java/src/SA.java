import java.util.Random;

public class SA extends Optimiser {

	private double temperature; // = 3;
	private final double coolingSchedule;// = 0.9999999;

	SA(int[][] familyData, int[] initialAssignments, Random prng, double temperature, double coolingSchedule) {
		super(familyData, initialAssignments, null, prng);
		this.temperature = temperature;
		this.coolingSchedule = coolingSchedule;
	}

	private double acceptanceProbability(final double oldScore, final double newScore, final double temperature) {
		final double d = newScore - oldScore;
		if(d==0) return 0.01;
		// less damaging moves have higher probability
		return Math.exp(-d / temperature);
	}

	private double localMinima(final double temperature, final int maxLoops) {
		double currentPenalty = getPenalty(assignments);
		double currentAccount = getAccountingCost();

		double current = currentPenalty + currentAccount;

		boolean improvement;
		int max = maxLoops;
		do {
			improvement = false;
			//for (int i = 0; i < assignments.length; i++) { // each family i
			final int start = prng.nextInt(assignments.length);
			fams: for(int k = 0; k < assignments.length; k++) {
			//for(int i = assignments.length; --i >= 0; ) {
				final int i = (k+start) % assignments.length;
				final int[] prefs = familyPrefs[i];
				final int famSize = familySize[i];
				final int assignedDay = assignments[i];
				final int assignedDayCap = dayCapacities[assignedDay];
				if (assignedDayCap - famSize >= MIN_PPL) {
					for (int j = 0; j < 10; j++) {
						final int candidateDay = prefs[j];
						if (candidateDay != assignedDay) {
							final int candidateDayCap = dayCapacities[candidateDay];
							if (candidateDayCap + famSize <= MAX_PPL) {

								final double penaltyDelta = getPenaltyDelta(i, assignedDay, candidateDay);
								final double accountDelta = getAccountingDelta(assignedDay, candidateDay, famSize);
								final double delta = penaltyDelta + accountDelta;
								final double candidateScore = current + delta;

								if (candidateScore < current || (maxLoops > 0 &&
										acceptanceProbability(current, candidateScore, temperature) >= prng.nextDouble())) {
									dayCapacities[assignedDay] -= famSize;
									dayCapacities[candidateDay] += famSize;
									assignments[i] = candidateDay;

									current += delta;
									currentPenalty += penaltyDelta;
									currentAccount += accountDelta;

									if (delta < 0.0) {
										improvement = true;
										if (temperature == 0) break fams; // start from family 0 again
										else break;
										//break fams;
									} else {
										break; // family was moved, move on to next family
									}
								}
							}
						}
					}
				}
			}
		} while (improvement && (maxLoops == 0 || --max > 0));
		//return current;
		return currentPenalty + currentAccount;
	}

	double optimise() {
		//double best = localMinima(0, 0);
		double best = cost(assignments);
		//System.out.println("best = " + String.format("%.2f", best));
		int i = 0;
		while (temperature > 1 && !Thread.currentThread().isInterrupted()) {
			i++;
			localMinima(temperature, 1);
			double score = localMinima(0, 0);
//			if(i % 10000 == 0) {
//				// 10 million rounds of random brute force: do not go too far astray
//				final double diff3 = randomBrute(100000,3 , 5, score);
//				final double diff5 = randomBrute(100000,5 , 5, score);
//				if(diff3 < 0) {
//					score += diff3;
//				}
//				if(diff5 < 0) {
//					score += diff5;
//				}
//			}
			if(i % 1000 == 0) {
				System.out.println(Thread.currentThread().getName() + " " + String.format("%.2f", score) + " T = " + ANSI_GREEN + String.format("%.6f", temperature) + ANSI_RESET + " I = " + i);
			}
			if (score < best && Math.abs(score - best) > 0.00001) {
				best = score;
				sanity(assignments);
				System.out.println("**** new best = " + String.format("%.2f", best) + " **** T = " + temperature);
  				break;
			}
			temperature *= coolingSchedule;
		}
		return best;
	}

}
