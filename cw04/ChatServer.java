import java.rmi.AccessException;
import java.rmi.AlreadyBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

public class ChatServer extends UnicastRemoteObject implements ChatRemoteInterface {

	private int counter;
	private Map<Integer, String> id2username;
	private Map<Integer, Queue<String>> id2messageQueue;

	public ChatServer() throws RemoteException {
		id2username = new HashMap<Integer, String>();
		id2messageQueue = new HashMap<Integer, Queue<String>>();
	}

	@Override
	synchronized public int registerUser(String username)
			throws RemoteException {
		counter++;
		id2username.put(counter, username);
		id2messageQueue.put(counter, new LinkedList<String>());
		id2messageQueue.get(counter).offer("Welcome!");
		return counter;
	}

	private void testIfExists(int id) throws RemoteException {
		if (!id2username.containsKey(id))
			throw new RemoteException("err: user unknown");
	}

	@Override
	synchronized public String getMessage(int id) throws RemoteException {
		testIfExists(id);
		return id2messageQueue.get(id).poll();
	}

	@Override
	synchronized public void sendMessage(int id, String message)
			throws RemoteException {
		testIfExists(id);

		String messageU = id2username.get(id) + "> " + message;

		System.out.println( messageU );
		
		for (Integer i : id2messageQueue.keySet()) {
			if ( i != id ) id2messageQueue.get(i).offer(messageU);
		}

	}

	public static void main(String[] args) throws AccessException, RemoteException, AlreadyBoundException {
		int PORT = 1099;
	     Registry registry = 
	        java.rmi.registry.LocateRegistry.createRegistry(PORT);

	     registry.rebind( "CHAT", new ChatServer() );
	     for ( String service : registry.list() ) {
	    	 System.out.println( "Service : " + service );
	     }
	     
	     
	}
}
