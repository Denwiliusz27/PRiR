import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

interface Sleeper {
    public static void sleep(long howLong) {
        try {
            Thread.sleep(howLong);
        } catch (InterruptedException e) {
        }
    }
}

class Factory {
    AtomicInteger counter = new AtomicInteger();

    public void inc() {
        counter.incrementAndGet();
    }

    public int get() {
        return counter.get();
    }
}

class Worker implements Runnable {
    private Factory factory;
    private AtomicInteger commonCounter;

    public void run() {
        while (true) {
            factory.inc();
            Thread.yield();
            commonCounter.incrementAndGet();
        }
    }

    public Worker(Factory factory, AtomicInteger commonCounter) {
        this.factory = factory;
        this.commonCounter = commonCounter;
    }
}

class TeamOfWorkers implements Sleeper {
    private List<Thread> threads = new ArrayList<>();
    private AtomicInteger commonCounter = new AtomicInteger();
    private Factory factory = new Factory();

    public TeamOfWorkers(int workers) {
        for (int i = 0; i < workers; i++) {
            Thread th = new Thread(new Worker(factory, commonCounter));
            th.setDaemon(true);
            th.start();
            threads.add(th);
            Sleeper.sleep(2);
        }
    }

    public void suspendTestResume() {
        threads.forEach(th -> th.suspend());
        Sleeper.sleep(100);
        System.out.println("Co widzi fabryka " + factory.get() + " co widzi commonCounter " + commonCounter.get());
        threads.forEach(th -> th.resume());
    }

    public static void main(String[] args) {
        TeamOfWorkers team = new TeamOfWorkers(10);
        for (int i = 0; i < 10; i++) {
            Sleeper.sleep(125);
            team.suspendTestResume();
        }
    }
}
