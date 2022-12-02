import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ReservationSystem extends UnicastRemoteObject implements Cinema{
    private Set<Integer> avaliableSeats;
    private Map<String, Set<Integer>> reservations;

    private Map<String, Long> timeOfReservation;
    private String[] owners;
    private long timeForConfirmation;

    public ReservationSystem() throws RemoteException {
        super();
        this.avaliableSeats = new HashSet<>();
        this.reservations = new HashMap<>();
        this.timeForConfirmation = 0;
        this.timeOfReservation = new HashMap<>();
    }


    @Override
    public void configuration(int seats, long timeForConfirmation) throws RemoteException {
        if (seats <= 0 || timeForConfirmation <= 0){
            return;
        }

        this.timeForConfirmation = timeForConfirmation;
        owners = new String[seats];

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
        if (seats.size() == 0 ){
            return false;
        }

        Set<Integer> available = new HashSet<>();

        cleanReservations();

        boolean ok = true;

        synchronized (avaliableSeats){
            for (int seat:seats) {
                if (avaliableSeats.contains(seat)){
                    available.add(seat);
                } else {
                    ok = false;
                }
            }
            if (!ok) {
                return false;
            }

            reservations.put(user, available);
            timeOfReservation.put(user, System.currentTimeMillis());
            avaliableSeats.removeAll(available);

            return true;
        }
    }

    @Override
    public boolean confirmation(String user) throws RemoteException {
        synchronized (reservations){
            if (reservations.containsKey(user)){
                Integer[] seats = reservations.get(user).toArray(new Integer[reservations.size()]);
                long now = System.currentTimeMillis() - timeForConfirmation;
                long v = timeOfReservation.get(user);

                reservations.remove(user);
                timeOfReservation.remove(user);

                if( v >= now){
                    for(int seat:seats){
                        owners[seat] = user;
                    }
                    return true;
                }

                return false;
            } else {
                return false;
            }
        }
    }

    @Override
    public String whoHasReservation(int seat) throws RemoteException {
        if (seat <= 0 || seat >= owners.length ){
            return "";
        }

        synchronized (owners){
            return owners[seat];
        }
    }

    private void cleanReservations(){
        long now = System.currentTimeMillis() - timeForConfirmation;
        Map<String, Long> timesToRemove = new HashMap<>();

        synchronized (timeOfReservation){
            for (String key:timeOfReservation.keySet()){
                if (timeOfReservation.get(key) < now){
                    timesToRemove.put(key, timeOfReservation.get(key));
                    Set<Integer> seats = reservations.get(key);

                    for (int seat: seats){
                        avaliableSeats.add(seat);
                    }

                    reservations.remove(key);
                }
            }

            for (String time:timesToRemove.keySet()){
                timeOfReservation.remove(time);
            }
        }
    }
}
