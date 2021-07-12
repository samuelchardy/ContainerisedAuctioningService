import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import org.json.JSONObject;


class UserHandler extends DataHandler
{
    private FileManager userFM;

    public void handle(HttpExchange t)
    {
        System.out.println(t.getRequestURI().toString());
        int httpNum = 200;
        String response = "", contentType = "application/json";

        if (t.getRequestMethod().equals("POST")) {
            JSONObject json = parseJsonRequest(t);
            if (!userFM.addUser(json)) {
                response = "Invalid username/password supplied";
                httpNum = 400;
                contentType = "application/text";
            }
        }


        sendResponse(t, httpNum, response, contentType);


    }

    public UserHandler(FileManager userFM)
    {
        super();
        this.userFM = userFM;
    }
}