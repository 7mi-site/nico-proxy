package xyz.n7mn.nico_proxy.data;

public class EncryptedTokenJSON {

    private String EncryptedURL;
    private String TokenSendURL;
    private String TokenValue;

    public EncryptedTokenJSON(String encryptedURL, String tokenSendURL, String tokenValue){
        this.EncryptedURL = encryptedURL;
        this.TokenSendURL = tokenSendURL;
        this.TokenValue = tokenValue;
    }

    public String getEncryptedURL() {
        return EncryptedURL;
    }

    public String getTokenSendURL() {
        return TokenSendURL;
    }

    public String getTokenValue() {
        return TokenValue;
    }

    public void setTokenSendURL(String tokenSendURL) {
        TokenSendURL = tokenSendURL;
    }

    public void setTokenValue(String tokenValue) {
        TokenValue = tokenValue;
    }
}
