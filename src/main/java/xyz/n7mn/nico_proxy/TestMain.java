package xyz.n7mn.nico_proxy;

import xyz.n7mn.nico_proxy.Site.NicoVideo;
import xyz.n7mn.nico_proxy.Site.ServiceAPI;
import xyz.n7mn.nico_proxy.Site.ServiceList;
import xyz.n7mn.nico_proxy.Site.fc2;

import java.net.InetSocketAddress;
import java.net.ProxySelector;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.regex.Pattern;

public class TestMain {

    public static void main(String[] args) {

        if (args.length < 1 || args.length > 5) {
            System.out.println(1);
            return;
        }

        final String URL = args[0];
        String token1 = args.length >= 3 ? args[1] : null;
        String token2 = args.length >= 3 ? args[2] : null;
        String Proxy = args.length == 4 ? args[3] : (args.length == 2 ? args[1] : null);

        ServiceAPI Service = null;
        for (ServiceAPI api : ServiceList.getServiceList()) {
            for (String s : api.getCorrespondingURL()) {
                Pattern compile = Pattern.compile(s.replaceAll("\\.", "\\.").replaceAll("\\*", ".*"));
                //System.out.println(s);
                if (URL.startsWith("http://"+s) ||  URL.startsWith("https://"+s) || URL.startsWith(s)) {
                    Service = api;
                    break;
                }

                if (URL.startsWith("http") && compile.matcher(URL).find() && !api.getServiceName().equals("ニコニコ")){
                    Service = api;
                    break;
                }
            }

            if (Service != null){
                break;
            }
        }

        if (Service == null){
            System.out.println(2);
            return;
        }

        try (HttpClient client = Proxy == null ? HttpClient.newBuilder()
                                                 .version(HttpClient.Version.HTTP_2)
                                                 .followRedirects(HttpClient.Redirect.NORMAL)
                                                 .connectTimeout(Duration.ofSeconds(5))
                                                 .build() :
                                                 HttpClient.newBuilder()
                                                 .version(HttpClient.Version.HTTP_2)
                                                 .followRedirects(HttpClient.Redirect.NORMAL)
                                                 .connectTimeout(Duration.ofSeconds(5))
                                                 .proxy(ProxySelector.of(new InetSocketAddress(Proxy.split(":")[0], Integer.parseInt(Proxy.split(":")[1]))))
                                                 .build()
        ){
            Service.setURL(URL);
            if (Service.getServiceName().equals("ツイキャス")){
                Service.setToken(new String[]{token1, token2});
            } else if (Service.getServiceName().equals("ニコニコ")){
                if (token1 != null && token2 != null){
                    Service.setToken(new String[]{token1, token2});
                }
            }

            Service.setHttpClient(client);

            if (Proxy != null){
                Service.setProxy(new ProxySetting(Proxy.split(":")[0], Integer.parseInt(Proxy.split(":")[1])));
            }

            try {
                String result = Service.get();
                if (result != null) {
                    System.out.println(0);
                    if (Service instanceof NicoVideo){
                        ((NicoVideo) Service).closeWebsocket();
                    }
                    if (Service instanceof fc2){
                        ((fc2) Service).closeWebsocket();
                    }
                }

            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        } catch (Exception e){
            System.out.println(3);
            e.printStackTrace();
        }


    }

}
