package com.creemama.swingconsole.jep;

import java.io.File;

import com.creemama.swingconsole.ConsoleConfig;

import jep.JepException;

/**
 * An application entry point that starts up a
 * <a href="https://github.com/ninia/jep">Java Embedded Python (JEP)</a>
 * interpreter.
 */
public class JepConsoleMain {
	public static void main(String[] args) throws JepException {
		ConsoleConfig config = new ConsoleConfig() //
				// .evalFile("/path/to/startup/script.py") //
				// .put("java_variable", new StringBuilder("Console-Accessible Variable")) //
				.banner("\nJEP {{VERSION}} Python {{PYTHON_VERSION}} Java " + System.getProperty("java.version") + "\n") //
				.historyFile(new File(System.getProperty("user.home"), ".jep"));
		new JepConsole().run(config);
	}
}
