package xyz.n7mn.nico_proxy;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import xyz.n7mn.nico_proxy.data.RequestVideoData;
import xyz.n7mn.nico_proxy.data.ResultVideoData;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TikTok implements ShareService{
    @Override
    public ResultVideoData getVideo(RequestVideoData data) throws Exception {

        // https://www.tiktok.com/@komedascoffee/video/7258220227773746433
        String url = data.getURL();

        final OkHttpClient.Builder builder = new OkHttpClient.Builder();
        final OkHttpClient client = data.getProxy() != null ? builder.proxy(new Proxy(Proxy.Type.HTTP, new InetSocketAddress(data.getProxy().getProxyIP(), data.getProxy().getPort()))).build() : new OkHttpClient();

        String HtmlText = "";
        try {
            Request request_html = new Request.Builder()
                    .url(url)
                    .build();
            Response response = client.newCall(request_html).execute();
            if (response.body() != null){
                HtmlText = response.body().string();
            }
            response.close();
        } catch (Exception e){
            throw new Exception(e.getMessage());
        }

        // "preloadList":[{"url":"https:\u002F\u002Fv16-webapp-prime.tiktok.com\u002Fvideo\u002Ftos\u002Falisg\u002Ftos-alisg-pve-0037\u002FoQZUqjYnAa2wbXp1eLALAyApefBlEeZffggV5A\u002F?a=1988&ch=0&cr=0&dr=0&lr=unwatermarked&cd=0%7C0%7C0%7C0&cv=1&br=4776&bt=2388&cs=0&ds=3&ft=_RwJrB9eq8ZmolzKqc_vju030AhLrus&mime_type=video_mp4&qs=0&rc=NzNmN2k5OGc6NTk4M2hmZ0BpajhxdDk6ZmQ5bDMzODgzNEBhMzFjMi1jNWMxYTQ0Mi9gYSNfb2prcjRvLnFgLS1kLy1zcw%3D%3D&btag=e00088000&expire=1690617828&l=2023072902033409B33266C883AC3DC41D&ply_type=2&policy=2&signature=b24a2f63e98149a3cd5ce4d6374940e3&tk=tt_chain_token","id":""}]
        Matcher matcher = Pattern.compile("\"preloadList\":\\[\\{\"url\":\"(.*)\",\"id\":\"\"\\}\\]").matcher(HtmlText);
        if (matcher.find()){
            String temp = matcher.group(1);
            temp = temp.replaceAll("\\\\u002F","/");

            return new ResultVideoData(temp, "", false, false, false, "");
        }
        throw new Exception("VideoURL Not Found");
    }

    @Override
    public ResultVideoData getLive(RequestVideoData data) throws Exception {
        // 実装予定はなし
        return null;
    }

    @Override
    public String getServiceName() {
        return "TikTok";
    }

    @Override
    public String getVersion() {
        return "20230729";
    }
}
