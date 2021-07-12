import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;


public class JwtAssembler
{
    private FileManager userFM;
    private JSONObject header;
    private JSONObject claims;

    /**
     * Turns a byte array into a base64 encoded string.
     * @param bytes A byte array containing header and claims for JWT.
     * @return Base64 encoded string.
     */
    private static String encode(byte[] bytes)
    {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }


    /**
     * Generates a full valid JWT token, factoring in users name, id, and a reasonable expiry time.
     * @param json JSON object containing the current users name, id, password etc.
     * @return JWT token in string form, containing header, claims, and signature, separated by regex "\\.".
     * @throws Exception
     */
    public String generateJWT(JSONObject json) throws NoSuchAlgorithmException, InvalidKeyException
    {
        String secret = "secret";
        long expTime = System.currentTimeMillis() + 900000;

        //Create header and claims JSONObjects
        JSONObject header = new JSONObject("{\"alg\": \"HS256\",\"typ\": \"JWT\"}");
        JSONObject claims = new JSONObject("{\"iss\": \"http://localhost:8080/api\",\"admin\": false}");
        claims.put("name", json.get("username").toString());
        claims.put("id", Integer.parseInt(userFM.getIdFromUsername(json.get("username").toString())));
        claims.put("exp", expTime);

        //Encode header and claims
        String encodedHeader = encode(header.toString().getBytes(StandardCharsets.UTF_8));
        String encodedclaims = encode(claims.toString().getBytes(StandardCharsets.UTF_8));

        //Create signature
        String data = encodedHeader + "." + encodedclaims;
        byte[] hash = secret.getBytes();
        Mac SHMAC = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKey = new SecretKeySpec(hash, "HmacSHA256");
        SHMAC.init(secretKey);
        byte[] sBytes = SHMAC.doFinal(data.getBytes());
        String signature = encode(sBytes);

        //Compose final JWT string
        return encodedHeader + "." + encodedclaims + "." + signature;
    }


    public boolean checkSignature(JSONObject header, JSONObject claims, String receivedJWT) throws NoSuchAlgorithmException, InvalidKeyException
    {
        boolean expired = false;
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

        String jwt = encodedHeader + "." + encodedClaims + "." + signature;

        if((long)claims.get("exp") < System.currentTimeMillis() ){
            expired = true;
        }


        if(jwt.equals(receivedJWT) && expired == false){
            System.out.println("Valid JWT");
            return true;
        }else if(jwt.equals(receivedJWT) && expired == true){
            System.out.println("Invalid JWT: Token Expired");
            return false;
        }else{
            System.out.println("Invalid JWT");
            return false;
        }
    }


    /**
     * Takes a full JWT string and decodes the header and claims from Base64 to text.
     * @param response
     */
    public void decodeJWT(String response)
    {
        String[] jwtParts = response.split("\\.");
        header = new JSONObject(new String(Base64.getUrlDecoder().decode(jwtParts[0])));
        claims = new JSONObject(new String(Base64.getUrlDecoder().decode(jwtParts[1])));
    }


    public boolean checkToken(String fullToken){
        try{
            return checkSignature(header, claims, fullToken);
        }catch(Exception e){
            System.out.println("Error parsing JWT.");
            return false;
        }
    }


    public JSONObject getHeader()
    {
        return header;
    }


    public JSONObject getClaims()
    {
        return claims;
    }


    public JwtAssembler(FileManager userFM)
    {
        this.userFM = userFM;
    }

}