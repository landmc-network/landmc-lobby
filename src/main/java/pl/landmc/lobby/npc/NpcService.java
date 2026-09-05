package pl.landmc.lobby.npc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.TextDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.EulerAngle;
import pl.landmc.lobby.config.LobbyConfig;
import pl.landmc.lobby.messaging.ServerCountsMessage;
import pl.landmc.platform.component.ComponentFormatter;
import pl.landmc.platform.config.ConfigService;
import pl.landmc.platform.paper.menu.Items;

/**
 * The figures on the spawn, one per game mode.
 *
 * <p>What the previous version of the network had: a dressed, posed armour stand wearing the
 * mode's name, a line above it saying how many people are playing, and a line above that
 * blinking an invitation to click. Walking into it or clicking it sent you there.
 *
 * <p>The figure is still an armour stand, because that is what it is - a mannequin with a skin
 * on its head, dyed leather on its body and its arms bent into a pose. The two lines above it
 * are text displays rather than the invisible marker stands they used to be: a marker stand is
 * an entity with a hitbox, an inventory and a name tag, spawned and then switched off piece by
 * piece until only the name remains, and a text display is that name without the entity around
 * it.
 *
 * <p>Nothing is saved with the world. The figures are put up on enable and taken down on
 * disable, so what is permanent is the file rather than the entity - which is also what stops a
 * crash from leaving two SkyBlock men standing in the same block.
 *
 * <p>The player count is not this server's to know. It comes from the proxy, which is the only
 * process that can see who is on SkyBlock, and until the first broadcast arrives the sign says
 * so rather than saying nought.
 */
public final class NpcService {

    /** The name of the plugin metadata put on our own entities, so a click can recognise one. */
    public static final String METADATA_KEY = "landmc-npc";

    private final Plugin plugin;
    private final LobbyConfig config;
    private final ConfigService configs;
    private final ComponentFormatter formatter;
    private final ServerCounts counts;

    /** The armour stand of each figure, by hologram id. */
    private final Map<String, UUID> figures = new HashMap<>();

    /** Its three labels, in the order they stand: name, count, invitation. */
    private final Map<String, UUID> nameLabels = new HashMap<>();
    private final Map<String, UUID> countLabels = new HashMap<>();
    private final Map<String, UUID> addonLabels = new HashMap<>();

    /** What each label was last told to say, so it is only told when that changes. */
    private final Map<String, String> written = new HashMap<>();

    /**
     * Which figure stands in which block, for the walk-in.
     *
     * <p>An index rather than a scan: this is read from a move event, which is the hottest path
     * a lobby has, and a list walked once per player per block would be the sort of thing that
     * only shows up when the lobby is full.
     */
    private final Map<Long, String> byBlock = new HashMap<>();

    /** Which half of the blink the invitation is on. */
    private int blink;

    private BukkitTask task;

    public NpcService(
            Plugin plugin,
            LobbyConfig config,
            ConfigService configs,
            ComponentFormatter formatter,
            ServerCounts counts) {

        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.config = Objects.requireNonNull(config, "config");
        this.configs = Objects.requireNonNull(configs, "configs");
        this.formatter = Objects.requireNonNull(formatter, "formatter");
        this.counts = Objects.requireNonNull(counts, "counts");
    }

    public boolean isEnabled() {
        return this.config.npcs.enabled;
    }

    public int count() {
        return this.config.npcs.list.size();
    }

    public List<LobbyConfig.NpcEntry> all() {
        return List.copyOf(this.config.npcs.list);
    }

    public List<String> ids() {
        List<String> ids = new ArrayList<>(this.config.npcs.list.size());
        for (LobbyConfig.NpcEntry entry : this.config.npcs.list) {
            ids.add(entry.id);
        }
        return ids;
    }

    public LobbyConfig.NpcEntry find(String id) {
        for (LobbyConfig.NpcEntry entry : this.config.npcs.list) {
            if (entry.id.equalsIgnoreCase(id)) {
                return entry;
            }
        }
        return null;
    }

    /** Which server the figure standing in this block sends people to, or null. */
    public LobbyConfig.NpcEntry standingIn(Location location) {
        String id = this.byBlock.get(blockKey(
                location.getBlockX(), location.getBlockY(), location.getBlockZ()));
        return id == null ? null : this.find(id);
    }

