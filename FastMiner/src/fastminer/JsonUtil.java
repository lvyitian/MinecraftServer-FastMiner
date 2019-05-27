package fastminer;

import java.util.ArrayList;

public class JsonUtil {
  public long MaxDepth=15;
  public boolean NotifyOnPlayerJoin=true;
  public ArrayList<EnableState> Enable=new ArrayList<EnableState>();
  public class EnableState{
	  public String UUID="";
	  public boolean enabled=false;
  }
}
