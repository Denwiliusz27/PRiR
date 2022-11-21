import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Semaphore;


public class ParallelCalculator implements DeltaParallelCalculator {
    private List<Data> vectors; // lista wektorów
    private HashMap<Data, Integer> calculationsAmount; // mapa ile który wektor był obliczany
    private CountingThread[] countingThreads;
    private List<Task> tasks;
    private Semaphore waitForTasks;
    private Semaphore tasksSemaphore;
    private DeltaReceiver deltaReceiver;
    private DeltaCollecting collectingDelta;


    public ParallelCalculator(){
        vectors = new ArrayList<Data>();
        tasks = new ArrayList<Task>();
        waitForTasks = new Semaphore(0);
        tasksSemaphore = new Semaphore(1);
        calculationsAmount = new HashMap<Data, Integer>();
    }

    /**
     * Metoda ustala liczbę wątków jaka ma być użyta do liczenia
     * delty.
     *
     * @param threads liczba wątków.
     */
    public void setThreadsNumber(int threads){
       setDeltaReceiver(new ReceiveOfResults());
       countingThreads = new CountingThread[threads];

       for(int i=0; i<threads; i++){
           countingThreads[i] = new CountingThread( i, this, collectingDelta);
       }
       startThreads();
    }

    public void startThreads(){
        for(int i = 0; i < countingThreads.length; i++){
            countingThreads[i].start();
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
        collectingDelta = new DeltaCollecting(this.deltaReceiver);
    }

    /**
     * Przekazanie danych do przetworzenia.
     *
     * @param data obiekt z danymi do przetworzenia
     */
    public void addData(Data data) {
        calculationsAmount.put(data, 0);
        addAndSortVectors(data);
        collectingDelta.setVectorSize(data.getSize());

//        System.out.println("lista wektorów w kalkulatorze: " + vectors);

        List<Data> vectorsToDelete = new ArrayList<Data>();

        for(int i = 0; i< vectors.size(); i++){
            Data vectorFromList = vectors.get(i);

            try {
                if (( vectorFromList.getDataId() == data.getDataId() + 1) || (vectorFromList.getDataId() == data.getDataId() - 1)){
                    for(int j=0; j<vectorFromList.getSize(); j++){
                        Task newTask = new Task(vectorFromList, data, j, j);

                        tasksSemaphore.acquire(); // rozpoczynam ochrone listy taskow
                        sortTasks(newTask);
//                        System.out.println("Ilosc tasków: " + tasks.size() + " lista: "+ tasks);
                        tasksSemaphore.release(); // koncze ochrone listy taskow
                        waitForTasks.release(); // dodaj do semafora 1
                    }

                    int vectorFromListCounter = calculationsAmount.get(vectorFromList);
                    int dataCounter = calculationsAmount.get(data);
                    vectorFromListCounter ++;
                    dataCounter ++;

                    if (vectorFromListCounter == 2){
                        vectorsToDelete.add(vectorFromList);
                        calculationsAmount.remove(vectorFromList);
                    } else {
                        calculationsAmount.put(vectorFromList, vectorFromListCounter);
                    }

                    if (dataCounter == 2){
                        vectorsToDelete.add(data);
                        calculationsAmount.remove(data);
                    } else {
                        calculationsAmount.put(data, dataCounter);
                    }
                }
            } catch (InterruptedException ex){
                ex.printStackTrace();
            }
        }
        vectors.removeAll(vectorsToDelete);
//        System.out.println("LISTA WEKTOROW: " + vectors);
    }

    public void addAndSortVectors(Data data){
        if (vectors.size() == 0){
            vectors.add(data);
        } else {
            for (int i = 0; i< vectors.size(); i++){
                if (data.getDataId() < vectors.get(i).getDataId()){
                    vectors.add(i, data);
                    break;
                }
            }
            if (!vectors.contains(data)){
                vectors.add(data);
            }
        }
    }

    public void sortTasks(Task task) {
        if (tasks.size() == 0){
            tasks.add(task);
        } else {
            for (int i = 0; i< tasks.size(); i++){
                if (task.getSmallerId() < tasks.get(i).getSmallerId()){
                    tasks.add(i, task);
                    return;
                }
            }
            if (!tasks.contains(task)){
                tasks.add(task);
            }
        }
    }

    public Task getNextTask(int id) throws InterruptedException {
//        System.out.println("Watek " + id+ " stoi przed semaforem");
        waitForTasks.acquire(); // stoi w miejscu dla 0 lub wykonuje -1 jesli jest dodatni
        tasksSemaphore.acquire();
        Task taskFromList = tasks.remove(0);
        tasksSemaphore.release();

        return taskFromList;
    }
}


class CountingThread extends Thread {
    private int id;
    private ParallelCalculator parallelCalculator;
    private DeltaCollecting deltsCalculation;

    public CountingThread(int id, ParallelCalculator parallelCalculator, DeltaCollecting deltsCalculation){ // 4 parametr Semaphore ochrona receivera
        this.id = id;
        this.parallelCalculator = parallelCalculator;
        this.deltsCalculation = deltsCalculation;
    }

    public void run(){
        while(true){
            try {
                Task task = parallelCalculator.getNextTask( id );
//                System.out.println("Watek "+ id + " otrzymalem task " + task + " i zaczynam go wykonywać");
//                System.out.println(id + " sprawdzam pare: " + task.getFirstVector().getDataId() + " i " + task.getSecondVector().getDataId());
                calculateTask(task);
            } catch (InterruptedException ex) {
                ex.printStackTrace();
            }
        }
    }

