package xyz.n7mn.nico_proxy.Exception;

public class FailedRetrieveException extends Exception {

    private String errorMessage;

    public FailedRetrieveException() {
        errorMessage = "";
    }

    public FailedRetrieveException(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}
