/* Laura Maisenhelder
 * CS544
 * 06/07/2019
 * Final Project Protocol Implementation - Server
 */



//package server;
import java.util.*;
import java.net.*;
import java.io.*;

//concurrent TCPServer set up from Forouzan, "Computer Networks A Top-Down Approach", p.833
public class ConcurTCPServer implements Runnable{

	Socket sock;
	InputStream recvStream;
	OutputStream sendStream;
	String request, response, option, choice, user, pass, cversion, code, new_Board, board;
	private static int state;   //state is protocol state, 0 = idle, 1 = authentication, 2 = game initialization, 3 = client's turn, 4 = server's turn, 5 = disconnect
	private static int div,i,eom;
	
	//**********************************************************************************************************************************************//
/*	HashMap<String, String> users;
	users = new HashMap<String, String>();
	users.put("Bob","1234");
	
	check = users.get("Bob");
	if(check.equals("1234")
	{
		System.out.println("worked");
	}
*/  //*************************************************************************************************************************************************//take out before submitting

	
	//current version number
	private static String version = "A";
	
	//games supported, right now only T for TicTacToe
	private static List<String> games = new ArrayList<String>(); 
	//games.add("T");//commented out because this causes an error
	
	//authentication portion of protocol, for user name & password, version number and option negotiations
	private String authentication(String request)
	{
		for(i = 3; i < request.length() - 3; i++)
		{
			if(request.substring(i, i+2).equals("EOM"))
			{
				div = i;
			}
		}	
		request = request.substring(0,i);
			
		option = request.substring(3,4);
		cversion = request.substring(4,5);
		for(int i = 5; i < request.length(); i++)
		{
			if(request.substring(i, i+1).equals(","))
			{
				div = i;
			}
		}
		user = request.substring(5,div);
		pass = request.substring(div + 1);
		
		if(user.equals("Bob") && pass.equals("1234") && cversion.equals(version) && games.contains(option))
		{
			code = "150";
			response = code;
			state = 2;
			System.out.println("User authorized");
		}
		
		else if(!user.equals("Bob") || !pass.equals("1234"))
		{
			code = "175";
			response = code;
		}
		
		else if(!version.equals(cversion))
		{
			code = "115";
			response = code;
		}
		
		else //if everything else matched, games must not contain the requested option 
		{
			code = "120";
			response = code;
		}

		return response;
	}
	
	//error handling for the TCPServer socket from Forouzan, "Computer Networks A Top-Down Approach", p.833
	ConcurTCPServer(Socket s) throws IOException, UnknownHostException
	{
		sock = s;
		recvStream = sock.getInputStream();
		sendStream = sock.getOutputStream();	
	}
	
	//running the thread for the individual client from Forouzan, "Computer Networks A Top-Down Approach", p.833
	public void run()
	{
		getRequest();
		process();
		sendResponse();
	//	close();                      //do I want it to close?*******************************************************************************************************
	}
	
	//getting the incoming message and turning it into a usable String from Forouzan, "Computer Networks A Top-Down Approach", p.833
	void getRequest()
	{
		try
		{
			int dataSize;
			while ((dataSize = recvStream.available()) == 0);
			byte [] recvBuff = new byte [dataSize];
			recvStream.read(recvBuff,0,dataSize);
			request = new String(recvBuff,0,dataSize);
		}
		
		catch (IOException ex)
		{
			System.err.println("IOException in getRequest" + ex.getMessage());
		}
	}
	
	
	//STATEFUL
	//processing the request and creating the response, abstract method taken from from Forouzan, "Computer Networks A Top-Down Approach", p.834 which I updated for my own protocol
	void process()
	{
		//parse for code
		if((request.substring(0,3).equals("100")) && ((state == 0) || state == 1)) 
		{
			authentication(request);
			state = 1; 
		}
		
		else if(request.substring(0,3).equals("250") && ((state == 1) || (state == 3)) )
 		{
			state = 5;  
			System.out.println("Client has left");
		}
          
		else if(request.substring(0,3).equals("350") && (((state == 3) || (state == 1)) || (state == 2)) )   //gameboard sent by client  
 		{
			state = 4;
			int l = request.length();
			for(i = 3; i < (l - 3); i++)
			{
				String test = request.substring(i, i+3);
				if(test.equals("EOM"))
				{
					eom = i;
				}
			}	
			board = request.substring(3, 12);
			check(board);  
			check2(new_Board);
			
			
		}
	}	
	
