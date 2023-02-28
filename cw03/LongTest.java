import java.util.ArrayList;
import java.util.List;

public class LongTest {
///	volatile
	private static long commonField = 0;

	private static final int REPETITIONS = 1000000;

	private static Runnable getWritter() {
		return new Runnable() {
			@Override
			public void run() {
				for (int i = 0; i < REPETITIONS; i++) {
					commonField++;
				//	Thread.yield();
				}

			}
		};
	}

	private static List<Thread> execute(List<Runnable> tasks) {
		List<Thread> ths = new ArrayList<>();
		tasks.forEach(t -> {
			Thread th = new Thread(t);
			ths.add(th);
			th.setDaemon(true);
			th.start();
		});
		return ths;
	}

	private static int joinAll(List<Thread> ths) {
		for (Thread th : ths) {
			try {
				th.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		return ths.size();
	}

	private static void sleep(long msec) {
		try {
			Thread.sleep(msec);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		List<Runnable> tasks = new ArrayList<>();
		tasks.add(getWritter());
		tasks.add(getWritter());
		tasks.add(getWritter());
		int threads = joinAll(execute(tasks));
		System.out.println("Wynik obliczen " + commonField + " oczekiwano " + (threads * REPETITIONS));
	}

}
