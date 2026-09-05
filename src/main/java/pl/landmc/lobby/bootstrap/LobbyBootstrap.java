package pl.landmc.lobby.bootstrap;

import dev.rollczi.litecommands.LiteCommands;
import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.Executor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;
import org.slf4j.Logger;
import pl.landmc.platform.api.ModuleLifecycle;
import pl.landmc.platform.component.ComponentFormatter;
import pl.landmc.platform.config.ConfigPlaceholders;
import pl.landmc.platform.config.ConfigService;
import pl.landmc.platform.database.DatabaseService;
import pl.landmc.platform.messaging.MessageBus;
import pl.landmc.platform.notice.AudienceNoticeService;
import pl.landmc.platform.notice.NoticeServiceProvider;
import pl.landmc.platform.paper.command.PaperCommands;
import pl.landmc.platform.paper.command.UnknownCommandListener;
import pl.landmc.platform.paper.notice.PaperNoticeService;
import pl.landmc.platform.paper.scheduler.MainThreadExecutor;
import pl.landmc.lobby.command.ProfileCommand;
import pl.landmc.lobby.command.SetSpawnCommand;
import pl.landmc.lobby.command.SpawnCommand;
import pl.landmc.lobby.config.LobbyConfig;
import pl.landmc.lobby.config.LobbyMessages;
import pl.landmc.lobby.hotbar.HotbarChannel;
import pl.landmc.lobby.hotbar.HotbarService;
import pl.landmc.lobby.sidebar.BalanceTracker;
import pl.landmc.lobby.sidebar.ScoreboardService;
import pl.landmc.lobby.tablist.RankPrefixes;
import pl.landmc.lobby.tablist.TablistService;
import pl.landmc.lobby.listener.HotbarListener;
import pl.landmc.lobby.listener.ProfileListener;
import pl.landmc.lobby.listener.ScoreboardListener;
import pl.landmc.lobby.listener.ServerLoadedListener;
import pl.landmc.lobby.listener.TablistListener;
import pl.landmc.lobby.listener.SpawnListener;
import pl.landmc.lobby.messaging.LobbyMessaging;
import pl.landmc.lobby.messaging.PingMessage;
import pl.landmc.lobby.messaging.PongMessage;
import pl.landmc.lobby.profile.ProfileRepository;
import pl.landmc.lobby.profile.ProfileService;
import pl.landmc.lobby.spawn.SpawnService;
import pl.landmc.lobby.world.WorldSetup;
import pl.landmc.lobby.world.WorldSetupListener;

/**
 * Builds the lobby out of platform pieces and takes it down again.
 *
 * <p>The order matters in both directions. Startup opens the database before anything can query
 * it and registers listeners last, so no player event arrives before the services behind it
 * exist. Shutdown persists first and closes second: profiles are written while the connection
 * pool is still open, and only then does the pool go away.
 */
public final class LobbyBootstrap {

    private final Plugin plugin;
    private final Logger logger;
    private final Path dataDirectory;

    private final ModuleLifecycle lifecycle;

    /** Read by the notice service's translation provider; assigned during {@link #start()}. */
    private LobbyMessages messages;

    private ConfigService configs;
    private LobbyConfig config;
    private DatabaseService database;
    private ProfileService profiles;
    private MessageBus bus;
    private LiteCommands<CommandSender> commands;

