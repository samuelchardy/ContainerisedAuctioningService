import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

class AuctionGetDataHandler extends DataHandler
{
    private FileManager auctionFM;

    public void handle(HttpExchange t)
    {
        System.out.println(t.getRequestURI().toString());
        int httpNum = 200;
        String response = "", contentType = "application/text";

        if(t.getRequestMethod().equals("GET")){
            response = Integer.toString(auctionFM.getLastID() + 1);
            contentType = "application/json";
        }else{
            response = "This endpoint does that use that method.";
        }

        sendResponse(t, httpNum, response, contentType);
    }

    public AuctionGetDataHandler(FileManager auctionFM)
    {
        super();
        this.auctionFM = auctionFM;
    }
}