    /** Which figure this entity belongs to, or null if it is not one of ours. */
    public LobbyConfig.NpcEntry owning(Entity entity) {
        for (Map.Entry<String, UUID> figure : this.figures.entrySet()) {
            if (figure.getValue().equals(entity.getUniqueId())) {
                return this.find(figure.getKey());
            }
        }
        return null;
    }

    public void start() {
        if (!this.isEnabled() || this.task != null) {
            return;
        }

        long interval = Math.max(1L, this.config.npcs.refreshTicks);
        this.task = this.plugin.getServer().getScheduler()
                .runTaskTimer(this.plugin, this::refresh, 1L, interval);
    }

    public void stop() {
        if (this.task != null) {
            this.task.cancel();
            this.task = null;
        }
        this.takeDownAll();
    }

    // -- what the command does ------------------------------------------------------------

    /** Puts a new figure where somebody is standing, facing the way they are facing. */
    public LobbyConfig.NpcEntry create(String id, String server, String name, Location location) {
        LobbyConfig.NpcEntry entry = new LobbyConfig.NpcEntry();
        entry.id = id;
        entry.server = server;
        entry.name = name;
        entry.addon = this.config.npcs.defaultAddon;
        // Dressed on the way up when the network has a preset for that mode. The old server
        // had one per mode written into its source; a figure standing there naked because
        // nobody typed a second command is a worse default than either.
        wear(entry, this.preset(server));
        write(entry, location);

        this.config.npcs.list.add(entry);
        this.configs.save(this.config);
        this.refresh();
        return entry;
    }

    public void remove(LobbyConfig.NpcEntry entry) {
        this.config.npcs.list.remove(entry);
        this.configs.save(this.config);
        this.takeDown(entry.id);
    }

    public void move(LobbyConfig.NpcEntry entry, Location location) {
        write(entry, location);
        this.configs.save(this.config);
        // Taken down rather than moved: an armour stand teleported keeps its pose but not
        // necessarily the block it is indexed under, and putting it up again is cheap.
        this.takeDown(entry.id);
        this.refresh();
    }

    public void setName(LobbyConfig.NpcEntry entry, String name) {
        entry.name = name;
        this.changed(entry);
    }

    public void setAddon(LobbyConfig.NpcEntry entry, String addon) {
        entry.addon = addon;
        this.changed(entry);
    }

    public void setSkin(LobbyConfig.NpcEntry entry, String skin) {
        entry.skin = skin;
        this.changed(entry);
    }

    public void setItem(LobbyConfig.NpcEntry entry, String item) {
        entry.item = item;
        this.changed(entry);
    }

    public void setArmourColour(LobbyConfig.NpcEntry entry, String colour) {
        entry.armourColour = colour;
        this.changed(entry);
    }

    /** The outfit under that name, or null when the file has none. */
    public LobbyConfig.NpcPreset preset(String id) {
        for (LobbyConfig.NpcPreset preset : this.config.npcs.presets) {
            if (preset.id.equalsIgnoreCase(id)) {
                return preset;
            }
        }
        return null;
    }

    public List<String> presets() {
        List<String> ids = new ArrayList<>(this.config.npcs.presets.size());
        for (LobbyConfig.NpcPreset preset : this.config.npcs.presets) {
            ids.add(preset.id);
        }
        return ids;
    }

    /** Puts an outfit on a figure that is already standing. */
    public void wear(LobbyConfig.NpcEntry entry, LobbyConfig.NpcPreset preset) {
        if (preset == null) {
            return;
        }

        entry.skin = preset.skin;
        entry.armourColour = preset.armourColour;
        entry.item = preset.item;
        entry.leftArm = new ArrayList<>(preset.leftArm);
        entry.rightArm = new ArrayList<>(preset.rightArm);
        entry.leftLeg = new ArrayList<>(preset.leftLeg);
        entry.rightLeg = new ArrayList<>(preset.rightLeg);
    }

    /** The same, from a command, so it is written down and put up again. */
    public void dressUp(LobbyConfig.NpcEntry entry, LobbyConfig.NpcPreset preset) {
        this.wear(entry, preset);
        this.changed(entry);
    }

