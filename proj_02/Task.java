

public class Task {
    private Data zestawWektor1;
    private Data zestawWektor2;
    private int poczatek_przedzialu;
    private int koniec_przedzialu;


    public Task(Data zestaw1, Data zestaw2, int poczatek, int koniec){
        this.zestawWektor1 = zestaw1;
        this.zestawWektor2 = zestaw2;
        this.poczatek_przedzialu = poczatek;
        this.koniec_przedzialu = koniec;
//        System.out.println("Utworzony task: " + toString());
    }

    public int daj_mniejsze_id(){
        if (zestawWektor1.getDataId() < zestawWektor2.getDataId()){
            return zestawWektor1.getDataId();
        } else {
            return zestawWektor2.getDataId();
        }
    }

    public int daj_poczatkowy_index(){
        return poczatek_przedzialu;
    }

    public Data daj_pierwszy_wektor(){
        int id = daj_mniejsze_id();

        if (id == zestawWektor1.getDataId()){
            return zestawWektor1;
        } else {
            return zestawWektor2;
        }
    }

    public Data daj_drugi_wektor(){
        int id = daj_mniejsze_id();

        if (id == zestawWektor1.getDataId()){
            return zestawWektor2;
        } else {
            return zestawWektor1;
        }
    }



    @Override
    public String toString() {
        return "Task{" +
                zestawWektor1 +
                ", " + zestawWektor2 +
                ", [" + poczatek_przedzialu +
                ", " + koniec_przedzialu +
                "]}";
    }
}
