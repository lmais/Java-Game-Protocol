/* Laura Maisenhelder
 * CS544
 * 06/07/2019
 * Final Project Protocol Implementation - Client
 */

//package client;

import java.net.*;
import java.util.Scanner;
import java.lang.*;
import java.io.*;

//TCPclient set up from Forouzan, "Computer Networks A Top-Down Approach", p.826
public class TCPClient
{
	
	Socket sock;
	static OutputStream sendStream;
	InputStream recvStream;
	private static String request, response, authentication, code, new_Board;
	private static Scanner wscan = new Scanner(System.in), coordscan = new Scanner(System.in);
	private static int dest, eom, i;
	private static int state = 1;    //state is protocol state, 0 = idle, 1 = authentication, 2 = game initialization, 3 = client's turn, 4 = server's turn, 5 = disconnect
	String board = "_________";
	
	//current version number
	private static String version = "A";
	
	//user sign in, pick game, and send user/pass/game choice/version # to server
	static String signIn() 
	{
		System.out.println("Please enter your username: ");
		String user = wscan.next();
		System.out.println("Please enter your password: ");
		String pass = wscan.next();
		System.out.println("Please select a game, T for TicTacToe: ");
		String game = wscan.next();
		code = "100";
		authentication = code + game + version + user + "," + pass;
		request = authentication;    
		while(request.length() != 13)
		{
			System.out.println("*Oops, something's not right here. Please try again");
			signIn();
		}
		return request;
	}
	
	//does user want to continue (or start a new game if last one finished)
	public String ready()
	{
		System.out.println();
		System.out.println("Are you ready to play Y/N?");
		
		String answer = wscan.nextLine();
		if(answer.equals("Y"))
		{
			board = "_________";     
			state = 3;   //state 3, client's turn in the game
			play(board);
		}
		else if(answer.equals("N"))
		{
			System.out.println();
			System.out.println("GoodBye!");        
			state = 5;     //state 5, disconnect
			request = "250"; 
		}
		else
		{
			System.out.println("I didn't understand you, please try again.");
			ready();
		}
		return request;
	}
	
	//print the current game board, laid out in a 3 x 3 grid
	public static void printBoard(String board)
	{
		for (int i = 0; i < board.length(); i++) 
		{ 
			if((i+1)%3 > 0) 
			{
				System.out.print(board.substring(i,i+1) + " ");
			}
			else
			{
				System.out.println(board.substring(i,i+1));
			}
		}
	}
	
	//keep the game going by having user take their next turn
		public static String play(String board)
		{
			System.out.println();
			System.out.println("It's your turn, here's the current board:");
			printBoard(board);
			System.out.println();
			System.out.println("What x/y coordinates will you take, A-C across the top and 1-3 down the column:");
			String coord = coordscan.nextLine();
			switch(coord) {
				case "A1":
					dest = 0;
					break;
				case "A2":
					dest = 3;
					break;
				case "A3":
					dest = 6;
					break;
				case "B1":
					dest = 1;
					break;
				case "B2":
					dest = 4;
					break;
				case "B3":
					dest = 7;
					break;
				case "C1":
					dest = 2;
					break;
				case "C2":
					dest = 5;
					break;
				case "C3":
					dest = 8;
					break;
				default: 
					System.out.println("*I didn't understand, please try again");
					play(board);
			}
			
			if(board.substring(dest,dest +1).equals("_"))    
			{
			
				if(dest < board.length())
				{
					String temp_1 = board.substring(0,dest);
					String temp_2 = board.substring(dest);
					temp_2 = "X";
					String temp_3 = board.substring(dest + 1,board.length());
					new_Board = temp_1 + temp_2 + temp_3;
				}
				
				else
				{
					String temp_1 = board.substring(0,dest);
					String temp_2 = board.substring(dest);
					temp_2 = "X";
					new_Board = temp_1 + temp_2;
				}	
			}
			
			else															
			{
				System.out.println();
				System.out.println("*Sorry that space is not available, try again");
				play(board);
			}
			
			System.out.println("Server is playing, one moment please");
			System.out.println();          
			code = "350";
			state = 4;  // move to state 4, server will take it's turn
			request = code + new_Board; //code will be sent to server along with new gameboard
			return request;
		}
	
