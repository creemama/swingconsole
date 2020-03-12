package com.creemama.swingconsole;

/**
 * The model for {@link SwingConsoleFrame}.
 */
public interface SwingConsoleModel {
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
