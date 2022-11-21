import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;

public class Ukladanie_delt {
    private DeltaReceiver deltaReceiver;
    private Semaphore ochronaPosortowaneDelty;
    private List<Delta> posortowane_delty;
    private int aktualne_id;
    private int rozmiar_wektora;


    public Ukladanie_delt(DeltaReceiver deltaRec){
        this.deltaReceiver = deltaRec;
        ochronaPosortowaneDelty = new Semaphore(1);
        posortowane_delty = new ArrayList<Delta>();
        aktualne_id = 1;
    }

    public void set_rozmiar_wektora(int rozmiar){
        this.rozmiar_wektora = rozmiar;
    }

    public void oddaj_delta(List<Delta> deltas){
        try {
            ochronaPosortowaneDelty.acquire();

            sortuj_i_oddawaj_do_receivera(deltas);
            System.out.println("Lista delt: " + posortowane_delty);

            ochronaPosortowaneDelty.release();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void sortuj_i_oddawaj_do_receivera(List<Delta> nowe_delty){
        for (int i=0; i<nowe_delty.size(); i++){
            Delta d = nowe_delty.get(i);
            dodaj_jedna_delte(d);
        }

        wyslij_delty_do_receivera();
    }

    public void dodaj_jedna_delte(Delta delta){
        if (posortowane_delty.size()==0){
            posortowane_delty.add(delta);
        } else {
            for (int i=0; i<posortowane_delty.size(); i++){
                if (delta.getDataID() < posortowane_delty.get(i).getDataID()){
                    posortowane_delty.add(i, delta);
                    return;
                }
            }
            if (!posortowane_delty.contains(delta)){
                posortowane_delty.add(delta);
            }
        }
    }

    public void wyslij_delty_do_receivera(){
        if (posortowane_delty.size() < rozmiar_wektora){
            return;
        }

        if (posortowane_delty.get(0).getDataID() == aktualne_id){
            for (int i=0; i<rozmiar_wektora; i++){
                if (i < posortowane_delty.size()){
                    if (posortowane_delty.get(i).getDataID() != aktualne_id) {
                        return;
                    }
                }
            }

            List<Delta> podlista = new ArrayList<Delta>();

            for (int i=0; i<rozmiar_wektora; i++){
                Delta wysylany = posortowane_delty.remove(0);
                podlista.add(wysylany);
            }

            deltaReceiver.accept(podlista);
            aktualne_id ++;

            wyslij_delty_do_receivera();
        }
    }

}
