package com.creemama.swingconsole.jruby;

import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.util.Optional;

import org.jruby.Ruby;
import org.jruby.embed.ScriptingContainer;
import org.jruby.internal.runtime.GlobalVariable;
import org.jruby.internal.runtime.ValueAccessor;

import com.creemama.swingconsole.ConsoleConfig;
import com.creemama.swingconsole.ConsoleConfig.EvalFileStartupCommand;
import com.creemama.swingconsole.ConsoleConfig.PutStartupCommand;
import com.creemama.swingconsole.ConsoleConfig.StartupCommand;
import com.creemama.swingconsole.ConsoleConfig.StartupCommandVisitor;

/**
 * A JRuby interactive Ruby (IRB) shell.
 */
public class JRubyConsole {
	static Optional<String> buildVersion(ConsoleConfig config, Ruby runtime) {
		String banner = config.getBanner().orElse(null);
		if (banner.contains("{{VERSION}}")) {
			String version;
			try {
				Object versionObj = runtime.evalScriptlet("JRUBY_VERSION");
				version = versionObj == null ? "?" : versionObj.toString();
			} catch (RuntimeException e) {
				version = "?";
			}
			return Optional.of(banner.replaceAll("\\{\\{VERSION\\}\\}", version));
		}
		return Optional.of(banner);
	}

	public void run(ConsoleConfig config) {
		// Read more about ScriptingContainer at
		// https://github.com/jruby/jruby/wiki/RedBridge.
		ScriptingContainer container = new ScriptingContainer();
		for (StartupCommand command : config.getStartupCommands()) {
			command.accept(new StartupCommandVisitor() {
				@Override
				public void visit(EvalFileStartupCommand command) {
					try (Reader reader = new FileReader(command.getFile(), Charset.forName("UTF-8"))) {
						container.runScriptlet(reader, command.getFile().getPath());
					} catch (IOException e) {
						throw new UncheckedIOException(e);
					}
				}

				@Override
				public void visit(PutStartupCommand command) {
					container.put(command.getVariableName(), command.getValue());
				}
			});
		}
		Ruby runtime = container.getProvider().getRuntime();
		new JRubyConsoleHistory(config.getHistoryFile().orElse(null)).setUpHistory(System.out, runtime);
		runtime.getGlobalVariables().defineReadonly("$$",
				new ValueAccessor(runtime.newFixnum(System.identityHashCode(runtime))), GlobalVariable.Scope.GLOBAL);
		runtime.evalScriptlet(
				"ARGV << '--readline' << '--prompt' << 'inf-ruby';" + "require 'irb'; require 'irb/completion';");
		buildVersion(config, runtime).ifPresent(System.out::println);
		runtime.evalScriptlet("IRB.start");
	}
}
