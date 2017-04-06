
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Scanner;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

final class ConnectionList {
	PrintWriter out;
	String login;
	ConnectionList tail;

	ConnectionList(String l, PrintWriter h, ConnectionList tl) {
		login = l;
		out = h;
		tail = tl;
	}
}

final class Essai {
	int N;
	int a;
	int b;
	int c;
	String joueur;

	public Essai(int N, int a, int b, int c, String joueur) {
		this.N = N;
		this.a = a;
		this.b = b;
		this.c = c;
		this.joueur = joueur;
	}
}

public class ChatServer {

	static void sleepSeconds(int i) {
		try {
			Thread.sleep(i * 1000);
		} catch (InterruptedException e) {
		}
	}

	static public ServerSocket createServer(int server_port) {
		try {
			return new ServerSocket(server_port);
		} catch (IOException e) {
			throw new RuntimeException("Impossible d'attendre sur le port " + server_port);
		}
	}

	static Socket acceptConnection(ServerSocket s) {
		try {
			return s.accept();
		} catch (IOException e) {
			throw new RuntimeException("Impossible de recevoir une connection");
		}
	}

	static Socket establishConnection(String ip, int port) {
		try {
			return new Socket(ip, port);
		} catch (UnknownHostException e) {
			throw new RuntimeException("Impossible de resoudre l'adresse");
		} catch (IOException e) {
			throw new RuntimeException("Impossible de se connecter a l'adresse");
		}
	}

	static PrintWriter connectionOut(Socket s) {
		try {
			return new PrintWriter(s.getOutputStream(), true);
		} catch (IOException e) {
			throw new RuntimeException("Impossible d'extraire le flux sortant");
		}
	}

	static BufferedReader connectionIn(Socket s) {
		try {
			return new BufferedReader(new InputStreamReader(s.getInputStream()));
		} catch (IOException e) {
			throw new RuntimeException("Impossible d'extraire le flux entrant");
		}
	}

	static ConnectionList outs = null;
	static boolean killed = false;

	static void print_all(String message) {
		ConnectionList cl = outs;
		if (message != null) {
			while (cl != null) {
				cl.out.println(message);
				cl = cl.tail;
			}
		}
	}

	static String[] getPlayers() {
		ConnectionList cl = outs;
		int count = 0;
		while (cl != null) {
			count++;
			cl = cl.tail;
		}
		String[] result = new String[count];
		cl = outs;
		count = 0;
		while (cl != null) {
			result[count] = cl.login;
			cl = cl.tail;
			count++;
		}
		return result;
	}

	static public boolean isThereMatch(Integer[] table) {
		for (int card1 : table) {
			if (card1 == -1)
				continue;
			for (int card2 : table) {
				if (card1 == card2)
					continue;
				if (card2 == -1)
					continue;
				for (int card3 : table) {
					if (card3 == -1)
						continue;
					if (card1 == card3 || card2 == card3)
						continue;
					if (Cards.isSet(card1, card2, card3)) {
						return true;
					}
				}
			}
		}
		return false;
	}

	static public int numeroDeCarteToK(int numeroDeCarte) {
		if (numeroDeCarte == -1)
			return -1;
		int a = numeroDeCarte % 3;
		int b = (numeroDeCarte - a) / 3 % 3;
		int c = (numeroDeCarte - a - 3 * b) / 9 % 3;
		int d = (numeroDeCarte - a - 3 * b - 9 * c) / 27 % 3;
		return ((a + 1) + 4 * (b + 1) + 16 * (c + 1) + 64 * (d + 1));
	}

	static public int kToNumeroDeCarte(int k) {
		if (k == -1)
			return -1;
		int a = k % 4;
		int b = (k - a) / 4 % 4;
		int c = (k - a - 4 * b) / 16 % 4;
		int d = (k - a - 4 * b - 16 * c) / 64 % 4;
		return ((a - 1) + 3 * (b - 1) + 9 * (c - 1) + 27 * (d - 1));
	}

	static public void sendGame(Integer[] jeu) {
		lock.lock();
		try {
			String game = "theGame/";
			game += N + "/";
			for (int i = 0; i < 14; i++) {
				game += jeu[i] + "/";
			}
			game += jeu[14] + "/";
			print_all(game);
			System.out.println(game);
		} finally {
			lock.unlock();
		}
	}

