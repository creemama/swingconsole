package com.creemama.swingconsole.jep;

import com.creemama.swingconsole.SwingConsoleFrame;

import jep.JepException;

/**
 * An application entry point that starts up a
 * <a href="https://github.com/ninia/jep">Java Embedded Python (JEP)</a>
 * interpreter as a Swing GUI.
 */
public class JepConsoleMain {
	public static void main(String[] args) throws JepException {
		SwingConsoleFrame console = new SwingConsoleFrame("JEP Console");
		console.run(args, new JepConsoleModel());
	}
}
