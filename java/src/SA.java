public class SA extends Optimiser {

	SA(int[][] familyData, int[] initialAssignments) {
		super(familyData, initialAssignments, null);
	}

	private double acceptanceProbability(final double oldScore, final double newScore, final double temperature) {
		final double d = newScore - oldScore;
		// less damaging moves have higher probability
		return Math.exp(-d / temperature);
	}

	private double localMinima(final double temperature, final int maxLoops) {
		double currentPenalty = getPenalty(assignments);
		double currentAccount = getAccountingCost();

		//double current = currentPenalty + currentAccount;
		//double current = Math.abs(62868 - currentPenalty) + Math.abs(6020.043432 - currentAccount);
		double current = Math.abs(62868 - currentPenalty) + currentAccount;

		boolean improvement;
		int max = maxLoops;
		do {
			improvement = false;
			fams:
			for (int i = 0; i < assignments.length; i++) { // each family i
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


								//final double delta = penaltyDelta + accountDelta;
								//final double delta = (Math.abs(62868 - (currentPenalty + penaltyDelta)) + Math.abs(6020.043432 - (currentAccount + accountDelta))) - current;
								final double delta = (Math.abs(62868 - (currentPenalty + penaltyDelta)) + (currentAccount + accountDelta)) - current;

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
		double temperature = 3.5;
		double coolingSchedule = 0.999999;
		//double best = localMinima(0, 0);
		double best = cost(assignments);
		System.out.println("best = " + String.format("%.2f", best));
		int i = 0;
		while (temperature > 0.2) {
			i++;
			localMinima(temperature, 1);
			double score = localMinima(0, 0);
			if(i % 100000 == -1) {
				// 10 million rounds of random brute force: do not go too far astray
				final double diff3 = randomBrute(10000000,3 , 5, score);
				if(diff3 < 0) {
					score += diff3;
				}
			}
			System.out.println(String.format("%.2f", score) + " T = " + ANSI_GREEN + String.format("%.6f", temperature) + ANSI_RESET + " I = " + i);
			if (score < best) {
				best = score;
				sanity(assignments);
				System.out.println("**** new best = " + String.format("%.2f", best) + " **** T = " + temperature);
				CsvUtil.write(assignments, "../../solutions/" + String.format("%.2f", score)  + "_sa.csv");
				CsvUtil.write(assignments, "../../solutions/best.csv");
			}
			temperature *= coolingSchedule;
		}
		final double diff1 = brute(1, 5, best);
		if(diff1 < 0) {
			best += diff1;
			CsvUtil.write(assignments, "../../solutions/" + String.format("%.2f", best)  + "_sa.csv");
			CsvUtil.write(assignments, "../../solutions/best.csv");
		}
		final double diff2 = brute(2, 5, best);
		if(diff2 < 0) {
			best += diff2;
			CsvUtil.write(assignments, "../../solutions/" + String.format("%.2f", best)  + "_sa.csv");
			CsvUtil.write(assignments, "../../solutions/best.csv");
		}
		final double diff3 = randomBrute(100000000, 3 , 5, best);
		if(diff3 < 0) {
			best += diff3;
			CsvUtil.write(assignments, "../../solutions/" + String.format("%.2f", best)  + "_sa.csv");
			CsvUtil.write(assignments, "../../solutions/best.csv");
		}
		return best;
	}


}
