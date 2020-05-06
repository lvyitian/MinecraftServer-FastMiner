
package fastminer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Objects;
import java.util.Random;
import java.util.UUID;
import java.util.Vector;

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

import com.gmail.nossr50.datatypes.skills.PrimarySkillType;
import com.gmail.nossr50.datatypes.skills.SuperAbilityType;
import com.gmail.nossr50.events.fake.FakeBlockBreakEvent;
import com.gmail.nossr50.events.skills.abilities.McMMOPlayerAbilityActivateEvent;
import com.gmail.nossr50.events.skills.abilities.McMMOPlayerAbilityDeactivateEvent;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import fastminer.JsonUtil.BlockType;
import net.minecraft.server.v1_12_R1.Block;
import net.minecraft.server.v1_12_R1.BlockLightStone;
import net.minecraft.server.v1_12_R1.BlockLog1;
import net.minecraft.server.v1_12_R1.BlockLog2;
import net.minecraft.server.v1_12_R1.BlockOre;
import net.minecraft.server.v1_12_R1.BlockPosition;
import net.minecraft.server.v1_12_R1.BlockRedstoneOre;

public class Main extends JavaPlugin implements Listener
{
  public JsonUtil config;
  public String configFileLocation = ".\\plugins\\FastMiner\\config.json";
  public Vector<String> childCommandList = new Vector<>();
  public Vector<BlockBreakEvent> ignoreList = new Vector<>();
  public Vector<FakeBlockBreakEvent> ignoreList2 = new Vector<>();
  public Vector<UUID> ignorePlayers = new Vector<>();
  public final Object lock = new Object();

  @Override
  public void onLoad()
  {
    try {
      this.childCommandList.add("help");
      this.childCommandList.add("toggle");
      this.childCommandList.add("enable");
      this.childCommandList.add("disable");
      this.childCommandList.add("reload");
    } catch (final Throwable e) {
      e.printStackTrace();
    }
  }

  @Override
  public void onEnable()
  {
    try {
      new File(new File(this.configFileLocation).getParent()).mkdirs();
      new File(this.configFileLocation).createNewFile();
      this.config = Main.parseJson(new String(Main.readFile(new File(this.configFileLocation)), "GBK"));
      if (this.config == null) {
        this.config = new JsonUtil();
        this.saveMyConfig();
      }
      if (Bukkit.getPluginManager().getPlugin("mcMMO") != null) {
        Bukkit.getPluginManager().registerEvent(McMMOPlayerAbilityActivateEvent.class, new Listener()
        {
        }, EventPriority.MONITOR, (e, l) ->
        {
          final McMMOPlayerAbilityActivateEvent e2 = (McMMOPlayerAbilityActivateEvent) e;
          if ((e2.getAbility() == SuperAbilityType.BLAST_MINING) || (e2.getSkill() == PrimarySkillType.MINING)) {
            synchronized (this.lock) {
              this.ignorePlayers.add(e2.getPlayer().getUniqueId());
            }
          }
        }, this);
        Bukkit.getPluginManager().registerEvent(McMMOPlayerAbilityDeactivateEvent.class, new Listener()
        {
        }, EventPriority.HIGHEST, (e, l) ->
        {
          final McMMOPlayerAbilityDeactivateEvent e2 = (McMMOPlayerAbilityDeactivateEvent) e;
          if ((e2.getAbility() == SuperAbilityType.BLAST_MINING) || (e2.getSkill() == PrimarySkillType.MINING)) {
            synchronized (this.lock) {
              this.ignorePlayers.remove(e2.getPlayer().getUniqueId());
            }
          }
        }, this);
        Bukkit.getPluginManager().registerEvent(FakeBlockBreakEvent.class, new Listener()
        {
        }, EventPriority.HIGHEST, (e, l) ->
        {
          if (e instanceof FakeBlockBreakEvent) {
            final FakeBlockBreakEvent e2 = (FakeBlockBreakEvent) e;
            if (this.isEnable(e2.getPlayer().getUniqueId().toString())) {
              this.ignoreList2.add(e2);
              e2.setCancelled(true);
            }
          }
        }, this);
      }
      Bukkit.getPluginManager().registerEvents(this, this);
    } catch (final Throwable e) {
      e.printStackTrace();
    }
  }

