package pl.landmc.lobby.bootstrap;

import dev.rollczi.litecommands.LiteCommands;
import dev.rollczi.litecommands.argument.ArgumentKey;
import dev.rollczi.litecommands.suggestion.SuggestionResult;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
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
import pl.landmc.lobby.bossbar.BossBarService;
import pl.landmc.lobby.command.FlyCommand;
import pl.landmc.lobby.fly.FlyService;
import pl.landmc.lobby.command.NpcCommand;
import pl.landmc.lobby.command.ProfileCommand;
import pl.landmc.lobby.command.SetSpawnCommand;
import pl.landmc.lobby.command.SpawnCommand;
import pl.landmc.lobby.config.LobbyConfig;
import pl.landmc.lobby.listener.NpcListener;
import pl.landmc.lobby.listener.SpawnBlockListener;
import pl.landmc.lobby.listener.ProtectionListener;
import pl.landmc.lobby.menu.MenuChannel;
import pl.landmc.lobby.messaging.ServerCountsMessage;
import pl.landmc.lobby.npc.NpcService;
import pl.landmc.lobby.npc.ServerCounts;
import pl.landmc.lobby.config.LobbyMessages;
import pl.landmc.lobby.hotbar.HotbarService;
import pl.landmc.lobby.sidebar.BalanceTracker;
import pl.landmc.lobby.sidebar.ScoreboardService;
import pl.landmc.lobby.listener.HotbarListener;
import pl.landmc.lobby.listener.ArrivalListener;
import pl.landmc.lobby.listener.ProfileListener;
import pl.landmc.lobby.listener.ScoreboardListener;
import pl.landmc.lobby.listener.ServerLoadedListener;
import pl.landmc.lobby.listener.SpawnListener;
import pl.landmc.lobby.messaging.LobbyMessaging;
import pl.landmc.lobby.messaging.PingMessage;
import pl.landmc.lobby.messaging.PongMessage;
import pl.landmc.lobby.profile.ProfileRepository;
import pl.landmc.lobby.profile.ProfileService;
import pl.landmc.lobby.sidebar.UiText;
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
    private NpcService npcs;
    private LiteCommands<CommandSender> commands;
    private BossBarService bossBar;

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

        FlyService fly = new FlyService(this.config);

        // Built here rather than beside the board, because the listener below needs them and it
        // is registered before the board is started. The board is handed the same tracker.
        BalanceTracker balances = new BalanceTracker(this.plugin, this.logger);
        this.bossBar = new BossBarService(
                this.config, formatter, new UiText(this.config.ui, formatter), balances,
                this.profiles);

        // A lobby with flight switched off does not answer /fly at all, rather than answering
        // it with a refusal - the command simply is not part of that server.
        // One pipe to the proxy, shared: the hotbar asks it to open menus and the figures
        // on the spawn ask it to move somebody to another server.
        MenuChannel menuChannel = new MenuChannel(this.plugin);

        ServerCounts serverCounts = new ServerCounts();
        this.npcs = new NpcService(
                this.plugin, this.config, this.configs, formatter, serverCounts);
        this.npcs.start();

        List<Object> commands = new ArrayList<>(List.of(
                new SpawnCommand(spawn, notices),
                new SetSpawnCommand(spawn, notices),
                new ProfileCommand(this.profiles, notices),
                new NpcCommand(this.npcs, notices)));

        if (fly.isEnabled()) {
            commands.add(new FlyCommand(fly, notices));
        }

        this.commands = PaperCommands.builder(this.plugin, formatter, platformNotices)
                // Tab on a figure argument offers the figures that are standing, not the word
                // "npc". Somebody who has to remember the names has to keep the file open
                // beside the game.
                .argumentSuggester(
                        String.class,
                        ArgumentKey.of(NpcCommand.NAME_ARGUMENT),
                        (invocation, argument, context) ->
                                SuggestionResult.of(this.npcs.ids()))
                .argumentSuggester(
                        String.class,
                        ArgumentKey.of(NpcCommand.PRESET_ARGUMENT),
                        (invocation, argument, context) ->
                                SuggestionResult.of(this.npcs.presets()))
                .argumentSuggester(
                        String.class,
                        ArgumentKey.of(NpcCommand.MENU_ARGUMENT),
                        (invocation, argument, context) ->
                                SuggestionResult.of(menuNames()))
                .commands(commands.toArray())
                .build();
        this.logger.info("Registered {} commands.", commands.size());



        this.plugin.getServer().getPluginManager()
                .registerEvents(new ProfileListener(this.profiles), this.plugin);
        this.plugin.getServer().getPluginManager()
                .registerEvents(new SpawnListener(spawn, this.plugin), this.plugin);
        this.plugin.getServer().getPluginManager()
                .registerEvents(
                        new ArrivalListener(this.bossBar, fly, this.plugin), this.plugin);
        this.plugin.getServer().getPluginManager()
                .registerEvents(new UnknownCommandListener(platformNotices), this.plugin);
        this.plugin.getServer().getPluginManager()
                .registerEvents(new ProtectionListener(this.config), this.plugin);

        // The portal and the launch pads share a listener, because they share the expensive
        // part: noticing that somebody changed block at all.
        if (this.config.portal.enabled || this.config.launchPads.enabled) {
            this.plugin.getServer().getPluginManager().registerEvents(
                    new SpawnBlockListener(this.plugin, menuChannel, this.config), this.plugin);
        }

        this.startScoreboards(formatter, balances);

        if (this.npcs.isEnabled()) {
            this.plugin.getServer().getPluginManager().registerEvents(
                    new NpcListener(this.npcs, menuChannel, this.config), this.plugin);

            // Only the proxy can count the people on another server, so the figures are told
            // rather than left to guess. Remembered on the messaging thread and read on the
            // main one, which is why the holder swaps a whole map rather than editing one.
            this.bus.subscribe(
                    ServerCountsMessage.class,
                    (message, context) -> serverCounts.accept(message));
        }
        else {
            this.logger.info("Lobby NPCs are off; the spawn has no server figures.");
        }

        HotbarService hotbar = new HotbarService(this.config, formatter, this.logger);
        if (hotbar.isEnabled()) {
            this.plugin.getServer().getPluginManager().registerEvents(
                    new HotbarListener(hotbar, menuChannel), this.plugin);
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

        // Checked once the worlds exist. Asking during enable always answered "not set",
        // because this plugin is enabled at STARTUP and the server has loaded no world yet -
        // so it warned on every start, whatever the config said.
        this.plugin.getServer().getPluginManager().registerEvents(
                new ServerLoadedListener(() -> {
                    if (!spawn.isSet()) {
                        this.logger.warn(
                                "Lobby spawn is not set - use /setspawn to configure it.");
                    }
                }),
                this.plugin);

        this.logger.info("LandMC Lobby ready ({} ms).", System.currentTimeMillis() - startedAt);
    }

    /**
     * Persists everything still dirty, then releases resources.
     *
     * <p>The final save is deliberately synchronous. Handing it to the database executor and
     * returning would race the JVM shutting down, and the last session of every online player
     * would be the thing that gets lost.
     */
    /** The menus a figure can be pointed at, for tab completion. */
    private static List<String> menuNames() {
        List<String> names = new ArrayList<>();
        for (pl.landmc.menus.protocol.MenuKind kind : pl.landmc.menus.protocol.MenuKind.values()) {
            names.add(kind.name());
        }
        return names;
    }

    public void stop() {
        this.logger.info("LandMC Lobby stopping...");

        if (this.commands != null) {
            this.commands.unregister();
            this.commands = null;
        }

        // Before the bus goes. A figure left standing after a reload is one the next start
        // puts up again beside it.
        if (this.npcs != null) {
            this.npcs.stop();
            this.npcs = null;
        }

        // A reload leaves the old bar on screen otherwise: the players stay connected and
        // nothing else ever tells their client the bar is gone.
        if (this.bossBar != null) {
            this.bossBar.hideAll(this.plugin.getServer().getOnlinePlayers());
            this.bossBar = null;
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
    private void startScoreboards(ComponentFormatter formatter, BalanceTracker balances) {
        ScoreboardService scoreboards = new ScoreboardService(this.config, balances, this.profiles, formatter);

        balances.onChanged(player -> {
            scoreboards.refresh(player);
            this.bossBar.refresh(player);
        });

        if (!scoreboards.isEnabled()) {
            this.logger.info("Lobby scoreboard is off.");
            return;
        }

        this.plugin.getServer().getPluginManager()
                .registerEvents(new ScoreboardListener(scoreboards), this.plugin);

        long ticks = Math.max(1L, this.config.scoreboard.refreshTicks);
        this.plugin.getServer().getScheduler().runTaskTimer(this.plugin, () -> {
            scoreboards.refreshAll();
            // On the same timer, because the bar shows the same kind of thing and there is no
            // reason for a second one. It is a single update for the whole server, not one per
            // player, so it costs a fraction of what the boards do.
            this.bossBar.refreshAll();
        }, ticks, ticks);
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
