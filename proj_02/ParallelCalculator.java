import java.lang.reflect.Array;
import java.util.*;
import java.util.concurrent.Semaphore;


public class ParallelCalculator implements DeltaParallelCalculator {
    private List<Data> vectors = new ArrayList<Data>(); // lista wektorów
    private HashMap<Integer, Integer> calculationsAmount = new HashMap<Integer, Integer>(); // mapa ile który wektor był obliczany
    private CountingThread[] countingThreads;
    private List<Task> tasks = new ArrayList<Task>();
    private Semaphore waitForTasks = new Semaphore(0);
    private Semaphore tasksSemaphore = new Semaphore(1);
    private DeltaReceiver deltaReceiver;
    private DeltaCollecting collectingDelta;


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
        for (CountingThread countingThread : countingThreads) {
            countingThread.start();
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
        calculationsAmount.put(data.getDataId(), 0);
        addAndSortVectors(data);
        collectingDelta.setVectorSize(data.getSize());

//        System.out.println("lista wektorów w kalkulatorze: " + vectors);
        List<Data> vectorsToDelete = new ArrayList<Data>();

        for (Data vectorFromList : vectors) {
            if (calculationsAmount.get(vectorFromList.getDataId()) == null) {
                continue;
            }

            if ((vectorFromList.getDataId() == data.getDataId() + 1) || (vectorFromList.getDataId() == data.getDataId() - 1)) {
                for (int j = 0; j < vectorFromList.getSize(); j++) {
                    Task newTask = new Task(vectorFromList, data, j, j);

//                    tasksSemaphore.acquireUninterruptibly(); // rozpoczynam ochrone listy taskow
                    sortTasks(newTask);
//                    System.out.println("Ilosc tasków: " + tasks.size() + " lista: "+ tasks);
//                    tasksSemaphore.release(); // koncze ochrone listy taskow
                    waitForTasks.release(); // dodaj do semafora 1
                }

                int vectorFromListCounter = calculationsAmount.get(vectorFromList.getDataId());
                int dataCounter = calculationsAmount.get(data.getDataId());
                vectorFromListCounter++;
                dataCounter++;

                if (vectorFromListCounter == 2) {
                    vectorsToDelete.add(vectorFromList);
                    calculationsAmount.remove(vectorFromList.getDataId());
                } else {
                    calculationsAmount.put(vectorFromList.getDataId(), vectorFromListCounter);
                }

                if (dataCounter == 2) {
                    vectorsToDelete.add(data);
                    calculationsAmount.remove(data.getDataId());
                } else {
                    calculationsAmount.put(data.getDataId(), dataCounter);
                }
            }
        }
        vectors.removeAll(vectorsToDelete);
//        System.out.println("LISTA WEKTOROW POOOO: " + vectors);
    }

    public void addAndSortVectors(Data data){
       for (int i = 0; i< vectors.size(); i++){
           if (data.getDataId() < vectors.get(i).getDataId()){
               vectors.add(i, data);
               return;
           }
       }
       vectors.add(data);
    }

    public void sortTasks(Task task) {
        synchronized (tasks){
            tasks.add(task);
            tasks.sort((Task task1,Task task2) -> task2.getSmallerId() - task1.getSmallerId());
//
//            if (tasks.size() == 0){
//                tasks.add(task);
//            } else {
//                for (int i = 0; i< tasks.size(); i++){
//                    if (task.getSmallerId() < tasks.get(i).getSmallerId()){
//                        tasks.add(i, task);
//                        return;
//                    }
//                }
//                if (!tasks.contains(task)){
//                    tasks.add(task);
//                }
//            }
        }

    }

    public Task getNextTask(){
//        System.out.println("Watek " + id+ " stoi przed semaforem");
        Task taskFromList;

        waitForTasks.acquireUninterruptibly(); // stoi w miejscu dla 0 lub wykonuje -1 jesli jest dodatni
        synchronized (tasks){
            taskFromList = tasks.remove(0);
        }

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
            Task task = parallelCalculator.getNextTask( );
//                System.out.println("Watek "+ id + " otrzymalem task " + task + " i zaczynam go wykonywać");
//                System.out.println(id + " sprawdzam pare: " + task.getFirstVector().getDataId() + " i " + task.getSecondVector().getDataId());
            calculateTask(task);
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
        if (dataSet1.getDataId() < dataSet2.getDataId()){
            this.dataSet1 = dataSet1;
            this.dataSet2 = dataSet2;
        } else {
            this.dataSet1 = dataSet2;
            this.dataSet2 = dataSet1;
        }
        this.startIndex = startIndex;
        this.endIndex = endIndex;
    }

    public int getSmallerId(){
        return Math.min( dataSet1.getDataId(), dataSet2.getDataId() );
    }

    public int getFirstIndex(){
        return startIndex;
    }

    public Data getFirstVector(){
        return dataSet1;
    }

    public Data getSecondVector(){
        return dataSet2;
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
    private final DeltaReceiver deltaReceiver;
    private final List<Delta> sortedDelts;
    private int computingVectorId;
    private int vectorSize;


    public DeltaCollecting(DeltaReceiver deltaReceiver){
        this.deltaReceiver = deltaReceiver;
        sortedDelts = new ArrayList<Delta>();
        computingVectorId = 0;
    }

    public void setVectorSize(int size){
        this.vectorSize = size;
    }

    public synchronized void returnDelta(List<Delta> delts){
        sortAndSendToReceiver(delts);
    }

    public synchronized void sortAndSendToReceiver(List<Delta> newDelts){
        for (Delta d : newDelts) {
            addDeltaAndSort(d);
        }
        sendDeltsToReceiver();
    }

    public synchronized void addDeltaAndSort(Delta delta){
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

    public synchronized void sendDeltsToReceiver(){
        if (sortedDelts.size() < vectorSize){
            return;
        }

        if (sortedDelts.get(0).getDataID() == computingVectorId){
            for (int i = 0; i< vectorSize; i++){
                if (sortedDelts.get(i).getDataID() != computingVectorId) {
//                    System.out.println("BŁĄD dla " + sortedDelts.get(i).getDataID());
                    return;
                }
            }

            List<Delta> sublist = new ArrayList<Delta>();

            for (int i = 0; i< vectorSize; i++){
                Delta vectorToSend = sortedDelts.remove(0);

                if (vectorToSend.getDelta() != 0) {
                    sublist.add(vectorToSend);
                }
            }

            sublist.sort((s1, s2) -> {
                return s2.getIdx() - s1.getIdx();
            });

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
