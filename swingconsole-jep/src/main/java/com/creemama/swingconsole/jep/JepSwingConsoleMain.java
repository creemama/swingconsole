package com.creemama.swingconsole.jep;

import java.io.File;

import com.creemama.swingconsole.ConsoleConfig;
import com.creemama.swingconsole.SwingConsoleFrame;

import jep.JepException;

/**
 * An application entry point that starts up a
 * <a href="https://github.com/ninia/jep">Java Embedded Python (JEP)</a>
 * interpreter as a Swing GUI.
 */
public class JepSwingConsoleMain {
	public static void main(String[] args) throws JepException {
		ConsoleConfig config = new ConsoleConfig() //
				// .evalFile("/path/to/startup/script.py") //
				// .put("java_variable", new StringBuilder("Console-Accessible Variable")) //
				.banner("JEP {{VERSION}} Python {{PYTHON_VERSION}} Java " + System.getProperty("java.version") + "\n") //
				.historyFile(new File(System.getProperty("user.home"), ".jep"));
		SwingConsoleFrame console = new SwingConsoleFrame("Java Embedded Python (JEP) Console");
		console.run(new JepSwingConsole(config));
	}
}
