public class Main {
	private static final int MAX_FAM_SIZE = 10;
	private static final int MAX_CHOICE = 10;

	private final double[][] penalties;


	Main() {
		this.penalties = new double[MAX_FAM_SIZE + 1][MAX_CHOICE];
		for (int i = 1; i < MAX_FAM_SIZE; i++) {
			penalties[i][0] = 0;
			penalties[i][1] = 50;
			penalties[i][2] = 50 + 9 * i;
			penalties[i][3] = 100 + 9 * i;
			penalties[i][4] = 200 + 9 * i;
			penalties[i][5] = 200 + 18 * i;
			penalties[i][6] = 300 + 18 * i;
			penalties[i][7] = 300 + 36 * i;
			penalties[i][8] = 400 + 36 * i;
			penalties[i][9] = 500 + 36 * i + 199 * i;
			penalties[i][10] = 500 + 36 * i + 398 * i;
		}
	}

	public static void main(String[] meh) {

	}

}
