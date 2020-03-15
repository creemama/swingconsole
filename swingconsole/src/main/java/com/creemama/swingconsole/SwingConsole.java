package com.creemama.swingconsole;

/**
 * A task that hooks up and runs an interactive console displayed within a
 * {@link SwingConsoleDialog} or {@link SwingConsoleFrame}.
 */
public interface SwingConsole {
	/**
	 * Runs the interactive console.
	 * <p>
	 * This method should block until processing finishes.
	 * </p>
	 * 
	 * @param console text component displaying an interactive console
	 * @throws NullPointerException if {@code console} is {@code null}
	 */
	void run(SwingConsolePane console);
}
