import java.util.ArrayList;
import java.util.List;

public class Watek_liczacy extends Thread {
    private int id;
    private ParallelCalculator parallelCalculator;
    private Ukladanie_delt ukladanie_delt;

    public Watek_liczacy(int id, ParallelCalculator parallelCalculator,  Ukladanie_delt ukladanie_delt){ // 4 parametr Semaphore ochrona receivera
        this.id = id;
//        System.out.println("Watek " + id + "utworzony");
        this.parallelCalculator = parallelCalculator;
        this.ukladanie_delt = ukladanie_delt;
    }

    public void run(){
//        System.out.println("Watek " + id + " wystartował");

        while(true){
            try {
                Task mojTask = parallelCalculator.dajKolejnyTask( id );
//                System.out.println("Watek "+ id + " otrzymalem task " + mojTask + " i zaczynam go wykonywać");
                System.out.println(id + " sprawdzam pare: " + mojTask.daj_pierwszy_wektor().getDataId() + " i " + mojTask.daj_drugi_wektor().getDataId());
                wykonaj_task(mojTask);
            } catch (InterruptedException ex) {
                System.out.println("Watek " + id +" nie dostal taska bo wystapilo przerwanie programu " + ex );
            }
        }
    }

    private void wykonaj_task(Task mojTask){
//        System.out.println("watek "+ id + "wykonuje task " + mojTask + " i oblicza delte lub delty" );
        List<Delta> deltas = new ArrayList<Delta>();

        int id_delty = mojTask.daj_mniejsze_id();
        int sprawdzany_index = mojTask.daj_poczatkowy_index();
        Data tablica_1 = mojTask.daj_pierwszy_wektor();
        Data tablica_2 = mojTask.daj_drugi_wektor();

        int roznica = tablica_2.getValue(sprawdzany_index) - tablica_1.getValue(sprawdzany_index);

        Delta delta = new Delta(id_delty, sprawdzany_index, roznica);
        System.out.println("watek " + id + " ma delte:" + delta);

        deltas.add(delta);


        ukladanie_delt.oddaj_delta(deltas);
//        // aquire
//        try {
//            ochronaDeltaReceiver.acquire();
//            deltaReceiver.accept(deltas);
//            ochronaDeltaReceiver.release();
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
//        // release
    }
}
