package xyz.n7mn.nico_proxy.data;

public class NicoCookie {

    private String CloudFront_Policy;
    private String CloudFront_Signature;
    private String CloudFront_Key_Pair_Id;
    private String session;

    public String getCloudFront_Policy() {
        return CloudFront_Policy;
    }

    public void setCloudFront_Policy(String cloudFront_Policy) {
        CloudFront_Policy = cloudFront_Policy;
    }

    public String getCloudFront_Signature() {
        return CloudFront_Signature;
    }

    public void setCloudFront_Signature(String cloudFront_Signature) {
        CloudFront_Signature = cloudFront_Signature;
    }

    public String getCloudFront_Key_Pair_Id() {
        return CloudFront_Key_Pair_Id;
    }

    public void setCloudFront_Key_Pair_Id(String cloudFront_Key_Pair_Id) {
        CloudFront_Key_Pair_Id = cloudFront_Key_Pair_Id;
    }

    public String getSession() {
        return session;
    }

    public void setSession(String session) {
        this.session = session;
    }
}
