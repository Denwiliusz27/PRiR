public class MainParallelCalculator {
    private ParallelCalculator parallelCalculator;

    public void uruchom(){
        parallelCalculator = new ParallelCalculator();
//        parallelCalculator.setDeltaReceiver(new OdbiorWynikow());
        parallelCalculator.setThreadsNumber(40);

        int[] tablica = {1, 2, 3, 4, 5, 7};

        for(int i=0; i<100; i++){
            ZestawWektor zestaw_wektor = new ZestawWektor(i, tablica);
            parallelCalculator.addData(zestaw_wektor);
            zmien_tablice(tablica);
//            try {
//                Thread.sleep(3000);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
        }
    }

    private void zmien_tablice(int[ ] tablica){
        for (int i=0; i<tablica.length; i++){
            tablica[i] += 1;
        }
    }

    public static void main(String[] args){
        MainParallelCalculator main_calc = new MainParallelCalculator();
        main_calc.uruchom();
    }
}
