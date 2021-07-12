import okhttp3.*;
import org.json.JSONObject;
import java.util.HashMap;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisSentinelPool;
import java.util.Arrays;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.Headers;

import java.net.InetSocketAddress;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.net.URI;
import java.net.URL;
import java.io.*;
import org.json.JSONObject;
import org.json.JSONTokener;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java. util. Iterator;

public class LoadBalancer implements HttpHandler
{
  public static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
  OkHttpClient lbSender = new OkHttpClient();
  OkHttpClient client = new OkHttpClient();
  public static ServerCheck sc = new ServerCheck();
  private static int position = 0;
  private static int counter = 0;
  private String jwt;


  public void handle(HttpExchange t) throws IOException
  {
      /*Connecting to Redis server on localhost
      Jedis jedis = new Jedis("localhost");
      System.out.println("Connection to server sucessfully");
        */

      int httpNum = 200;
      String response = "";
      URI absoluteURI = t.getRequestURI();      //get the URI                                                                      //Get the URI
      String uri = absoluteURI.toString();
      InetSocketAddress sockAddr = t.getLocalAddress(); //gets the hostname and port
      String hostname = sockAddr.getHostName();
      int port = 8080;
      String method = t.getRequestMethod();

      Headers header = t.getRequestHeaders();
      System.out.println("HEADERS FROM CLIENT:" + header.values());
      System.out.println("JWT:" + header.getFirst("Authorization"));
      jwt = header.getFirst("Authorization");


      //Get number of working servers
      int sizeServers = sc.getWorkingServers().size();
      String target = null;

      if (position > sizeServers - 1)
          position = 0;

      counter=0;
      Iterator iterator = sc.getWorkingServers().iterator();
      while(iterator.hasNext())
      {
          if(counter==position)
          {
              target = (String) iterator.next();
              break;
          }
          iterator.next();
          counter++;
      }

      position++;


      //Chosen server get details
      String serverDetails[] = target.split(":");
      String currentHost = serverDetails[0];
      String currentPort = serverDetails[1];


      System.out.println("\n\n" + "GOING TO SERVER: " + currentHost + ":" + currentPort + "\n\n");

      if(method.equals("POST"))
      {
          //insert new detauls here for hostname/port
          String url = "http://" + currentHost + ":" + currentPort + uri;
          System.out.println(url);

          //GET JSON OBJECT
          InputStream is = t.getRequestBody();
          JSONTokener jt = new JSONTokener(is);
          String output = "";
          char line;

          while ((line = jt.next()) != 0)
          {
              output += line;
          }
          System.out.println("output" + output);
          JSONObject json = new JSONObject(output);

          response = post(url, json);
          System.out.println("\n" + response);
      }

      else if(method.equals("GET"))
      {
          //insert new detauls here for hostname/port
          String url = "http://" + currentHost + ":" + currentPort + uri;
          System.out.println(url);
          response = get(url);
      }

      else if(method.equals("DELETE"))
      {
          //insert new detauls here for hostname/port
          String url = "http://" + currentHost + ":" + currentPort + uri;
          System.out.println(url);
          response = delete(url);
      }

      //Response to client body
      String[] splitResponse = response.split("\n");
      String newResponse = "";

      for (int i=0; i <splitResponse.length; i++){
          if(i > 2){
              newResponse = newResponse + splitResponse[i] + "\n";
          }
      }




      t.getResponseHeaders().add("Content-Type:","application/json");
      t.sendResponseHeaders(httpNum, newResponse.length());
      OutputStream os = t.getResponseBody();
      os.write(newResponse.getBytes());
      os.close();



  }

    private String post(String url, JSONObject json) throws IOException
    {
        String jsonText = json.toString();
        RequestBody body = RequestBody.create(jsonText, JSON);
        Request request;

        if(!url.contains("user")){
            request = new Request.Builder().url(url).addHeader("Authorization", jwt).post(body).build();
        }else{
            request = new Request.Builder().url(url).post(body).build();
        }

        try (Response response = client.newCall(request).execute())
        {
            return response.headers().toString() + response.body().string() ;
        }
    }

    /**
     * Build a GET request to a specific URL
     * @param url - url to get from
     * @return - return the response from the server (request from GET)
     * @throws IOException
     */
    private String get(String url) throws IOException
    {
        Request request = new Request.Builder().url(url).get().build();

        try (Response response = client.newCall(request).execute())
        {
            return response.headers().toString() + response.body().string() ;
        }
    }

    /**
     * Build a DELETE method to a specific url
     * @param url - url to delete from
     * @return - response from the web server
     * @throws IOException
     */
    private String delete(String url) throws IOException
    {
        Request request = new Request.Builder().url(url).addHeader("Authorization", jwt).delete().build();

        try (Response response = client.newCall(request).execute())
        {
            return response.headers().toString() + response.body().string() ;
        }
    }


  public static void main(String[] args) throws IOException
  {
      try
      {
          //Server check every 10 seconds
          Thread serverCheck = new Thread(sc);
          serverCheck.start();

          HttpServer loadBalancer = HttpServer.create(new InetSocketAddress(9090), 10);
          loadBalancer.createContext("/api/", new LoadBalancer());
          loadBalancer.setExecutor(Executors.newFixedThreadPool(20));
          loadBalancer.start();
          System.out.println("Serving...");
      }
      catch(IOException io)
      {
          io.printStackTrace();
      }
  }
}














/*import redis.clients.jedis.Jedis;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisCluster;

public class LoadBalancer
{
  public static void main(String[] args) {
    //Connecting to Redis server on localhost
    Jedis jedis = new Jedis("localhost");
    System.out.println("Connection to server sucessfully");
    //
    System.out.println("Server is running: "+jedis.ping());
    //set the data in redis string
    jedis.set("tutorial-name", "Redis tutorial");
    // Get the stored data and print it
    System.out.println("Stored string in redis:: "+ jedis.get("tutorial-name"));

   try {
     Set<HostAndPort> nodes = new HashSet<HostAndPort>();
     nodes.add(new HostAndPort("127.0.0.1", 8080));

     HostAndPort hp = new HostAndPort("localhost", 8080);

     JedisCluster cluster = new JedisCluster(hp);

     System.out.println(cluster.get("foo"));

     cluster.set("test", "6379");
     System.out.println(cluster.get("test"));
   }
   catch(Exception e)
   {
     e.printStackTrace();
   }

  }
}*/