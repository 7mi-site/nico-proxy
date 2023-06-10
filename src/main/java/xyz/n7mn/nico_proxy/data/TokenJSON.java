package xyz.n7mn.nico_proxy.data;

public class TokenJSON {

    private String TokenSendURL;
    private String TokenValue;

    public TokenJSON(String tokenSendURL, String tokenValue){
        this.TokenSendURL = tokenSendURL;
        this.TokenValue = tokenValue;
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
