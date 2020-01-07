import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class Main {

	//static AtomicInteger tripwire = new AtomicInteger(0);

	static class BruteWorker implements Runnable {
		Brute brute;
		BruteWorker(Brute brute) {
			this.brute = brute;
		}

		@Override
		public void run() {
			try {
				Thread.sleep(5*1000);
				brute.optimise();
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
			//System.out.println(Thread.currentThread().getName() + " brute worker. time to die...");
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

		public SAWorker(final int[][] family_data, final int[] assignemnts, final Random prng, BlockingQueue<Candidate> q,
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



				if(candidateScore < score) {

//					double lala = sa.cost(sa.getAssignments());
//					if(Math.abs(lala-candidateScore) > 0.00001) {
//						System.out.println(lala + " != " + candidateScore);
//						System.exit(0);
//					}

					try {
						//tripwire.incrementAndGet();
						Candidate candidate = new Candidate(Arrays.copyOf(sa.getAssignments(), 5000), candidateScore, "sa_"+id);
						q.put(candidate);
					} catch (InterruptedException e) {
						Thread.currentThread().interrupt();
					} finally {
						//tripwire.decrementAndGet();
					}
				}
			}
		}
	}

	public static Thread startSAWorker(final int[][] family_data, int[] assignments,
																		 BlockingQueue<Candidate> q, Random prng,
																		 double temperature, double coolingSchedule, String id) {
		SAWorker saWorker = new SAWorker(family_data, assignments, prng, q, temperature, coolingSchedule, id);
		Thread t = Executors.defaultThreadFactory().newThread(saWorker);
		t.start();
		//System.out.println("brute worker alive...");
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

				//long l = System.currentTimeMillis();
				double newScore = score + optimiser.randomBrute(1000, fam, 5, score);
				//System.out.println("rnd brute "+name+" took " + (System.currentTimeMillis() - l) + "ms.");
				//System.out.println(newScore);
				if (newScore < score) {
					score = newScore;
					try {
						//tripwire.incrementAndGet();
						Candidate candidate = new Candidate(Arrays.copyOf(optimiser.getAssignments(), 5000), newScore, "rnd_brute_"+name);
						q.put(candidate);
					} catch (InterruptedException e) {
						Thread.currentThread().interrupt();
					} finally {
						//tripwire.decrementAndGet();
					}
				}
			}
			//System.out.println(Thread.currentThread().getName() + " random brute worker. time to die...");
		}
	}

