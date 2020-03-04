package com.creemama.swingconsole;

import java.io.File;
import java.util.List;

import javax.script.ScriptException;

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
	 *             {@link javax.swing.text.JTextComponent}, displaying an
	 *             interactive console
	 * @throws NullPointerException if {@code tar} is {@code null}
	 */
	void setUp(List<String> args, TextAreaReadline tar);

	/**
	 * Runs the specified {@code script}.
	 * 
	 * @param script the location of the script to run
	 * @throws ScriptException               if evaluating the specified
	 *                                       {@code script} throws an error
	 * @throws NullPointerException          if {@code script} is {@code null}
	 * @throws UnsupportedOperationException if this model does not support this
	 *                                       method
	 */
	void eval(File script) throws ScriptException;

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
	void put(String variableName, Object value);

	/**
	 * Runs the interactive console.
	 * <p>
	 * This method should block until processing finishes.
	 * </p>
	 * 
	 * @param tar the controller of a text area, a
	 *            {@link javax.swing.text.JTextComponent}, displaying an interactive
	 *            console
	 * @throws NullPointerException if {@code tar} is {@code null}
	 */
	void run(TextAreaReadline tar);
}
