import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ReservationSystem implements Cinema{
    private Set<Integer> avaliableSeats;
    private Map<String, Set<Integer>> users;
    private long timeForConfirmation;

    public void Cinema() {
        this.avaliableSeats = new HashSet<>();
        this.users = new HashMap<>();
        this.timeForConfirmation = 0;
    }


    @Override
    public void configuration(int seats, long timeForConfirmation) throws RemoteException {
        this.timeForConfirmation = timeForConfirmation;

        for(int i=1; i<=seats; i++){
            avaliableSeats.add(i);
        }
    }

    @Override
    public Set<Integer> notReservedSeats() throws RemoteException {
        return avaliableSeats;
    }

    @Override
    public boolean reservation(String user, Set<Integer> seats) throws RemoteException {
        return false;
    }

    @Override
    public boolean confirmation(String user) throws RemoteException {
        return false;
    }

    @Override
    public String whoHasReservation(int seat) throws RemoteException {
        return null;
    }
}
