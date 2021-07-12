import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.Headers;

import java.io.OutputStream;
import java.io.InputStream;

import org.json.JSONObject;
import org.json.JSONTokener;

import java.util.*;


public class DataHandler implements HttpHandler
{
    private int id = -1;
    private JwtAssembler JWT;
    private Headers headers;
    private String fullJwt;

    public JSONObject parseJsonRequest(HttpExchange t)
    {
        findJWT(t);
        InputStream is = t.getRequestBody();
        JSONTokener jt = new JSONTokener(is);
        String output = "";
        char line;

        while ((line = jt.next()) != 0) {
            output += line;
        }
        return new JSONObject(output);

    }


    public void findJWT(HttpExchange t)
    {
        headers = t.getRequestHeaders();
        if(headers.containsKey("Authorization")){
            JWT = new JwtAssembler(new FileManager("users.txt"));
            fullJwt = headers.getFirst("Authorization");
            JWT.decodeJWT(fullJwt);
            JSONObject claims = JWT.getClaims();
            id = Integer.parseInt(claims.get("id").toString());
        }
    }


    public boolean checkToken()
    {
        return JWT.checkToken(fullJwt);
    }


    public void sendResponse(HttpExchange t, int httpNum, String response, String contentType)
    {
        try {
            t.getResponseHeaders().add("Content-Type:", contentType);
            t.sendResponseHeaders(httpNum, response.length());
            OutputStream os = t.getResponseBody();
            os.write(response.getBytes());
            os.close();
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    public int getId()
    {
        return id;
    }

    public void handle(HttpExchange t)
    {

    }

    public DataHandler()
    {

    }



}