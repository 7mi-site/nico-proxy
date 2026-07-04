package xyz.n7mn.nico_proxy;

public class ProxySetting {

    private String IP;
    private int Port;

    public ProxySetting(String IP, int Port) {
        this.IP = IP;
        this.Port = Port;
    }

    public String getIP() {
        return IP;
    }

    public void setIP(String IP) {
        this.IP = IP;
    }

    public int getPort() {
        return Port;
    }

    public void setPort(int port) {
        this.Port = port;
    }
}
