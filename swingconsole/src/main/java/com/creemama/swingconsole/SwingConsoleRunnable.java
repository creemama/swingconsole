package com.creemama.swingconsole;

/**
 * A task that hooks up and runs an interactive console displayed within a
 * {@link SwingConsoleDialog} or {@link SwingConsoleFrame}.
 */
public interface SwingConsoleRunnable {
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
