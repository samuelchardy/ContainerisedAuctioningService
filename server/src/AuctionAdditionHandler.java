import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import org.json.JSONObject;

class AuctionAdditionHandler extends DataHandler
{
    private FileManager auctionFM;

    public void handle(HttpExchange t)
    {
        System.out.println(t.getRequestURI().toString());
        int httpNum = 200;
        String response = "";

        JSONObject obj = parseJsonRequest(t);
        findJWT(t);
        if(checkToken()) {
            if (t.getRequestMethod().equals("POST")) {
                response = auctionFM.addAuction(obj, true);
            } else {
                response = "This endpoint does that use that method";
            }
        }else{
            response = "Invalid Authentication";
            httpNum = 405;
        }

        sendResponse(t, httpNum, response, "application/json");
    }

    public AuctionAdditionHandler(FileManager auctionFM)
    {
        super();
        this.auctionFM = auctionFM;
    }
}