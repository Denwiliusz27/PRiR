import java.util.ArrayList;
import java.util.List;

public class Watek_liczacy extends Thread {
    private int id;
    private ParallelCalculator parallelCalculator;
    private DeltaReceiver deltaReceiver;
//    private Semaphore ochronaDeltaReceiver;

    public Watek_liczacy(int id, ParallelCalculator parallelCalculator, DeltaReceiver deltaReceiver){ // 4 parametr Semaphore ochrona receivera
        this.id = id;
        System.out.println("Watek " + id + "utworzony");
        this.parallelCalculator = parallelCalculator;
        this.deltaReceiver = deltaReceiver;
    }

    public void run(){
        System.out.println("Watek " + id + " wystartował");

        while(true){
            try {
                Task mojTask = parallelCalculator.dajKolejnyTask( id );
                System.out.println("Watek "+ id + " otrzymalem task " + mojTask + " i zaczynam go wykonywać");
                wykonaj_task(mojTask);
            } catch (InterruptedException ex) {
                System.out.println("Watek " + id +" nie dostal taska bo wystapilo przerwanie programu " + ex );
            }
        }
    }

    private void wykonaj_task(Task mojTask){
        System.out.println("watek "+ id + "wykonuje task " + mojTask + " i oblicza delte lub delty" );

        int id_delty = mojTask.daj_mniejsze_id();
        int sprawdzany_index = mojTask.daj_poczatkowy_index();
        Data tablica_1 = mojTask.daj_pierwszy_wektor();
        Data tablica_2 = mojTask.daj_drugi_wektor();

        int roznica = tablica_2.getValue(sprawdzany_index) - tablica_1.getValue(sprawdzany_index);

        Delta delta = new Delta(id_delty, sprawdzany_index, roznica);
        System.out.println("watek " + id + " ma delte:" + delta);

        List<Delta> deltas = new ArrayList<Delta>();
        deltas.add(delta);

        // aquire
        deltaReceiver.accept(deltas);
        // release
    }
}
