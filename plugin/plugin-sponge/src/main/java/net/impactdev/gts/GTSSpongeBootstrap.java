package net.impactdev.gts;

import com.google.gson.reflect.TypeToken;
import com.google.inject.Inject;
import net.impactdev.gts.api.blacklist.Blacklist;
import net.impactdev.gts.api.util.PrettyPrinter;
import net.impactdev.gts.common.config.ConfigKeys;
import net.impactdev.gts.placeholders.GTSSpongePlaceholderManager;
import net.impactdev.impactor.api.Impactor;
import net.impactdev.impactor.api.dependencies.classloader.PluginClassLoader;
import net.impactdev.impactor.api.dependencies.classloader.ReflectionClassLoader;
import net.impactdev.impactor.api.logging.Logger;
import lombok.Getter;
import net.impactdev.gts.api.GTSService;
import net.impactdev.gts.api.events.extension.PlaceholderRegistryEvent;
import net.impactdev.gts.common.plugin.bootstrap.GTSBootstrap;
import net.impactdev.gts.common.utils.exceptions.ExceptionWriter;
import org.bstats.sponge.Metrics2;
import org.spongepowered.api.Platform;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.config.ConfigDir;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.Order;
import org.spongepowered.api.event.game.GameRegistryEvent;
import org.spongepowered.api.event.game.GameReloadEvent;
import org.spongepowered.api.event.game.state.GameInitializationEvent;
import org.spongepowered.api.event.game.state.GamePreInitializationEvent;
import org.spongepowered.api.event.game.state.GameStartedServerEvent;
import org.spongepowered.api.event.game.state.GameStartingServerEvent;
import org.spongepowered.api.plugin.Dependency;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.plugin.PluginContainer;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.text.placeholder.PlaceholderParser;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Plugin(
		id = "gts",
		name = "GTS",
		version = "@version@",
		description = "@gts_description",
		dependencies = {
				@Dependency(id = "impactor", version = "[3.0.0,)")
		})
public class GTSSpongeBootstrap implements GTSBootstrap {

	private final GTSSpongePlugin plugin;

	@Getter
	@Inject
	private PluginContainer container;

	@Inject
	@ConfigDir(sharedRoot = false)
	private Path configDir;

	private final PluginClassLoader classLoader;

	private Throwable exception;

	@Inject
	public GTSSpongeBootstrap(org.slf4j.Logger fallback) {
		this.plugin = new GTSSpongePlugin(this, fallback);
		this.classLoader = new ReflectionClassLoader(this);
	}

	@Listener(order = Order.EARLY)
	public void onPreInit(GamePreInitializationEvent event) {
		long start = System.nanoTime();
		try {
			this.plugin.preInit();

			long end = System.nanoTime();
			this.getPluginLogger().info("Pre-Initialization complete, took " + TimeUnit.MILLISECONDS.convert(end - start, TimeUnit.NANOSECONDS) + " ms");
		} catch (Throwable e) {
			this.exception = e;
			this.disable();
			ExceptionWriter.write(e);
		}
	}

	@Listener
	public void onInit(GameInitializationEvent event) {
		try {
			this.plugin.init();
		} catch (Throwable e) {
			this.exception = e;
			this.disable();
			ExceptionWriter.write(e);
		}
	}

	@Listener
	public void onStart(GameStartingServerEvent event) {
		try {
			this.plugin.started();
		} catch (Throwable e) {
			this.exception = e;
			this.disable();
		}
	}

	@Listener
	public void onReload(GameReloadEvent event) {
		this.plugin.getConfiguration().reload();
		this.plugin.getMsgConfig().reload();

		Impactor.getInstance().getRegistry().get(Blacklist.class).clear();
		this.plugin.getConfiguration().get(ConfigKeys.BLACKLIST).read();
	}

	@Listener
	public void registerPlaceholders(GameRegistryEvent.Register<PlaceholderParser> event) {
		GTSSpongePlaceholderManager manager = new GTSSpongePlaceholderManager();
		for(PlaceholderParser pasrser : manager.getAllParsers()) {
			event.register(pasrser);
		}

		Impactor.getInstance().getEventBus().post(PlaceholderRegistryEvent.class, new TypeToken<GameRegistryEvent.Register<PlaceholderParser>>(){}, event);
	}

	@Override
	public Logger getPluginLogger() {
		return this.plugin.getPluginLogger();
	}

	@Override
	public Path getDataDirectory() {
		return this.configDir.resolve("data");
	}

	@Override
	public Path getConfigDirectory() {
		return this.configDir;
	}

	@Override
	public InputStream getResourceStream(String path) {
		return this.plugin.getResourceStream(path);
	}

	@Override
	public PluginClassLoader getPluginClassLoader() {
		return this.classLoader;
	}

	@Override
	public Optional<Throwable> getLaunchError() {
		return Optional.ofNullable(this.exception);
	}

	@Override
	public void disable() {
		Sponge.getEventManager().unregisterPluginListeners(this);
		Sponge.getCommandManager().getOwnedBy(this).forEach(Sponge.getCommandManager()::removeMapping);
		Sponge.getScheduler().getScheduledTasks(this).forEach(Task::cancel);

		Sponge.getEventManager().registerListener(this, GameStartedServerEvent.class, event -> this.displayErrorOnStart());
	}

	private void displayErrorOnStart() {
		PrettyPrinter printer = new PrettyPrinter(80)
				.add("GTS FAILED TO LOAD").center()
				.hr()
				.bigX()
				.hr('-')
				.add("GTS encountered an error during server start and did not enable successfully")
				.add("No commands, listeners, or tasks are registered.")
				.add()
				.add("This means the plugin is set to not function and is in safe mode!")
				.add()
				.add("Below is the encountered stacktrace:")
				.hr('-');

		if(!this.getLaunchError().isPresent()) {
			printer.add("No exception information was logged...");
		} else {
			printer.add(this.getLaunchError().get());
		}

		printer.hr('-')
				.add("If this error persists, ensure you are running the latest GTS versions")
				.add("If you are on latest, please report this error to the GTS team at here:")
				.add("https://github.com/NickImpact/GTS/issues")
				.hr('-')
				.add("Server Information").center()
				.hr('-')
				.add("Impactor Version: " + Sponge.getPluginManager().getPlugin("impactor").get().getVersion().get())
				.add("GTS Version:      " + this.plugin.getMetadata().getVersion())
				.add("Sponge Version:   " + Sponge.getGame().getPlatform().getContainer(Platform.Component.IMPLEMENTATION).getName() + " " + Sponge.getGame().getPlatform().getContainer(Platform.Component.IMPLEMENTATION).getVersion().orElse("?"));

		if(!GTSService.getInstance().getAllExtensions().isEmpty()) {
			printer.hr('-')
					.add("Installed Extensions")
					.hr('-');
			GTSService.getInstance().getAllExtensions().forEach(x -> printer.add(x.getMetadata().getName() + " - " + x.getMetadata().getVersion()));
		}

		printer.hr()
				.add("END GTS ERROR REPORT")
				.center()
				.log(this.getPluginLogger(), PrettyPrinter.Level.ERROR);
	}
}
