import java.util.Arrays;

public class ZestawWektor implements Data {
    private int id;
    private int[] tablica;

    public ZestawWektor(int id, int[] dane_wejsciowe){
        this.id = id;
        this.tablica = new int[dane_wejsciowe.length];

        for(int i=0; i<dane_wejsciowe.length; i++){
            tablica[i] = dane_wejsciowe[i];
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
        return tablica.length;
    }

    /**
     * Odczyt danej z podanego indeksu. Poprawne indeksy dla danych mieszczą się od
     * 0 do getSize-1.
     *
     * @param idx numer indeksu
     * @return odczytana wartość
     */
    public int getValue(int idx){
        return tablica[idx];
    }

    @Override
    public String toString() {
        return "ZestawWektor{" +
                "id=" + id +
                ", " + Arrays.toString(tablica) +
                '}';
    }
}