    private void changed(LobbyConfig.NpcEntry entry) {
        this.configs.save(this.config);
        // Everything about how the figure looks is decided when it is spawned, so a change to
        // any of it is a figure put up again.
        this.takeDown(entry.id);
        this.refresh();
    }

    // -- the timer ------------------------------------------------------------------------

    /**
     * Brings every figure up to date, putting up any that are not there.
     *
     * <p>Missing is normal rather than exceptional: nothing here is saved with the world, so a
     * figure whose chunk unloaded is simply gone and comes back when the chunk does. A chunk
     * that is not loaded is skipped - spawning into it would load it, and holding a chunk in
     * memory to show a sign nobody is standing near is a poor trade.
     */
    private void refresh() {
        this.blink++;

        for (LobbyConfig.NpcEntry entry : this.config.npcs.list) {
            World world = this.plugin.getServer().getWorld(entry.world);
            if (world == null || !isLoaded(world, entry)) {
                continue;
            }

            this.figure(entry, world);
            this.label(entry, world, this.nameLabels, this.config.npcs.nameOffset,
                    entry.id + ":name", entry.name);
            this.label(entry, world, this.countLabels, this.config.npcs.countOffset,
                    entry.id + ":count", this.countText(entry));

            if (!entry.addon.isBlank()) {
                this.label(entry, world, this.addonLabels, this.config.npcs.addonOffset,
                        entry.id + ":addon", this.addonText(entry));
            }
        }
    }

    /** The line that says how busy the mode is. */
    private String countText(LobbyConfig.NpcEntry entry) {
        ServerCountsMessage.Server server = this.counts.of(entry.server);
        if (server == null) {
            return this.config.npcs.countUnknown;
        }
        if (!server.reachable()) {
            return this.config.npcs.countOffline;
        }
        return this.config.npcs.countFormat.replace("{ONLINE}", Integer.toString(server.online()));
    }

    /**
     * The invitation, in whichever colour it is on this pass.
     *
     * <p>Blinking rather than a fixed colour because that is what the old server did, and the
     * reason it did is that a line which changes is the one thing on a spawn full of signs that
     * somebody's eye actually goes to.
     */
    private String addonText(LobbyConfig.NpcEntry entry) {
        List<String> colours = this.config.npcs.addonColours;
        if (colours.isEmpty()) {
            return entry.addon;
        }
        return colours.get(Math.floorMod(this.blink, colours.size())) + entry.addon;
    }

    private void label(
            LobbyConfig.NpcEntry entry,
            World world,
            Map<String, UUID> labels,
            double offset,
            String key,
            String text) {

        UUID shownId = labels.get(entry.id);
        TextDisplay display = null;

        if (shownId != null) {
            Entity existing = this.plugin.getServer().getEntity(shownId);
            if (existing instanceof TextDisplay found && !found.isDead()) {
                display = found;
            }
            else {
                labels.remove(entry.id);
                this.written.remove(key);
            }
        }

        if (display == null) {
            display = this.spawnLabel(world, entry, offset);
            if (display == null) {
                return;
            }
            labels.put(entry.id, display.getUniqueId());
        }

        if (!text.equals(this.written.put(key, text))) {
            display.text(this.formatter.format(text));
        }
    }

    private TextDisplay spawnLabel(World world, LobbyConfig.NpcEntry entry, double offset) {
        LobbyConfig.NpcSection npcs = this.config.npcs;
        Location at = new Location(world, entry.x, entry.y + offset, entry.z);

        try {
            return world.spawn(at, TextDisplay.class, spawned -> {
                spawned.setBillboard(Display.Billboard.CENTER);
                spawned.setAlignment(TextDisplay.TextAlignment.CENTER);
                spawned.setBackgroundColor(Color.fromARGB(
                        Math.clamp(npcs.backgroundOpacity, 0, 255), 0, 0, 0));
                spawned.setShadowed(false);
                spawned.setSeeThrough(false);
                spawned.setViewRange((float) npcs.viewRange);
                spawned.setPersistent(false);
                spawned.setInvulnerable(true);
                spawned.setSilent(true);
            });
        }
        catch (RuntimeException refused) {
            this.plugin.getLogger().warning(
                    "Could not put up the label for " + entry.id + " (" + refused.getMessage() + ").");
            return null;
        }
    }

