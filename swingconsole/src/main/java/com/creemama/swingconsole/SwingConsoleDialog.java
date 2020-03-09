package com.creemama.swingconsole;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.Insets;
import java.awt.Window;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;

import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.SwingUtilities;

/**
 * A {@code JDialog} that displays an interactive console with possible readline
 * functionality like tab completion and command history.
 */
public class SwingConsoleDialog extends JDialog {
	private static final long serialVersionUID = 3746242973444417387L;

	private boolean running;

	private TextAreaReadline tar;

	public SwingConsoleDialog(Window owner, String title) {
		super(owner, title);
		setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
	}

	@Override
	public void dispose() {
		if (SwingUtilities.isEventDispatchThread())
			disposeOnAWTEDT();
		else
			SwingUtilities.invokeLater(this::disposeOnAWTEDT);
	}

	private void disposeOnAWTEDT() {
		// Since tar.shutdown could take a few seconds, let us hide the window if it is
		// not already hidden.
		setVisible(false);
		if (tar != null) {
			tar.shutdown();
			tar = null;
		}
		getContentPane().removeAll();
		super.dispose();
		running = false;
	}

	public boolean isRunning() {
		return running;
	}

	public void run(String[] args, SwingConsoleModel model) {
		if (SwingUtilities.isEventDispatchThread())
			runOnAWTEDT(args, model);
		else
			SwingUtilities.invokeLater(() -> runOnAWTEDT(args, model));
	}

	private void runOnAWTEDT(String[] args, SwingConsoleModel model) {
		// NOTE: If you update this method, also update SwingConsoleFrame#run.

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

		tar = new TextAreaReadline(text, " Welcome to the " + getTitle() + " \n\n");

		JScrollPane pane = new JScrollPane();
		pane.setBorder(BorderFactory.createLineBorder(Color.darkGray));
		pane.setViewportView(text);

		setSize(700, 600);
		getContentPane().setLayout(new BorderLayout());
		getContentPane().add(pane, BorderLayout.CENTER);

		Thread swingConsoleThread = new Thread(() -> {
			model.setUp(Arrays.asList(args), tar);
			try {
				SwingUtilities.invokeAndWait(() -> setVisible(true));
			} catch (InvocationTargetException | InterruptedException e) {
				// Do nothing.
			}
			try {
				model.run(tar);
			} finally {
				SwingUtilities.invokeLater(() -> dispose());
			}
		}, getTitle());
		swingConsoleThread.setDaemon(true);
		swingConsoleThread.start();
	}

	@Override
	public void setVisible(boolean visible) {
		if (SwingUtilities.isEventDispatchThread())
			setVisibleOnAWTEDT(visible);
		else
			SwingUtilities.invokeLater(() -> setVisibleOnAWTEDT(visible));
	}

	private void setVisibleOnAWTEDT(boolean visible) {
		if (visible && !running)
			throw new IllegalStateException("Call #run first.");
		super.setVisible(visible);
	}
}
