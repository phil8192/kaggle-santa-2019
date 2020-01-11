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
		// less damaging moves have higher probability
		return Math.exp(-d / temperature);
	}

	private double localMinima(final double temperature, final int maxLoops) {
		double currentPenalty = getPenalty(assignments);
		double currentAccount = getAccountingCost();

		double current = currentPenalty + currentAccount;
		//double current = Math.abs(62868 - currentPenalty) + Math.abs(6020.043432 - currentAccount);
		//double current = Math.abs(62868 - currentPenalty) + currentAccount;
		//double current = currentPenalty + Math.abs(6020.043432 - currentAccount);

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
										//System.out.println(current);
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
//			if(i % 100000 == 0) {
//				// 10 million rounds of random brute force: do not go too far astray
//				final double diff3 = randomBrute(10000000,3 , 5, score);
//				if(diff3 < 0) {
//					score += diff3;
//				}
//			}
			if(i % 1000 == 0) {
				System.out.println(Thread.currentThread().getName() + " " + String.format("%.2f", score) + " T = " + ANSI_GREEN + String.format("%.6f", temperature) + ANSI_RESET + " I = " + i);
			}
			if (score < best) {
				best = score;
				sanity(assignments);
				System.out.println("**** new best = " + String.format("%.2f", best) + " **** T = " + temperature);
//				CsvUtil.write(assignments, "../../solutions/" + String.format("%.2f", score)  + "_sa.csv");
//				CsvUtil.write(assignments, "../../solutions/best.csv");
  				break;
			}
			temperature *= coolingSchedule;
		}
//		final double diff1 = brute(1, 5, best);
//		if(diff1 < 0) {
//			best += diff1;
//			CsvUtil.write(assignments, "../../solutions/" + String.format("%.2f", best)  + "_sa.csv");
//			CsvUtil.write(assignments, "../../solutions/best.csv");
//		}
//		final double diff2 = brute(2, 5, best);
//		if(diff2 < 0) {
//			best += diff2;
//			CsvUtil.write(assignments, "../../solutions/" + String.format("%.2f", best)  + "_sa.csv");
//			CsvUtil.write(assignments, "../../solutions/best.csv");
//		}
//		final double diff3 = randomBrute(100000000, 3 , 5, best);
//		if(diff3 < 0) {
//			best += diff3;
//			CsvUtil.write(assignments, "../../solutions/" + String.format("%.2f", best)  + "_sa.csv");
//			CsvUtil.write(assignments, "../../solutions/best.csv");
//		}
		return best;
	}

	double optimise2() {
		double best = cost(assignments);
		double current = best;
		//double currentPenalty = getPenalty(assignments);
		//double currentAccount = getAccountingCost();

		int x = 0;
		while (temperature > 0.7 && !Thread.currentThread().isInterrupted()) {
			final int start = prng.nextInt(assignments.length);
			fams: for(int k = 0; k < assignments.length; k++) {
				final int i = (k+start) % assignments.length;
				final int[] prefs = familyPrefs[i];
				final int famSize = familySize[i];
				final int assignedDay = assignments[i];
				final int assignedDayCap = dayCapacities[assignedDay];
				if (assignedDayCap - famSize >= MIN_PPL) {

					final int xx = prng.nextInt(10);
					for (int jj = 0; jj < 10; jj++) {
						final int j = (jj+xx)%10;
						final int candidateDay = prefs[j];
						if (candidateDay != assignedDay) {
							final int candidateDayCap = dayCapacities[candidateDay];
							if (candidateDayCap + famSize <= MAX_PPL) {

								final double penaltyDelta = getPenaltyDelta(i, assignedDay, candidateDay);
								final double accountDelta = getAccountingDelta(assignedDay, candidateDay, famSize);
								final double delta = penaltyDelta + accountDelta;
								final double candidateScore = current + delta;

								if (candidateScore < current
										|| acceptanceProbability(current, candidateScore, temperature) >= prng.nextDouble()) {

									dayCapacities[assignedDay] -= famSize;
									dayCapacities[candidateDay] += famSize;
									assignments[i] = candidateDay;

									current += delta;
									//currentPenalty += penaltyDelta;
									//currentAccount += accountDelta;

									if(candidateScore < best) {
										if(Math.abs(candidateScore-best) > 0.0001) {
											System.out.println(candidateScore + " < " + best);
											return candidateScore;
										}
									}

									break fams; // found a move.. adjust temp and move on
								}
							}
						}
					}
				}
			}
			temperature *= coolingSchedule;
			x++;
			if(x % 10000 == 0) {
				System.out.println(Thread.currentThread().getName() + " " + String.format("%.2f", current) + " T = " + ANSI_GREEN + String.format("%.6f", temperature) + ANSI_RESET + " I = " + x);
				current = localMinima(0, 0);
				//final double diff3 = randomBrute(1000000,4 , 5, current);
				//if(diff3 < 0) {
				//	current += diff3;
				//}

				if(current < best) {
					return current;
				}
			}
		}
		//return currentPenalty + currentAccount;
		return best;
	}

}
