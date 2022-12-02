import java.net.MalformedURLException;
import java.rmi.RemoteException;
import java.rmi.*;
import java.rmi.registry.*;

public class Server {
    public static void main(String[] args) throws RemoteException {
        Cinema service = new ReservationSystem();
        LocateRegistry.createRegistry(1900);
        try {
            Naming.rebind("rmi://localhost:1900/Cinema", service);
        } catch (MalformedURLException e) {
            System.out.println(e.getMessage());
        }
    }
}
