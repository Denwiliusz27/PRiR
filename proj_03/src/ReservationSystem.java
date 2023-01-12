import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;
import java.lang.*;

public class ReservationSystem extends UnicastRemoteObject implements Cinema{
    private Set<Integer> avaliableSeats = new HashSet<>();
    private Map<String, Set<Integer>> reservations = new HashMap<>();
    private Map<String, Long> timeOfReservation = new HashMap<>();
    private String[] owners;
    private long timeForConfirmation = 0;

    public ReservationSystem() throws RemoteException{
        try{
            java.rmi.Naming.bind(Cinema.SERVICE_NAME, this);
        } catch(Exception e){
            e.printStackTrace();
        }
    }


    @Override
    public synchronized void configuration(int seats, long timeForConfirmation) throws RemoteException {
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

        reservations.put(user, seats);
        timeOfReservation.put(user, System.currentTimeMillis());
        avaliableSeats.removeAll(seats);

        return true;
    }

    @Override
    public synchronized boolean confirmation(String user) throws RemoteException {
        cleanReservations();

        if (reservations.containsKey(user)) {
            var seats = reservations.remove(user);
            long now = System.currentTimeMillis() - timeForConfirmation;
            long v = timeOfReservation.remove(user);
            boolean expired = v < now;

            if (expired) {
                if (avaliableSeats.containsAll(seats)) {
                    avaliableSeats.removeAll(seats);

                    for (int seat : seats) {
                        owners[seat] = user;
                    }
                    return true;
                } else {
                    return false;
                }
            } else {
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
        long now = new Date().getTime();

        for (String user:timeOfReservation.keySet()){
            if ((timeOfReservation.get(user) != (long) 0) && ((now - timeOfReservation.get(user)) > timeForConfirmation)){
                avaliableSeats.addAll(reservations.get(user));
                timeOfReservation.put(user, (long) 0);
           }
        }
    }
}
