import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import org.json.JSONObject;


public class UserGetDataHandler extends DataHandler
{
    private FileManager userFM;

    public void handle(HttpExchange t) {
        System.out.println(t.getRequestURI().toString());
        int httpNum = 200;
        String response = "", contentType = "application/json";

        if (t.getRequestURI().toString().equals("/api/user/id")) {                                    //Get last user id + 1
            response = Integer.toString(userFM.getLastID() + 1);

        } else if (t.getRequestURI().toString().contains("/api/user/username/")) {                            //Get user ID given a username
            String username = t.getRequestURI().toString().split("/")[4];
            response = userFM.getIdFromUsername(username);

            if (response.equals("Could not find user with that name")) {
                httpNum = 400;
                contentType = "application/text";
            }
        }

        sendResponse(t, httpNum, response, contentType);
    }

    public UserGetDataHandler(FileManager userFM)
    {
        super();
        this.userFM = userFM;
    }
}