  @Override
  public void onDisable()
  {
    try {
      this.saveMyConfig();
    } catch (final Throwable e) {
      e.printStackTrace();
    }
  }

  public boolean isEnable(final String UUID)
  {
    boolean result = false;
    for (int i = 0; i < this.config.enable.size(); i++) {
      if (this.config.enable.get(i).uuid.equals(UUID)) {
        result = this.config.enable.get(i).enabled;
        break;
      }
    }
    return result;
  }

  public int getUUIDLocation(final String UUID)
  {
    int result = -1;
    for (int i = 0; i < this.config.enable.size(); i++) {
      if (this.config.enable.get(i).uuid.equals(UUID)) {
        result = i;
        break;
      }
    }
    return result;
  }

  public void setEnable(final String UUID, final boolean enable, final boolean save) throws Throwable
  {
    final int location = this.getUUIDLocation(UUID);
    final JsonUtil.EnableState temp = new JsonUtil.EnableState();
    temp.uuid = UUID;
    temp.enabled = enable;
    if (location == -1) {
      this.config.enable.add(temp);
    } else {
      this.config.enable.remove(location);
      this.config.enable.add(location, temp);
    }
    if (save && this.config.saveState) {
      this.saveMyConfig();
    }
  }

  @Override
  public boolean onCommand(final CommandSender sender, final Command command, final String label, final String[] args)
  {
    try {
      if ("fm".equalsIgnoreCase(command.getName())) {
        if (args.length > 0) {
          if ("reload".equalsIgnoreCase(args[0])) {
            if (sender.hasPermission("fastminer.reload")) {
              this.reloadMyConfig();
              sender.sendMessage("FastMiner Reload Complete!");
            } else {
              sender.sendMessage("No Enough Permission");
            }
          } else if ("help".equalsIgnoreCase(args[0])) {
            if (sender.hasPermission("fastminer.help")) {
              sender.sendMessage("childCommandList:");
              for (int i = 0; i < this.childCommandList.size(); i++) {
                sender.sendMessage(this.childCommandList.get(i));
              }
            } else {
              sender.sendMessage("No Enough Permission");
            }
          } else if ("toggle".equalsIgnoreCase(args[0])) {
            if (sender.hasPermission("fastminer.toggle")) {
              if (sender instanceof Player) {
                final boolean temp = this.isEnable(((Player) sender).getUniqueId().toString());
                this.setEnable(((Player) sender).getUniqueId().toString(), !temp, true);
                if (temp) {
                  sender.sendMessage("已关闭连锁挖矿!");
                } else {
                  sender.sendMessage("已开启连锁挖矿!");
                }
              } else {
                sender.sendMessage("You are not a Player!");
              }
            } else {
              sender.sendMessage("No Enough Permission");
            }
          } else if ("enable".equalsIgnoreCase(args[0])) {
            if (sender.hasPermission("fastminer.enable")) {
              if (sender instanceof Player) {
                this.setEnable(((Player) sender).getUniqueId().toString(), true, true);
                sender.sendMessage("已开启连锁挖矿!");
              } else {
                sender.sendMessage("You are not a Player!");
              }
            } else {
              sender.sendMessage("No Enough Permission");
            }
          } else if ("disable".equalsIgnoreCase(args[0])) {
            if (sender.hasPermission("fastminer.disable")) {
              if (sender instanceof Player) {
                this.setEnable(((Player) sender).getUniqueId().toString(), false, true);
                sender.sendMessage("已关闭连锁挖矿!");
              } else {
                sender.sendMessage("You are not a Player!");
              }
            } else {
              sender.sendMessage("No Enough Permission");
            }
          } else {
            sender.sendMessage("Unknown ChildCommand!");
            if (sender.hasPermission("fastminer.help")) {
              sender.sendMessage("childCommandList:");
              for (int i = 0; i < this.childCommandList.size(); i++) {
                sender.sendMessage(this.childCommandList.get(i));
              }
            }
          }
          return true;
        }
      }
      return false;
    } catch (final Throwable e) {
      e.printStackTrace();
      return true;
    }
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerJoin(final PlayerJoinEvent e)
  {
    if (this.config.notifyOnPlayerJoin) {
      e.getPlayer().sendMessage(
          "连锁挖矿已" + (this.isEnable(e.getPlayer().getUniqueId().toString()) ? "开启" : "关闭") + "! 请输入/fm toggle来切换开启状态!");
    }
  }

  @SuppressWarnings("deprecation")
  @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
  public void onBlockDestroy(final BlockBreakEvent e)
  {
    try {
      if (this.ignoreList2.contains(e)) {
        return;
      }
    } finally {
      this.ignoreList2.clear();
    }
    if (!e.isCancelled()) {
      if (this.ignoreList.contains(e)) {
        return;
      }
      if (e.getPlayer().hasPermission("fastminer.use")) {
        if (this.isEnable(e.getPlayer().getUniqueId().toString())
            && !Objects.equals(e.getPlayer().getGameMode(), GameMode.CREATIVE) && this.isRightTools(
                e.getBlock().getType(), e.getBlock().getData(), e.getPlayer().getItemInHand().getType())) {
          boolean ignore = false;
          synchronized (this.lock) {
            ignore = this.ignorePlayers.contains(e.getPlayer().getUniqueId());
          }
          if (ignore) {
            return;
          }
          e.setCancelled(true);
          this.execute(1, e.getBlock().getLocation(), e.getBlock().getType(), e.getBlock().getData(),
              e.getPlayer().getItemInHand(), e.getPlayer());

        }
      }
    }
  }

  @SuppressWarnings("deprecation")
  public boolean isCustomBlock(final Material block, final int data)
  {
    for (final BlockType i : this.config.extraBlockType) {
      if (Block.getById(block.getId()).fromLegacyData(i.damage >= 0 ? data : 0).getBlock().getClass().equals(Block
          .getByName(i.namespace + ":" + i.name).fromLegacyData(i.damage >= 0 ? i.damage : 0).getBlock().getClass())) {
        if ((i.damage < 0) || (data == i.damage)) {
          return true;
        }
      }
    }
    return false;
  }

  @SuppressWarnings("deprecation")
  public boolean isRightTools(final Material block, final int data, final Material tools)
  {
    boolean result = false;
    /*
     * if(block==Material.DIAMOND_ORE || block==Material.EMERALD_ORE ||
     * block==Material.REDSTONE_ORE || block==Material.GLOWING_REDSTONE_ORE ||
     * block==Material.GOLD_ORE) if(tools==Material.IRON_PICKAXE ||
     * tools==Material.DIAMOND_PICKAXE) result=true; if(block==Material.COAL_ORE ||
     * block==Material.QUARTZ_ORE) if(tools==Material.WOOD_PICKAXE ||
     * tools==Material.STONE_PICKAXE || tools==Material.IRON_PICKAXE ||
     * tools==Material.DIAMOND_PICKAXE || tools==Material.GOLD_PICKAXE) result=true;
     * if(block==Material.GLOWING_REDSTONE_ORE || block==Material.LOG ||
     * block==Material.LOG_2) result=true; if(block==Material.IRON_ORE ||
     * block==Material.LAPIS_ORE) if(tools==Material.STONE_PICKAXE ||
     * tools==Material.IRON_PICKAXE|| tools==Material.DIAMOND_PICKAXE) result=true;
     */
    if ((Block.getById(block.getId()).fromLegacyData(data).getBlock() instanceof BlockOre)
        || (Block.getById(block.getId()).fromLegacyData(data).getBlock() instanceof BlockRedstoneOre)
        || (Block.getById(block.getId()).fromLegacyData(data).getBlock() instanceof BlockLog1)
        || (Block.getById(block.getId()).fromLegacyData(data).getBlock() instanceof BlockLog2)
        || (Block.getById(block.getId()).fromLegacyData(data).getBlock() instanceof BlockLightStone)
        || this.isCustomBlock(block, data)) {
      if (Block.getById(block.getId()).fromLegacyData(data).getBlock().getBlockData().getMaterial()
          .isAlwaysDestroyable()
          || net.minecraft.server.v1_12_R1.Item.getById(tools.getId())
              .canDestroySpecialBlock(Block.getById(block.getId()).fromLegacyData(data))) {
        result = true;
      }
    }
    return result;
  }

  public static void subtractDurability(final ItemStack tools, final Player player)
  {
    if (tools.getItemMeta().isUnbreakable()) {
      return;
    }
    long level = 0;
    for (final Enchantment i : tools.getEnchantments().keySet()) {
      if (i.getName().equals(Enchantment.DURABILITY.getName())) {
        level = tools.getEnchantmentLevel(i);
        break;
      }
    }
    final int temp = new Random(System.nanoTime()).nextInt(100) + 1;
    if (temp <= (100 / (level + 1))) {
      int damage = 1;
      final PlayerItemDamageEvent tempevent = new PlayerItemDamageEvent(player, tools, damage);
      Bukkit.getPluginManager().callEvent(tempevent);
      if (tempevent.isCancelled()) {
        return;
      }
      damage = tempevent.getDamage();
      tools.setDurability((short) (tools.getDurability() + damage));
    }
  }

  public static boolean isOriginal(final ItemStack tools)
  {
    for (final Enchantment i : tools.getEnchantments().keySet()) {
      if (i.getName().equals(Enchantment.SILK_TOUCH.getName())) {
        return true;
      }
    }
    return false;
  }

  /*
   * public static int getLuckyCount(Material type,int level) { Random temp=new
   * Random(System.nanoTime()); int i = temp.nextInt(level + 2) - 1;
   *
   * if (i < 0) { i = 0; }
   *
   * return this.getCount(type,temp) * (i + 1); }
   */
  /*
   * public static int getCount(Material type,Random temp) { return
   * (type==Material.LAPIS_ORE)?4+temp.nextInt(5):1; }
   */
  public static int getLuckyLevel(final ItemStack tools)
  {
    int result = 0;
    for (final Enchantment i : tools.getEnchantments().keySet()) {
      if (i.getName().equals(Enchantment.LOOT_BONUS_BLOCKS.getName())) {
        result = tools.getEnchantmentLevel(i);
        break;
      }
    }
    return result;
  }

  public static int getOriginalLevel(final ItemStack tools)
  {
    int result = 0;
    for (final Enchantment i : tools.getEnchantments().keySet()) {
      if (i.getName().equals(Enchantment.SILK_TOUCH.getName())) {
        result = tools.getEnchantmentLevel(i);
        break;
      }
    }
    return result;
  }

  /*
   * public static ItemStack getLuckyBlockItem(Material block,byte data,int
   * count,int level) { ItemStack result=null; if(block==Material.COAL_ORE)
   * result=new ItemStack(Material.COAL,count); if(block==Material.EMERALD_ORE)
   * result=new ItemStack(Material.EMERALD,count); if(block==Material.DIAMOND_ORE)
   * result=new ItemStack(Material.DIAMOND,count);
   * if(block==Material.GLOWING_REDSTONE_ORE || block==Material.REDSTONE_ORE)
   * result=new ItemStack(Material.REDSTONE,count); if(block==Material.GLOWSTONE)
   * result=new ItemStack(Material.GLOWSTONE_DUST,count);
   * if(block==Material.GOLD_ORE || block==Material.IRON_ORE) result=new
   * ItemStack(block,count); if(block==Material.LAPIS_ORE) result=new
   * ItemStack(Material.INK_SACK,getLuckyCount(block,level),(short)4);
   * if(block==Material.QUARTZ_ORE) result=new ItemStack(Material.QUARTZ,count);
   * if(block==Material.LOG || block==Material.LOG_2) result=new
   * ItemStack(block,1,data); return result; }
   */
  public boolean hasLava(final Location block, final Player player)
  {
    if (player.hasPermission("fastminer.lavadetect") && this.config.lavaDetect) {
      if ((new Location(block.getWorld(), block.getX() + 1, block.getY(), block.getZ()).getBlock()
          .getType() == Material.LAVA)
          || (new Location(block.getWorld(), block.getX() + 1, block.getY(), block.getZ()).getBlock()
              .getType() == Material.STATIONARY_LAVA)) {
        return true;
      }
      if ((new Location(block.getWorld(), block.getX(), block.getY() + 1, block.getZ()).getBlock()
          .getType() == Material.LAVA)
          || (new Location(block.getWorld(), block.getX(), block.getY() + 1, block.getZ()).getBlock()
              .getType() == Material.STATIONARY_LAVA)) {
        return true;
      }
      if ((new Location(block.getWorld(), block.getX(), block.getY(), block.getZ() + 1).getBlock()
          .getType() == Material.LAVA)
          || (new Location(block.getWorld(), block.getX(), block.getY(), block.getZ() + 1).getBlock()
              .getType() == Material.STATIONARY_LAVA)) {
        return true;
      }
      if ((new Location(block.getWorld(), block.getX() - 1, block.getY(), block.getZ()).getBlock()
          .getType() == Material.LAVA)
          || (new Location(block.getWorld(), block.getX() - 1, block.getY(), block.getZ()).getBlock()
              .getType() == Material.STATIONARY_LAVA)) {
        return true;
      }
      if ((new Location(block.getWorld(), block.getX(), block.getY() - 1, block.getZ()).getBlock()
          .getType() == Material.LAVA)
          || (new Location(block.getWorld(), block.getX(), block.getY() - 1, block.getZ()).getBlock()
              .getType() == Material.STATIONARY_LAVA)) {
        return true;
      }
      if ((new Location(block.getWorld(), block.getX(), block.getY(), block.getZ() - 1).getBlock()
          .getType() == Material.LAVA)
          || (new Location(block.getWorld(), block.getX(), block.getY(), block.getZ() - 1).getBlock()
              .getType() == Material.STATIONARY_LAVA)) {
        return true;
      }
    }
    return false;
  }

  @SuppressWarnings("deprecation")
  public void execute(final long depth, final Location block, final Material type, final byte data,
      final ItemStack tools, final Player player)
  {
    if (depth >= this.config.maxDepth) {
      return;
    }
    Material originalType = type;
    if (originalType == Material.GLOWING_REDSTONE_ORE) {
      originalType = Material.REDSTONE_ORE;
    }
    Material nowType = block.getBlock().getType();
    if (nowType == Material.GLOWING_REDSTONE_ORE) {
      nowType = Material.REDSTONE_ORE;
    }
    if ((nowType == originalType) && (block.getBlock().getData() == data)) {
      if (!player.isOnline()) {
        return;
      }
      boolean canbreak = true;
      boolean isdrop = Boolean.parseBoolean(block.getWorld().getGameRuleValue("doTileDrops"));
      int exptodrop = isdrop ? (!Main.isOriginal(tools)
          ? Block.getById(type.getId()).getExpDrop(((CraftWorld) block.getWorld()).getHandle(),
              Block.getById(type.getId()).fromLegacyData(data), Main.getOriginalLevel(tools))
          : 0) : 0;
      if (this.hasLava(block, player)) {
        if (this.config.lavaNotify) {
          player.sendMessage(
              "方块 x=" + block.getBlockX() + " y=" + block.getBlockY() + " z=" + block.getBlockZ() + " 周围有岩浆，已取消破坏事件!");
        }
        canbreak = false;
      } else {
        final BlockBreakEvent tempevent = new BlockBreakEvent(block.getBlock(), player);
        tempevent.setDropItems(isdrop);
        tempevent.setExpToDrop(exptodrop);
        this.ignoreList.add(tempevent);
        Bukkit.getPluginManager().callEvent(tempevent);
        this.ignoreList.remove(tempevent);
        exptodrop = tempevent.getExpToDrop();
        isdrop = tempevent.isDropItems();
        canbreak = !tempevent.isCancelled();
      }
      final Material before = tools.getType();
      if (canbreak) {
        if (!Main.isOriginal(tools)) {
          /*
           * if(type==Material.COAL_ORE) exptodrop=new
           * Random(System.nanoTime()).nextInt(2-0)+0; if(type==Material.DIAMOND_ORE ||
           * type==Material.EMERALD_ORE) exptodrop=new
           * Random(System.nanoTime()).nextInt(7-3)+3; if(type==Material.LAPIS_ORE ||
           * type==Material.QUARTZ_ORE) exptodrop=new
           * Random(System.nanoTime()).nextInt(5-2)+2;
           */
          /*
           * int level=getLuckyLevel(tools); if(level==0) { if(isdrop)
           * block.getBlock().breakNaturally(tools); else
           * block.getBlock().setType(Material.AIR); }else {
           */
          // Material temp=block.getBlock().getType();
          if (isdrop) {
            /*
             * int count=new Random(System.nanoTime()).nextInt((1+level)-1)+1;
             * ((Item)(block.getWorld().spawnEntity(block,
             * EntityType.DROPPED_ITEM))).setItemStack(getLuckyBlockItem(temp,data,count,
             * level));
             */
            final boolean origr = Boolean.parseBoolean(block.getWorld().getGameRuleValue("doTileDrops"));
            block.getWorld().setGameRuleValue("doTileDrops", "true");
            Block.getById(type.getId()).dropNaturally(((CraftWorld) block.getWorld()).getHandle(),
                new BlockPosition(block.getX(), block.getY(), block.getZ()),
                Block.getById(type.getId()).fromLegacyData(data), 1, Main.getLuckyLevel(tools));
            block.getWorld().setGameRuleValue("doTileDrops", String.valueOf(origr));
          }
          block.getBlock().setType(Material.AIR);
          // }
          if (exptodrop != 0) {
            try {
              ((ExperienceOrb) (block.getWorld().spawnEntity(block, EntityType.EXPERIENCE_ORB)))
                  .setExperience(exptodrop);
            } catch (final Throwable e) {
              // ignore
            }
          }
        } else {
          // Material temp=block.getBlock().getType();
          if (isdrop) {
            ((Item) (block.getWorld().spawnEntity(block, EntityType.DROPPED_ITEM)))
                .setItemStack(new ItemStack(block.getBlock().getType(), 1, data));
          }
          block.getBlock().setType(Material.AIR);
          if (exptodrop != 0) {
            try {
              ((ExperienceOrb) (block.getWorld().spawnEntity(block, EntityType.EXPERIENCE_ORB)))
                  .setExperience(exptodrop);
            } catch (final Throwable e) {
              // ignore
            }
          }
        }
        if (tools.getType().getMaxDurability() >= 1) {
          Main.subtractDurability(tools, player);
        }
        if (tools.getDurability() > tools.getType().getMaxDurability()) {
          tools.subtract();
        }
      }
      if ((tools.getType() != Material.AIR) || (before == Material.AIR)) {
        this.execute(depth + 1, new Location(block.getWorld(), block.getX() + 1, block.getY(), block.getZ()), type,
            data, tools, player);
      }
      if ((tools.getType() != Material.AIR) || (before == Material.AIR)) {
        this.execute(depth + 1, new Location(block.getWorld(), block.getX() - 1, block.getY(), block.getZ()), type,
            data, tools, player);
      }
      if ((tools.getType() != Material.AIR) || (before == Material.AIR)) {
        this.execute(depth + 1, new Location(block.getWorld(), block.getX(), block.getY() + 1, block.getZ()), type,
            data, tools, player);
      }
      if ((tools.getType() != Material.AIR) || (before == Material.AIR)) {
        this.execute(depth + 1, new Location(block.getWorld(), block.getX(), block.getY() - 1, block.getZ()), type,
            data, tools, player);
      }
      if ((tools.getType() != Material.AIR) || (before == Material.AIR)) {
        this.execute(depth + 1, new Location(block.getWorld(), block.getX(), block.getY(), block.getZ() + 1), type,
            data, tools, player);
      }
      if ((tools.getType() != Material.AIR) || (before == Material.AIR)) {
        this.execute(depth + 1, new Location(block.getWorld(), block.getX(), block.getY(), block.getZ() - 1), type,
            data, tools, player);
      }
    }
  }

  public static byte[] readFile(final File file) throws Throwable
  {
    try (final FileInputStream input = new FileInputStream(file)) {
      final byte[] ret = new byte[input.available()];
      input.read(ret, 0, input.available());
      return ret;
    }
  }

  public static boolean writeFile(final File file, final byte[] content) throws Throwable
  {
    try (final FileOutputStream output = new FileOutputStream(file)) {
      output.write(content, 0, content.length);
      output.flush();
      return true;
    }
  }

  public static JsonUtil parseJson(final String json) throws Throwable
  {
    final Gson parse = new GsonBuilder().setLenient().setPrettyPrinting().enableComplexMapKeySerialization().create();
    return parse.fromJson(json, JsonUtil.class);
  }

  public static String toJsonString(final JsonUtil json) throws Throwable
  {
    final Gson parse = new GsonBuilder().setLenient().setPrettyPrinting().enableComplexMapKeySerialization().create();
    return parse.toJson(json);
  }

  public boolean saveMyConfig() throws Throwable
  {
    return Main.writeFile(new File(this.configFileLocation), Main.toJsonString(this.config).getBytes("GBK"));
  }

  public void reloadMyConfig() throws Throwable
  {
    this.config = Main.parseJson(new String(Main.readFile(new File(this.configFileLocation)), "GBK"));
  }
}
