import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * Interfejs prostego systemu chat
 * 
 * @author oramus
 *
 */
public interface ChatRemoteInterface extends Remote {
	/**
	 * Metoda rejestruje uzytkownika i przydziela mu unikalne ID
	 * 
	 * @param username
	 *            - nazwa uzytkownika do zarejestrowania w systemie
	 * @return - uniklany identyfikator
	 * @throws RemoteException
	 *             - wyjatek zglaszany w przypadku wystapienia bledu
	 */
	int registerUser(String username) throws RemoteException;

	/**
	 * Metoda zwraca wiadomosc przeznaczona dla uzytkownika o podanym ID
	 * 
	 * @param id
	 *            - identyfikator uzytkownika
	 * @return wiadomosc dla uzytkownika, null - brak wiadomosci
	 * @throws RemoteException
	 *             - wyjatek zglaszany w przypadku wystapienia bledu
	 */
	String getMessage(int id) throws RemoteException;

	/**
	 * Metoda pozwala na przeslanie do systemu wiadomosci od danego uzytkownika
	 * 
	 * @param id
	 *            - identyfikator uzytkownika
	 * @param message
	 *            wiadomosc od uzytkownika
	 * @throws RemoteException
	 *             - wyjatek zglaszany w przypadku wystapienia bledu
	 */
	void sendMessage(int id, String message) throws RemoteException;
}
