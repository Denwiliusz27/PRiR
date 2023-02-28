import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

class Zakupy {

	private static long commonField = 1000;
	private static long suma;
	private static final int THREADS = 1000;
	private static final CyclicBarrier tuCzekamy = new CyclicBarrier(THREADS);
	private static final int cena = 999;

	private static Runnable getWritter() {
		return new Runnable() {
			@Override
			public void run() {

				try {
					tuCzekamy.await();
				} catch (InterruptedException | BrokenBarrierException e) {
					e.printStackTrace();
				}

				boolean ok = cena < commonField;

				try {
					tuCzekamy.await();
				} catch (InterruptedException | BrokenBarrierException e) {
					e.printStackTrace();
				}
				
				if (ok) {
					commonField -= cena;

					synchronized (Zakupy.class) {
						suma += cena;
					}
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

	public static void main(String[] args) {
		List<Runnable> tasks = new ArrayList<>();
		for (int i = 0; i < THREADS; i++)
			tasks.add(getWritter());
		joinAll(execute(tasks));
		System.out.println("Konto     " + commonField);
		System.out.println("Zakupy za " + suma);
		System.out.println("Konto + zakupy za " + (commonField + suma));
	}

}
