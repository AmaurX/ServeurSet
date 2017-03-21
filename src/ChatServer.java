

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Random;
import java.util.Scanner;


final class ConnectionList {
	PrintWriter out;
	String login;
	ConnectionList tail;
	ConnectionList(String l, PrintWriter h, ConnectionList tl){
		login = l;
		out = h;
		tail = tl;
	}
}

public class ChatServer {

	static void sleepSeconds(int i) {
		try {
			Thread.sleep(i*1000);
		} catch (InterruptedException e) {
		}
	}

	static public ServerSocket createServer(int server_port){ 	
		try {
			return new ServerSocket(server_port);
		} catch (IOException e) {
			throw new RuntimeException("Impossible d'attendre sur le port "+ server_port);
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
			return new Socket(ip,port);
		} 
		catch (UnknownHostException e){
			throw new RuntimeException("Impossible de resoudre l'adresse");
		}
		catch (IOException e){
			throw new RuntimeException("Impossible de se connecter a l'adresse");	
		}
	}

	static PrintWriter connectionOut(Socket s){
		try {
			return new PrintWriter(s.getOutputStream(),	true);
		} catch (IOException e) {
			throw new RuntimeException("Impossible d'extraire le flux sortant");
		}
	}

	static BufferedReader connectionIn(Socket s){
		try {
			return new BufferedReader(new InputStreamReader(s.getInputStream()));
		} catch (IOException e) {
			throw new RuntimeException("Impossible d'extraire le flux entrant");
		}
	}

	static ConnectionList outs = null;
	static boolean killed = false;

	static void print_all(String message){
		ConnectionList cl = outs;
		if(message!=null){
			while(cl != null){
				cl.out.println(message);	
				cl = cl.tail;
			}		
		}
	}
	
	public static void main(String args[]){
		
		ServerSocket server = createServer(1708);
		InetAddress address = null;
		try {
			address = InetAddress.getLocalHost();
		} catch (UnknownHostException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	      String hostIP = address.getHostAddress() ;
		  String hostName = address.getHostName();
		  System.out.println( "Le nom de serveur est : " + hostName + "\nIP: " + hostIP);
		 
		Integer[] jeu = new Integer[15];
		Integer[] table = new Integer[15];
		boolean[] deck = new boolean[81];
		
		while(!killed){
			final Socket s = acceptConnection(server);
			  System.out.println("Le serveur est à l'écoute du port "+s.getLocalPort());

			System.out.println("connection etablie");
			final PrintWriter s_out = connectionOut(s);
			final BufferedReader s_in = connectionIn(s);
			
			
			Thread t = new Thread(){
				

				public void genererNouveauJeu(){
			        Random tirage = new Random();
			        for(int i =0; i<81;i++){
			        	deck[i]=false;
			        };
					for(int i = 0; i<12;i++){
						boolean flag = true;
						int numeroDeCarte = -1;
						while(flag){
							numeroDeCarte = tirage.nextInt(81);
							flag = deck[numeroDeCarte];
						}
						
						jeu[i]= numeroDeCarte;
						deck[numeroDeCarte]=true;
						table[i]= numeroDeCarteToK(numeroDeCarte);
						if(table[i]<85){
						}
					}
					jeu[12]=-1;
					jeu[13]=-1;
					jeu[14]=-1;
					table[12]=-1;
					table[13]=-1;
					table[14]=-1;
					if(isThereMatch()){

					}
					else{
					}
				}
				
				public boolean isThereMatch() {

			        for (int card1 : table) {

			        	if(card1 == -1) continue;
			            for (int card2 : table) {

			                if (card1 == card2) continue;
			                if(card2 == -1) continue;
			                for (int card3 : table) {

			                	if(card3 == -1)continue;
			                    if (card1 == card3 || card2 == card3) continue;
			                    if (Cards.isSet(card1, card2, card3)){

			                    	return true;
			                    }
			                }
			            }
			        }

			        return false;

			    }
				
				public int numeroDeCarteToK(int numeroDeCarte) {
					if(numeroDeCarte == -1)return -1;
			        int a = numeroDeCarte % 3;
			        int b = (numeroDeCarte - a) / 3 % 3;
			        int c = (numeroDeCarte - a - 3 * b) / 9 % 3;
			        int d = (numeroDeCarte - a - 3 * b - 9 * c) / 27 % 3;
			        return ((a + 1) + 4 * (b + 1) + 16 * (c + 1) + 64 * (d + 1));
			    }

			    public int kToNumeroDeCarte(int k) {
			    	if(k == -1)return -1;
			        int a = k % 4;
			        int b = (k - a) / 4 % 4;
			        int c = (k - a - 4 * b) / 16 % 4;
			        int d = (k - a - 4 * b - 16 * c) / 64 % 4;
			        return ((a - 1) + 3 * (b - 1) + 9 * (c - 1) + 27 * (d - 1));
			    }
				
				public void sendGame(){
					String game = "theGame/";
					for(int i = 0;i<15;i++){
						game+= jeu[i] + "/"; 
					}
					print_all(game);
					System.out.println(game);
				}
				
				public void run(){
					String my_login = null;
					try {
						while(true){

							String line;

							try { line = s_in.readLine(); }
							catch (IOException e){
								throw new RuntimeException("Cannot read from socket");
							}
							Scanner sc = new Scanner(line);
							sc.useDelimiter("/");
							String token = sc.next();
							if(my_login != null){
								//La on met notre truc
								if(token.equals("NEWGAME")){
									genererNouveauJeu();
									sendGame();
								}
								
								else if(token.equals("TRY")){
								}
								
								
								
								else if(token.equals("SEND")){
									sc.useDelimiter("\n");
									String message = sc.next();
									print_all(my_login + ":" + message);
								} 
								
								else if(token.equals("LOGOUT")){
										s_out.println("Bye Bye " + my_login );
										throw new RuntimeException("Requested by user");
								} 
								else
										throw new RuntimeException("Unknown Command");
							} 
							else if(token.equals("LOGIN")){
								my_login = sc.next();
								ConnectionList cl = outs;
								while(cl != null){
									if(cl.login.equals(my_login)){
										my_login = null;
										throw new RuntimeException("Login already used");
									}
									cl = cl.tail;
								}
								outs = new ConnectionList(my_login,s_out,outs);
								print_all("Welcome "+my_login);
							} 
							else if(token.equals("KILL")){
								killed = true;	
								throw new RuntimeException("Waiting for next connection to kill");
							} 
							else {
								throw new RuntimeException("Expecting LOGIN command");
							}
						}	
					} 
					catch(RuntimeException error){
						System.out.println("disconnection");
						s_out.println("DISCONNECTED: exn "+error);
						s_out.flush();
						try { s.close(); } catch(IOException e){}
						if(my_login != null){
							ConnectionList cl = null;
							while(outs != null){
								if(outs.out == s_out){
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
		}
		System.exit(0);
	}

}



