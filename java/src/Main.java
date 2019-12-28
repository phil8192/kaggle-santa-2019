/* h0 h0 h0 */

import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.SynchronousQueue;

import static java.lang.Math.abs;
import static java.lang.Math.random;

public class Main extends Optimiser {

	private final Random prng = new Random();
	private final BlockingQueue<Candidate> best = new SynchronousQueue<>();

	private Main(int[][] familyData, int[] initialAssignments) {
		super(familyData, initialAssignments);
	}

	private double acceptanceProbability(final double oldScore, final double newScore, final double temperature) {
		final double d = newScore - oldScore;
		// less damaging moves have higher probability
		return Math.exp(-d / temperature);
	}

	private double localMinima(final int[] assignments, final double temperature, final int maxLoops) {
		double currentPenalty = getPenalty(assignments);
		double currentAccount = getAccountingCost();
		double current = currentPenalty + currentAccount;
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
					for (int j = 0; j < 5; j++) {
						final int candidateDay = prefs[j];
						if (candidateDay != assignedDay) {
							final int candidateDayCap = dayCapacities[candidateDay];
							if (candidateDayCap + famSize <= MAX_PPL) {

								final double penaltyDelta = getPenaltyDelta(i, assignedDay, candidateDay);
								final double accountDelta = getAccountingDelta(assignedDay, candidateDay, famSize);
								final double delta = penaltyDelta + accountDelta;

								final double candidateScore = current + delta;
								if (candidateScore < current || (maxLoops > 0 && acceptanceProbability(current, candidateScore, temperature) >= prng.nextDouble())) {
									dayCapacities[assignedDay] -= famSize;
									dayCapacities[candidateDay] += famSize;
									assignments[i] = candidateDay;

									current += delta;
									currentPenalty += penaltyDelta;
									currentAccount += accountDelta;

//									final double sanity = cost(assignments) - current;
//									if(Math.abs(sanity) >= 0.00001) {
//										System.out.println(current + " != " + cost(assignments));
//										System.exit(1);
//									}

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
		return current;
	}

	private double optimise(final int[] assignments) {
		double temperature = 4;
		double coolingSchedule = 0.999999;
		double best = localMinima(assignments, 0, 0);
		System.out.println("best = " + String.format("%.2f", best));
		int i = 0;
		while (temperature > 0.2) {
			i++;
			localMinima(assignments, temperature, 1);
			double score = localMinima(assignments, 0, 0);
			if(i % 100000 == 0) {

				// 10 million rounds of random brute force: do not go too far astray

//				final double diff1 = brute(assignments, 1, 4, score);
//				if(diff1 < 0) {
//					score += diff1;
//				}
//				final double diff2 = brute(assignments, 2, 4, score);
//				if(diff2 < 0) {
//					score += diff2;
//				}
				final double diff3 = randomBrute(10000000, assignments, 3 , 5, score);
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
		final double diff1 = brute(assignments, 1, 4, best);
		if(diff1 < 0) {
			best += diff1;
			CsvUtil.write(assignments, "../../solutions/" + String.format("%.2f", best)  + "_sa.csv");
			CsvUtil.write(assignments, "../../solutions/best.csv");
		}
		final double diff2 = brute(assignments, 2, 4, best);
		if(diff2 < 0) {
			best += diff2;
			CsvUtil.write(assignments, "../../solutions/" + String.format("%.2f", best)  + "_sa.csv");
			CsvUtil.write(assignments, "../../solutions/best.csv");
		}
		final double diff3 = randomBrute(100000000, assignments, 3 , 4, best);
		if(diff3 < 0) {
			best += diff3;
			CsvUtil.write(assignments, "../../solutions/" + String.format("%.2f", best)  + "_sa.csv");
			CsvUtil.write(assignments, "../../solutions/best.csv");
		}
		return best;
	}

	private double scan(final Integer[] fams, final int[] assignments, final int maxChoice, final double current, final double currentPenalty) {

		// todo: use getPenaltyDelta(final int fam, final int assignedDay, final int candidateDay)
		//double penaltyDelta = 0.0;

		// stash the original assignments
		final int[] original = new int[fams.length];
		for (int i = 0; i < fams.length; i++) {
			original[i] = assignments[fams[i]];
		}

		// try all possible configurations
		final List<List<Integer>> prods = Cartisian.product(fams.length, maxChoice);


		for (final List<Integer> prod : prods) { // for each set of choices
			//System.out.println(prod);

			double penaltyDelta = 0.0;

			// set all the fams
			for (int i = 0; i < fams.length; i++) {
				final int fam = fams[i];
				final int famSize = familySize[fam];
				final int choice = prod.get(i);
				final int assignedDay = assignments[fam];
				final int candidateDay = familyPrefs[fam][choice];

				// assign this family
				dayCapacities[assignedDay] -= famSize;
				dayCapacities[candidateDay] += famSize;
				assignments[fam] = candidateDay;

				penaltyDelta += getPenaltyDelta(fam, assignedDay, candidateDay);

			}

//			final double __penalty = currentPenalty + penaltyDelta;
//			final double xpenalty = getPenalty(assignments);
//			if(Math.abs(__penalty - xpenalty) > 0.00001) {
//				System.out.println("X _pen = " + __penalty + " X pen = " + xpenalty);
//			}

			// if no constraint violation and improvement, return improvement delta
			if (checkCapacityConstraints()) {
				// expensive -> final double candidateCost = cost(assignments);


				final double _penalty = currentPenalty + penaltyDelta;
				//final double _penalty = getPenalty(assignments);
				//if(Math.abs(_penalty - penalty) > 0.00001) {
				//	System.out.println("X _pen = " + _penalty + " X pen = " + penalty);
				//}

				//System.out.println("pen = " + penalty + " _pen = " + _penalty);
				final double accountingCost = getAccountingCost();
				final double candidateCost = _penalty + accountingCost;
				final double delta = candidateCost - current;
				if (delta < 0) {
					//final double penalty = getPenalty(assignments);
					//System.out.println("pen = " + penalty + " _pen = " + _penalty);
					return delta;
				}
			}
			// put the back so can do penalty delta in next set of choices
			for (int i = 0; i < original.length; i++) {
				final int fam = fams[i];
				final int famSize = familySize[fam];
				final int assignedDay = assignments[fam];
				final int originalDay = original[i];
				dayCapacities[assignedDay] -= famSize;
				dayCapacities[originalDay] += famSize;
				assignments[fam] = originalDay;
			}
		}
		return 0;
	}

	private double brute(final int[] assignments, final int fams, final int maxChoice, final double current) {
		double score = current;
		double currentPenalty = getPenalty(assignments);
		boolean improvement;
		List<List<Integer>> famCombos = Cartisian.product(fams, 5000);
		do {
			improvement = false;
			for (final List<Integer> combo : famCombos) {
				final Integer[] famIndices = combo.toArray(new Integer[]{});
				final double delta = scan(famIndices, assignments, maxChoice, score, currentPenalty);
				System.out.println(Arrays.toString(famIndices));
				if (delta < 0) {
					score += delta;

					//double p = getPenalty(assignments);
					//double a = getAccountingCost();
					//double c = p + a;

					currentPenalty = getPenalty(assignments);
//					System.out.println(String.format("%.2f", score) + " (" + c + ")");
//					if(Math.abs(c - score) > 0.00001) {
//						System.exit(0);
//					} else {


					//CsvUtil.write(assignments, "../../solutions/" + String.format("%.2f", score) + "_b.csv");
					//CsvUtil.write(assignments, "../../solutions/best.csv");


					//}




					//improvement = true;



				}
			}
		} while (improvement);
		return score - current;
	}

	private double brute3(final int[] assignments, final int maxChoice, final double current, final int from, final int to) {
		double score = current;
		double currentPenalty = getPenalty(assignments);
		boolean improvement;
		do {
			improvement = false;
			for (int i = from; i < to; i++) {
				for (int j = i + 1; j < 5000; j++) {
					for (int k = j + 1; k < 5000; k++) {
						final Integer[] famIndices = new Integer[]{i, j, k};
						final double delta = scan(famIndices, assignments, maxChoice, score, currentPenalty);
						System.out.println(ANSI_GREEN + String.format("%.2f", score) + ANSI_RESET + " " + Arrays.toString(famIndices));
						if (delta < 0) {
							score += delta;
							currentPenalty = getPenalty(assignments);
							System.out.println(String.format("%.2f", score));
							improvement = true;
							CsvUtil.write(assignments, "../../solutions/" + String.format("%.2f", score) + "_b3.csv");
							CsvUtil.write(assignments, "../../solutions/best.csv");
						}
					}
				}
			}
		} while (improvement);
		return score - current;
	}

	private double randomBrute(final int[] assignments, final int fams, final int maxChoice, final double current, final double currentPenalty) {
		// get list of random family indices
		final Integer[] randomFams = prng.ints(0, 5000)
				.boxed()
				.distinct()
				.limit(fams)
				.toArray(Integer[]::new);
		return scan(randomFams, assignments, maxChoice, current, currentPenalty);
	}

	private double randomBrute(final int rounds, final int[] assignments, final int fams, final int maxChoice, final double score) {
		double currentPenalty = getPenalty(assignments);
		double current = score;
		for (int i = 0; i < rounds; i++) {
			final double delta = randomBrute(assignments, fams, maxChoice, current, currentPenalty);
			System.out.println("probe = " + ANSI_GREEN + i + ANSI_RESET + " (" + String.format("%.2f", current) + ")");
			if (delta < 0) {
				current += delta;
				currentPenalty = getPenalty(assignments);
				System.out.println("**** new brute score = " + current + "****");


				//CsvUtil.write(assignments, "../../solutions/" + String.format("%.2f", current) + "_rb.csv");
				//CsvUtil.write(assignments, "../../solutions/best.csv");


			}
		}
		return current - score;
	}

	private final class Candidate {
		private final int[] ass;
		private final double score;
		private final String method;

		public Candidate(int[] ass, double score, final String method) {
			this.ass = ass;
			this.score = score;
			this.method = method;
		}

		public int[] getAss() {
			return ass;
		}

		public double getScore() {
			return score;
		}

		public String getMethod() {
			return method;
		}
	}

	private final class Coordinator implements Runnable {
		private boolean alive = true;
		private double bestScore;

		public Coordinator(final double bestScore) {
			this.bestScore = bestScore;
		}

		public void kill() {
			this.alive = false;
		}

		@Override
		public void run() {
			while(alive) {
				try {
					final Candidate candidate = best.take();
					final double score = candidate.getScore();
					if(score < bestScore) {
						bestScore = score;
						final int[] ass = candidate.getAss();
						final String method = candidate.getMethod();
						System.out.println("score = " + String.format("%.2f", score) + " (" + method + ")");
						CsvUtil.write(ass, "../../solutions/" + String.format("%.2f", score) + "_" + method + ".csv");
					}
				} catch(InterruptedException e) {
					Thread.currentThread().interrupt();
				}
			}
		}
	}

	private final class BruteWorker implements Runnable {
		private boolean alive = true;

		public BruteWorker() {
			this.alive = alive;
		}

		public void kill() {
			this.alive = false;
		}

		@Override
		public void run() {
//			while (alive) {
//				double score = current;
//				double currentPenalty = getPenalty(assignments);
//				boolean improvement;
//				do {
//					improvement = false;
//					for (int i = from; i < to; i++) {
//						for (int j = i + 1; j < 5000; j++) {
//							for (int k = j + 1; k < 5000; k++) {
//								final Integer[] famIndices = new Integer[]{i, j, k};
//								final double delta = scan(famIndices, assignments, maxChoice, score, currentPenalty);
//								System.out.println(ANSI_GREEN + String.format("%.2f", score) + ANSI_RESET + " " + Arrays.toString(famIndices));
//								if (delta < 0) {
//									score += delta;
//									currentPenalty = getPenalty(assignments);
//									System.out.println(String.format("%.2f", score));
//									improvement = true;
//									best.put(new Candidate(assignments, score, "b3"));
//								}
//							}
//						}
//					}
//				} while (improvement);
//				return score - current;
//			}
		}
	}

	public static void main(String[] meh) {
		int[][] family_data = CsvUtil.read("../../../data/family_data.csv");
		//int[][] starting_solution = CsvUtil.read("../../../submission_71647.5625.csv");
		//int[][] starting_solution = CsvUtil.read("/tmp/lala.csv"); // 77124.66595889143
		int[][] starting_solution = CsvUtil.read("../../solutions/best.csv");


		// 71757.52
		//int[][] starting_solution = CsvUtil.read("../../solutions/71535.25513531327.csv");

		assert starting_solution != null;
		int[] initialAsignments = new int[starting_solution.length];
		for (int i = 0; i < starting_solution.length; i++) {
			initialAsignments[i] = starting_solution[i][1];
		}
		Main main = new Main(family_data, initialAsignments);


		double score = main.cost(initialAsignments);
		System.out.println("initial = " + score);


//		double diff1 = main.brute(initialAsignments, 1, 4, score);
//		System.out.println("score = " + (score + diff1));
//
//		double diff2 = main.brute(initialAsignments, 2, 4, score);
//		System.out.println("score = " + (score + diff2));

		//double diff3 = main.randomBrute(100000000, initialAsignments, 4 , 4, score);
		//System.out.println("score = " + (score + diff3));


//		int from = Integer.parseInt(meh[0]);
//		int to = Integer.parseInt(meh[1]);
//		double diff = main.brute3(initialAsignments, 5, score, from, to);


//
		double diff = main.optimise(initialAsignments);
		System.out.println("score = " + (score + diff));
	}
}
