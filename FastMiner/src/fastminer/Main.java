package fastminer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Random;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.v1_12_R1.CraftWorld;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import net.minecraft.server.v1_12_R1.Block;
import net.minecraft.server.v1_12_R1.BlockLightStone;
import net.minecraft.server.v1_12_R1.BlockLog1;
import net.minecraft.server.v1_12_R1.BlockLog2;
import net.minecraft.server.v1_12_R1.BlockOre;
import net.minecraft.server.v1_12_R1.BlockPosition;
import net.minecraft.server.v1_12_R1.BlockRedstoneOre;

public class Main extends JavaPlugin implements Listener{
	public JsonUtil config=null;
	public String ConfigFileLocation=".\\plugins\\FastMiner\\config.json";
	public ArrayList<String> ChildCommandList=new ArrayList<String>();
	public ArrayList<BlockBreakEvent> IgnoreList=new ArrayList<BlockBreakEvent>();
	public void onLoad()
	{
		try {
			ChildCommandList.add("help");
			ChildCommandList.add("toggle");
			ChildCommandList.add("enable");
			ChildCommandList.add("disable");
			ChildCommandList.add("reload");
		}catch(Throwable e) {e.printStackTrace();}
	}
	public void onEnable()
	{
		try {
		new File(new File(ConfigFileLocation).getParent()).mkdirs();
	    new File(ConfigFileLocation).createNewFile();
	    config=parseJson(new String(readFile(new File(ConfigFileLocation)),"GBK"));
	    if(config==null)
	    {
	    	config=new JsonUtil();
	    	SaveConfig();
	    }
		Bukkit.getPluginManager().registerEvents(this, this);
		}catch(Throwable e) {e.printStackTrace();}
	}
	public void onDisable()
	{
		try {
			SaveConfig();
		}catch(Throwable e) {e.printStackTrace();}
	}
	public boolean getEnable(String UUID)
	{
		boolean result=false;
		for(int i=0;i<config.Enable.size();i++)
		{
			if(config.Enable.get(i).UUID.equals(UUID))
			{
				result=config.Enable.get(i).enabled;
				break;
			}
		}
		return result;
	}
	public int getUUIDLocation(String UUID)
	{
		int result=-1;
		for(int i=0;i<config.Enable.size();i++)
		{
			if(config.Enable.get(i).UUID.equals(UUID))
			{
				result=i;
				break;
			}
		}
		return result;
	}
	public void setEnable(String UUID,boolean enable,boolean save) throws Throwable
	{
		int location=getUUIDLocation(UUID);
		JsonUtil.EnableState temp=new JsonUtil().new EnableState();
		temp.UUID=UUID;
		temp.enabled=enable;
		if(location==-1)
		{
			config.Enable.add(temp);
		}else {
			config.Enable.remove(location);
			config.Enable.add(location, temp);
		}
		if(save)
		 SaveConfig();
	}
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args)
	{
		try {
		if(command.getName().equalsIgnoreCase("fm"))
		{
			if(args.length>0)
  		    {
  			  if(args[0].equals("reload"))
  			  {
  				  if(sender.hasPermission("fastminer.reload"))
  				  {
  					ReloadConfig();  
  					sender.sendMessage("FastMiner Reload Complete!");
  				  }else {
  					  sender.sendMessage("No Enough Permission");
  				  }
  			  }else if(args[0].equals("help")){
  				  if(sender.hasPermission("fastminer.help"))
  				  {
  					sender.sendMessage("ChildCommandList:");
    				for(int i=0;i<ChildCommandList.size();i++)
    			      sender.sendMessage(ChildCommandList.get(i));  
  				  }else {
  					  sender.sendMessage("No Enough Permission");
  				  }
  			  }else if(args[0].equals("toggle")){
  				  if(sender.hasPermission("fastminer.toggle"))
  				  {
  					  if(sender instanceof Player)
  					  {
  						  boolean temp=getEnable(((Player)sender).getUniqueId().toString());
  						  setEnable(((Player)sender).getUniqueId().toString(),!temp,true);
  						  if(temp)
  							  sender.sendMessage("已关闭连锁挖矿!");
  						  else
  							  sender.sendMessage("已开启连锁挖矿!");
  					  }else {
  						  sender.sendMessage("You are not a Player!");
  					  }
  				  }else {
  					  sender.sendMessage("No Enough Permission");
  				  }
  			  }else if(args[0].equals("enable"))
  			  {
  				if(sender.hasPermission("fastminer.enable"))
  				{
  					if(sender instanceof Player)
  					{
  						setEnable(((Player)sender).getUniqueId().toString(),true,true);
  						sender.sendMessage("已开启连锁挖矿!");
  					}else {
  						sender.sendMessage("You are not a Player!");
  					}
  				}else {
  					sender.sendMessage("No Enough Permission");
  				}
  			  }else if(args[0].equals("disable"))
  			  {
  				if(sender.hasPermission("fastminer.disable"))
  				{
  					if(sender instanceof Player)
  					{
  						setEnable(((Player)sender).getUniqueId().toString(),false,true);
  						sender.sendMessage("已关闭连锁挖矿!");
  					}else {
  						sender.sendMessage("You are not a Player!");
  					}
  				}else {
  					sender.sendMessage("No Enough Permission");
  				}
  			  }else{
  				sender.sendMessage("Unknown ChildCommand!");
  				if(sender.hasPermission("fastminer.help"))
  				{
  				  sender.sendMessage("ChildCommandList:");
  				  for(int i=0;i<ChildCommandList.size();i++)
  				    sender.sendMessage(ChildCommandList.get(i));	
  				}
  			  }
  			  return true;
  		  }
		}
		return false;
		}catch(Throwable e) {e.printStackTrace();return true;}
	}
	@EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true)
	public void onPlayerJoin(PlayerJoinEvent e)
	{
		if(config.NotifyOnPlayerJoin)
		  e.getPlayer().sendMessage("连锁挖矿已"+(getEnable(e.getPlayer().getUniqueId().toString())?"开启":"关闭")+"! 请输入/fm toggle来切换开启状态!");
	}
	@SuppressWarnings("deprecation")
	@EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=false)
	public void onBlockDestroy(BlockBreakEvent e)
	{
		if(!e.isCancelled())
		{
			if(IgnoreList.contains(e))
			{
				return;
			}
			if(e.getPlayer().hasPermission("fastminer.use"))
			{
				if(getEnable(e.getPlayer().getUniqueId().toString()))
				{
					if(!(e.getPlayer().getGameMode()==GameMode.CREATIVE))
					{
						if(isRightTools(e.getBlock().getType(),e.getBlock().getData(),e.getPlayer().getItemInHand().getType()))
						{
								e.setCancelled(true);
								Execute(1,e.getBlock().getLocation(),e.getBlock().getType(),e.getBlock().getData(),e.getPlayer().getItemInHand(),e.getPlayer());
						}	
					}	
				}	
			}
		}
	}
	@SuppressWarnings("deprecation")
	public boolean isRightTools(Material block,int data,Material tools)
	{
		boolean result=false;
		/*if(block==Material.DIAMOND_ORE || block==Material.EMERALD_ORE || block==Material.REDSTONE_ORE || block==Material.GLOWING_REDSTONE_ORE || block==Material.GOLD_ORE)
			if(tools==Material.IRON_PICKAXE || tools==Material.DIAMOND_PICKAXE)
				result=true;
		if(block==Material.COAL_ORE || block==Material.QUARTZ_ORE)
			if(tools==Material.WOOD_PICKAXE || tools==Material.STONE_PICKAXE || tools==Material.IRON_PICKAXE || tools==Material.DIAMOND_PICKAXE || tools==Material.GOLD_PICKAXE)
				result=true;
		if(block==Material.GLOWING_REDSTONE_ORE || block==Material.LOG || block==Material.LOG_2)
			result=true;
		if(block==Material.IRON_ORE || block==Material.LAPIS_ORE)
			if(tools==Material.STONE_PICKAXE || tools==Material.IRON_PICKAXE|| tools==Material.DIAMOND_PICKAXE)
				result=true;*/
		if(Block.getById(block.getId()).fromLegacyData(data).getBlock() instanceof BlockOre || Block.getById(block.getId()).fromLegacyData(data).getBlock() instanceof BlockRedstoneOre || Block.getById(block.getId()).fromLegacyData(data).getBlock() instanceof BlockLog1 || Block.getById(block.getId()).fromLegacyData(data).getBlock() instanceof BlockLog2 || Block.getById(block.getId()).fromLegacyData(data).getBlock() instanceof BlockLightStone)
			if(Block.getById(block.getId()).fromLegacyData(data).getBlock().getBlockData().getMaterial().isAlwaysDestroyable() || net.minecraft.server.v1_12_R1.Item.getById(tools.getId()).canDestroySpecialBlock(Block.getById(block.getId()).fromLegacyData(data)))
				result=true;
		return result;
	}
	public void subtractDurability(ItemStack tools,Player player)
	{
		if(tools.getItemMeta().isUnbreakable())
			return;
		long level=0;
		for(Enchantment i : tools.getEnchantments().keySet())
		{
			if(i.getName().equals(Enchantment.DURABILITY.getName()))
			{
				level=tools.getEnchantmentLevel(i);				
				break;
			}
		}
		int temp=new Random(System.nanoTime()).nextInt(100)+1;
		if(temp<=(100/(level+1)))
		{
			int damage=1;
			PlayerItemDamageEvent tempevent=new PlayerItemDamageEvent(player,tools,damage);
			Bukkit.getPluginManager().callEvent(tempevent);
			if(tempevent.isCancelled())
				return;
			damage=tempevent.getDamage();
			tools.setDurability((short)(tools.getDurability()+damage));	
		}
	}
	public boolean isOriginal(ItemStack tools)
	{
		for(Enchantment i : tools.getEnchantments().keySet())
		{
			if(i.getName().equals(Enchantment.SILK_TOUCH.getName()))
				return true;
		}
		return false;
	}
	/*public int getLuckyCount(Material type,int level)
	{
		Random temp=new Random(System.nanoTime());
		int i = temp.nextInt(level + 2) - 1;

        if (i < 0)
        {
            i = 0;
        }

        return this.getCount(type,temp) * (i + 1);
	}*/
	/*public int getCount(Material type,Random temp)
	{
		return (type==Material.LAPIS_ORE)?4+temp.nextInt(5):1;
	}*/
	public int getLuckyLevel(ItemStack tools)
	{
		int result=0;
		for(Enchantment i : tools.getEnchantments().keySet())
		{
			if(i.getName().equals(Enchantment.LOOT_BONUS_BLOCKS.getName()))
			{
				result=tools.getEnchantmentLevel(i);
				break;
			}
		}
		return result;
	}
	public int getOriginalLevel(ItemStack tools)
	{
		int result=0;
		for(Enchantment i : tools.getEnchantments().keySet())
		{
			if(i.getName().equals(Enchantment.SILK_TOUCH.getName()))
			{
				result=tools.getEnchantmentLevel(i);
				break;
			}
		}
		return result;
	}
	/*public ItemStack getLuckyBlockItem(Material block,byte data,int count,int level)
	{
		ItemStack result=null;
		if(block==Material.COAL_ORE)
			result=new ItemStack(Material.COAL,count);
		if(block==Material.EMERALD_ORE)
			result=new ItemStack(Material.EMERALD,count);
		if(block==Material.DIAMOND_ORE)
			result=new ItemStack(Material.DIAMOND,count);
		if(block==Material.GLOWING_REDSTONE_ORE || block==Material.REDSTONE_ORE)
			result=new ItemStack(Material.REDSTONE,count);
		if(block==Material.GLOWSTONE)
			result=new ItemStack(Material.GLOWSTONE_DUST,count);
		if(block==Material.GOLD_ORE || block==Material.IRON_ORE)
			result=new ItemStack(block,count);
		if(block==Material.LAPIS_ORE)
			result=new ItemStack(Material.INK_SACK,getLuckyCount(block,level),(short)4);
		if(block==Material.QUARTZ_ORE)
			result=new ItemStack(Material.QUARTZ,count);
		if(block==Material.LOG || block==Material.LOG_2)
			result=new ItemStack(block,1,data);
		return result;
	}*/
	public boolean hasLava(Location block,Player player)
	{
		if(player.hasPermission("fastminer.lavadetect") && config.lavaDetect)
		{
			if(new Location(block.getWorld(),block.getX()+1,block.getY(),block.getZ()).getBlock().getType()==Material.LAVA || new Location(block.getWorld(),block.getX()+1,block.getY(),block.getZ()).getBlock().getType()==Material.STATIONARY_LAVA)
				return true;
			if(new Location(block.getWorld(),block.getX(),block.getY()+1,block.getZ()).getBlock().getType()==Material.LAVA || new Location(block.getWorld(),block.getX(),block.getY()+1,block.getZ()).getBlock().getType()==Material.STATIONARY_LAVA)
				return true;
			if(new Location(block.getWorld(),block.getX(),block.getY(),block.getZ()+1).getBlock().getType()==Material.LAVA || new Location(block.getWorld(),block.getX(),block.getY(),block.getZ()+1).getBlock().getType()==Material.STATIONARY_LAVA)
				return true;
			if(new Location(block.getWorld(),block.getX()-1,block.getY(),block.getZ()).getBlock().getType()==Material.LAVA || new Location(block.getWorld(),block.getX()-1,block.getY(),block.getZ()).getBlock().getType()==Material.STATIONARY_LAVA)
				return true;
			if(new Location(block.getWorld(),block.getX(),block.getY()-1,block.getZ()).getBlock().getType()==Material.LAVA || new Location(block.getWorld(),block.getX(),block.getY()-1,block.getZ()).getBlock().getType()==Material.STATIONARY_LAVA)
				return true;
			if(new Location(block.getWorld(),block.getX(),block.getY(),block.getZ()-1).getBlock().getType()==Material.LAVA || new Location(block.getWorld(),block.getX(),block.getY(),block.getZ()-1).getBlock().getType()==Material.STATIONARY_LAVA)
				return true;	
		}
		return false;
	}
	@SuppressWarnings("deprecation")
	public void Execute(long depth,Location block,Material type,byte data,ItemStack tools,Player player)
	{
		if(depth>=config.MaxDepth)
			return;
		Material originalType=type;
		if(originalType==Material.GLOWING_REDSTONE_ORE)
			originalType=Material.REDSTONE_ORE;
		Material nowType=block.getBlock().getType();
		if(nowType==Material.GLOWING_REDSTONE_ORE)
			nowType=Material.REDSTONE_ORE;
		if(nowType==originalType && block.getBlock().getData()==data)
		{
			if(!player.isOnline())
				return;
			boolean canbreak=true;
			boolean isdrop=Boolean.valueOf(block.getWorld().getGameRuleValue("doTileDrops"));
			int exptodrop=Boolean.valueOf(block.getWorld().getGameRuleValue("doTileDrops"))?(!isOriginal(tools)?Block.getById(type.getId()).getExpDrop(((CraftWorld)block.getWorld()).getHandle(), Block.getById(type.getId()).fromLegacyData(data),getOriginalLevel(tools)):0):0;
			if(hasLava(block,player))
			{
				if(config.lavaNotify)
				  player.sendMessage("方块 x="+block.getBlockX()+" y="+block.getBlockY()+" z="+block.getBlockZ()+" 周围有岩浆，已取消破坏事件!");
				canbreak=false;
			}else {
				BlockBreakEvent tempevent=new BlockBreakEvent(block.getBlock(),player);
				tempevent.setDropItems(isdrop);
				tempevent.setExpToDrop(exptodrop);
			    IgnoreList.add(tempevent);
				Bukkit.getPluginManager().callEvent(tempevent);
				IgnoreList.remove(tempevent);
				exptodrop=tempevent.getExpToDrop();
				isdrop=tempevent.isDropItems();
				canbreak=!tempevent.isCancelled();
			}
			Material before=tools.getType();
			if(canbreak)
			{
				if(!isOriginal(tools))
				{
					/*if(type==Material.COAL_ORE)
						exptodrop=new Random(System.nanoTime()).nextInt(2-0)+0;
					if(type==Material.DIAMOND_ORE || type==Material.EMERALD_ORE)
						exptodrop=new Random(System.nanoTime()).nextInt(7-3)+3;
					if(type==Material.LAPIS_ORE || type==Material.QUARTZ_ORE)
						exptodrop=new Random(System.nanoTime()).nextInt(5-2)+2;*/
					/*int level=getLuckyLevel(tools);
					if(level==0)
					{
						if(isdrop)
						  block.getBlock().breakNaturally(tools);	
						else
						  block.getBlock().setType(Material.AIR);
					}else {*/
						//Material temp=block.getBlock().getType();
						if(isdrop)
						{
							/*int count=new Random(System.nanoTime()).nextInt((1+level)-1)+1;
							((Item)(block.getWorld().spawnEntity(block, EntityType.DROPPED_ITEM))).setItemStack(getLuckyBlockItem(temp,data,count,level));	*/
							boolean origr=Boolean.valueOf(block.getWorld().getGameRuleValue("doTileDrops"));
							block.getWorld().setGameRuleValue("doTileDrops", "true");
							Block.getById(type.getId()).dropNaturally(((CraftWorld)block.getWorld()).getHandle(), new BlockPosition(block.getX(),block.getY(),block.getZ()), Block.getById(type.getId()).fromLegacyData(data), 1, getLuckyLevel(tools));
							block.getWorld().setGameRuleValue("doTileDrops", String.valueOf(origr));
						}
						block.getBlock().setType(Material.AIR);
					//}
					if(exptodrop!=0)
					{
						try {
							((ExperienceOrb)(block.getWorld().spawnEntity(block, EntityType.EXPERIENCE_ORB))).setExperience(exptodrop);
						}catch(Throwable e) {}
					}
					}else {
						//Material temp=block.getBlock().getType();
						if(isdrop)
						  ((Item)(block.getWorld().spawnEntity(block, EntityType.DROPPED_ITEM))).setItemStack(new ItemStack(block.getBlock().getType(),1,data));
						block.getBlock().setType(Material.AIR);
						if(exptodrop!=0)
						{
							try {
								((ExperienceOrb)(block.getWorld().spawnEntity(block, EntityType.EXPERIENCE_ORB))).setExperience(exptodrop);
							}catch(Throwable e) {}
						}
					}
					if(tools.getType().getMaxDurability()>=1)
					  subtractDurability(tools,player);
					if(tools.getDurability()>tools.getType().getMaxDurability())
						tools.subtract();	
			        }
				if(tools.getType()!=Material.AIR || before==Material.AIR)
					Execute(depth+1,new Location(block.getWorld(),block.getX()+1,block.getY(),block.getZ()),type,data,tools,player);
				if(tools.getType()!=Material.AIR || before==Material.AIR)	
				    Execute(depth+1,new Location(block.getWorld(),block.getX()-1,block.getY(),block.getZ()),type,data,tools,player);
				if(tools.getType()!=Material.AIR || before==Material.AIR)	
				    Execute(depth+1,new Location(block.getWorld(),block.getX(),block.getY()+1,block.getZ()),type,data,tools,player);
				if(tools.getType()!=Material.AIR || before==Material.AIR)	
				    Execute(depth+1,new Location(block.getWorld(),block.getX(),block.getY()-1,block.getZ()),type,data,tools,player);
				if(tools.getType()!=Material.AIR || before==Material.AIR)	
				    Execute(depth+1,new Location(block.getWorld(),block.getX(),block.getY(),block.getZ()+1),type,data,tools,player);
				if(tools.getType()!=Material.AIR || before==Material.AIR)	
				    Execute(depth+1,new Location(block.getWorld(),block.getX(),block.getY(),block.getZ()-1),type,data,tools,player);	
		}
	}
	public static byte[] readFile(File file) throws Throwable
    {
  	 FileInputStream input=new FileInputStream(file);
  	 byte[] t_ret=new byte[input.available()];
  	 input.read(t_ret, 0, input.available());
  	 input.close();
  	 return t_ret;
    }
    public static boolean writeFile(File file,byte[] content) throws Throwable
    {
  	  FileOutputStream output=new FileOutputStream(file);
  	  output.write(content, 0, content.length);
  	  output.flush();
  	  output.close();
  	  return true;
    }
    public static JsonUtil parseJson(String json) throws Throwable
    {
  	  Gson parse=new GsonBuilder().setLenient().setPrettyPrinting().enableComplexMapKeySerialization().create();
  	  return parse.fromJson(json, JsonUtil.class);
    }
    public static String toJsonString(JsonUtil json) throws Throwable
    {
  	  Gson parse=new GsonBuilder().setLenient().setPrettyPrinting().enableComplexMapKeySerialization().create();
  	  return parse.toJson(json);
    }
    public boolean SaveConfig() throws Throwable
    {
  	  return writeFile(new File(ConfigFileLocation), toJsonString(config).getBytes());
    }
    public void ReloadConfig() throws Throwable
    {
  	  this.config=parseJson(new String(readFile(new File(ConfigFileLocation)),"GBK"));
    }
}