//	public static Thread startBruteWorker(final int[][] family_data, int[] assignments, int from, int to,
//																				BlockingQueue<Candidate> q, Random prng) {
//		BruteWorker bw = new BruteWorker(new Brute(family_data, Arrays.copyOf(assignments, assignments.length), from, to, 5, q, prng));
//		Thread t = Executors.defaultThreadFactory().newThread(bw);
//		t.start();
//		return t;
//	}

	static Thread startBruteWorker(final int[][] family_data, final int[] assignments,
																 BlockingQueue<int[]> in, BlockingQueue<Candidate> out, Random prng) {
		Thread t = Executors.defaultThreadFactory().newThread(new Runnable() {
			@Override
			public void run() {
				try {
					while(!Thread.currentThread().isInterrupted()) {
						int[] job = in.take();
						int from = job[0];
						int to = job[1];
						System.out.println(in.size() + " - " + Thread.currentThread().getName() + " " + Arrays.toString(job));
						Brute brute = new Brute(family_data, Arrays.copyOf(assignments, assignments.length), from, to, 5, out, prng);
						brute.optimise();
					}
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
			}
		});
		t.start();
		return t;
	}

	public static Thread startRandomBruteWorker(final int[][] family_data, int[] assignments,
																							BlockingQueue<Candidate> q, Random prng, int fam, String name) {
		Optimiser optimiser = new Optimiser(family_data, Arrays.copyOf(assignments, assignments.length), q, prng);
		RandomBruteWorker randomBruteWorker = new RandomBruteWorker(optimiser, q, fam, name);
		Thread t = Executors.defaultThreadFactory().newThread(randomBruteWorker);
		t.start();
		return t;
	}

	public static List<Thread> startBruteWorkers(int[][] family_data, int[] initialAsignments,
																										BlockingQueue<Candidate> q, Random prng, boolean brute) {
		List<Thread> l = new ArrayList<>();


		for(int i = 0; i < 0; i++) {
			l.add(startSAWorker(family_data, initialAsignments, q, new Random(prng.nextInt()), 3, 0.999999, "slow"));
		}
//		for(int i = 0; i < 5; i++) {
//			l.add(startSAWorker(family_data, initialAsignments, q, new Random(prng.nextInt()), 2.5, 0.99999, "fast"));
//		}

		// 1 random brute worker

//		l.add(startRandomBruteWorker(family_data, initialAsignments, q, new Random(prng.nextInt()), 4, "4"));
//		l.add(startRandomBruteWorker(family_data, initialAsignments, q, new Random(prng.nextInt()), 5, "5"));
//		l.add(startRandomBruteWorker(family_data, initialAsignments, q, new Random(prng.nextInt()), 6, "6"));
//		l.add(startRandomBruteWorker(family_data, initialAsignments, q, new Random(prng.nextInt()), 7, "7"));
//		l.add(startRandomBruteWorker(family_data, initialAsignments, q, new Random(prng.nextInt()), 8, "8"));
//		l.add(startRandomBruteWorker(family_data, initialAsignments, q, new Random(prng.nextInt()), 9, "9"));
//		l.add(startRandomBruteWorker(family_data, initialAsignments, q, new Random(prng.nextInt()), 10,"10"));

		ArrayList<int[]> jobs = new ArrayList<>(4999);
		for(int i = 0; i < 4999; i++) {
			jobs.add(new int[] {i, i+1});
		}
		Collections.shuffle(jobs);
		ArrayBlockingQueue<int[]> in = new ArrayBlockingQueue<>(4999, true, jobs);
		for(int i = 0; i < 12; i++) {
			l.add(startBruteWorker(family_data, initialAsignments, in, q, prng));
		}


			// 10 brute force threads
//			l.add(startBruteWorker(family_data, initialAsignments, 0, 500, q, prng));
//			l.add(startBruteWorker(family_data, initialAsignments, 500, 1000, q, prng));
//			l.add(startBruteWorker(family_data, initialAsignments, 1000, 1500, q, prng));
//			l.add(startBruteWorker(family_data, initialAsignments, 1500, 2000, q, prng));
//			l.add(startBruteWorker(family_data, initialAsignments, 2000, 2500, q, prng));
//			l.add(startBruteWorker(family_data, initialAsignments, 2500, 3000, q, prng));
//			l.add(startBruteWorker(family_data, initialAsignments, 3000, 3500, q, prng));
//			l.add(startBruteWorker(family_data, initialAsignments, 3500, 4000, q, prng));
//			l.add(startBruteWorker(family_data, initialAsignments, 4000, 4500, q, prng));
//			l.add(startBruteWorker(family_data, initialAsignments, 4500, 5000, q, prng));

		return l;
	}

	public static void killBruteWorkers(List<Thread> bruteWorkerList) {
		for(Thread bruteWorker : bruteWorkerList) {
			bruteWorker.interrupt();
		}
	}

	public static void main(String[] meh) {
		Random prng = new Random();

		int[][] family_data = CsvUtil.read("../../../data/family_data.csv");
		int[][] starting_solution = CsvUtil.read("../../solutions/best.csv");
//		int[][] starting_solution = CsvUtil.read("/tmp/lala.csv");

		assert starting_solution != null;
		int[] initialAsignments = new int[starting_solution.length];
		for (int i = 0; i < starting_solution.length; i++) {
			initialAsignments[i] = starting_solution[i][1];
		}

		final BlockingQueue<Candidate> q = new SynchronousQueue<>();
		List<Thread> bruteWorkers = null;
		boolean useBrute = true;
		if(useBrute) {

			bruteWorkers = startBruteWorkers(family_data, initialAsignments, q, prng, true);

			double best = Double.MAX_VALUE;

			while(true) {
				try {
					//System.out.println("waiting for new score...");
					long l = System.currentTimeMillis();
					Candidate candidate = q.take();
					final double score = candidate.getScore();
					final int[] ass = candidate.getAss();
					final String method = candidate.getMethod();
					//System.out.println("got new score: " + String.format("%.2f", score) + " (" + method + ")");
					if(score < best) {
						System.out.println("score: " + String.format("%.2f", score) + " (" + method + ")");
						CsvUtil.write(ass, "../../solutions/" + String.format("%.2f", score) + "_" + method + ".csv");
						CsvUtil.write(ass, "../../solutions/best.csv");
						//System.out.println("killing workers");

						//if(tripwire.get() == 0) { // only killall if noting waiting to share a result.
							System.out.println("killall");
							killBruteWorkers(bruteWorkers);
							bruteWorkers = startBruteWorkers(family_data, ass, q, prng, true);
						//}
						best = score;
					}
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
			}

		} else {
//			SA sa = new SA(family_data, initialAsignments, prng, 3,999999);
//			double pen = sa.getPenalty(initialAsignments);
//			double acc = sa.getAccountingCost();
//			double score = pen + acc;
//			System.out.println(score + " (" + pen + ") (" + acc + ")");
//			sa.optimise();
			Optimiser optimiser = new Optimiser(family_data, initialAsignments, q, prng);
			double initial = optimiser.cost(initialAsignments);
			double delta = optimiser.brute(2, 5, initial);
			System.out.println(delta);

		}
	}
}
