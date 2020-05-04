
package fastminer;

import java.util.Vector;

public class JsonUtil
{
  public long maxDepth = 15;
  public boolean notifyOnPlayerJoin = true;
  public boolean lavaDetect = true;
  public boolean lavaNotify = true;
  public Vector<EnableState> enable = new Vector<>();
  public Vector<BlockType> extraBlockType = new Vector<>();
  public static class BlockType{
    public String namespace;
    public String name;
    public int damage;
  }
  public boolean saveState=true;
  public static class EnableState
  {
    public String uuid = "";
    public boolean enabled;
  }
}
