package xyz.n7mn.nico_proxy;

import xyz.n7mn.nico_proxy.data.ProxyData;
import xyz.n7mn.nico_proxy.data.RequestVideoData;
import xyz.n7mn.nico_proxy.data.ResultVideoData;

public interface ShareService {

    @Deprecated
    String getVideo(String url, ProxyData proxy) throws Exception;
    ResultVideoData getVideo(RequestVideoData data) throws Exception;

    @Deprecated
    String getLive(String url, ProxyData proxy) throws Exception;
    ResultVideoData getLive(RequestVideoData data) throws Exception;

    String getServiceName();
    String getVersion();

}
