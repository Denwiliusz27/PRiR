import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class Client {

    public static void main(String[] args){
        try {
            Cinema access =
                    (Cinema) Naming.lookup("CINEMA");

            access.configuration(30, 1000);
            Integer[] seats = new Integer[]{3,4,5};
            Set<Integer> set = new HashSet<>();
            set.addAll(Arrays.stream(seats).toList());
            boolean ok = access.reservation("Jan", set);
            System.out.println("Reservation: " + ok);

            try {
                Thread.sleep(600);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

            set.removeAll(Arrays.stream(seats).toList());
            seats = new Integer[]{5,6,7};
            set.addAll(Arrays.stream(seats).toList());
            ok = access.reservation("Marek", set);
            System.out.println("Reservation: " + ok);

            ok = access.confirmation("Marek");
            System.out.println("confirmation: " + ok);

            ok = access.confirmation("Jan");
            System.out.println("confirmation: " + ok);

            String who = access.whoHasReservation(5);
            System.out.println("Owner of " + 5 + " is " + who);



//            set.removeAll(Arrays.stream(seats).toList());
//            seats = new Integer[]{5,6,7};
//            set.addAll(Arrays.stream(seats).toList());
//            ok = access.reservation("Marek", set);
//            System.out.println("Reservation: " + ok);

            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
//            ok = access.confirmation("Marek");
//            System.out.println("confirmation: " + ok);

        } catch (NotBoundException e) {
            System.out.println(e.getMessage());
        } catch (MalformedURLException e) {
            System.out.println(e.getMessage());
        } catch (RemoteException e) {
            System.out.println(e.getMessage());
        }
    }
}
