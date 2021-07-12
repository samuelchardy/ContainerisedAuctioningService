import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

import java.util.ArrayList;

import org.json.JSONObject;

public class FileManager
{
    private Semaphore semaphore = new Semaphore(1);
    private ArrayList<JSONObject> fileObjects = new ArrayList<JSONObject>();
    private ArrayList<String> changeBuffer = new ArrayList<String>();
    private String file;

    /**
     * FileManager constructor that sets the file to manage.
     * @param file
     */
    public FileManager(String file)
    {
        this.file = file;
    }


    public ArrayList<String> getChangeBuffer()
    {
        return changeBuffer;
    }

    public void emptyChangeBuffer()
    {
        changeBuffer.clear();
    }


    /**
     * Write a JSON object to the file.
     * @param json JSONObject that will be written to the file.
     * @return Boolean TRUE once the file has been written.
     */
    public String addAuction(JSONObject json, boolean useBuffer)
    {
        try {
            for (JSONObject auction : fileObjects) {
                if (json.get("id") == auction.get("id")) {
                    int newId = getLastID();
                    JSONObject tempJson = new JSONObject();
                    tempJson.put("id", newId);
                    tempJson.put("username", json.get("username"));
                    tempJson.put("password", json.get("password"));
                    json = new JSONObject(tempJson.toString());
                    System.out.println(json.toString());
                }
            }
            if (useBuffer) {
                changeBuffer.add("ADD\n" + file + "\n" + "\n" + json.toString());
            }else {
                writeToFile(json);
            }
            return json.toString();
        }catch(Exception e){
            return "Invalid Input";
        }
    }

    /**
     * Update a JSONObject stored on a file.
     * @param json The new version of the JSONObject that is being updating.
     * @param id Selecting the JSONObject to update.
     * @return A string that specifies whether or not a JSONObject was successfully updated.
     */
    public String updateById(JSONObject json, int id, boolean useBuffer)
    {
        readFromFile();

        try{
            String name = json.get("name").toString();

            for(int i=0; i<fileObjects.size(); i++){
                if((Integer)fileObjects.get(i).get("id") == id){
                    JSONObject jsonNew = new JSONObject();
                    jsonNew.put("id",       fileObjects.get(i).get("id"));
                    jsonNew.put("name",     json.get("name").toString());
                    jsonNew.put("firstBid", json.get("firstBid").toString());
                    jsonNew.put("sellerId", json.get("sellerId"));
                    jsonNew.put("status",   fileObjects.get(i).get("status"));
                    fileObjects.set(i, jsonNew);

                    if(useBuffer) {
                        changeBuffer.add("UPDATE\n" + file + "\n" + id + "\n" + jsonNew.toString());
                    }else {
                        System.out.println("Updating2.0");
                        updateFile(fileObjects);
                    }
                    return fileObjects.get(i).toString();
                }
            }
            return "Auction not found";
        }catch(Exception e){
            return "Invalid input";
        }
    }

    /**
     * Gets the last ID of a stored auction.
     * @return ID of last auction stored on file.
     */
    public int getLastAuctionID()
    {
        readFromFile();
        JSONObject fileLine = fileObjects.get(fileObjects.size()-1);
        return (Integer)fileLine.get("id");
    }

    /**
     * Get a specific JSONObject in the form of a string, chosen by a specific id.
     * @param id ID of JSONObject you want to retreive.
     * @return String of JSONObject or error message.
     */
    public String getById(int id)
    {
        String label;
        ArrayList<JSONObject> matchingBids = new ArrayList<JSONObject>();

        readFromFile();

        for(JSONObject jsonObj : fileObjects){
            if(file.equals("auctions.txt")) {
                if((Integer)jsonObj.get("id") == id) {
                    return jsonObj.toString();
                }
            }else{
                if (jsonObj.get("auctionId").equals(Integer.toString(id))) {
                    matchingBids.add(jsonObj);
                }
            }
        }

        if(!file.equals("auctions.txt") && matchingBids.size() != 0){
            return matchingBids.toString();
        }

        if(file.equals("auctions.txt")) {
            return "Auction not found";
        }else{
            return "No bids found for this auction";
        }
    }


    public double getHighestBid(int auctionId)
    {
        readFromFile();
        double highestBid = -1.00;

        for(JSONObject bid : fileObjects){
            if (Integer.parseInt(bid.get("auctionId").toString()) == auctionId ) {
                if (Double.parseDouble(bid.get("bidAmount").toString()) > highestBid) {
                    highestBid = Double.parseDouble(bid.get("bidAmount").toString());
                }
            }
        }

        return highestBid;
    }


