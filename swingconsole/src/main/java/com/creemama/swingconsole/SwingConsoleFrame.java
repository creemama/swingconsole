package com.creemama.swingconsole;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.Insets;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import javax.swing.BorderFactory;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.SwingUtilities;

/**
 * A {@link JFrame} that displays an interactive console with possible readline
 * functionality like tab completion and command history.
 */
public class SwingConsoleFrame extends JFrame {
	private TextAreaReadline tar;

	public SwingConsoleFrame(String title) {
		super(title);
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
	}

	public void run(String[] args, SwingConsoleModel model) {
		// NOTE: If you update this method, also update SwingConsoleDialog#run.

		List<String> list = Arrays.asList(args);

		JEditorPane text = new JTextPane();
		text.setBackground(new Color(0xf2, 0xf2, 0xf2));
		text.setCaretColor(new Color(0xa4, 0x00, 0x00));
		Font font = SwingConsoleUtil.findFont("Monospaced", Font.PLAIN, 14, new String[] { "Monaco", "Andale Mono" });
		text.setFont(font);
		text.setForeground(new Color(0xa4, 0x00, 0x00));
		text.setMargin(new Insets(8, 8, 8, 8));

		JScrollPane pane = new JScrollPane();
		pane.setBorder(BorderFactory.createLineBorder(Color.darkGray));
		pane.setViewportView(text);

		setSize(700, 600);
		getContentPane().setLayout(new BorderLayout());
		getContentPane().add(pane, BorderLayout.CENTER);

		tar = new TextAreaReadline(text, " Welcome to the " + getTitle() + " \n\n");

		model.setUp(list, tar);

		// We use this future to wait for the AWT Event Dispatch Thread to start. Since
		// we have marked most of our threads as daemon threads, the AWT EDT might be
		// the only thread preventing the JVM from closing. If we do not wait, sometimes
		// the application immediately exits without the console window showing up.
		CompletableFuture<Void> waitForAWTEDTToStart = new CompletableFuture<>();

		Thread swingConsoleThread = new Thread(() -> {
			setVisible(true);
			waitForAWTEDTToStart.complete(null);
			try {
				model.run(tar);
			} finally {
				SwingUtilities.invokeLater(() -> dispose());
			}
		}, getTitle());
		swingConsoleThread.setDaemon(true);
		swingConsoleThread.start();

		try {
			waitForAWTEDTToStart.get();
		} catch (ExecutionException | InterruptedException e) {
			// Ignore.
		}
	}

	@Override
	public void dispose() {
		// Since tar.shutdown could take a few seconds, let us hide the window if it is
		// not already hidden.
		setVisible(false);
		if (tar != null)
			tar.shutdown();
		super.dispose();
	}

	private static final long serialVersionUID = 3746242973444417387L;
}
