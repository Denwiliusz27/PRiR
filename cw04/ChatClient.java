import java.io.IOException;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Scanner;

public class ChatClient {

	private static Scanner in = new Scanner(System.in);

	public static void main(String[] args) throws IOException,
			NotBoundException {

		System.out.print("Proszę podac adres serwera > ");

		String server = in.nextLine();

		Remote r = java.rmi.Naming.lookup("//" + server + ":1099/CHAT");

		ChatRemoteInterface cri = (ChatRemoteInterface) r;
		
		System.out.println( "Oto referencja do proxy: " + cri );

		System.out.print("Proszę podac username > ");

		String input = in.nextLine();

		int id = cri.registerUser(input);

		Thread th = new Thread( new Runnable() {
			
			@Override
			public void run() {
				String input = null;
				while ( true ) {
					while ( true ) {
						try {
							input = cri.getMessage( id );
						} catch (RemoteException e) {
							e.printStackTrace();
						}
						if ( input != null )
							System.out.println( input );
						else
							break;
					} 
					try {
						Thread.sleep( 5000 );
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		} );
		th.setDaemon( true );
		th.start();
		
		while (true) {
			input = in.nextLine();
			cri.sendMessage(id, input);
		}

	}
}
