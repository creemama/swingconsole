package org.python.util;

import com.creemama.swingconsole.SwingConsoleFrame;

/**
 * An application entry point that starts up a Jython interpreter as a Swing
 * GUI.
 */
public class JythonConsoleMain {
	public static void main(String[] args) {
		SwingConsoleFrame console = new SwingConsoleFrame("Jython Console");
		console.run(args, new JythonConsoleModel());
	}
}