	//server side takes its turn
	public String serverTurn(String board)
	{
		int server_space = (int) Math.floor(Math.random() * 9);
		System.out.println("Server trying space " + server_space);
		if(board.substring(server_space, server_space + 1).equals("_"))    //if space is available, play the space
		{
			if(server_space < board.length())  //if it's not the 9th space do it this way
			{
				String temp_1 = board.substring(0,server_space);
				String temp_2 = board.substring(server_space);
				temp_2 = "O";
				String temp_3 = board.substring(server_space + 1,board.length());
				new_Board = temp_1 + temp_2 + temp_3;
			}
			
			else                                //if it is the 9th space do it this way
			{
				String temp_1 = board.substring(0,server_space);
				String temp_2 = board.substring(server_space);
				temp_2 = "X";
				new_Board = temp_1 + temp_2;
			}
		}
		else                                   //if space is not available, try again
		{                                 
			serverTurn(board);
		}
				code = "325";
				response = code + new_Board;
				return response;
	}
	
	//1st check for a winner, after board received from client, before server plays its turn
	
	public String check(String board)
	{
	//	state = 3;
		//conditions for server being the winner
		if( /*top row*/(board.substring(0,1).equals("O") && board.substring(1,2).equals("O") && board.substring(2,3).equals("O")) 
			|| /*middle row*/ (board.substring(3,4).equals("O") && board.substring(4,5).equals("O") && board.substring(5,6).equals("O")) 
			|| /*bottom row*/ (board.substring(6,7).equals("O") && board.substring(7,8).equals("O") && board.substring(8,9).equals("O")) 
			|| /*column one*/ (board.substring(0,1).equals("O") && board.substring(3,4).equals("O") && board.substring(6,7).equals("O"))
			|| /*column two*/ (board.substring(1,2).equals("O") && board.substring(4,5).equals("O") && board.substring(7,8).equals("O")) 
			|| /*column three*/ (board.substring(2,3).equals("O") && board.substring(5,6).equals("O") && board.substring(8,9).equals("O")) 
			|| /*diagonal right*/ (board.substring(0,1).equals("O") && board.substring(4,5).equals("O") && board.substring(8,9).equals("O")) 
			|| /*diagonal left*/ (board.substring(6,7).equals("O") && board.substring(4,5).equals("O") && board.substring(2,3).equals("O")))
			{
				code = "375";
				response = code + board;               
				state = 2;
				return response;
			}
		
		//conditions for client being the winner
		else if( /*top row*/(board.substring(0,1).equals("X") && board.substring(1,2).equals("X") && board.substring(2,3).equals("X"))
			|| /*middle row*/ (board.substring(3,4).equals("X") && board.substring(4,5).equals("X") && board.substring(5,6).equals("X"))
			|| /*bottom row*/ (board.substring(6,7).equals("X") && board.substring(7,8).equals("X") && board.substring(8,9).equals("X"))
			|| /*column one*/ (board.substring(0,1).equals("X") && board.substring(3,4).equals("X") && board.substring(6,7).equals("X"))
			|| /*column two*/ (board.substring(1,2).equals("X") && board.substring(4,5).equals("X") && board.substring(7,8).equals("X"))
			|| /*column three*/ (board.substring(2,3).equals("X") && board.substring(5,6).equals("X") && board.substring(8,9).equals("X"))
			|| /*diagonal right*/(board.substring(0,1).equals("X") && board.substring(4,5).equals("X") && board.substring(8,9).equals("X"))
			|| /*diagonal left*/ (board.substring(6,7).equals("X") && board.substring(4,5).equals("X") && board.substring(2,3).equals("X")))
			{
				code = "380";
				response = code + board;               
				state = 2;
				return response;
			}
		
		//if space available change to state 4 and play the server's turn 
		else if(board.contains("_"))
			{
				code = "325";                         
				serverTurn(board);
				response = code + new_Board;
				state = 3;
				return response;
			}
		
		//otherwise it must be a stalemate
		else 
			{
				code = "385";
				response = code + board;               
				state = 2;
				return response;
			}
	}
	
