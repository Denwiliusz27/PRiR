import java.util.*;


public class ParallelCalculator implements DeltaParallelCalculator {
    private final List<Data> vectors = new ArrayList<Data>(); // lista wektorów
    private HashMap<Integer, Integer> calculationsAmount = new HashMap<Integer, Integer>(); // mapa ile który wektor był obliczany
    private int nrOfThreads;
    private DeltaReceiver deltaReceiver;
    private DeltaCollecting collectingDelta;
    private Boolean isWorking = Boolean.FALSE;


    /**
     * Metoda ustala liczbę wątków jaka ma być użyta do liczenia
     * delty.
     *
     * @param threads liczba wątków.
     */
    public void setThreadsNumber(int threads){
        nrOfThreads = threads;
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
        synchronized (vectors){
            calculationsAmount.put(data.getDataId(), 0);
            addAndSortVectors(data);
            collectingDelta.setVectorSize(data.getSize());

            if (!isWorking) {
                var helpThread = new HelpThread(vectors, calculationsAmount, collectingDelta, nrOfThreads, isWorking);
                helpThread.start();
                isWorking = true;
            }
        }
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
}


class HelpThread extends Thread {
    private final List<Data> vectors;
    private HashMap<Integer, Integer> calculationsAmount; // mapa ile który wektor był obliczany
    private int nrOfThreads;
    private DeltaCollecting collectingDelta;
    private Boolean isWorking;
    private List<Task> tasks = new ArrayList<Task>();


    public HelpThread( List<Data> vectors, HashMap<Integer, Integer> calculationsAmount, DeltaCollecting collectingDelta, int nrOfThreads, Boolean isWorking){
        this.vectors = vectors;
        this.calculationsAmount = calculationsAmount;
        this.collectingDelta = collectingDelta;
        this.nrOfThreads = nrOfThreads;
        this.isWorking = isWorking;
    }

    public void run(){
        Data data1 = null, data2 = null;

        while(true){
            boolean found = false;

            synchronized (vectors){
                for (int i=1; i<vectors.size(); i++){
                    data1 = vectors.get(i-1);
                    data2 = vectors.get(i);

                    if (calculationsAmount.get(data1.getDataId()) == 1){
                        continue;
                    }

                    if (data2.getDataId() == data1.getDataId() + 1) {
                        found = true;
                        break;
                    }
                }

                if (!found){
                    isWorking = false;
                    return;
                }
            }

            int chunk = data1.getSize() / nrOfThreads;
            if (chunk == 0){
                chunk = 1;
            }

            var countingThreads = new CountingThread[nrOfThreads];

            for (int j = 0, i=0 ; j < data1.getSize(); j += chunk, i++) {
                int endIdx = Math.min(data1.getSize(), j+chunk);
                Task newTask = new Task(data1, data2, j, endIdx);

                countingThreads[i] = new CountingThread( this, collectingDelta, newTask);
            }

            for(int i=1; i<nrOfThreads; i++){
                countingThreads[i].start();
            }

            countingThreads[0].run();

            for(int i=1; i<countingThreads.length; i++){
                try {
                    countingThreads[i].join();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }

            List<Delta> delts = new ArrayList<>();

            for(int i=0; i<countingThreads.length; i++){
                delts.addAll(countingThreads[i].getDeltas());
            }

            collectingDelta.returnDelta(data1.getDataId(), delts);
            calculationsAmount.put(data1.getDataId(), 1);
        }
    }


    public void sortTasks(Task task) {
        synchronized (tasks){
            tasks.add(task);
            tasks.sort((Task task1,Task task2) -> task2.getSmallerId() - task1.getSmallerId());
        }
    }

    public Task getNextTask(){
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
    private HelpThread helpThread;
    private DeltaCollecting deltsCalculation;
    private Task newTask;

    private List<Delta> deltas = new ArrayList<Delta>();;

    public CountingThread(HelpThread helpThread, DeltaCollecting deltsCalculation, Task newTask){
        this.helpThread = helpThread;
        this.deltsCalculation = deltsCalculation;
        this.newTask = newTask;
    }

    public void run(){
        calculateTask(newTask);
    }

    public List<Delta> getDeltas(){
        return deltas;
    }

    private void calculateTask(Task task){
        int delta_id = task.getSmallerId();
        int checking_index = task.getFirstIndex();
        Data vector1 = task.getFirstVector();
        Data vector2 = task.getSecondVector();

        for (int i=checking_index; i<task.getSecondIndex(); i++){
            int difference = vector2.getValue(i) - vector1.getValue(i);

            if (difference == 0){
                continue;
            }

            Delta delta = new Delta(delta_id, i, difference);
            deltas.add(delta);
        }
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
    private final Map<Integer, List<Delta>> sortedDelts;
    private int computingVectorId;
    private int vectorSize;


    public DeltaCollecting(DeltaReceiver deltaReceiver){
        this.deltaReceiver = deltaReceiver;
        sortedDelts = new HashMap<>();
        computingVectorId = 0;
    }

    public void setVectorSize(int size){
        this.vectorSize = size;
    }

    public synchronized void returnDelta(int id, List<Delta> delts){
        sortAndSendToReceiver(id, delts);
    }

    public synchronized void sortAndSendToReceiver(int id, List<Delta> newDelts){
        sortedDelts.put(id ,newDelts);
        sendDeltsToReceiver();
    }


    public synchronized void sendDeltsToReceiver(){
        while (sortedDelts.containsKey(computingVectorId)){
            if (sortedDelts.get(computingVectorId).size() != 0){
                deltaReceiver.accept(sortedDelts.get(computingVectorId));
            }
            computingVectorId++;
        }
    }
}


class ReceiveOfResults implements DeltaReceiver {

    public void accept( List<Delta> deltas ){
//        System.out.println("******************************************");
        System.out.println("Result received: " + deltas);
    }
}