    private void calculateTask(Task task){
        List<Delta> deltas = new ArrayList<Delta>();
        int delta_id = task.getSmallerId();
        int checking_index = task.getFirstIndex();
        Data vector1 = task.getFirstVector();
        Data vector2 = task.getSecondVector();

        int difference = vector2.getValue(checking_index) - vector1.getValue(checking_index);
        Delta delta = new Delta(delta_id, checking_index, difference);
//        System.out.println("watek " + id + " ma delte:" + delta);
        deltas.add(delta);

        deltsCalculation.returnDelta(deltas);
    }
}


class Task {
    private Data dataSet1;
    private Data dataSet2;
    private int startIndex;
    private int endIndex;

    public Task(Data dataSet1, Data dataSet2, int startIndex, int endIndex){
        this.dataSet1 = dataSet1;
        this.dataSet2 = dataSet2;
        this.startIndex = startIndex;
        this.endIndex = endIndex;
    }

    public int getSmallerId(){
        if (dataSet1.getDataId() < dataSet2.getDataId()){
            return dataSet1.getDataId();
        } else {
            return dataSet2.getDataId();
        }
    }

    public int getFirstIndex(){
        return startIndex;
    }

    public Data getFirstVector(){
        int id = getSmallerId();

        if (id == dataSet1.getDataId()){
            return dataSet1;
        } else {
            return dataSet2;
        }
    }

    public Data getSecondVector(){
        int id = getSmallerId();

        if (id == dataSet1.getDataId()){
            return dataSet2;
        } else {
            return dataSet1;
        }
    }

    @Override
    public String toString() {
        return "Task{" +
                dataSet1 +
                ", " + dataSet2 +
                ", [" + startIndex +
                ", " + endIndex +
                "]}";
    }
}


class DataSet implements Data {
    private int id;
    private int[] vector;

    public DataSet(int id, int[] vector){
        this.id = id;
        this.vector = new int[vector.length];

        for(int i=0; i<vector.length; i++){
            this.vector[i] = vector[i];
        }
    }

    /**
     * Numer zestawu danych. Każdy zestaw danych ma unikalny numer. Zestawy
     * numerowane są od 0 wzwyż.
     *
     * @return liczba całkowita oznaczająca numer zestawu danych
     */
    public int getDataId(){
        return id;
    }

    /**
     * Rozmiar zestawu danych. Poprawne indeksy dla danych mieszczą się od 0 do
     * getSize-1.
     *
     * @return liczba danych.
     */
    public int getSize(){
        return vector.length;
    }

    /**
     * Odczyt danej z podanego indeksu. Poprawne indeksy dla danych mieszczą się od
     * 0 do getSize-1.
     *
     * @param idx numer indeksu
     * @return odczytana wartość
     */
    public int getValue(int idx){
        return vector[idx];
    }

    @Override
    public String toString() {
        return "Set{" +
                "id=" + id +
                ", " + Arrays.toString(vector) +
                '}';
    }
}


class DeltaCollecting {
    private DeltaReceiver deltaReceiver;
    private Semaphore sortedDeltsSemaphore;
    private List<Delta> sortedDelts;
    private int computingVectorId;
    private int vectorSize;


    public DeltaCollecting(DeltaReceiver deltaReceiver){
        this.deltaReceiver = deltaReceiver;
        sortedDeltsSemaphore = new Semaphore(1);
        sortedDelts = new ArrayList<Delta>();
        computingVectorId = 1;
    }

    public void setVectorSize(int size){
        this.vectorSize = size;
    }

    public void returnDelta(List<Delta> delts){
        try {
            sortedDeltsSemaphore.acquire();

            sortAndSendToReceiver(delts);
//            System.out.println("Lista delt: " + sortedDelts);

            sortedDeltsSemaphore.release();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void sortAndSendToReceiver(List<Delta> newDelts){
        for (int i=0; i<newDelts.size(); i++){
            Delta d = newDelts.get(i);
            addDeltaAndSort(d);
        }
        sendDeltsToReceiver();
    }

    public void addDeltaAndSort(Delta delta){
        if (sortedDelts.size()==0){
            sortedDelts.add(delta);
        } else {
            for (int i = 0; i< sortedDelts.size(); i++){
                if (delta.getDataID() < sortedDelts.get(i).getDataID()){
                    sortedDelts.add(i, delta);
                    return;
                }
            }
            if (!sortedDelts.contains(delta)){
                sortedDelts.add(delta);
            }
        }
    }

    public void sendDeltsToReceiver(){
        if (sortedDelts.size() < vectorSize){
            return;
        }

        if (sortedDelts.get(0).getDataID() == computingVectorId){
            for (int i = 0; i< vectorSize; i++){
                if (i < sortedDelts.size()){
                    if (sortedDelts.get(i).getDataID() != computingVectorId) {
                        return;
                    }
                }
            }

            List<Delta> sublist = new ArrayList<Delta>();

            for (int i = 0; i< vectorSize; i++){
                Delta vectorToSend = sortedDelts.remove(0);
                sublist.add(vectorToSend);
            }

            deltaReceiver.accept(sublist);
            computingVectorId++;
            sendDeltsToReceiver();
        }
    }
}


class ReceiveOfResults implements DeltaReceiver {

    public void accept( List<Delta> deltas ){
//        System.out.println("******************************************");
        System.out.println("Result received: " + deltas);
    }
}