		//2nd Check for a Winner after server turn, before sending board back to client side
	public String check2(String board)
	{
		//conditions for server being the winner
		if( /*top row*/(board.substring(0,1).equals("O") && board.substring(1,2).equals("O") && board.substring(2,3).equals("O")) 
			|| /*middle row*/ (board.substring(3,4).equals("O") && board.substring(4,5).equals("O") && board.substring(5,6).equals("O")) 
			|| /*bottom row*/ (board.substring(6,7).equals("O") && board.substring(7,8).equals("O") && board.substring(8,9).equals("O")) 
			|| /*column one*/ (board.substring(0,1).equals("O") && board.substring(3,4).equals("O") && board.substring(6,7).equals("O"))
			|| /*column two*/ (board.substring(1,2).equals("O") && board.substring(4,5).equals("O") && board.substring(7,8).equals("O")) 
			|| /*column three*/ (board.substring(2,3).equals("O") && board.substring(5,6).equals("O") && board.substring(8,9).equals("O")) 
			|| /*diagonal right*/ (board.substring(0,1).equals("O") && board.substring(4,5).equals("O") && board.substring(8,9).equals("O")) 
			|| /*diagonal left*/ (board.substring(6,7).equals("O") && board.substring(4,5).equals("O") && board.substring(2,3).equals("O")))
			{
				code = "375";
				response = code + board;               
				state = 2;
				return response;
			}
		
		//conditions for client being the winner
		else if( /*top row*/(board.substring(0,1).equals("X") && board.substring(1,2).equals("X") && board.substring(2,3).equals("X"))
			|| /*middle row*/ (board.substring(3,4).equals("X") && board.substring(4,5).equals("X") && board.substring(5,6).equals("X"))
			|| /*bottom row*/ (board.substring(6,7).equals("X") && board.substring(7,8).equals("X") && board.substring(8,9).equals("X"))
			|| /*column one*/ (board.substring(0,1).equals("X") && board.substring(3,4).equals("X") && board.substring(6,7).equals("X"))
			|| /*column two*/ (board.substring(1,2).equals("X") && board.substring(4,5).equals("X") && board.substring(7,8).equals("X"))
			|| /*column three*/ (board.substring(2,3).equals("X") && board.substring(5,6).equals("X") && board.substring(8,9).equals("X"))
			|| /*diagonal right*/(board.substring(0,1).equals("X") && board.substring(4,5).equals("X") && board.substring(8,9).equals("X"))
			|| /*diagonal left*/ (board.substring(6,7).equals("X") && board.substring(4,5).equals("X") && board.substring(2,3).equals("X")))
			{
				code = "380";
				response = code + board;               
				state = 2;
				return response;
			}
		
		//if space not left but neither side won, must be a stalemate
		else if(!board.contains("_"))
			{
				code = "385";
				response = code + board;               
				state = 2;
				return response;
			}
		return response;
	}
	
	
	//sending the response back to the client, again as a String, from Forouzan, "Computer Networks A Top-Down Approach", p.834
	void sendResponse()
	{
		String end = "EOM"; //add end of message indicator per protocol
		response = response + end;
		try
		{
			byte [] sendBuff = new byte [response.length()];
			sendBuff = response.getBytes();
			sendStream.write(sendBuff,0,sendBuff.length);
			System.out.println("Response sent to client"); 
		}
		
		catch(IOException ex)
		{
			System.err.println("IOException in sendResponse");
		}
	}
	
	//close socket from Forouzan, "Computer Networks A Top-Down Approach", p.833
	void close()
	{
		try
		{
			recvStream.close();
			sendStream.close();
			sock.close();
		}
		
		catch(IOException ex)
		{
			System.err.println("IOException in close");
		}
	}
	
	
	
	
//general concurrent TCP server main method taken from from Forouzan, "Computer Networks A Top-Down Approach", p.835, augmented with parts specific to my protocol
	public static void main(String[] args) throws IOException
	{
		final int port = 1030;                                          //SERVICE
		ServerSocket listenSock = new ServerSocket (port);
		games.add("T");
		while (true)
		{
			ConcurTCPServer server = new ConcurTCPServer(listenSock.accept());
			Thread thread = new Thread(server);                         //CONCURRENT
			thread.start();
			while(true)      //added to keep communication going
			{
				server.getRequest();
				server.process();
				server.sendResponse();
			}
		}
	}

}