	static public void sendGameToOne(Integer[] jeu, String login) {
		lock.lock();
		try {
			String game = "theGame/";
			game += N + "/";
			for (int i = 0; i < 14; i++) {
				game += jeu[i] + "/";
			}
			game += jeu[14] + "/";

			ConnectionList cl = outs;
			while (!cl.login.equals(login)) {
				cl = cl.tail;
			}
			cl.out.println(game);
			System.out.println("Game inchangé, erreur de: " + login);
		} finally {
			lock.unlock();
		}
	}

	public static ReentrantLock lock = new ReentrantLock();
	public static AtomicInteger N = new AtomicInteger(0);
	static SynchronousQueue<Essai> essaiQueue = new SynchronousQueue<Essai>();

	static Runnable correcteur = new Runnable() {
		@Override
		public void run() {
			HashMap<String, Integer> score = new HashMap<String, Integer>();
			int nombreJoueurs = 0;
			while (true) {
				Essai essai;
				try {
					String[] joueurs = getPlayers();
					for (String player : joueurs) {
						if (!score.containsKey(player)) {
							score.put(player, 0);
							nombreJoueurs++;
						}
					}
					
					System.out.println("debug: really trying");
					essai = essaiQueue.take();
					if (essai == null) {
						continue;
					}
					if (!score.containsKey(essai.joueur)) {
						score.put(essai.joueur, 0);
						nombreJoueurs++;
					}
					Integer[] tentative = new Integer[3];
					tentative[0] = table[essai.a];
					tentative[1] = table[essai.b];
					tentative[2] = table[essai.c];
					ConnectionList cl = outs;
					while (!cl.login.equals(essai.joueur)) {
						cl = cl.tail;
					}
					if (isThereMatch(tentative)) {
						score.put(essai.joueur, score.get(essai.joueur) + 1);
						int[] carteAModifier = new int[3];
						for (int i = 0; i < 15; i++) {
							if (i == essai.a) {
								carteAModifier[0] = i;
							}
							if (i == essai.b) {
								carteAModifier[1] = i;
							}
							if (i == essai.c) {
								carteAModifier[2] = i;
							}
						}
						deck[jeu[essai.a]] = false;
						deck[jeu[essai.b]] = false;
						deck[jeu[essai.c]] = false;
						Random tirage = new Random();
						for (int i : carteAModifier) {
							boolean flag = true;
							int numeroDeCarte = -1;
							while (flag) {
								numeroDeCarte = tirage.nextInt(81);
								flag = deck[numeroDeCarte];
							}
							jeu[i] = numeroDeCarte;
							deck[numeroDeCarte] = true;
							table[i] = numeroDeCarteToK(numeroDeCarte);
						}
						// il me semble que je doive update le N comme on est
						// dans un nouveau jeu
						N.getAndIncrement();
						cl.out.println("result/+/");
						sendGame(jeu);
					} 
					else {
						score.put(essai.joueur, score.get(essai.joueur) - 1);
						cl.out.println("result/-/");
						sendGameToOne(jeu, essai.joueur);
					}
					
					String scoreMessage = "scores/";
					for (Map.Entry<String, Integer> entry : score.entrySet()) {
					    String key = entry.getKey();
					    Object value = entry.getValue();
					    scoreMessage += key +"/";
					    scoreMessage += value +"/";
					}
					print_all(scoreMessage);
					
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
			}
		}
	};

	public static Integer[] jeu;
	public static Integer[] table;
	public static boolean[] deck;

	public static void genererNouveauJeu() {
		lock.lock();
		try {
			Random tirage = new Random();
			for (int i = 0; i < 81; i++) {
				deck[i] = false;
			}
			;
			for (int i = 0; i < 12; i++) {
				boolean flag = true;
				int numeroDeCarte = -1;
				while (flag) {
					numeroDeCarte = tirage.nextInt(81);
					flag = deck[numeroDeCarte];
				}
				jeu[i] = numeroDeCarte;
				deck[numeroDeCarte] = true;
				table[i] = numeroDeCarteToK(numeroDeCarte);
			}
			jeu[12] = -1;
			jeu[13] = -1;
			jeu[14] = -1;
			table[12] = -1;
			table[13] = -1;
			table[14] = -1;
			if (!isThereMatch(table)) {
				for (int i = 12; i < 15; i++) {
					boolean flag = true;
					int numeroDeCarte = -1;
					while (flag) {
						numeroDeCarte = tirage.nextInt(81);
						flag = deck[numeroDeCarte];
					}

					jeu[i] = numeroDeCarte;
					deck[numeroDeCarte] = true;
					table[i] = numeroDeCarteToK(numeroDeCarte);
				}
			}
			N.getAndIncrement();
		} finally {
			lock.unlock();
		}
	}
	
	public static void main(String args[]) {

		ServerSocket server = createServer(1709);
		InetAddress address = null;
		try {
			address = InetAddress.getLocalHost();
		} catch (UnknownHostException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		String hostIP = address.getHostAddress();
		String hostName = address.getHostName();
		System.out.println("Le nom de serveur est : " + hostName + "\nIP: " + hostIP);

		jeu = new Integer[15];
		table = new Integer[15];
		deck = new boolean[81];
		genererNouveauJeu();
		
		while (!killed) {
			final Socket s = acceptConnection(server);
			System.out.println("Le serveur est à l'écoute du port " + s.getLocalPort());

			System.out.println("connection etablie");
			final PrintWriter s_out = connectionOut(s);
			final BufferedReader s_in = connectionIn(s);

			Thread t = new Thread() {

				

				public void run() {
					String my_login = null;
					try {
						while (true) {

							String line;

							try {
								line = s_in.readLine();
							} catch (IOException e) {
								throw new RuntimeException("Cannot read from socket");
							}
							Scanner sc = new Scanner(line);
							sc.useDelimiter("/");
							String token = sc.next();
							if (my_login != null) {
								// La on met notre truc
								if (token.equals("GAMEPLEASE")) {
								
									sendGameToOne(jeu,my_login);
								} else if (token.equals("TRY")) {
									System.out.println("trying");
									String indice = sc.next();
									int n = Integer.parseInt(indice);
									if (N.intValue() == n) { // on arrive jamais
																// dedans, j'ai
																// changé le !=
																// en ==

										Essai e = new Essai(n, sc.nextInt(), sc.nextInt(), sc.nextInt(), my_login);
										try {
											essaiQueue.put(e);
										} catch (InterruptedException e1) {
											// TODO Auto-generated catch block
											e1.printStackTrace();
										}
									}
								} else if (token.equals("SEND")) {
									sc.useDelimiter("\n");
									String message = sc.next();
									print_all(my_login + ":" + message);
								} else if (token.equals("LOGOUT")) {
									s_out.println("Bye Bye " + my_login);
									throw new RuntimeException("Requested by user");
								} else
									throw new RuntimeException("Unknown Command");
							} else if (token.equals("LOGIN")) {
								my_login = sc.next();
								ConnectionList cl = outs;
								while (cl != null) {
									if (cl.login.equals(my_login)) {
										my_login = null;
										throw new RuntimeException("Login already used");
									}
									cl = cl.tail;
								}
								outs = new ConnectionList(my_login, s_out, outs);
								print_all("Welcome " + my_login);
							} else if (token.equals("KILL")) {
								killed = true;
								throw new RuntimeException("Waiting for next connection to kill");
							} else {
								throw new RuntimeException("Expecting LOGIN command");
							}
						}
					} catch (RuntimeException error) {
						System.out.println("disconnection");
						s_out.println("DISCONNECTED: exn " + error);
						s_out.flush();
						try {
							s.close();
						} catch (IOException e) {
						}
						if (my_login != null) {
							ConnectionList cl = null;
							while (outs != null) {
								if (outs.out == s_out) {
								} else {
									cl = new ConnectionList(outs.login, outs.out, cl);
								}
								outs = outs.tail;
							}
							outs = cl;
							print_all(my_login + " left.");
						}
					}
				}
			};
			t.start();
			Thread corrige = new Thread(correcteur);
			corrige.start();
		}
		System.exit(0);
	}

}
