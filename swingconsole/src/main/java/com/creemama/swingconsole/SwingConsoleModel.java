package com.creemama.swingconsole;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * The model for {@link SwingConsoleFrame}.
 */
public interface SwingConsoleModel {
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
	 * @throws NullPointerException if {@code tar} is {@code null}
	 */
	void setUp(List<String> args, TextAreaReadline tar);

	/**
	 * Runs the specified {@link script}.
	 * 
	 * @param script the location of the script to run
	 * @throws IOException                   if evaluating the specified
	 *                                       {@code script} throws an error
	 * @throws NullPointerException          if {@code script} is {@code null}
	 * @throws UnsupportedOperationException if this model does not support this
	 *                                       method
	 */
	void runScript(File script) throws IOException;

	/**
	 * Assigns the specified Java {@code value} object to the specified
	 * {@code variableName} in the console.
	 * 
	 * @param variableName the name of the variable
	 * @param value        the Java value object
	 * @throws NullPointerException          if {@code variableName} is {@code null}
	 * @throws UnsupportedOperationException if this model does not support this
	 *                                       method
	 */
	void putVariable(String variableName, Object value);

	/**
	 * Runs the interactive console.
	 * <p>
	 * This method should block until processing finishes.
	 * </p>
	 * 
	 * @param tar the controller of a text area, a
	 *            {@link javax.swing.JTextComponent}, displaying an interactive
	 *            console
	 * @throws NullPointerException if {@code tar} is {@code null}
	 */
	void run(TextAreaReadline tar);
}
