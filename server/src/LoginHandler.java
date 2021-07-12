import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import org.json.JSONObject;


public class LoginHandler extends DataHandler
{
    private FileManager userFM;

    public void handle(HttpExchange t)
    {
        System.out.println(t.getRequestURI().toString());
        int httpNum = 200;
        String response = "", contentType = "application/json";
        JSONObject json = parseJsonRequest(t);

        if (t.getRequestMethod().equals("POST")) {
            if (!userFM.login(json)) {
                response = "Invalid username/password supplied";
                httpNum = 400;
                contentType = "application/text";
            } else {
                try {
                    response = new JwtAssembler(userFM).generateJWT(json);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }else{
            response = "This endpoint does that use that method.";
        }

        sendResponse(t, httpNum, response, contentType);
    }


    public LoginHandler(FileManager userFM)
    {
        super();
        this.userFM = userFM;
    }
}