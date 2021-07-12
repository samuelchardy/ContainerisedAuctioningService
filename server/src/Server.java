import com.sun.net.httpserver.HttpServer;

import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

import redis.clients.jedis.Jedis;

import org.jgroups.JChannel;
import org.jgroups.Message;
import org.jgroups.ReceiverAdapter;
import org.jgroups.View;
import org.jgroups.util.Util;

import java.io.*;
import java.util.*;

import org.json.JSONObject;


public class Server extends ReceiverAdapter
{
	private JChannel accessChannel;
	private String port;


	private void joinGroup(String username, ArrayList<FileManager> fileManagers) throws Exception
	{
		accessChannel = new JChannel();
		accessChannel.setReceiver(this);
		accessChannel.connect("ChatCluster");
		accessChannel.getState(null, 10000);
		waitForEvent(username, fileManagers);
		accessChannel.close();
	}

	private void waitForEvent(String username, ArrayList<FileManager> fileManagers) throws Exception
	{
		BufferedReader in=new BufferedReader(new InputStreamReader(System.in));
		while(true) {
			try {
				for (FileManager FM : fileManagers) {
					ArrayList<String> changeBuffer = FM.getChangeBuffer();

					for (String change : changeBuffer) {
						Message msg=new Message(null, null, change);
						accessChannel.send(msg);
					}
					FM.emptyChangeBuffer();

				}
			}
			catch(Exception e) {
			}
		}
	}


	public void viewAccepted(View view)
	{
		System.out.println("** view: " + view);
	}


	public void receive(Message msg)
	{
		System.out.println("RECEIVE : " + msg.getSrc() + "\n" + msg.getObject());
		String[] changes = msg.getObject().toString().split("\n");
		FileManager FM = new FileManager(changes[1]);

		System.out.println("OPERATION: " + changes[0]);

		if(changes[0].equals("ADD")){
			if(changes[1].equals("users.txt")){
				FM.addUser(new JSONObject(changes[3]));
			}else{
				FM.addAuction(new JSONObject(changes[3]), false);
			}
		}else if(changes[0].equals("DELETE")){
			FM.deleteById(Integer.parseInt(changes[2]), false);
		}else if(changes[0].equals("UPDATE")){
			System.out.println("Updating");
			FM.updateById(new JSONObject(changes[3]), Integer.parseInt(changes[2]), false);
		}

	}




	/**
	 * Creates a new HttpServer and creates a pool of 10 threads that handle server requests.
	 * @param args
	 */
	public static void main(String[] args)
	{
		if(args.length > 0) {
			Jedis jedis = new Jedis("localhost");
			System.out.println("Connected to redis server sucessfully...");

			FileManager userFM = new FileManager("users.txt");
			FileManager auctionFM = new FileManager("auctions.txt");
			FileManager bidFM = new FileManager("bids.txt");

			ArrayList<FileManager> fileManagers = new ArrayList<FileManager>();
			fileManagers.add(userFM);
			fileManagers.add(auctionFM);
			fileManagers.add(bidFM);




			Server S = new Server();
			Thread groupThread = new Thread(new Runnable() {
				public void run() {
					try{
						S.joinGroup(args[0], fileManagers);
					}catch(Exception e){
						e.printStackTrace();
					}
				}
			});
			groupThread.start();





			try {
				HttpServer server = HttpServer.create(new InetSocketAddress(Integer.parseInt(args[0])), 10);

				server.createContext("/api/auctions", new AuctionDisplayHandler(auctionFM));
				server.createContext("/api/auction", new AuctionAdditionHandler(auctionFM));
				server.createContext("/api/auction/", new AuctionIdHandler(auctionFM, bidFM));
				server.createContext("/api/auction/id", new AuctionGetDataHandler(auctionFM));
				server.createContext("/api/user/id", new UserGetDataHandler(userFM));
				server.createContext("/api/user/username", new UserGetDataHandler(userFM));
				server.createContext("/api/user/login", new LoginHandler(userFM));
				server.createContext("/api/user", new UserHandler(userFM));

				server.setExecutor(Executors.newFixedThreadPool(20));
				System.out.println("Serving...");
				server.start();

				jedis.set("127.0.0.1:" + args[0], "Active");
			} catch (Exception exc) {
				exc.printStackTrace();
			}
		}
	}
}
