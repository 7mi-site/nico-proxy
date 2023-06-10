package xyz.n7mn.nico_proxy.data;

public class RequestVideoData {
    private final String URL;
    private final ProxyData Proxy;
    public RequestVideoData (String URL, ProxyData proxy){
        this.URL = URL;
        this.Proxy = proxy;
    }

    public String getURL() {
        return URL;
    }

    public ProxyData getProxy() {
        return Proxy;
    }
}
