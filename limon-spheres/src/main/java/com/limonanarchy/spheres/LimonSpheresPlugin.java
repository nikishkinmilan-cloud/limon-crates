package com.limonanarchy.spheres;

import com.limonanarchy.spheres.commands.GiveSphereCommand;
import com.limonanarchy.spheres.commands.GiveSphereShardCommand;
import com.limonanarchy.spheres.commands.SpheresCommand;
import com.limonanarchy.spheres.commands.SpheromantCommand;
import com.limonanarchy.spheres.gui.SpheromantGUI;
import com.limonanarchy.spheres.listeners.AnvilCombineListener;
import com.limonanarchy.spheres.listeners.EffectApplyTask;
import com.limonanarchy.spheres.listeners.FurnaceSmeltListener;
import com.limonanarchy.spheres.listeners.SpheromantGUIListener;
import org.bukkit.plugin.java.JavaPlugin;

public class LimonSpheresPlugin extends JavaPlugin {

    private SphereRegistry sphereRegistry;
    private SphereItemFactory sphereItemFactory;
    private ShardItemFactory shardItemFactory;
    private ShardManager shardManager;
    private SphereManager sphereManager;
    private SpheromantGUI spheromantGUI;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        saveResource("spheres.yml", false);

        this.sphereRegistry = new SphereRegistry();
        this.sphereRegistry.load(getDataFolder());

        this.sphereItemFactory = new SphereItemFactory(this);
        this.shardItemFactory = new ShardItemFactory(this);
        this.shardManager = new ShardManager(this);
        this.sphereManager = new SphereManager(this);
        this.spheromantGUI = new SpheromantGUI(this);

        // Listeners
        getServer().getPluginManager().registerEvents(new AnvilCombineListener(this), this);
        getServer().getPluginManager().registerEvents(new FurnaceSmeltListener(this), this);
        getServer().getPluginManager().registerEvents(new SpheromantGUIListener(this), this);

        // Периодическое применение эффектов от сферы в офф-руке
        long interval = getConfig().getLong("effect-tick-interval", 40L);
        new EffectApplyTask(this).runTaskTimer(this, 20L, interval);

        // Commands
        getCommand("givesphere").setExecutor(new GiveSphereCommand(this));
        getCommand("givesphereshard").setExecutor(new GiveSphereShardCommand(this));
        getCommand("spheromant").setExecutor(new SpheromantCommand(this));
        getCommand("spheres").setExecutor(new SpheresCommand(this));

        // Регистрируем рецепт печи: PLAYER_HEAD -> осколки (см. FurnaceSmeltListener)
        FurnaceSmeltListener.registerRecipe(this);

        getLogger().info("LimonSpheres включен. Загружено типов сфер: " + sphereRegistry.getKeys().size());
    }

    @Override
    public void onDisable() {
        getLogger().info("LimonSpheres выключен.");
    }

    public void reload() {
        reloadConfig();
        sphereRegistry.load(getDataFolder());
    }

    public SphereRegistry getSphereRegistry() {
        return sphereRegistry;
    }

    public SphereItemFactory getSphereItemFactory() {
        return sphereItemFactory;
    }

    public ShardItemFactory getShardItemFactory() {
        return shardItemFactory;
    }

    public ShardManager getShardManager() {
        return shardManager;
    }

    public SphereManager getSphereManager() {
        return sphereManager;
    }

    public SpheromantGUI getSpheromantGUI() {
        return spheromantGUI;
    }
}