    public LobbyBootstrap(Plugin plugin, Logger logger, Path dataDirectory) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.logger = Objects.requireNonNull(logger, "logger");
        this.dataDirectory = Objects.requireNonNull(dataDirectory, "dataDirectory");
        this.lifecycle = new ModuleLifecycle(logger);
    }

    /**
     * @throws pl.landmc.platform.api.PlatformException when the configuration, the database or
     *     messaging cannot be initialised - the lobby stores player data, so coming up without
     *     a database would lose every profile written while it was "running"
     */
    public void start() {
        long startedAt = System.currentTimeMillis();
        this.logger.info("LandMC Lobby starting...");

        ComponentFormatter formatter = ComponentFormatter.standard();

        // Built before the configuration because loading a config with Notice fields needs the
        // serdes pack it exposes; it reads its translations through a provider filled in below.
        PaperNoticeService<LobbyMessages> notices =
                new PaperNoticeService<>(locale -> this.messages, formatter);

        this.configs = new ConfigService(
                ConfigPlaceholders.forPlugin(this.dataDirectory), notices.okaeriSerdes());
        this.config = this.configs.load(this.dataDirectory, "config.yml", LobbyConfig.class);
        this.messages = this.configs.load(this.dataDirectory, "messages.yml", LobbyMessages.class);
        this.logger.info("Loaded configuration.");

        this.database = new DatabaseService(
                "landmc-lobby", this.config.database, this.dataDirectory, this.logger);

        this.bus = LobbyMessaging.create(this.config, this.plugin.getServer(), this.logger);
        this.registerMessageHandlers();

        // Registered in start order; disableAll() stops them in reverse.
        this.lifecycle.register(this.database).register(this.bus).enableAll();

        ProfileRepository repository = new ProfileRepository(this.database);
        repository.createTables();
        this.logger.info("Database ready ({}).", this.config.database.type);

        Executor mainThread = new MainThreadExecutor(this.plugin);
        this.profiles = new ProfileService(repository, this.database, mainThread, this.logger);

        SpawnService spawn = new SpawnService(this.config, this.configs, this.plugin.getServer());

        NoticeServiceProvider<CommandSender> platformNotices =
                new AudienceNoticeService<>(this.messages.platform, formatter);

        this.commands = PaperCommands.builder(this.plugin, formatter, platformNotices)
                .commands(
                        new SpawnCommand(spawn, notices),
                        new SetSpawnCommand(spawn, notices),
                        new ProfileCommand(this.profiles, notices))
                .build();
        this.logger.info("Registered 3 commands.");

        this.plugin.getServer().getPluginManager()
                .registerEvents(new ProfileListener(this.profiles), this.plugin);
        this.plugin.getServer().getPluginManager()
                .registerEvents(new SpawnListener(spawn, this.plugin), this.plugin);
        this.plugin.getServer().getPluginManager()
                .registerEvents(new UnknownCommandListener(platformNotices), this.plugin);

        this.startScoreboards(formatter);
        this.startTablist(formatter);

        HotbarService hotbar = new HotbarService(this.config, formatter, this.logger);
        if (hotbar.isEnabled()) {
            this.plugin.getServer().getPluginManager().registerEvents(
                    new HotbarListener(hotbar, new HotbarChannel(this.plugin)), this.plugin);
        }
        else {
            this.logger.info("Lobby hotbar is off; players arrive with an empty inventory.");
        }

        this.startAutosave();

        // Not applied here: this plugin is enabled at STARTUP so that it can generate the
        // default world, which means no world exists yet. ServerLoadEvent is the first moment
        // they do.
        this.plugin.getServer().getPluginManager().registerEvents(
                new WorldSetupListener(
                        new WorldSetup(this.plugin.getServer(), this.config, this.logger)),
                this.plugin);

        if (!spawn.isSet()) {
            this.logger.warn("Lobby spawn is not set - use /setspawn to configure it.");
        }

        this.logger.info("LandMC Lobby ready ({} ms).", System.currentTimeMillis() - startedAt);
    }

    /**
     * Persists everything still dirty, then releases resources.
     *
     * <p>The final save is deliberately synchronous. Handing it to the database executor and
     * returning would race the JVM shutting down, and the last session of every online player
     * would be the thing that gets lost.
     */
    public void stop() {
        this.logger.info("LandMC Lobby stopping...");

        if (this.commands != null) {
            this.commands.unregister();
            this.commands = null;
        }

        if (this.profiles != null && this.database != null && this.database.isConnected()) {
            try {
                int saved = this.profiles.saveDirty().join();
                this.logger.info("Saved {} lobby profile(s) before shutdown.", saved);
            }
            catch (RuntimeException exception) {
                this.logger.error("Final save of lobby profiles failed", exception);
            }
            this.profiles.clear();
        }

        // Closes the bus and then the database, in reverse registration order.
        this.lifecycle.disableAll();

        this.logger.info("LandMC Lobby stopped.");
    }

    /** Answers the proxy's {@code test.ping}, closing the Proxy -> Redis -> Paper loop. */
    private void registerMessageHandlers() {
        this.bus.subscribe(PingMessage.class, (message, context) -> {
            this.logger.debug("Ping from {}", context.source());
            context.reply(new PongMessage(this.bus.serverId(), message.sentAt()));
        });
    }

    /**
     * Writes changed profiles on a timer.
     *
     * <p>One repeating task for the whole server, not one per player: the task collects the
     * dirty profiles on the main thread and hands a single batch to the database.
     */
    /**
     * Starts the sidebar, and the one task that keeps it current.
     *
     * <p>The balances it shows come from the proxy over a plugin channel - this server does not
     * own a wallet and must not query one per player per second. A change arriving redraws that
     * player straight away; the periodic pass is for the lines nothing announces, such as how
     * many people are online.
     */
    /**
     * Starts the tab list.
     *
     * <p>Refreshed on the same timer as the scoreboard rather than on a LuckPerms event: an
     * entry is only written when it actually changed, so a pass over the players costs a string
     * comparison each and nothing goes out over the network unless a rank moved.
     */
    private void startTablist(ComponentFormatter formatter) {
        TablistService tablist = new TablistService(this.config, formatter);

        if (!tablist.isEnabled()) {
            this.logger.info("Tab list prefixes are off; names are shown plain.");
            return;
        }

        this.plugin.getServer().getPluginManager()
                .registerEvents(new TablistListener(tablist), this.plugin);

        // Bound after the server has loaded, not now: this plugin is enabled at STARTUP so it
        // can generate the default world, which puts it ahead of LuckPerms.
        this.plugin.getServer().getPluginManager().registerEvents(
                new ServerLoadedListener(() -> {
                    tablist.prefixes(RankPrefixes.create(this.logger));
                    tablist.applyAll(this.plugin.getServer().getOnlinePlayers());
                }),
                this.plugin);

        long ticks = Math.max(1L, this.config.scoreboard.refreshTicks);
        this.plugin.getServer().getScheduler().runTaskTimer(
                this.plugin,
                () -> tablist.applyAll(this.plugin.getServer().getOnlinePlayers()),
                ticks,
                ticks);
    }

    private void startScoreboards(ComponentFormatter formatter) {
        BalanceTracker balances = new BalanceTracker(this.plugin, this.logger);
        ScoreboardService scoreboards = new ScoreboardService(this.config, balances, formatter);
        balances.onChanged(scoreboards::refresh);

        if (!scoreboards.isEnabled()) {
            this.logger.info("Lobby scoreboard is off.");
            return;
        }

        this.plugin.getServer().getPluginManager()
                .registerEvents(new ScoreboardListener(scoreboards), this.plugin);

        long ticks = Math.max(1L, this.config.scoreboard.refreshTicks);
        this.plugin.getServer().getScheduler()
                .runTaskTimer(this.plugin, scoreboards::refreshAll, ticks, ticks);
    }

    private void startAutosave() {
        int seconds = this.config.lobby.autosaveSeconds;
        if (seconds <= 0) {
            this.logger.info("Profile autosave is disabled; profiles are saved on quit and shutdown.");
            return;
        }

        long ticks = seconds * 20L;
        this.plugin.getServer().getGlobalRegionScheduler()
                .runAtFixedRate(this.plugin, task -> this.profiles.saveDirty(), ticks, ticks);
        this.logger.info("Profile autosave every {}s.", seconds);
    }
}
