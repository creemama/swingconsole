package com.creemama.swingreadline;

import java.util.List;

import org.jruby.demo.readline.TextAreaReadline;

/**
 * The model for {@link org.jruby.demo.readline.IRBConsole}.
 */
public interface JReadlineFrameModel {
	/**
	 * Hooks up the specified {@code tar}.
	 * <p>
	 * This may include hooking up the I/O streams of {@code tar} to a framework
	 * like JRuby or Jython.
	 * </p>
	 * 
	 * @param args the list of args sent into a {@code main} method
	 * @param tar  the controller of a text area, a
	 *             {@link javax.swing.JTextComponent}, displaying an interactive
	 *             console
	 */
	void setUp(List<String> args, TextAreaReadline tar);

	/**
	 * Runs the interactive console.
	 * <p>
	 * This method should block until processing finishes.
	 * </p>
	 * 
	 * @param tar the controller of a text area, a
	 *            {@link javax.swing.JTextComponent}, displaying an interactive
	 *            console
	 */
	void run(TextAreaReadline tar);
}
