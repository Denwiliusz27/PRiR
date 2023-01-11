import java.net.MalformedURLException;
import java.rmi.AccessException;
import java.rmi.AlreadyBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.lang.*;

public class ReservationSystem extends UnicastRemoteObject implements Cinema{
    private Set<Integer> avaliableSeats;
    private Map<String, Set<Integer>> reservations;

    private Map<String, Boolean> usersTimeExpiration;
    
    private Map<String, Long> timeOfReservation;
    private String[] owners;
    private long timeForConfirmation;

    public ReservationSystem() throws RemoteException, MalformedURLException, AlreadyBoundException {
//        super();
        this.avaliableSeats = new HashSet<>();
        this.reservations = new HashMap<>();
        this.timeForConfirmation = 0;
        this.timeOfReservation = new HashMap<>();
        this.usersTimeExpiration = new HashMap<>();

//        Registry registry = java.rmi.registry.LocateRegistry.createRegistry(1099);
//        registry.rebind( Cinema.SERVICE_NAME,this );
        java.rmi.Naming.bind(Cinema.SERVICE_NAME, this);
    }

    
    @Override
    public void configuration(int seats, long timeForConfirmation) throws RemoteException {
//        if (seats <= 0 || timeForConfirmation <= 0){
//            return;
//        }

        this.timeForConfirmation = timeForConfirmation;
        owners = new String[seats];

        for(int i=0; i<seats; i++){
            avaliableSeats.add(i);
            owners[i] = null;
        }
    }

    @Override
    public synchronized Set<Integer> notReservedSeats() throws RemoteException {
        cleanReservations();
        return avaliableSeats;
    }

    @Override
    public synchronized boolean reservation(String user, Set<Integer> seats) throws RemoteException {
        if (seats.size() == 0 ){
            return false;
        }

        cleanReservations();

        for (int seat:seats) {
            if (!avaliableSeats.contains(seat)) {
                return false;
            }
        }

        usersTimeExpiration.put(user, false);
        reservations.put(user, seats);
        timeOfReservation.put(user, System.currentTimeMillis());
        avaliableSeats.removeAll(seats);

        return true;
    }

    @Override
    public synchronized boolean confirmation(String user) throws RemoteException {
        if (reservations.containsKey(user)) {
            var seats = reservations.remove(user);
            long now = System.currentTimeMillis() - timeForConfirmation;
            long v = timeOfReservation.remove(user);

            boolean expired = v < now;

            if (expired) {
                if (avaliableSeats.containsAll(seats)) {
                    avaliableSeats.removeAll(seats);
//                    reservations.remove(user);
//                    timeOfReservation.remove(user);

                    for (int seat : seats) {
                        owners[seat] = user;
                    }

                    return true;
                } else {

                    return false;
                }
            } else {
//                reservations.remove(user);
//                timeOfReservation.remove(user);

                for (int seat : seats) {
                    owners[seat] = user;
                }

                return true;
            }
        }
        return false;
    }

    @Override
    public synchronized String whoHasReservation(int seat) throws RemoteException {
        if (seat < 0 || seat >= owners.length ){
            return null;
        }

        return owners[seat];
    }

    private synchronized void cleanReservations(){
        long now = System.currentTimeMillis() - timeForConfirmation;
        Map<String, Long> timesToRemove = new HashMap<>();

       for (String key:timeOfReservation.keySet()){
           if (timeOfReservation.get(key) < now){
               timesToRemove.put(key, timeOfReservation.get(key));
               Set<Integer> seats = reservations.get(key);
               usersTimeExpiration.put(key, true);

               avaliableSeats.addAll(seats);
           }
       }

       for (String time:timesToRemove.keySet()){
           timeOfReservation.remove(time);
       }
    }
}
