package randoop.plugin.tests.ui.launchConfigurations;

public class MutableBoolean {
  boolean fValue;

  public MutableBoolean(boolean value) {
    fValue = value;
  }

  public boolean getValue() {
    return fValue;
  }

  public void setResponse(boolean value) {
    fValue = value;
  }
}