    /** The figure itself, put up if it is not standing. */
    private void figure(LobbyConfig.NpcEntry entry, World world) {
        UUID shownId = this.figures.get(entry.id);
        if (shownId != null) {
            Entity existing = this.plugin.getServer().getEntity(shownId);
            if (existing instanceof ArmorStand stand && !stand.isDead()) {
                return;
            }
            this.figures.remove(entry.id);
        }

        Location at = new Location(world, entry.x, entry.y, entry.z, entry.yaw, 0.0F);

        ArmorStand stand;
        try {
            stand = world.spawn(at, ArmorStand.class, spawned -> {
                spawned.setGravity(false);
                spawned.setCanPickupItems(false);
                spawned.setBasePlate(false);
                spawned.setArms(true);
                spawned.setInvulnerable(true);
                spawned.setSilent(true);
                spawned.setPersistent(false);
                // The name is one of our own displays, above, so all three lines are
                // spaced by us rather than by wherever Minecraft chooses to draw a name tag.

                this.dress(spawned, entry);
                pose(spawned, entry);

                // Read by the listener, so a click can tell one of ours from an armour stand
                // somebody built. Keyed by plugin as well as by name, so it cannot be confused
                // with another plugin's mark.
                spawned.setMetadata(METADATA_KEY, new org.bukkit.metadata.FixedMetadataValue(
                        this.plugin, entry.id));
            });
        }
        catch (RuntimeException refused) {
            this.plugin.getLogger().warning(
                    "Could not put up the figure " + entry.id + " (" + refused.getMessage() + ").");
            return;
        }

        this.figures.put(entry.id, stand.getUniqueId());
        this.byBlock.put(
                blockKey(at.getBlockX(), at.getBlockY(), at.getBlockZ()), entry.id);
    }

    /** Head, body and hand. Everything optional: an undressed figure is still a figure. */
    private void dress(ArmorStand stand, LobbyConfig.NpcEntry entry) {
        if (!entry.skin.isBlank()) {
            stand.getEquipment().setHelmet(Items.head(entry.skin).build());
        }

        Color colour = colour(entry.armourColour);
        if (colour != null) {
            stand.getEquipment().setChestplate(dyed(Material.LEATHER_CHESTPLATE, colour));
            stand.getEquipment().setLeggings(dyed(Material.LEATHER_LEGGINGS, colour));
            stand.getEquipment().setBoots(dyed(Material.LEATHER_BOOTS, colour));
        }

        Material item = Material.matchMaterial(entry.item.toUpperCase(Locale.ROOT));
        if (item != null && !item.isAir()) {
            stand.getEquipment().setItemInMainHand(new ItemStack(item));
        }
    }

    private static void pose(ArmorStand stand, LobbyConfig.NpcEntry entry) {
        EulerAngle leftArm = angle(entry.leftArm);
        if (leftArm != null) {
            stand.setLeftArmPose(leftArm);
        }
        EulerAngle rightArm = angle(entry.rightArm);
        if (rightArm != null) {
            stand.setRightArmPose(rightArm);
        }
        EulerAngle leftLeg = angle(entry.leftLeg);
        if (leftLeg != null) {
            stand.setLeftLegPose(leftLeg);
        }
        EulerAngle rightLeg = angle(entry.rightLeg);
        if (rightLeg != null) {
            stand.setRightLegPose(rightLeg);
        }
    }

    /** Three degrees, as configuration writes them, or null for the default pose. */
    private static EulerAngle angle(List<Double> degrees) {
        if (degrees == null || degrees.size() != 3) {
            return null;
        }
        return new EulerAngle(
                Math.toRadians(degrees.get(0)),
                Math.toRadians(degrees.get(1)),
                Math.toRadians(degrees.get(2)));
    }

    private static ItemStack dyed(Material material, Color colour) {
        ItemStack piece = new ItemStack(material);
        if (piece.getItemMeta() instanceof LeatherArmorMeta meta) {
            meta.setColor(colour);
            piece.setItemMeta(meta);
        }
        return piece;
    }

