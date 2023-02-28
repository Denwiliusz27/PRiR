import java.util.ArrayList;
import java.util.List;

class Worker implements Runnable {
  
  final static int SIZE = 128;
  final static long CALC_TIME = 10000;
  final static int REPETITIONS = 100;
  final static java.util.concurrent.atomic.AtomicLong counter = new  
                    java.util.concurrent.atomic.AtomicLong();

  static long vec[] = new long[ SIZE ];

  final static int SYNC = 6;
  static Object syn[] = new Object[ SYNC ];

  static {
   for ( int i = 0; i < SYNC; i++ ) 
    syn[ i ] = new Object();
  }

  java.util.Random rnd;

  public void calc() {
    int gen;
    for ( int i = 0; i < REPETITIONS; i++ ) {
      gen = rnd.nextInt( SIZE );

    synchronized( syn[ gen % SYNC ] ) {
//    synchronized ( vec ) {
         vec[ gen ]++;
    }
      counter.getAndIncrement();
    }
  }

  public void run() {

   rnd = java.util.concurrent.ThreadLocalRandom.current();

    long now = System.currentTimeMillis();

    long end = now + CALC_TIME;

    while ( System.currentTimeMillis() < end ) {
      calc();
    }
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

  public static void main( String[] arg ) {

    List< Runnable > tasks = new ArrayList<>();

    tasks.add( new Worker() );
    tasks.add( new Worker() );
    tasks.add( new Worker() );
    tasks.add( new Worker() );
    tasks.add( new Worker() );
    tasks.add( new Worker() );

    joinAll(execute(tasks));

    System.out.println( "Counter = " + counter.get() );

  }
}


