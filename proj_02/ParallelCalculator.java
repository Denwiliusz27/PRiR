import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;


public class ParallelCalculator implements DeltaParallelCalculator {
    private List<Data> wektor_list;
    private Watek_liczacy[] watki_liczace;
    private List<Task> lista_taskow;
    private Semaphore czekaj_na_niepuste_taski;
    private Semaphore ochrona_listy_taskow;
    private DeltaReceiver deltaReceiver;


    public ParallelCalculator(){
        wektor_list = new ArrayList<Data>();
        lista_taskow = new ArrayList<Task>();
        czekaj_na_niepuste_taski = new Semaphore(0);
        ochrona_listy_taskow = new Semaphore(1);
    }

    /**
     * Metoda ustala liczbę wątków jaka ma być użyta do liczenia
     * delty.
     *
     * @param threads liczba wątków.
     */
    public void setThreadsNumber(int threads){
       setDeltaReceiver(new OdbiorWynikow());
       watki_liczace = new Watek_liczacy[threads];

       for(int i=0; i<threads; i++){
           watki_liczace[i] = new Watek_liczacy( i, this, deltaReceiver);
       }
       startuj_watki();
    }

    public void startuj_watki(){
        for(int i=0; i < watki_liczace.length; i++){
            watki_liczace[i].start();
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
    }

    /**
     * Przekazanie danych do przetworzenia.
     *
     * @param data obiekt z danymi do przetworzenia
     */
    public void addData(Data data) {
        wektor_list.add(data);
        System.out.println("lista wektorów w kalkulatorze: " + wektor_list);

        for(int i=0; i<wektor_list.size(); i++){
            Data zListy = wektor_list.get(i);

            try {
                for(int j=0; j<zListy.getSize(); j++){
                    if (( zListy.getDataId() == data.getDataId() + 1) || (zListy.getDataId() == data.getDataId() - 1)){
                        Task nowytask = new Task(zListy, data, j, j);
                        ochrona_listy_taskow.acquire(); // rozpoczynam ochrone listy taskow
                        lista_taskow.add(nowytask);
                        System.out.println("Ilosc tasków: " + lista_taskow.size() + "lista: "+ lista_taskow);
                        ochrona_listy_taskow.release(); // koncze ochrone listy taskow
                        czekaj_na_niepuste_taski.release(); // dodaj do semafora 1
                    }
                }
            } catch (InterruptedException ex){
                ex.printStackTrace();
            }
        }
    }

    public Task dajKolejnyTask(int id) throws InterruptedException {

        System.out.println("Watek " + id+ " stoi przed semaforem");
        czekaj_na_niepuste_taski.acquire(); // stoi w miejscu dla 0 lub wykonuje -1 jesli jest dodatni
        System.out.println("Watek " + id+ " nie czeka juz na semaforze");

        ochrona_listy_taskow.acquire();
        Task zabrany_task = lista_taskow.remove(0);
        System.out.println("Watek " + id+ " wziął task:" + zabrany_task + " size: "+ lista_taskow.size() + "lista: " + lista_taskow);
        ochrona_listy_taskow.release();

        return zabrany_task;
    }
}
