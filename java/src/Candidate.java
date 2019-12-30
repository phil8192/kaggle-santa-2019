final class Candidate {
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