package se.sics.mspsim.core;

public class StopExecutionException extends RuntimeException {
  protected int r15val;

  public StopExecutionException(int r15val, String msg) {
    super(msg);
    this.r15val = r15val;
  }

  public int getR15Val() {
    return r15val;
  }
}
