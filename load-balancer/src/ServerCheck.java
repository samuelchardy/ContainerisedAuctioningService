import java.util.*;
import java.util.HashSet;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.HashMap;

import java.io.IOException;

import java.net.InetSocketAddress;
import java.net.Socket;

import redis.clients.jedis.Jedis;

class ServerCheck implements Runnable
{
    private HashMap<String, String> servers = new HashMap<String, String>();
    private SortedSet<String> workingServers = new TreeSet<String>();

    private boolean getServerState(String address, int port, int timeout) {
        try {

            try (Socket crunchifySocket = new Socket()) {
                // Connects this socket to the server with a specified timeout value.
                crunchifySocket.connect(new InetSocketAddress(address, port), timeout);
            }
            // Return true if connection successful
            return true;
        } catch (IOException exception) {
            //exception.printStackTrace();

            // Return false if connection fails
            return false;
        }
    }


    public void run()
    {
        Jedis jedis = new Jedis("localhost");

        try
        {
            while(true)
            {
                Set<String> jedisData = jedis.keys("127.0.0.1:*");
                for (String key : jedisData){
                    servers.put(key, jedis.get(key));
                }

                for (String key : servers.keySet()) {
                    String address = key.split(":")[0];
                    int port = Integer.parseInt(key.split(":")[1]);

                    if(getServerState(address, port, 1000)) {
                        workingServers.add(key);
                        //System.out.println("Local: " + key);
                    }
                }

                Thread.sleep(3000);
            }

        }
        catch(Exception e)
        {
            e.printStackTrace();
        }

    }

    public SortedSet<String> getWorkingServers()
    {
        return workingServers;
    }




}
