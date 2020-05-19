package hudson.plugins.customer.data.exception;

/**
 * Created by wanghf on 2018/09/22
 */
public class HttpStatusException extends RuntimeException {

  private static final long serialVersionUID = -7259381434228279782L;

  private int statusCode;

  public int getStatusCode() {
    return statusCode;
  }

  public void setStatusCode(int statusCode) {
    this.statusCode = statusCode;
  }

  public HttpStatusException() {}

  public HttpStatusException(int statusCode) {
    this.statusCode = statusCode;
  }

  public HttpStatusException(Exception e) {
    super(e);
  }

  public HttpStatusException(String message) {
    super(message);
  }

  @Override
  public String getMessage() {
    String statusCode = "Status Code:" + getStatusCode();
    String msg = super.getMessage();
    if (msg != null) {
      msg += statusCode;
    } else
      msg = statusCode;
    return msg;
  }

}