    /**
     * Delete a given JSONObject stored on a file, specified by a given ID.
     * @param id ID of JSONObject to delete from the file.
     * @return True if successful operation, otherwise false.
     */
    public boolean deleteById(int id, boolean useBuffer)
    {
        readFromFile();

        for(JSONObject auction : fileObjects){
            if((Integer)auction.get("id") == id){
                fileObjects.remove(auction);
                if (useBuffer) {
                    changeBuffer.add("DELETE\n" + file + "\n" + id + "\n");
                } else {
                    updateFile(fileObjects);
                }
                return true;
            }
        }
        return false;
    }

    /**
     * Return a string of all JSONObjects stored in a given file.
     * @return All JSONObjects stored in a given file
     */
    public String getAll()
    {
        readFromFile();
        return fileObjects.toString();
    }

    /**
     * Reads all JSONObjects from a file, and stores them in an ArrayList of JSONObjects.
     */
    private void readFromFile()
    {
        semaphore.P();
        try {
            BufferedReader buffReader = new BufferedReader(new FileReader(new File(file)));
            String line;
            fileObjects.clear();
            while((line = buffReader.readLine()) != null){
                fileObjects.add(new JSONObject(line));
            }
            buffReader.close();
        }catch(Exception e){
            e.printStackTrace();
        }
        semaphore.V();
    }

    /**
     * Re-writes the whole file via the ArrayList of JSONObjects.
     * @param fileObjects Edited contents of the file.
     */
    private void updateFile(ArrayList<JSONObject> fileObjects)
    {
        semaphore.P();
        try {
            BufferedWriter buffWriter = new BufferedWriter(new FileWriter(new File(file)));
            for(JSONObject jsonObject : fileObjects) {
                buffWriter.write(jsonObject.toString());
                buffWriter.newLine();
            }
            buffWriter.close();
        }catch(IOException e){
            e.printStackTrace();
        }
        semaphore.V();
    }

    /**
     * Appends a new JSONObject to a new line at the end of the file.
     * @param json JSONObject to be appended to the end of the file.
     */
    private void writeToFile(JSONObject json)
    {
        semaphore.P();
        try {
            BufferedWriter buffWriter = new BufferedWriter(new FileWriter(new File(file), true));
            buffWriter.write(json.toString());
            buffWriter.newLine();
            buffWriter.close();
        }catch(IOException e){
            e.printStackTrace();
        }
        semaphore.V();
    }

    /**
     * Adds a new user given that the provided username has not already been taken.
     * @param json JSONObject containing username, password and id.
     * @return True if the user is successfully added, otherwise False.
     */
    public boolean addUser(JSONObject json)
    {
        readFromFile();
        for(JSONObject user : fileObjects){
            if(user.get("username").toString().equals(json.get("username").toString())){
                return false;
            }
            if(json.get("id") == user.get("id") ){
                int newId = getLastID();
                JSONObject tempJson = new JSONObject();
                tempJson.put("id", newId);
                tempJson.put("username", json.get("username"));
                tempJson.put("password", json.get("password"));
                json = new JSONObject(tempJson.toString());
            }
        }
        changeBuffer.add("ADD\n" + file + "\n" + "\n" + json.toString());
        writeToFile(json);
        return true;
    }

    /**
     * Checks a users username and password and if they are a match logs them in.
     * @param json JSONObject containing username/ password/ id.
     * @return True if username/ password match a stored pair, otherwise False.
     */
    public boolean login(JSONObject json)
    {
        readFromFile();
        for(JSONObject line : fileObjects){
            if(line.get("username").toString().equals(json.get("username").toString()) && line.get("password").toString().equals(json.get("password").toString())){
                return true;
            }
        }
        return false;
    }

    /**
     * Gets the ID of the last stored user.
     * @return Integer of the last ID of a stored user.
     */
    public int getLastID()
    {
        readFromFile();
        return (Integer)fileObjects.get(fileObjects.size()-1).get("id");
    }

    /**
     * Get the ID of a user given the provided ID.
     * @param username String username which you wish to find the ID of.
     * @return The ID of a user.
     */
    public String getIdFromUsername(String username)
    {
        readFromFile();

        for(JSONObject user : fileObjects){
            if(user.get("username").equals(username)){
                return user.get("id").toString();
            }
        }

        return "Could not find user with that name";
    }

}