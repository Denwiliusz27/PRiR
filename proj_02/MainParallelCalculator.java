public class MainParallelCalculator {
    private ParallelCalculator parallelCalculator;

    public void uruchom(){
        parallelCalculator = new ParallelCalculator();
//        parallelCalculator.setDeltaReceiver(new OdbiorWynikow());
        parallelCalculator.setThreadsNumber(40);

        int[] t1 = {1, 0, 0, 0, 0, 0};
        int[] t2 = {0, 1, 0, 0, 0, 0};
        int[] t3 = {0, 0, 1, 0, 0, 0};
        int[] t4 = {0, 0, 0, 1, 0, 0};
        int[] t5 = {0, 0, 0, 0, 1, 0};
        int[] t6 = {0, 0, 0, 0, 0, 1};
        int[] t7 = {1, 1, 0, 0, 0, 0};
        int[] t8 = {0, 1, 1, 0, 0, 0};
        int[] t9 = {0, 0, 1, 1, 0, 0};


        ZestawWektor zestaw_wektor = new ZestawWektor(2, t2);
        parallelCalculator.addData(zestaw_wektor);


//        try {
//            Thread.sleep(3000);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }

        zestaw_wektor = new ZestawWektor(5, t5);
        parallelCalculator.addData(zestaw_wektor);

//        try {
//            Thread.sleep(3000);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }

        zestaw_wektor = new ZestawWektor(3, t3);
        parallelCalculator.addData(zestaw_wektor);


//        try {
//            Thread.sleep(3000);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }

        zestaw_wektor = new ZestawWektor(6, t6);
        parallelCalculator.addData(zestaw_wektor);

//        try {
//            Thread.sleep(3000);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }

        zestaw_wektor = new ZestawWektor(4, t4);
        parallelCalculator.addData(zestaw_wektor);

//        try {
//                Thread.sleep(3000);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }

        zestaw_wektor = new ZestawWektor(1, t1);
        parallelCalculator.addData(zestaw_wektor);


//        try {
//            Thread.sleep(3000);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }

        zestaw_wektor = new ZestawWektor(7, t7);
        parallelCalculator.addData(zestaw_wektor);

//        try {
//            Thread.sleep(3000);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }


        int[] tablica = {1, 2, 3, 4, 5, 7};


        for(int i=100; i>=8; i--){
            zestaw_wektor = new ZestawWektor(i, tablica);
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
