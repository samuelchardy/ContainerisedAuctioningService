import okhttp3.*;
import org.json.JSONObject;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Scanner;

public class Client
{
  public static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
  private OkHttpClient client = new OkHttpClient();
  private String jwt = "";
  private int userId = -1;

  /**
   * Send a POST request to the server, sending the json object provided, to the uri provided.
   * @param uri Uniform Resource Indentifier, address of the location you wish to send the request.
   * @param json JSONObject that you wish to send to the server.
   * @return Response from the server.
   * @throws IOException
   */
  private String post(String uri, JSONObject json) throws IOException
  {
    Request request;
    String jsonText = json.toString();
    RequestBody body = RequestBody.create(jsonText, JSON);

    if(!uri.contains("user")){
      request = new Request.Builder().url(uri).addHeader("Authorization", jwt).post(body).build();
    }else{
      request = new Request.Builder().url(uri).post(body).build();
    }

    try (Response response = client.newCall(request).execute()){
      return response.headers().toString() + response.body().string() ;
    }
  }

  /**
   * Send a GET request to the server, to the uri provided.
   * @param uri Uniform Resource Indentifier, address of the location you wish to send the request.
   * @return Response from the server.
   * @throws IOException
   */
  private String get(String uri) throws IOException
  {
    Request request;
    if(!uri.contains("bids") && !uri.contains("user")) {
      request = new Request.Builder().url(uri).addHeader("Authorization", jwt).get().build();
    }else{
      request = new Request.Builder().url(uri).get().build();
    }

    try (Response response = client.newCall(request).execute()){
      return response.headers().toString() + response.body().string() ;
    }
  }

  /**
   * Send a DELETE request to the server, to the uri provided.
   * @param uri Uniform Resource Indentifier, address of the location you wish to send the request.
   * @return Response from the server.
   * @throws IOException
   */
  private String delete(String uri) throws IOException
  {
    Request request = new Request.Builder().url(uri).addHeader("Authorization", jwt).delete().build();

    try (Response response = client.newCall(request).execute()){
      return response.headers().toString() + response.body().string() ;
    }
  }

  /**
   * Provides a command line interface for the client to interract with the server, includes all available options to the user.
   * @throws IOException
   * @throws InterruptedException
   */
  public void cmdInterface() throws IOException, InterruptedException
  {
    while(true) {
      Scanner scanner = new Scanner(System.in);
      String response = "Invalid username/password supplied", input = "", username = "";
      boolean login = false;

      clearCMD();
      System.out.print("\033[1;91m" + "Welcome! Would you like to:" + "\033[0m" + "\n1)Login.\n2)Exit.\n");
      input = scanner.next();

      if(input.equals("1")) {
        while (response.contains("Invalid username/password supplied")) {
          JSONObject json = new JSONObject();
          clearCMD();

          System.out.print("Username: ");
          username = scanner.next();

          System.out.print("Password: ");
          String password = scanner.next();

          json.put("username", username);
          json.put("password", password);

          response = post("http://localhost:9090/api/user/login", json);

          clearCMD();
          System.out.println(response);
          Thread.sleep(4000);
        }

        String jwtString = response.split("\n")[3];
        System.out.println("\n\nFUCK:");
        decodeJWT(jwtString);

        while (true) {
          clearCMD();
          System.out.println("\033[1;91m" + "Welcome! Would you like to:" + "\n\033[0m" + "1) Create a new user.\n2) List all auctions.\n3) Find an auction.\n4) Add a new auction.\n5) Update an auction.\n6) Delete an auction.\n7) List bids on an auction.\n8) Bid on an auction.\n9) Log out.\n");
          input = scanner.next();

          JSONObject json = new JSONObject();
          if (input.equals("1")) {                                                                                          //Create User
            clearCMD();
            int id = Integer.parseInt(get("http://localhost:9090/api/user/id").split("\n")[3]);

            System.out.print("Username: ");
            String usernameToAdd = scanner.next();

            System.out.print("Password: ");
            String password = scanner.next();

            json.put("id", id);
            json.put("username", usernameToAdd);
            json.put("password", password);

            response = post("http://localhost:9090/api/user", json);

          } else if (input.equals("2")) {                                                                                   //List all auctions
            clearCMD();
            response = get("http://localhost:9090/api/auctions");
            response = parseJSONObjects(response);

          } else if (input.equals("3")) {                                                                                   //Get auction by id
            clearCMD();
            System.out.print("Enter auction ID: ");
            input = scanner.next();
            try {
              int id = Integer.parseInt(input);
              response = get("http://localhost:9090/api/auction/" + input.toString());
            } catch (Exception e) {
              response = "Need to input an integer";
            }

          } else if (input.equals("4")) {                                                                                   //Add a new auction
            clearCMD();

            String temp = get("http://localhost:9090/api/auction/id").split("\n")[3];
            int id = Integer.parseInt(temp);
            json.put("id", id);

            System.out.println("Enter name: ");
            input = scanner.next();
            json.put("name", input);

            while (true) {
              try {
                System.out.println("Enter first bid: ");
                input = scanner.next();
                Double.parseDouble(input);
                break;
              } catch (Exception e) {
                clearCMD();
                System.out.println("First bid must be a double value!");
                Thread.sleep(2000);
                clearCMD();
              }
            }
            json.put("firstBid", input);


            id = Integer.parseInt(get("http://localhost:9090/api/user/username/"+username).split("\n")[3]);
            System.out.println(id);
            json.put("sellerId", id);

            json.put("status", "available");
            response = post("http://localhost:9090/api/auction", json);

          } else if (input.equals("5")) {                                                                                   //Update an auction
            clearCMD();

            System.out.println("Enter auction ID: ");
            String id = scanner.next();

            System.out.println("Enter name: ");
            input = scanner.next();
            json.put("name", input);

            try {
              System.out.println("Enter first bid: ");
              input = scanner.next();
              json.put("firstBid", Float.parseFloat(input));
            }catch(Exception e){
              e.printStackTrace();
            }

            System.out.println("Enter seller id: ");
            input = scanner.next();
            json.put("sellerId", Integer.parseInt(input));

            response = post("http://localhost:9090/api/auction/" + id, json);

          } else if (input.equals("6")) {                                                                                   //Delete an auction
            clearCMD();
            System.out.println("Enter auction ID: ");
            String id = scanner.next();
            response = delete("http://localhost:9090/api/auction/" + id);

          } else if (input.equals("7")) {                                                                                   //List bids on an auction
            clearCMD();
            System.out.println("Enter auction ID: ");
            String id = scanner.next();
            response = get("http://localhost:9090/api/auction/" + id + "/bids");
            response = parseJSONObjects(response);

          } else if (input.equals("8")) {                                                                                   //Bid on an auction
            clearCMD();

            int id = Integer.parseInt(get("http://localhost:9090/api/auction/bids/id").split("\n")[3]);
            json.put("id", id);

            System.out.println("Enter auction ID: ");
            input = scanner.next();
            json.put("auctionId", input);

            System.out.println("Enter bid amount: ");
            input = scanner.next();
            json.put("bidAmount", input);

            id = Integer.parseInt(get("http://localhost:9090/api/user/username/"+username).split("\n")[3]);
            json.put("bidderId", id);

            response = post("http://localhost:9090/api/auction/" + id + "/bid", json);

          } else if (input.equals("9")) {                                                                               //Log out
            clearCMD();
            break;
          }
          System.out.println("\n" + response + "\n\nContinue?");
          input = scanner.next();
        }
      }else if(input.equals("2")){
        clearCMD();
        System.out.println("Goodbye...");
        Thread.sleep(2000);
        clearCMD();
        System.exit(0);
      }else{
        System.out.println("Try inputting 1 or 2.");
        Thread.sleep(1500);
      }
    }
  }

