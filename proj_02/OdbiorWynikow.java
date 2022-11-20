import java.util.List;

public class OdbiorWynikow implements DeltaReceiver {

    public void accept( List<Delta> deltas ){
        System.out.println("odbior wynikow odebral wynik " + deltas);
    }
}
