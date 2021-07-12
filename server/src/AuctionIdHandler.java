import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import org.json.JSONObject;

class AuctionIdHandler extends DataHandler
{
    private FileManager auctionFM, bidFM;

    public void handle(HttpExchange t)
    {
        System.out.println(t.getRequestURI().toString());
        String response = "", contentType = "application/json";
        int httpNum = 200;

        if (t.getRequestMethod().equals("GET")) {
            if(t.getRequestURI().toString().contains("bids/id")){                                                       //Get id of last id
                response = Integer.toString(bidFM.getLastID() + 1);
                contentType = "application/text";

            }else if(t.getRequestURI().toString().contains("bids")){                                                    //List bids for an auction
                try {
                    String[] uriSplit = t.getRequestURI().toString().split("/");
                    int id = Integer.parseInt(uriSplit[3]);
                    response = bidFM.getById(id);

                    if(response.equals("Auction not found")){
                        httpNum = 404;
                    }

                }catch(Exception e){
                    httpNum = 400;
                    response = "Invalid ID supplied";
                }

            }else if(t.getRequestURI().toString().contains("/auction/")){                                               //List auction by id
                findJWT(t);

                if(checkToken()) {

                    String[] uriSplit = t.getRequestURI().toString().split("/");
                    int id = Integer.parseInt(uriSplit[3]);
                    response = auctionFM.getById(id);
                }else{
                    response = "Invalid Authentication";
                    httpNum = 405;
                }
            }

        } else if (t.getRequestMethod().equals("POST")) {                                                               //Adding a bid
            JSONObject obj = parseJsonRequest(t);

            if(checkToken()) {
                if (t.getRequestURI().toString().contains("/bid")) {
                    String item = auctionFM.getById(Integer.parseInt(obj.get("auctionId").toString()));
                    JSONObject auctionObj = new JSONObject(item);

                    int auctionId = Integer.parseInt(obj.get("auctionId").toString());
                    double highestBid = bidFM.getHighestBid(auctionId);

                    if (!item.equals("Auction not found") && !item.equals("No bids found for this auction")) {
                        if (getId() != Integer.parseInt(auctionObj.get("sellerId").toString())) {
                            if (Double.parseDouble(obj.get("bidAmount").toString()) > highestBid) {
                                response = bidFM.addAuction(obj, true);
                            } else {
                                response = "Must bid higher can current highest.";
                                httpNum = 400;
                            }
                        } else {
                            response = "Sellers cannot bid on their own auctions.";
                            httpNum = 400;
                        }
                    }

                } else if (t.getRequestURI().toString().contains("/auction/")) {
                    String[] uriSplit = t.getRequestURI().toString().split("/");
                    int id = Integer.parseInt(uriSplit[3]);                                                                        //Update an auction
                    response = auctionFM.updateById(obj, id, true);

                } else {
                    httpNum = 404;
                    response = "Not found";
                }
            }else{
                response = "Invalid Authentication";
                httpNum = 405;
            }

        } else if (t.getRequestMethod().equals("DELETE")) {                                                             //Delete an auction
            findJWT(t);
            if (checkToken()) {
                try {
                    String[] uriSplit = t.getRequestURI().toString().split("/");
                    int id = Integer.parseInt(uriSplit[3]);
                    JSONObject auctionObj = new JSONObject(auctionFM.getById(id));

                    if (getId() == Integer.parseInt(auctionObj.get("sellerId").toString())) {
                        if (auctionFM.deleteById(id, true)) {
                            response = "Successful operation";
                        } else {
                            httpNum = 404;
                            response = "Auction not found";
                        }
                    } else {
                        response = "Only seller can delete his/her auction.";
                        httpNum = 400;
                    }
                } catch (Exception e) {
                    response = "Invalid input";
                    httpNum = 400;
                }
            }else{
                response = "Invalid Authentication";
                httpNum = 405;
            }
        }


        sendResponse(t, httpNum, response, contentType);
    }

    public AuctionIdHandler(FileManager auctionFM, FileManager bidFM)
    {
        super();
        this.auctionFM = auctionFM;
        this.bidFM = bidFM;
    }
}