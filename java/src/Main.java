/* h0 h0 h0 */

import java.util.*;
import java.util.concurrent.*;

public class Main {

	static class BruteWorker implements Runnable {
		Brute brute;

		BruteWorker(Brute brute) {
			this.brute = brute;
		}

		@Override
		public void run() {
			try {
				Thread.sleep(5 * 1000);
				brute.optimise();
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}
	}

	static class SAWorker implements Runnable {
		BlockingQueue<Candidate> q;
		final int[][] family_data;
		final int[] assignments;
		final Random prng;
		double temperature;
		double coolingSchedule;
		String id;

		SAWorker(final int[][] family_data, final int[] assignemnts, final Random prng, BlockingQueue<Candidate> q,
						 double temperature, double coolingSchedule, String id) {
			this.q = q;
			this.family_data = family_data;
			this.assignments = Arrays.copyOf(assignemnts, assignemnts.length);
			this.prng = prng;
			this.temperature = temperature;
			this.coolingSchedule = coolingSchedule;
			this.id = id;
		}

		@Override
		public void run() {
			while (!Thread.currentThread().isInterrupted()) {
				SA sa = new SA(family_data, Arrays.copyOf(assignments, assignments.length), prng, temperature, coolingSchedule);
				double score = sa.cost(sa.getAssignments());
				double candidateScore = sa.optimise();
				if (candidateScore < score) {
					try {
						Candidate candidate = new Candidate(Arrays.copyOf(sa.getAssignments(), 5000), candidateScore, "sa_" + id);
						q.put(candidate);
					} catch (InterruptedException e) {
						Thread.currentThread().interrupt();
					}
				}
			}
		}
	}

	private static Thread startSAWorker(final int[][] family_data, int[] assignments,
																			BlockingQueue<Candidate> q, Random prng,
																			double temperature, double coolingSchedule, String id) {
		SAWorker saWorker = new SAWorker(family_data, assignments, prng, q, temperature, coolingSchedule, id);
		Thread t = Executors.defaultThreadFactory().newThread(saWorker);
		t.start();
		return t;
	}

	static class RandomBruteWorker implements Runnable {
		Optimiser optimiser;
		BlockingQueue<Candidate> q;
		int fam;
		String name;

		public RandomBruteWorker(Optimiser optimiser, BlockingQueue<Candidate> q, int fam, String name) {
			this.optimiser = optimiser;
			this.q = q;
			this.fam = fam;
			this.name = name;
		}

		@Override
		public void run() {
			double score = optimiser.cost(optimiser.getAssignments());
			while (!Thread.currentThread().isInterrupted()) {

				long l = System.currentTimeMillis();
				double newScore = score + optimiser.randomBrute(100000000, fam, 5, score);
				System.out.println("rnd brute " + name + " took " + (System.currentTimeMillis() - l) + "ms.");
				if (newScore < score) {
					score = newScore;
					try {
						Candidate candidate = new Candidate(Arrays.copyOf(optimiser.getAssignments(), 5000), newScore, "rnd_brute_" + name);
						q.put(candidate);
					} catch (InterruptedException e) {
						Thread.currentThread().interrupt();
					}
				}
			}
		}
	}

	static Thread startBruteWorker(final int[][] family_data, final int[] assignments,
																 BlockingQueue<int[]> in, BlockingQueue<Candidate> out, Random prng) {
		Thread t = Executors.defaultThreadFactory().newThread(() -> {
			try {
				while (!Thread.currentThread().isInterrupted()) {
					int[] job = in.take();
					int from = job[0];
					int to = job[1];
					System.out.println(String.format("%05.2f", 100 * (1 - ((in.size() / 4999.0)))) + "% - " + Thread.currentThread().getName() + "\t" + Arrays.toString(job));
					Brute brute = new Brute(family_data, Arrays.copyOf(assignments, assignments.length), from, to, 5, out, prng);
					brute.optimise();
				}
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		});
		t.start();
		return t;
	}

	private static Thread startRandomBruteWorker(final int[][] family_data, int[] assignments,
																							 BlockingQueue<Candidate> q, Random prng, int fam, String name) {
		Optimiser optimiser = new Optimiser(family_data, Arrays.copyOf(assignments, assignments.length), q, prng);
		RandomBruteWorker randomBruteWorker = new RandomBruteWorker(optimiser, q, fam, name);
		Thread t = Executors.defaultThreadFactory().newThread(randomBruteWorker);
		t.start();
		return t;
	}

	/**
	 * mix of algos.
	 *
	 * @param family_data family preferences + data.
	 * @param initialAsignments original assignments.
	 * @param q queue to dump candidate solutions on.
	 * @param prng random number generator.
	 * @return
	 */
	private static List<Thread> startBruteWorkers(int[][] family_data, int[] initialAsignments,
																								BlockingQueue<Candidate> q, Random prng) {
		List<Thread> l = new ArrayList<>();

		// mod...

		// simulated annealing workers
		for (int i = 0; i < 1; i++) {
			l.add(startSAWorker(family_data, initialAsignments, q, new Random(prng.nextInt()), 3, 0.999999, "slow"));
		}

		// random brute force workers.
		for (int i = 0; i < 1; i++) {
			l.add(startRandomBruteWorker(family_data, initialAsignments, q, new Random(prng.nextInt()), 4, "4"));
		}

		// brute force workers with order of scan randomised (random and complete algo.)
		ArrayList<int[]> jobs = new ArrayList<>(4999);
		for (int i = 0; i < 4999; i++) {
			jobs.add(new int[]{i, i + 1});
		}
		Collections.shuffle(jobs);
		ArrayBlockingQueue<int[]> in = new ArrayBlockingQueue<>(4999, true, jobs);
		for (int i = 0; i < 10; i++) {
			l.add(startBruteWorker(family_data, initialAsignments, in, q, prng));
		}

		return l;
	}

	private static void killBruteWorkers(List<Thread> bruteWorkerList) {
		for (Thread bruteWorker : bruteWorkerList) {
			bruteWorker.interrupt();
		}
	}

	public static void main(String[] meh) {
		Random prng = new Random();

		int[][] family_data = CsvUtil.read("../../../data/family_data.csv");
		int[][] starting_solution = CsvUtil.read("../../solutions/best.csv");

		assert starting_solution != null;
		int[] initialAsignments = new int[starting_solution.length];
		for (int i = 0; i < starting_solution.length; i++) {
			initialAsignments[i] = starting_solution[i][1];
		}

		final BlockingQueue<Candidate> q = new SynchronousQueue<>();
		List<Thread> bruteWorkers = null;

		bruteWorkers = startBruteWorkers(family_data, initialAsignments, q, prng);

		double best = Double.MAX_VALUE;

		while (true) {
			try {
				long l = System.currentTimeMillis();
				Candidate candidate = q.take();
				final double score = candidate.getScore();
				final int[] ass = candidate.getAss();
				final String method = candidate.getMethod();
				if (score < best) {
					if (score < 69000) { // def smt. wrong.
						System.exit(0);
					}
					System.out.println("score: " + String.format("%.2f", score) + " (" + method + ")");
					CsvUtil.write(ass, "../../solutions/" + String.format("%.2f", score) + "_" + method + ".csv");
					CsvUtil.write(ass, "../../solutions/best.csv");
					//System.out.println("killing workers");

					System.out.println("killall");
					killBruteWorkers(bruteWorkers);
					bruteWorkers = startBruteWorkers(family_data, ass, q, prng);

					best = score;
				}
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}
	}
}
