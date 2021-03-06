package com.creemama.swingconsole;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Window;
import java.lang.reflect.InvocationTargetException;
import java.util.function.Consumer;

import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

/**
 * Common code between {@link SwingConsoleDialog} and {@link SwingConsoleFrame}.
 */
class SwingConsoleWindow {
	final static int DEFAULT_CLOSE_OPERATION = WindowConstants.DISPOSE_ON_CLOSE;

	final static Dimension DEFAULT_SIZE = new Dimension(700, 600);

	private boolean running;

	private SwingConsolePane text;

	void dispose(Container container, Runnable superDispose, Window window) {
		if (SwingUtilities.isEventDispatchThread())
			disposeOnAWTEDT(container, superDispose, window);
		else
			SwingUtilities.invokeLater(() -> disposeOnAWTEDT(container, superDispose, window));
	}

	private void disposeOnAWTEDT(Container container, Runnable superDispose, Window window) {
		// Since tar.shutdown could take a few seconds, let us hide the window if it is
		// not already hidden.
		window.setVisible(false);
		if (text != null) {
			text.shutDown();
			text = null;
		}
		container.removeAll();
		superDispose.run();
		running = false;
	}

	boolean isRunning() {
		return running;
	}

	void run(Container container, SwingConsole runnable, String title, boolean visible, Window window) {
		if (SwingUtilities.isEventDispatchThread())
			runOnAWTEDT(container, runnable, title, visible, window);
		else
			SwingUtilities.invokeLater(() -> runOnAWTEDT(container, runnable, title, visible, window));
	}

	private void runOnAWTEDT(Container container, SwingConsole runnable, String title, boolean visible, Window window) {
		if (running)
			throw new IllegalStateException(
					"You already called #run. Only call #run again after disposing of this window.");
		running = true;

		SwingConsolePane text = new SwingConsolePane(" Welcome to the " + title + " \n\n");

		JScrollPane pane = new JScrollPane();
		pane.setViewportView(text);

		container.setLayout(new BorderLayout());
		container.add(pane, BorderLayout.CENTER);

		Thread swingConsoleThread = new Thread(() -> {
			try {
				if (visible)
					SwingUtilities.invokeAndWait(() -> window.setVisible(true));
			} catch (InvocationTargetException | InterruptedException e) {
				// Do nothing.
			}
			try {
				runnable.run(text);
			} finally {
				SwingUtilities.invokeLater(() -> window.dispose());
			}
		}, title);
		swingConsoleThread.setDaemon(true);
		swingConsoleThread.start();
	}

	void setVisible(Consumer<Boolean> superSetVisible, boolean visible) {
		if (SwingUtilities.isEventDispatchThread())
			setVisibleOnAWTEDT(superSetVisible, visible);
		else
			SwingUtilities.invokeLater(() -> setVisibleOnAWTEDT(superSetVisible, visible));
	}

	private void setVisibleOnAWTEDT(Consumer<Boolean> superSetVisible, boolean visible) {
		if (visible && !running)
			throw new IllegalStateException("Call #run first.");
		superSetVisible.accept(visible);
	}
}
