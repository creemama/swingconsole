package com.creemama.swingconsole.jep;

import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.Optional;

import javax.script.ScriptException;

import com.creemama.swingconsole.ConsoleConfig;
import com.creemama.swingconsole.SwingConsole;
import com.creemama.swingconsole.SwingConsolePane;
import com.creemama.swingconsole.jep.JepConsole.JepConsoleRunModel;

import jep.python.PyObject;
import jline.console.completer.Completer;
import jline.console.history.FileHistory;
import jline.console.history.History;
import jline.console.history.MemoryHistory;

/**
 * A task that hooks up and runs a <a href="https://github.com/ninia/jep">Java
 * Embedded Python (JEP)</a> shell within a
 * {@link com.creemama.swingconsole.SwingConsoleDialog} or
 * {@link com.creemama.swingconsole.SwingConsoleFrame}.
 */
public class JepSwingConsole implements SwingConsole {
	public static class JepStdOut {
		final private SwingConsolePane console;

		public JepStdOut(SwingConsolePane console) {
			this.console = console;
		}

		public void write(PyObject b) {
			try {
				console.getOutputStream().write(b.toString().getBytes(Charset.forName("UTF-8")));
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		}
	}

	private static class JepSwingConsoleRunModel implements JepConsoleRunModel {
		final private String banner;

		final private Completer completer;

		final private SwingConsolePane console;

		private History history;

		final private File historyFile;

		JepSwingConsoleRunModel(String banner, Completer completer, SwingConsolePane console, File historyFile) {
			this.banner = banner;
			this.completer = completer;
			this.console = console;
			this.history = new MemoryHistory();
			this.historyFile = historyFile;
		}

		@Override
		public Optional<String> getBanner() {
			return Optional.ofNullable(banner);
		}

		@Override
		public Writer getWriter() {
			return new OutputStreamWriter(console.getOutputStream());
		}

		@Override
		public String readLine(String prompt) {
			String line = console.readLine(prompt);
			if (line != null && !line.isEmpty())
				history.add(line);
			return line;
		}

		@Override
		public void saveHistory() {
			try {
				if (history instanceof FileHistory)
					((FileHistory) history).flush();
			} catch (Exception e) {
				// Ignore.
			}
		}

		@Override
		public void setUpHistory() throws Exception {
			History newHistory = new MemoryHistory();
			try {
				if (historyFile != null)
					newHistory = new FileHistory(historyFile);
			} catch (RuntimeException e) {
				// There is no history from file.
				throw e;
			} finally {
				console.inject(completer, this.history = newHistory);
			}
		}
	}

	final private ConsoleConfig config;

	public JepSwingConsole(ConsoleConfig config) {
		this.config = config;
	}

	@Override
	public void run(SwingConsolePane swingConsole) {
		JepEngine engine = new JepEngine();
		String jepIOClass = "import io\n" //
				+ "\n" //
				+ "class JepIO(io.RawIOBase):\n" //
				+ "    def __init__(self, java_io):\n" //
				+ "        super(JepIO, self).__init__()\n" //
				+ "        self._java_io = java_io\n" //
				+ "\n" //
				+ "    def read(self, size=-1):\n" //
				+ "        return self._java_io.read(size)\n" //
				+ "\n" //
				+ "    def readinto(self, b):\n" //
				+ "        return self._java_io.readinto(b)\n" //
				+ "\n" //
				+ "    def write(self, b):\n" //
				+ "        return self._java_io.write(b)\n";
		try {
			// Redirect stdout so that print commands work.
			engine.put("jep_stdout", new JepStdOut(swingConsole));
			engine.eval("import sys");
			engine.eval("sys.stdout = jep_stdout");
			engine.eval("sys.stderr = jep_stdout");
			engine.eval(jepIOClass);

		} catch (ScriptException e) {
			throw new RuntimeException(e);
		}
		String banner = config.getBanner().map(str -> JepConsole.buildBanner(str, engine)).orElse(null);
		JepConsoleRunModel runModel = new JepSwingConsoleRunModel(banner, JepCompleter.create(engine).orElse(null),
				swingConsole, config.getHistoryFile().orElse(null));
		new JepConsole().run(engine, runModel, config.getStartupCommands());
	}
}