	//creation of the TCPClient socket from Forouzan, "Computer Networks A Top-Down Approach", p.826
	TCPClient(String server, int port) throws IOException, UnknownHostException
	{
		sock = new Socket(server,port);
		sendStream = sock.getOutputStream();
		recvStream = sock.getInputStream();
	}

	//sending the request to the server from Forouzan, "Computer Networks A Top-Down Approach", p.826
	void sendRequest()
	{
		String end = "EOM"; //add end of message indicator per protocol
		request = request + end;
		try
		{
			byte [] sendBuff = new byte [request.length()];
			sendBuff = request.getBytes();
			sendStream.write(sendBuff,0,sendBuff.length);
		}
	
		catch (IOException ex)
		{
			System.err.println("IOException in sendRequest");
		}
	
	}

	//taking incoming response from the server and transforming it into a usable String from Forouzan, "Computer Networks A Top-Down Approach", p.827
	void getResponse()
	{
		try
		{
			int dataSize;
			while ((dataSize = recvStream.available())==0);
			byte [] recvBuff = new byte [dataSize];
			recvStream.read(recvBuff,0,dataSize);
			response = new String(recvBuff,0,dataSize);
		}
		
		catch (IOException ex)
		{
			System.err.println("IOException in getResponse");
		}
	}

	//STATEFUL
	//interpreting response from server based on codes (abstract method from Forouzan, "Computer Networks A Top-Down Approach", p.827 which I updated for my own protocol)
	void useResponse()
	{
		//if code is 325, game board received from server, move to state 3 - client's turn
		if((response.substring(0,3).equals("325"))  && ((state == 2) || (state == 4)) )     
		{
			int l = response.length();
			for(i = 3; i < (l - 3); i++)
			{
				String test = response.substring(i, i+3);
				if(test.equals("EOM"))
				{
					eom = i;
				}
			}	
		//	System.out.println("EOM = " + eom);
//			board = response.substring(3, eom);
			board = response.substring(3,12);
			state = 3;             
			play(board);
		}

		else if((response.substring(0,3).equals("115"))  && (state == 1))          
		{
			System.out.println();
			System.out.println("Sorry but that version is not supported, please update your program or select a different version.");
			signIn();
		}
		
		else if((response.substring(0,3).equals("120"))  && (state == 1))          
		{
			System.out.println();
			System.out.println("Sorry but that game is not supported, please try again.");
			signIn();
		}
		
		else if((response.substring(0,3).equals("175"))  && (state == 1))         
		{
			System.out.println();
			System.out.println("Sorry but that username or password is incorrect, please try again.");
			signIn();
		}
		
		//if code is 150, state 1 authentication is complete, move to state 2 game initialization 
		else if((response.substring(0,3).equals("150")) && (state == 1))      
		{
			ready();
			state = 2;       
		}
		
		else if((response.substring(0,3).equals("375")) && (state == 4))
		{
			System.out.println("Game over, the server won.");
			board = response.substring(3,12);  
			printBoard(board);
			ready();
		}
		
		else if((response.substring(0,3).equals("380")) && (state == 4))		
		{
			System.out.println("Game over, you won!");
			board = response.substring(3,12);      
			printBoard(board);
			ready();
		}
		
		else if((response.substring(0,3).equals("385")) && (state == 4))			
		{
			System.out.println("Game over, it was a stalemate.");
			board = response.substring(3,12);         
			printBoard(board);
			ready();
		}
		
		else if (response.substring(0,3).equals("550"))
		{
			System.out.println("Server is down, please try again later.");
		}
	}
	
	//closing client socket from Forouzan, "Computer Networks A Top-Down Approach", p.827
	void close()
	{
		try
		{
			sendStream.close();
			recvStream.close();
			sock.close();
			System.out.println("Closing");
		}
		
		catch (IOException ex)
		{
			System.err.println("IOException in close");
		}
	}
	
// general main method from Forouzan, "Computer Networks A Top-Down Approach", p.830 which I have updated for my own protocol
	public static void main (String [] args) throws IOException
	{
		final int servPort = 1030;                                                              //PORT NUMBER
	    final String servName = "localhost";                                                    //CLIENT
	    InetAddress local = InetAddress.getLocalHost();											//IP ADDRESS								
		TCPClient client = new TCPClient(servName,servPort);
	    signIn();   
		while(true)
		{            //added to keep communication going
			client.sendRequest(); 
			client.getResponse(); 
			client.useResponse();
		}
	
		//	client.close();
		
	}
}