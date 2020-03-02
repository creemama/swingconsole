package org.python.util;

import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.UnsupportedCharsetException;
import java.util.List;

import org.python.core.PlainConsole;
import org.python.core.Py;

import com.creemama.swingconsole.SwingConsoleModel;
import com.creemama.swingconsole.TextAreaReadline;

import jline.console.completer.AggregateCompleter;
import jline.console.completer.Completer;
import jline.console.history.History;
import jline.console.history.MemoryHistory;

/**
 * The model for a Jython {@link com.creemama.swingconsole.SwingConsoleFrame}.
 */
public class JythonConsoleModel implements SwingConsoleModel {

	public static class JythonConsoleReader {

		public final History history = new MemoryHistory();

		public final AggregateCompleter completer = new AggregateCompleter();

		public final String prompt = "";

		public boolean addCompleter(Completer additionalCompleter) {
			return completer.getCompleters().add(additionalCompleter);
		}
	}

	public static class JythonConsole extends PlainConsole {
		public final JythonConsoleReader reader = new JythonConsoleReader();

		public JythonConsole(String encoding) throws IllegalCharsetNameException, UnsupportedCharsetException {
			super(encoding);
		}
	}

	private static class JythonConsoleInputStream extends ConsoleInputStream {

		private final History history;

		private final TextAreaReadline readline;

		JythonConsoleInputStream(InputStream in, Charset encoding, String eol, History history,
				TextAreaReadline readline) {
			super(in, encoding, EOLPolicy.ADD, eol);
			this.history = history;
			this.readline = readline;
		}

		@Override
		protected CharSequence getLine() throws IOException, EOFException {

			String line = readline.readLine("");
			if (line != null && !line.isBlank())
				history.add(line);
			return line;
		}
	}

	@Override
	public void setUp(List<String> args, TextAreaReadline tar) {
		// Do nothing.
	}

	@Override
	public void runScript(File script) throws IOException {
		// TODO Auto-generated method stub
	}

	@Override
	public void putVariable(String variableName, Object value) {
		// TODO Auto-generated method stub
	}

	@Override
	public void run(TextAreaReadline tar) {
		System.setProperty("python.launcher.tty", "true");
		System.setProperty("python.console", JythonConsole.class.getName());
		// TODO PYTHONIOENCODING
		jython.run(new String[0], interpreter -> {
			JythonConsole c = ((JythonConsole) Py.getConsole());
			interpreter.setIn(new JythonConsoleInputStream(tar.getInputStream(), Charset.forName(c.getEncoding()),
					System.getProperty("line.separator"), c.reader.history, tar));
			interpreter.setOut(tar.getOutputStream());
			interpreter.setErr(tar.getOutputStream());
			tar.inject(c.reader.completer, c.reader.history);
		});
	}
}
