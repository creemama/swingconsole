package com.creemama.swingconsole.jep;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.script.ScriptException;

import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.UserInterruptException;

import com.creemama.swingconsole.ConsoleConfig;
import com.creemama.swingconsole.ConsoleConfig.EvalFileStartupCommand;
import com.creemama.swingconsole.ConsoleConfig.PutStartupCommand;
import com.creemama.swingconsole.ConsoleConfig.StartupCommand;
import com.creemama.swingconsole.ConsoleConfig.StartupCommandVisitor;

/**
 * A read-eval-print loop (REPL), an interactive console, for
 * <a href="https://github.com/ninia/jep">Java Embedded Python (JEP)</a>.
 */
public class JepConsole {
	interface JepConsoleRunModel {
		Optional<String> getBanner();

		Writer getWriter();

		String readLine(String prompt);

		void saveHistory();

		void setUpHistory() throws Exception;
	}

	private static class JepConsoleRunModelImpl implements JepConsoleRunModel {
		private final String banner;

		private final LineReader reader;

		JepConsoleRunModelImpl(String banner, LineReader reader) {
			this.banner = banner;
			this.reader = reader;
		}

		@Override
		public Optional<String> getBanner() {
			return Optional.ofNullable(banner);
		}

		@Override
		public Writer getWriter() {
			return reader.getTerminal().writer();
		}

		@Override
		public String readLine(String prompt) {
			return reader.readLine(prompt);
		}

		@Override
		public void saveHistory() {
			try {
				reader.getHistory().save();
			} catch (Exception e) {
				// Ignore.
			}
		}

		@Override
		public void setUpHistory() throws Exception {
			// Do nothing.
		}
	}

	static String buildBanner(String banner, JepEngine engine) {
		String newBanner = banner;
		if (newBanner.contains("{{VERSION}}")) {
			String version;
			try {
				engine.eval("import jep");
				Object versionObj = engine.eval("jep.__VERSION__");
				version = versionObj == null ? "?" : versionObj.toString();
			} catch (ScriptException e) {
				version = "?";
			}
			newBanner = newBanner.replaceAll("\\{\\{VERSION\\}\\}", version);
		}
		if (newBanner.contains("{{PYTHON_VERSION}}")) {
			String version;
			try {
				engine.eval("from platform import python_version");
				Object versionObj = engine.eval("python_version()");
				version = versionObj == null ? "?" : versionObj.toString();
			} catch (ScriptException e) {
				version = "?";
			}
			newBanner = newBanner.replaceAll("\\{\\{PYTHON_VERSION\\}\\}", version);
		}
		return newBanner;
	}

	private volatile boolean runCalled = false;

	private boolean jepeval(JepEngine engine, List<String> evalLines, String line, Writer writer) throws IOException {
		// Compare this method with a method of the same name at
		// https://github.com/ninia/jep/blob/master/src/main/python/jep/console.py.
		if (line.isEmpty()) {
			if (!evalLines.isEmpty()) {
				String code = evalLines.stream().collect(Collectors.joining("\n"));
				evalLines.clear();
				String output;
				try {
					Object result = engine.eval(code);
					output = result == null ? null : result.toString();
				} catch (ScriptException e) {
					output = e.getMessage();
				}
				if (output != null) {
					writer.write(output);
					writer.write("\n");
					writer.flush();
				}
			}
			return true;
		} else if (evalLines.isEmpty()) {
			String output;
			try {
				Object result = engine.eval(line);
				output = result == null ? null : result.toString();
			} catch (ScriptException e) {
				output = e.getMessage();
				if (output.contains("<class 'SyntaxError'>")) {
					evalLines.add(line);
					return false;
				}
			}
			if (output != null) {
				writer.write(output);
				writer.write("\n");
				writer.flush();
			}
			return true;
		} else {
			evalLines.add(line);
			return false;
		}
	}

	public void run(ConsoleConfig config) {
		JepEngine engine = new JepEngine();
		LineReader reader = LineReaderBuilder.builder().completer(JepCompleter.create(engine).orElse(null))
				.option(LineReader.Option.HISTORY_IGNORE_SPACE, false)
				.option(LineReader.Option.HISTORY_REDUCE_BLANKS, false)
				.option(LineReader.Option.HISTORY_TIMESTAMPED, false)
				.variable(LineReader.HISTORY_FILE, config.getHistoryFile().orElse(null)).build();
		JepConsoleRunModelImpl runModel = new JepConsoleRunModelImpl(config.getBanner().orElse(null), reader);
		run(engine, runModel, config.getStartupCommands());
	}

	public void run(JepEngine engine, JepConsoleRunModel runModel, List<StartupCommand> startupCommands) {
		if (runCalled)
			throw new IllegalStateException("You should only call run once.");
		runCalled = true;

		for (StartupCommand command : startupCommands) {
			command.accept(new StartupCommandVisitor() {
				@Override
				public void visit(EvalFileStartupCommand command) {
					try {
						engine.eval(command.getFile());
					} catch (ScriptException e) {
						throw new RuntimeException(e);
					}
				}

				@Override
				public void visit(PutStartupCommand command) {
					engine.put(command.getVariableName(), command.getValue());
				}
			});
		}

		// This call fires up the command-processing thread and throws a
		// RuntimeException if not successful.
		engine.getCommandQueue();

		try (BufferedWriter writer = new BufferedWriter(runModel.getWriter())) {

			String banner = runModel.getBanner().map(str -> buildBanner(str, engine)).orElse(null);
			if (banner != null) {
				writer.write(banner);
				writer.write("\n");
				writer.flush();
			}

			try {
				runModel.setUpHistory();
				Runtime.getRuntime().addShutdownHook(new Thread(runModel::saveHistory, "JEP History Writer"));
			} catch (Exception e) {
				writer.write("No history from file: " + e.getMessage());
				writer.write("\n");
				writer.flush();
			}

			List<String> evalLines = new LinkedList<>();
			boolean ran = true;
			while (true) {
				try {
					String line = runModel.readLine(ran ? ">>> " : "... ");
					if (line == null)
						break;
					if (line.trim().matches("exit\\([^)]*\\)"))
						// Calling exit() bypasses the shutdown hook set up above, so let us save
						// history here.
						runModel.saveHistory();
					ran = jepeval(engine, evalLines, line, writer);
				} catch (UserInterruptException e) {
					// Ignore.
				}
			}
		} catch (EndOfFileException e) {
			// Ignore.
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		} finally {
			engine.shutDown();
		}
	}
}
