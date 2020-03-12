package com.creemama.swingconsole;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Font;
import java.awt.Insets;
import java.awt.Window;
import java.lang.reflect.InvocationTargetException;
import java.util.function.Consumer;

import javax.swing.BorderFactory;
import javax.swing.JEditorPane;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.SwingUtilities;

/**
 * Common code between {@link SwingConsoleDialog} and {@link SwingConsoleFrame}.
 */
class SwingConsoleWindow {
	private boolean running;

	private TextAreaReadline tar;

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
		if (tar != null) {
			tar.shutdown();
			tar = null;
		}
		container.removeAll();
		superDispose.run();
		running = false;
	}

	boolean isRunning() {
		return running;
	}

	void run(Container container, SwingConsoleModel model, String title, boolean visible, Window window) {
		if (SwingUtilities.isEventDispatchThread())
			runOnAWTEDT(container, model, title, visible, window);
		else
			SwingUtilities.invokeLater(() -> runOnAWTEDT(container, model, title, visible, window));
	}

	private void runOnAWTEDT(Container container, SwingConsoleModel model, String title, boolean visible,
			Window window) {
		if (running)
			throw new IllegalStateException(
					"You already called #run. Only call #run again after disposing of this window.");
		running = true;

		JEditorPane text = new JTextPane();
		text.setBackground(new Color(0xf2, 0xf2, 0xf2));
		text.setCaretColor(new Color(0xa4, 0x00, 0x00));
		Font font = SwingConsoleUtil.findFont("Monospaced", Font.PLAIN, 14, new String[] { "Monaco", "Andale Mono" });
		text.setFont(font);
		text.setForeground(new Color(0xa4, 0x00, 0x00));
		text.setMargin(new Insets(8, 8, 8, 8));

		tar = new TextAreaReadline(text, " Welcome to the " + title + " \n\n");

		JScrollPane pane = new JScrollPane();
		pane.setBorder(BorderFactory.createLineBorder(Color.darkGray));
		pane.setViewportView(text);

		window.setSize(700, 600);
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
				model.run(tar);
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