  /**
   * Formats server responses that contain more than one json object.
   * @param response Full string response from the server.
   * @return Formatted server response with each json object on its own line.
   */
  private String parseJSONObjects(String response)
  {
    String jsonObjects = response.split("\n")[3];
    String[] indivJson = jsonObjects.split(", ");
    String output = "";

    for(int i=0; i<3; i++){
      output = output + response.split("\n")[i] + "\n";
    }

    for(int i=0; i<indivJson.length; i++){
        output = output + "\n" + indivJson[i];
    }

    return output;
  }

  /**
   * Uses regex string to clear command line.
   */
  private void clearCMD()
  {
    System.out.print("\033[H\033[2J");
    System.out.flush();
  }

  /**
   * Takes a full JWT string and decodes the header and claims from Base64 to text.
   * @param response
   */
  private void decodeJWT(String response)
  {
    System.out.println("\n\n\n\nBIGAIDS: " + response + "\n\n\n\n");
    String[] jwtParts = response.split("\\.");
    System.out.println("Header: " + jwtParts[0]);
    System.out.println("Claims: " + jwtParts[1]);

    JSONObject header = new JSONObject(new String(Base64.getUrlDecoder().decode(jwtParts[0])));
    JSONObject claims = new JSONObject(new String(Base64.getUrlDecoder().decode(jwtParts[1])));
    userId = Integer.parseInt(claims.get("id").toString());
    try{
      checkSignature(header, claims, response);
    }catch(Exception e){
      System.out.println("Error parsing JWT.");
    }
  }

  /**
   * Encodes an array of bytes that represent the header/ claims.
   * @param bytes Array of bytes that will be Base64 encoded.
   * @return A string of the provided bytes encoded using Base64.
   */
  private String encode(byte[] bytes)
  {
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
  }

  /**
   * Generates a JWT token and compares it to the received token to ensure it is valid.
   * @param header JSONObject JWT header (alg/ type).
   * @param claims JSONObject JWT claims (exp/ name/ id/ iss).
   * @param receivedJWT String of the received JWT from the server after user login.
   * @throws Exception
   */
  private void checkSignature(JSONObject header, JSONObject claims, String receivedJWT) throws NoSuchAlgorithmException, InvalidKeyException
  {
    boolean expired = true;
    String secret = "secret";

    String encodedHeader = encode(header.toString().getBytes(StandardCharsets.UTF_8));
    String encodedClaims = encode(claims.toString().getBytes(StandardCharsets.UTF_8));

    //Create Signature
    String data = encodedHeader + "." + encodedClaims;
    byte[] hash = secret.getBytes();
    Mac sha256Hmac = Mac.getInstance("HmacSHA256");
    SecretKeySpec secretKey = new SecretKeySpec(hash, "HmacSHA256");
    sha256Hmac.init(secretKey);
    byte[] signedBytes = sha256Hmac.doFinal(data.getBytes());
    String signature = encode(signedBytes);

    jwt = encodedHeader + "." + encodedClaims + "." + signature;

    if((long)claims.get("exp") > System.currentTimeMillis()){
      expired = false;
    }


    if(jwt.equals(receivedJWT) && expired == false){
      System.out.println("Valid JWT");
    }else {
      System.out.println("Invalid JWT: Token Expired");
    }
  }


  public static void main(String[] args) throws IOException, InterruptedException
  {
    Client client  = new Client();
    client.cmdInterface();
  }
  
}