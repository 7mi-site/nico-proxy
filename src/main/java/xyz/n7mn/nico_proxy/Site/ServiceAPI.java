package xyz.n7mn.nico_proxy.Site;

import xyz.n7mn.nico_proxy.ProxySetting;

import java.net.http.HttpClient;

public interface ServiceAPI {

    void setHttpClient(HttpClient client);
    void setURL(String URL);
    void setToken(String[] token);
    void setProxy(ProxySetting proxy);
    String[] getCorrespondingURL();
    String get() throws Exception;
    String getServiceName();
}