    /**
     * The colours a name can be given, so dyed armour does not have to be written in hex.
     *
     * <p>Minecraft's own sixteen, plus the few words somebody reaches for anyway. Anybody
     * setting a figure's armour is standing in the game looking at it, and "green" is what they
     * will type.
     */
    private static final Map<String, Integer> NAMED_COLOURS = Map.ofEntries(
            Map.entry("black", 0x000000),
            Map.entry("dark_blue", 0x0000AA),
            Map.entry("dark_green", 0x00AA00),
            Map.entry("dark_aqua", 0x00AAAA),
            Map.entry("dark_red", 0xAA0000),
            Map.entry("dark_purple", 0xAA00AA),
            Map.entry("gold", 0xFFAA00),
            Map.entry("gray", 0xAAAAAA),
            Map.entry("dark_gray", 0x555555),
            Map.entry("blue", 0x5555FF),
            Map.entry("green", 0x55FF55),
            Map.entry("aqua", 0x55FFFF),
            Map.entry("red", 0xFF5555),
            Map.entry("light_purple", 0xFF55FF),
            Map.entry("yellow", 0xFFFF55),
            Map.entry("white", 0xFFFFFF),
            Map.entry("lime", 0x55FF55),
            Map.entry("orange", 0xFF8000),
            Map.entry("brown", 0xA06540),
            Map.entry("pink", 0xFF9AC0),
            Map.entry("cyan", 0x00AAAA),
            Map.entry("magenta", 0xFF55FF),
            Map.entry("olive", 0x556B2F),
            Map.entry("leather", 0xA06540));

    /**
     * A colour by name or as {@code #rrggbb}, or null when the field is empty or unreadable.
     *
     * <p>Null rather than a fallback colour: a figure with no armour reads as one nobody has
     * dressed yet, and a figure in a colour nobody asked for reads as a bug.
     */
    private static Color colour(String written) {
        String value = written.trim();
        if (value.isEmpty()) {
            return null;
        }

        Integer named = NAMED_COLOURS.get(value.toLowerCase(Locale.ROOT));
        if (named != null) {
            return Color.fromRGB(named);
        }

        String hex = value.startsWith("#") ? value.substring(1) : value;
        if (hex.length() != 6) {
            return null;
        }

        try {
            return Color.fromRGB(Integer.parseInt(hex, 16));
        }
        catch (IllegalArgumentException wrong) {
            return null;
        }
    }

    private void takeDown(String id) {
        this.despawn(this.figures.remove(id));
        this.despawn(this.nameLabels.remove(id));
        this.despawn(this.countLabels.remove(id));
        this.despawn(this.addonLabels.remove(id));
        this.written.remove(id + ":name");
        this.written.remove(id + ":count");
        this.written.remove(id + ":addon");
        this.byBlock.values().remove(id);
    }

    private void takeDownAll() {
        for (String id : this.ids()) {
            this.takeDown(id);
        }
        // Anything left is a figure whose entry has already gone from the file.
        for (UUID shownId : new ArrayList<>(this.figures.values())) {
            this.despawn(shownId);
        }
        this.figures.clear();
        this.nameLabels.clear();
        this.countLabels.clear();
        this.addonLabels.clear();
        this.written.clear();
        this.byBlock.clear();
    }

    private void despawn(UUID entityId) {
        if (entityId == null) {
            return;
        }

        Entity entity = this.plugin.getServer().getEntity(entityId);
        if (entity != null && entity.getType() != EntityType.PLAYER) {
            entity.remove();
        }
    }

    private static void write(LobbyConfig.NpcEntry entry, Location location) {
        entry.world = Objects.requireNonNull(location.getWorld(), "location world").getName();
        entry.x = location.getX();
        entry.y = location.getY();
        entry.z = location.getZ();
        entry.yaw = location.getYaw();
    }

    /**
     * Whether the chunk this figure stands in is in memory.
     *
     * <p>Worked out from the coordinates rather than by asking the location for its chunk,
     * because asking loads it - which is the thing this is here to avoid.
     */
    private static boolean isLoaded(World world, LobbyConfig.NpcEntry entry) {
        return world.isChunkLoaded(
                (int) Math.floor(entry.x) >> 4,
                (int) Math.floor(entry.z) >> 4);
    }

    /** One block, as a single number, so the walk-in is a map lookup and not a search. */
    private static long blockKey(int x, int y, int z) {
        return ((long) x & 0x3FFFFFFL) << 38
                | ((long) z & 0x3FFFFFFL) << 12
                | ((long) y & 0xFFFL);
    }
}
