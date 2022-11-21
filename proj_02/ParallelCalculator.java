import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Semaphore;


public class ParallelCalculator implements DeltaParallelCalculator {
    private List<Data> vectors_set; // lista wektorów
    private HashMap<Data, Integer> calculations_amount; // mapa ile który wektor był obliczany
    private Watek_liczacy[] counting_threads; //
    private List<Task> lista_taskow;
    private Semaphore czekaj_na_niepuste_taski;
    private Semaphore ochrona_listy_taskow;
    private DeltaReceiver deltaReceiver;
    private Ukladanie_delt ukladanieDelty;


    public ParallelCalculator(){
        vectors_set = new ArrayList<Data>();
        lista_taskow = new ArrayList<Task>();
        czekaj_na_niepuste_taski = new Semaphore(0);
        ochrona_listy_taskow = new Semaphore(1);
        calculations_amount = new HashMap<Data, Integer>();
    }

    /**
     * Metoda ustala liczbę wątków jaka ma być użyta do liczenia
     * delty.
     *
     * @param threads liczba wątków.
     */
    public void setThreadsNumber(int threads){
       setDeltaReceiver(new OdbiorWynikow());
       counting_threads = new Watek_liczacy[threads];

       for(int i=0; i<threads; i++){
           counting_threads[i] = new Watek_liczacy( i, this, ukladanieDelty );
       }
       startuj_watki();
    }

    public void startuj_watki(){
        for(int i = 0; i < counting_threads.length; i++){
            counting_threads[i].start();
        }
    }

    /**
     * Przekazany jako parametr obiekt ma być używany
     * do przekazywania za jego pomocą rezultatu.
     *
     * @param receiver obiekt odbierający wyniki
     */
    public void setDeltaReceiver(DeltaReceiver receiver){
        this.deltaReceiver = receiver;
        ukladanieDelty = new Ukladanie_delt(this.deltaReceiver);
    }

    /**
     * Przekazanie danych do przetworzenia.
     *
     * @param data obiekt z danymi do przetworzenia
     */
    public void addData(Data data) {
        calculations_amount.put(data, 0);
        add_and_sort_vectors(data);

        ukladanieDelty.set_rozmiar_wektora(data.getSize());

        System.out.println("lista wektorów w kalkulatorze: " + vectors_set);

        List<Data> usuwane = new ArrayList<Data>();

        for(int i = 0; i< vectors_set.size(); i++){
            Data zListy = vectors_set.get(i);


            try {
                if (( zListy.getDataId() == data.getDataId() + 1) || (zListy.getDataId() == data.getDataId() - 1)){
                    for(int j=0; j<zListy.getSize(); j++){
                        Task nowytask = new Task(zListy, data, j, j);

                        ochrona_listy_taskow.acquire(); // rozpoczynam ochrone listy taskow

//                        lista_taskow.add(nowytask);
                        sort_tasks(nowytask);

                        System.out.println("Ilosc tasków: " + lista_taskow.size() + " lista: "+ lista_taskow);
                        ochrona_listy_taskow.release(); // koncze ochrone listy taskow
                        czekaj_na_niepuste_taski.release(); // dodaj do semafora 1
                    }

                    int licznik_zlisty = calculations_amount.get(zListy);
                    int licznik_data = calculations_amount.get(data);
                    licznik_zlisty ++;
                    licznik_data ++;

                    if (licznik_zlisty == 2){
                        usuwane.add(zListy);
                        calculations_amount.remove(zListy);
                    } else {
                        calculations_amount.put(zListy, licznik_zlisty);
                    }

                    if (licznik_data == 2){
                        usuwane.add(data);
                        calculations_amount.remove(data);
                    } else {
                        calculations_amount.put(data, licznik_data);
                    }
                }
            } catch (InterruptedException ex){
                ex.printStackTrace();
            }
        }

        vectors_set.removeAll(usuwane);

        System.out.println("LISTA WEKTOROW: " + vectors_set);
    }

    public void add_and_sort_vectors(Data data){
        if (vectors_set.size() == 0){
            vectors_set.add(data);
        } else {
            for (int i = 0; i< vectors_set.size(); i++){
                if (data.getDataId() < vectors_set.get(i).getDataId()){
                    vectors_set.add(i, data);
                    break;
                }
            }
            if (!vectors_set.contains(data)){
                vectors_set.add(data);
            }
        }
    }

    public void sort_tasks(Task task) {
        if (lista_taskow.size() == 0){
            lista_taskow.add(task);
        } else {
            for (int i=0; i<lista_taskow.size(); i++){
                if (task.daj_mniejsze_id() < lista_taskow.get(i).daj_mniejsze_id()){
                    lista_taskow.add(i, task);
                    return;
                }
            }
            if (!lista_taskow.contains(task)){
                lista_taskow.add(task);
            }
        }
    }

    public Task dajKolejnyTask(int id) throws InterruptedException {

        System.out.println("Watek " + id+ " stoi przed semaforem");
        czekaj_na_niepuste_taski.acquire(); // stoi w miejscu dla 0 lub wykonuje -1 jesli jest dodatni
//        System.out.println("Watek " + id+ " nie czeka juz na semaforze");

        ochrona_listy_taskow.acquire();
        Task zabrany_task = lista_taskow.remove(0);
//        System.out.println("Watek " + id+ " wziął task:" + zabrany_task + " size: "+ lista_taskow.size() + "lista: " + lista_taskow);
        ochrona_listy_taskow.release();

        return zabrany_task;
    }
}
