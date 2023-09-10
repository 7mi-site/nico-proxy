package xyz.n7mn.nico_proxy;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import xyz.n7mn.nico_proxy.data.RequestVideoData;
import xyz.n7mn.nico_proxy.data.ResultVideoData;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Pornhub implements ShareService{
    @Override
    public ResultVideoData getVideo(RequestVideoData data) throws Exception {

        Matcher matcher = Pattern.compile("https://(.*).pornhub.com/view_video.php\\?viewkey=(.*)").matcher(data.getURL());
        if (!matcher.find()){
            throw new Exception("Not Found");
        }
        String id = matcher.group(2);
        //System.out.println(id);


        final OkHttpClient.Builder builder = new OkHttpClient.Builder();
        final OkHttpClient client = data.getProxy() != null ? builder.proxy(new Proxy(Proxy.Type.HTTP, new InetSocketAddress(data.getProxy().getProxyIP(), data.getProxy().getPort()))).build() : new OkHttpClient();

        String HtmlText = "";
        try {
            Request request_html = new Request.Builder()
                    .url("https://jp.pornhub.com/view_video.php?viewkey="+id)
                    .build();
            Response response = client.newCall(request_html).execute();
            if (response.body() != null){
                HtmlText = response.body().string();
            }
            response.close();
        } catch (Exception e){
            throw new Exception(e.getMessage());
        }

        //System.out.println(HtmlText);
        Matcher matcher1 = Pattern.compile("var media_0=(.*);flashvars_(\\d+)\\['mediaDefinitions'\\]\\[0\\]").matcher(HtmlText);

        if (!matcher1.find()){
            throw new Exception("Not Found");
        }

        String mediaUrlTemp = matcher1.group(1);
        //System.out.println(mediaUrlTemp);

        List<String> strName = new ArrayList<>();

        boolean skip = false;
        for (String str : mediaUrlTemp.split(" ")){
            if (skip){
                if (str.startsWith("*/")){
                    skip = false;
                    strName.add(str.replaceAll("\\*/", "").split(";")[0]);
                }

                continue;
            }

            if (str.equals("/*")){
                skip = true;
                continue;
            }

            if (str.equals("+")){
                continue;
            }
            strName.add(str.split(";")[0]);
        }

        //System.out.println(strName.size());
        StringBuffer sb = new StringBuffer();
        Matcher matcher2 = Pattern.compile("var ([a-zA-Z0-9]+)=\"(.{0,16})\";").matcher(HtmlText);
        HashMap<String, String> tempStrList = new HashMap<>();
        while (matcher2.find()){
            //String group = matcher2.group();
            String group1 = matcher2.group(1);
            String group2 = matcher2.group(2);
            tempStrList.put(group1, group2);
            //System.out.println(group+" : " + group1 + " / " + group2);
        }

        for (String str : strName){
            if (tempStrList.get(str) != null){
                sb.append(tempStrList.get(str));
            }

        }

        //System.out.println(sb.toString().replaceAll("\"","").replaceAll(" ", "").replaceAll("\\+",""));

        return new ResultVideoData(sb.toString().replaceAll("\"","").replaceAll(" ", "").replaceAll("\\+",""), "", true, false, false, "");
    }

    @Override
    public ResultVideoData getLive(RequestVideoData data) throws Exception {
        return null;
    }

    @Override
    public String getTitle(RequestVideoData data) throws Exception {
        String title = "";

        Matcher matcher = Pattern.compile("https://(.*).pornhub.com/view_video.php\\?viewkey=(.*)").matcher(data.getURL());
        if (!matcher.find()){
            throw new Exception("Not Found");
        }
        String id = matcher.group(2);
        //System.out.println(id);


        final OkHttpClient.Builder builder = new OkHttpClient.Builder();
        final OkHttpClient client = data.getProxy() != null ? builder.proxy(new Proxy(Proxy.Type.HTTP, new InetSocketAddress(data.getProxy().getProxyIP(), data.getProxy().getPort()))).build() : new OkHttpClient();

        String HtmlText = "";
        try {
            Request request_html = new Request.Builder()
                    .url("https://jp.pornhub.com/view_video.php?viewkey="+id)
                    .build();
            Response response = client.newCall(request_html).execute();
            if (response.body() != null){
                HtmlText = response.body().string();
            }
            response.close();
        } catch (Exception e){
            return "";
        }

        Matcher matcher1 = Pattern.compile("<meta name=\"twitter:title\" content=\"(.*)\">").matcher(HtmlText);
        if (!matcher1.find()){
            //System.out.println("naiyo");
            return title;
        }

        title = matcher1.group(1);

        return title;
    }

    @Override
    public String getServiceName() {
        return "Pornhub";
    }

    @Override
    public String getVersion() {
        return "20230910";
    }
}
