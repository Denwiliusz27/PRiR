import java.net.MalformedURLException;
import java.rmi.RemoteException;
import java.rmi.*;
import java.rmi.registry.*;

public class Server {
    public static void main(String[] args) throws RemoteException, MalformedURLException, AlreadyBoundException {
        int PORT = 1099;
        Registry registry =
                java.rmi.registry.LocateRegistry.createRegistry(PORT);

        Cinema service = new ReservationSystem();
        service.configuration(100, 1000);
    }
}
