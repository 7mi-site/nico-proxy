package xyz.n7mn.nico_proxy;

import xyz.n7mn.nico_proxy.data.RequestVideoData;
import xyz.n7mn.nico_proxy.data.ResultVideoData;

public interface ShareService {

    ResultVideoData getVideo(RequestVideoData data) throws Exception;

    ResultVideoData getLive(RequestVideoData data) throws Exception;

    String getServiceName();
    String getVersion();

}
