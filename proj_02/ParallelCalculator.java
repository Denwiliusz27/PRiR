import java.util.*;


public class ParallelCalculator implements DeltaParallelCalculator {
    private List<Data> vectors = new ArrayList<Data>(); // lista wektorów
    private HashMap<Integer, Integer> calculationsAmount = new HashMap<Integer, Integer>(); // mapa ile który wektor był obliczany

    private int nrOfThreads;
    private List<Task> tasks = new ArrayList<Task>();
    private DeltaReceiver deltaReceiver;
    private DeltaCollecting collectingDelta;


    /**
     * Metoda ustala liczbę wątków jaka ma być użyta do liczenia
     * delty.
     *
     * @param threads liczba wątków.
     */
    public void setThreadsNumber(int threads){
//        setDeltaReceiver(new ReceiveOfResults());
        nrOfThreads = threads;
    }

//    public void startThreads(){
//        for (CountingThread countingThread : countingThreads) {
//            countingThread.start();
//        }
//    }

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

        Data data1;
        Data data2;

        for (Data vectorFromList : vectors) {
            if (calculationsAmount.get(vectorFromList.getDataId()) == null) {
                continue;
            }

            if ((vectorFromList.getDataId() == data.getDataId() + 1) || (vectorFromList.getDataId() == data.getDataId() - 1)) {

                data1 = vectorFromList;
                data2 = data;  // koniec ifa i fora


                int chunk = vectorFromList.getSize() / nrOfThreads;
                if (chunk == 0){
                    chunk = 1;
                }


                for (int j = 0; j < vectorFromList.getSize(); j += chunk) {
                    int endIdx = Math.min(vectorFromList.getSize(), j+chunk);
                    Task newTask = new Task(vectorFromList, data, j, endIdx);

                    sortTasks(newTask);
                }

                var countingThreads = new CountingThread[nrOfThreads];

                for(int i=1; i<nrOfThreads; i++){
                    countingThreads[i] = new CountingThread( this, collectingDelta);
                    countingThreads[i].start();
                }

                countingThreads[0] = new CountingThread( this, collectingDelta);
                countingThreads[0].run();



                for(int i=1; i<countingThreads.length; i++){
                    try {
                        countingThreads[i].join();
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
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
        }
    }

    public Task getNextTask(){
//        System.out.println("Watek " + id+ " stoi przed semaforem");
        Task taskFromList;

        synchronized (tasks){
            if (tasks.size() == 0){
                return null;
            }

            taskFromList = tasks.remove(0);
        }

        return taskFromList;
    }
}


class CountingThread extends Thread {
    private ParallelCalculator parallelCalculator;
    private DeltaCollecting deltsCalculation;

    public CountingThread(ParallelCalculator parallelCalculator, DeltaCollecting deltsCalculation){
        this.parallelCalculator = parallelCalculator;
        this.deltsCalculation = deltsCalculation;
    }

    public void run(){
        while(true){
            Task task = parallelCalculator.getNextTask( );
//                System.out.println("Watek "+ id + " otrzymalem task " + task + " i zaczynam go wykonywać");
//                System.out.println(id + " sprawdzam pare: " + task.getFirstVector().getDataId() + " i " + task.getSecondVector().getDataId());
            if (task == null){
                return;
            }

            calculateTask(task);
        }
    }

    private void calculateTask(Task task){

        List<Delta> deltas = new ArrayList<Delta>();
        int delta_id = task.getSmallerId();
        int checking_index = task.getFirstIndex();
        Data vector1 = task.getFirstVector();
        Data vector2 = task.getSecondVector();

        for (int i=checking_index; i<task.getSecondIndex(); i++){
            int difference = vector2.getValue(i) - vector1.getValue(i);
            Delta delta = new Delta(delta_id, i, difference);
            deltas.add(delta);
        }

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

    public int getSecondIndex(){
        return endIndex;
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

            if (sublist.size() != 0){
                deltaReceiver.accept(sublist);
            }

//            deltaReceiver.accept(sublist);
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
