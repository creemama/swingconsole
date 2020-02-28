/***** BEGIN LICENSE BLOCK *****
 * Version: EPL 1.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Eclipse Public
 * License Version 1.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/epl-v10.html
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 *
 * Copyright (C) 2006 Ola Bini <ola@ologix.com>
 * Copyright (C) 2006 Damian Steer <pldms@mac.com>
 * Copyright (C) 2008 Joseph LaFata <joe@quibb.org>
 * 
 * Alternatively, the contents of this file may be used under the terms of
 * either of the GNU General Public License Version 2 or later (the "GPL"),
 * or the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the EPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the EPL, the GPL or the LGPL.
 ***** END LICENSE BLOCK *****/
package org.jruby.ext.readline;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.nio.CharBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import jline.*;
import jline.console.ConsoleReader;
import jline.console.CursorBuffer;
import jline.console.completer.CandidateListCompletionHandler;
import jline.console.completer.Completer;
import jline.console.completer.CompletionHandler;
import jline.console.completer.FileNameCompleter;
import jline.console.history.History;
import jline.console.history.MemoryHistory;

import org.jruby.*;
import org.jruby.anno.JRubyMethod;
import org.jruby.anno.JRubyModule;
import org.jruby.runtime.*;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ByteList;
import org.jruby.util.log.Logger;
import org.jruby.util.log.LoggerFactory;

import static org.jruby.runtime.Visibility.*;

/**
 * @author <a href="mailto:ola.bini@ki.se">Ola Bini</a>
 * @author <a href="mailto:pldms@mac.com">Damian Steer</a>
 * @author <a href="mailto:koichiro@meadowy.org">Koichiro Ohba</a>
 */
@JRubyModule(name = "Readline")
public class Readline {

    private final static boolean DEBUG = false;

    private static final Logger LOG = LoggerFactory.getLogger(Readline.class);

    static {
        if (DEBUG) {
            LOG.setDebugEnable(true);

            try {
                if (System.getProperty("jline.internal.Log.debug") == null) {
                    System.setProperty("jline.internal.Log.debug", "true");
                }
            } catch (SecurityException ex) { /* ignored */ }
        }
    }

    public static final char ESC_KEY_CODE = (char) 27;
    public static class ConsoleHolder {
        public ConsoleReader readline;
        transient volatile Completer currentCompletor;
        public final History history = new MemoryHistory();
    }

    public static void load(Ruby runtime) {
        createReadline(runtime);
    }

    public static void createReadline(final Ruby runtime) {
        final ConsoleHolder holder = new ConsoleHolder();

        RubyModule mReadline = runtime.defineModule("Readline");

        mReadline.dataWrapStruct(holder);

        mReadline.defineAnnotatedMethods(Readline.class);
        mReadline.setConstant("COMPLETION_CASE_FOLD", runtime.getNil(), true); // private_constant

        IRubyObject hist = runtime.getObject().callMethod(runtime.getCurrentContext(), "new");
        mReadline.setConstant("HISTORY", hist);
        hist.getSingletonClass().includeModule(runtime.getEnumerable());
        hist.getSingletonClass().defineAnnotatedMethods(HistoryMethods.class);

        // MRI does similar thing on MacOS X with 'EditLine wrapper'.
        mReadline.setConstant("VERSION", runtime.newString("JLine wrapper"));
    }

    // We lazily initialize this in case Readline.readline has been overridden in ruby (s_readline)
    protected static void initReadline(final Ruby runtime, final ConsoleHolder holder) {
        final ConsoleReader readline;
        try {
            final Terminal terminal = TerminalFactory.create();
            readline = holder.readline = new ConsoleReader(null, runtime.getInputStream(), runtime.getOutputStream(), terminal);
        } catch (IOException ioe) {
            throw runtime.newIOErrorFromException(ioe);
        }

        readline.setHistoryEnabled(false);
        readline.setPaginationEnabled(true);
        readline.setBellEnabled(true);

        if (holder.currentCompletor == null) {
            holder.currentCompletor = new RubyFileNameCompletor();
        }
        readline.addCompleter(holder.currentCompletor);
        readline.setHistory(holder.history);

        // JRUBY-852, ignore escape key (it causes IRB to quit if we pass it out through readline)
        readline.addTriggeredAction(ESC_KEY_CODE, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    holder.readline.beep();
                } catch (IOException ex) {
                    LOG.debug(ex);
                }
            }
        });

        RubyModule Readline = runtime.getModule("Readline");
        BlockCallback callback = new BlockCallback() {
            public IRubyObject call(ThreadContext context, IRubyObject[] iRubyObjects, Block block) {
                LOG.debug("finalizing readline (console) backend");

                try {
                    holder.readline.close();
                } catch (Exception ex) {
                    LOG.debug("failed to close console reader:", ex);
                }

                try {
                    holder.readline.getTerminal().restore();
                } catch (Exception ex) {
                    LOG.debug("failed to restore terminal:", ex);
                }

                return context.nil;
            }
        };
        Block block = CallBlock.newCallClosure(Readline, Readline, Signature.NO_ARGUMENTS, callback, runtime.getCurrentContext());
        Readline.addFinalizer(RubyProc.newProc(runtime, block, block.type));
    }

    public static History getHistory(ConsoleHolder holder) {
        return holder.history;
    }

    public static ConsoleHolder getHolder(Ruby runtime) {
        return (ConsoleHolder) (runtime.getModule("Readline").dataGetStruct());
    }

    public static ConsoleHolder getHolderWithReadline(Ruby runtime) {
        ConsoleHolder holder = getHolder(runtime);
        if (holder.readline == null) {
            initReadline(runtime, holder);
        }
        return holder;
    }

    public static void setCompletor(ConsoleHolder holder, Completer completor) {
        if (holder.readline != null) {
            holder.readline.removeCompleter(holder.currentCompletor);
        }
        holder.currentCompletor = completor;
        if (holder.readline != null) {
            holder.readline.addCompleter(holder.currentCompletor);
        }
    }

    public static Completer getCompletor(ConsoleHolder holder) {
        return holder.currentCompletor;
    }

    @JRubyMethod(name = "readline", module = true, visibility = PRIVATE)
    public static IRubyObject readline(ThreadContext context, IRubyObject recv) {
        return readlineImpl(context, "", false);
    }

    @JRubyMethod(name = "readline", module = true, visibility = PRIVATE)
    public static IRubyObject readline(ThreadContext context, IRubyObject recv, IRubyObject prompt) {
        return readlineImpl(context, prompt.toString(), false);
    }

    @JRubyMethod(name = "readline", module = true, visibility = PRIVATE)
    public static IRubyObject readline(ThreadContext context, IRubyObject recv, IRubyObject prompt, IRubyObject addHistory) {
        return readlineImpl(context, prompt.toString(), addHistory.isTrue());
    }

    private static IRubyObject readlineImpl(ThreadContext context, String prompt, final boolean addHistory) {
        final Ruby runtime = context.runtime;
        ConsoleHolder holder = getHolderWithReadline(runtime);
        holder.readline.setExpandEvents(false);

        String line;
        while (true) {
            try {
                holder.readline.getTerminal().setEchoEnabled(false);
                line = holder.readline.readLine(prompt);
                break;
            } catch (IOException ioe) {
                throw runtime.newIOErrorFromException(ioe);
            } finally {
                holder.readline.getTerminal().setEchoEnabled(true);
            }
        }

        if (line == null) return context.nil;

        if (addHistory) holder.readline.getHistory().add(line);

        // Enebo: This is a little weird and a little broken.  We just ask
        // for the bytes and hope they match default_external.  This will
        // work for common cases, but not ones in which the user explicitly
        // sets the default_external to something else.  The second problem
        // is that no al M17n encodings are valid encodings in java.lang.String.
        // We clearly need a byte[]-version of JLine since we cannot totally
        // behave properly using Java Strings.
        ByteList bytes = new ByteList(line.getBytes(), runtime.getDefaultExternalEncoding());
        return RubyString.newString(runtime, bytes);
    }

    @JRubyMethod(name = "input=", module = true, visibility = PRIVATE)
    public static IRubyObject setInput(ThreadContext context, IRubyObject recv, IRubyObject input) {
        // FIXME: JRUBY-3604
        return context.runtime.getNil();
    }

    @JRubyMethod(name = "output=", module = true, visibility = PRIVATE)
    public static IRubyObject setOutput(ThreadContext context, IRubyObject recv, IRubyObject output) {
        // FIXME: JRUBY-3604
        return context.runtime.getNil();
    }

    @JRubyMethod(name = "basic_word_break_characters=", module = true, visibility = PRIVATE)
    public static IRubyObject s_set_basic_word_break_character(IRubyObject recv, IRubyObject achar) {
        Ruby runtime = recv.getRuntime();
        if (!achar.respondsTo("to_str")) {
            throw runtime.newTypeError("can't convert " + achar.getMetaClass() + " into String");
        }
        ProcCompleter.setDelimiter(achar.convertToString().toString());
        return achar;
    }

    @JRubyMethod(name = "basic_word_break_characters", module = true, visibility = PRIVATE)
    public static IRubyObject s_get_basic_word_break_character(IRubyObject recv) {
        return recv.getRuntime().newString(ProcCompleter.getDelimiter());
    }

    @JRubyMethod(name = "completion_proc", module = true, visibility = PRIVATE)
    public static IRubyObject s_get_completion_proc(IRubyObject recv) {
        Completer completer = getCompletor(getHolder(recv.getRuntime()));
        if (completer instanceof ProcCompleter) {
            return ((ProcCompleter) completer).proc;
        }
        return recv.getRuntime().getNil();
    }

    @JRubyMethod(name = "completion_proc=", module = true, visibility = PRIVATE)
    public static IRubyObject s_set_completion_proc(IRubyObject recv, IRubyObject proc) {
        if (!proc.respondsTo("call")) {
            throw recv.getRuntime().newArgumentError("argument must respond to call");
        }
        setCompletor(getHolder(recv.getRuntime()), new ProcCompleter(proc));
        return recv.getRuntime().getNil();
    }

    @JRubyMethod(name = "get_screen_size", module = true, visibility = PRIVATE)
    public static IRubyObject s_get_screen_size(ThreadContext context, IRubyObject recv) {
        Ruby runtime = context.runtime;
        ConsoleHolder holder = getHolderWithReadline(runtime);
        IRubyObject[] ary = new IRubyObject[2];
        ary[0] = runtime.newFixnum(holder.readline.getTerminal().getHeight());
        ary[1] = runtime.newFixnum(holder.readline.getTerminal().getWidth());
        return RubyArray.newArray(runtime, ary);
    }

    @JRubyMethod(name = "set_screen_size", module = true, visibility = PRIVATE)
    public static IRubyObject s_set_screen_size(ThreadContext context, IRubyObject recv, IRubyObject height, IRubyObject width) {
        final int h = height.convertToInteger().getIntValue();
        final int w = width.convertToInteger().getIntValue();
        ConsoleHolder holder = getHolderWithReadline(context.runtime);
        // NOTE: we could do a :
        // ((UnixTerminal) holder.readline.getTerminal()).getSettings().set(...);
        // ... (on *nix) - MRI seems to be silent on set_screen_size (on Linux)
        return recv;
    }

    @JRubyMethod(name = "line_buffer", module = true, visibility = PRIVATE)
    public static IRubyObject s_get_line_buffer(ThreadContext context, IRubyObject recv) {
        Ruby runtime = context.runtime;
        ConsoleHolder holder = getHolderWithReadline(runtime);
        CursorBuffer cb = holder.readline.getCursorBuffer();
        return newString(runtime, cb.buffer);
    }

    @JRubyMethod(name = "point", module = true, visibility = PRIVATE)
    public static IRubyObject s_get_point(ThreadContext context, IRubyObject recv) {
        Ruby runtime = context.runtime;
        ConsoleHolder holder = getHolderWithReadline(runtime);
        CursorBuffer cb = holder.readline.getCursorBuffer();
        return runtime.newFixnum(cb.cursor);
    }

    @JRubyMethod(name = "refresh_line", module = true, visibility = PRIVATE)
    public static IRubyObject s_refresh_line(ThreadContext context, IRubyObject recv) {
        Ruby runtime = context.runtime;
        ConsoleHolder holder = getHolderWithReadline(runtime);
        try {
            holder.readline.redrawLine(); // not quite the same as rl_refresh_line()
        } catch (IOException ioe) {
            throw runtime.newIOErrorFromException(ioe);
        }
        return context.nil;
    }

    @JRubyMethod(name = "basic_quote_characters", module = true)
    public static IRubyObject basic_quote_characters(ThreadContext context, IRubyObject recv) {
        return context.nil; // NOT IMPLEMENTED
    }

    @JRubyMethod(name = "basic_quote_characters=", module = true)
    public static IRubyObject set_basic_quote_characters(ThreadContext context, IRubyObject recv, IRubyObject chars) {
        warn(context, recv, "Readline.basic_quote_characters= not implemented");
        return context.nil; // NOT IMPLEMENTED
    }

    @JRubyMethod(name = "filename_quote_characters", module = true)
    public static IRubyObject filename_quote_characters(ThreadContext context, IRubyObject recv) {
        return context.nil; // NOT IMPLEMENTED
    }

    @JRubyMethod(name = "filename_quote_characters=", module = true)
    public static IRubyObject set_filename_quote_characters(ThreadContext context, IRubyObject recv, IRubyObject chars) {
        warn(context, recv, "Readline.filename_quote_characters= not implemented");
        return context.nil; // NOT IMPLEMENTED
    }

    @JRubyMethod(name = "completer_quote_characters", module = true)
    public static IRubyObject completer_quote_characters(ThreadContext context, IRubyObject recv) {
        return context.nil; // NOT IMPLEMENTED
    }

    @JRubyMethod(name = "completer_quote_characters=", module = true)
    public static IRubyObject set_completer_quote_characters(ThreadContext context, IRubyObject recv, IRubyObject chars) {
        warn(context, recv, "Readline.completer_quote_characters= not implemented");
        return context.nil; // NOT IMPLEMENTED
    }

    @JRubyMethod(name = "completer_word_break_characters", module = true)
    public static IRubyObject completer_word_break_characters(ThreadContext context, IRubyObject recv) {
        return context.nil; // NOT IMPLEMENTED
    }

    @JRubyMethod(name = "completer_word_break_characters=", module = true)
    public static IRubyObject set_completer_word_break_characters(ThreadContext context, IRubyObject recv, IRubyObject chars) {
        warn(context, recv, "Readline.completer_word_break_characters= not implemented");
        return context.nil; // NOT IMPLEMENTED
    }

    @JRubyMethod(name = "completion_append_character", module = true)
    public static IRubyObject completion_append_character(ThreadContext context, IRubyObject recv) {
        ConsoleHolder holder = getHolderWithReadline(context.runtime);
        CompletionHandler handler = holder.readline.getCompletionHandler();
        if (handler instanceof CandidateListCompletionHandler) {
            if (((CandidateListCompletionHandler) handler).getPrintSpaceAfterFullCompletion()) { // since JLine 2.13
                return RubyString.newString(context.runtime, " ");
            }
        }
        return context.nil;
    }

    @JRubyMethod(name = "completion_append_character=", module = true)
    public static IRubyObject set_completion_append_character(ThreadContext context, IRubyObject recv, IRubyObject achar) {
        ConsoleHolder holder = getHolderWithReadline(context.runtime);
        CompletionHandler handler = holder.readline.getCompletionHandler();
        // JLine has ' ' hard-coded on completion we support enabling/disabling ' ' :
        if (achar == context.nil) {
            setPrintSpaceAfterCompletion(handler, false);
        } else {
            IRubyObject c = achar.convertToString().op_aref(context, context.runtime.newFixnum(0));
            if (c == context.nil) { // ''
                setPrintSpaceAfterCompletion(handler, false);
            } else {
                if (c.convertToString().getByteList().charAt(0) == ' ') {
                    setPrintSpaceAfterCompletion(handler, true);
                } else {
                    // (could use a custom completion handler if its really a desired feature)
                    warn(context, recv, "Readline.completion_append_character '" + c + "' not supported");
                }
            }
        }
        return context.nil;
    }

    private static void setPrintSpaceAfterCompletion(CompletionHandler handler, boolean print) {
        if (handler instanceof CandidateListCompletionHandler) {
            ((CandidateListCompletionHandler) handler).setPrintSpaceAfterFullCompletion(print);
        }
    }

    @JRubyMethod(name = "completion_case_fold", module = true)
    public static IRubyObject completion_case_fold(ThreadContext context, IRubyObject recv) {
        return context.runtime.getModule("Readline").getConstant("COMPLETION_CASE_FOLD");
    }

    @JRubyMethod(name = "completion_case_fold=", module = true)
    public static IRubyObject set_completion_case_fold(ThreadContext context, IRubyObject recv, IRubyObject fold) {
        context.runtime.getModule("Readline").setConstant("COMPLETION_CASE_FOLD", fold, true);
        // FIXME: NOT IMPLEMENTED - this is really a noop
        return fold;
    }

    @JRubyMethod(name = { "emacs_editing_mode", "emacs_editing_mode?" }, module = true)
    public static IRubyObject emacs_editing_mode(ThreadContext context, IRubyObject recv) {
        return context.nil; // NOT IMPLEMENTED
    }

    @JRubyMethod(name = { "vi_editing_mode", "vi_editing_mode?" }, module = true)
    public static IRubyObject vi_editing_mode(ThreadContext context, IRubyObject recv) {
        return context.nil; // NOT IMPLEMENTED
    }

    public static class HistoryMethods {
        @JRubyMethod(name = {"push", "<<"}, rest = true)
        public static IRubyObject s_push(IRubyObject recv, IRubyObject[] lines) {
            ConsoleHolder holder = getHolder(recv.getRuntime());
            for (int i = 0; i < lines.length; i++) {
                RubyString line = lines[i].convertToString();
                holder.history.add(line.getUnicodeValue());
            }
            return recv;
        }

        @JRubyMethod(name = "pop")
        public static IRubyObject s_pop(IRubyObject recv) {
            Ruby runtime = recv.getRuntime();
            ConsoleHolder holder = getHolder(runtime);

            if (holder.history.isEmpty()) return runtime.getNil();

            return newString(runtime, holder.history.removeLast());
        }

        @JRubyMethod(name = "to_a")
        public static IRubyObject s_hist_to_a(IRubyObject recv) {
            Ruby runtime = recv.getRuntime();
            ConsoleHolder holder = getHolder(runtime);
            RubyArray histList = runtime.newArray(holder.history.size());

            ListIterator<History.Entry> historyIterator = holder.history.entries();
            while (historyIterator.hasNext()) {
                History.Entry nextEntry = historyIterator.next();
                histList.append(newString(runtime, nextEntry.value()));
            }

            return histList;
        }

        @JRubyMethod(name = "to_s")
        public static IRubyObject s_hist_to_s(IRubyObject recv) {
            return recv.getRuntime().newString("HISTORY");
        }

        @JRubyMethod(name = "[]")
        public static IRubyObject s_hist_get(IRubyObject recv, IRubyObject index) {
            Ruby runtime = recv.getRuntime();
            ConsoleHolder holder = getHolder(runtime);
            int i = index.convertToInteger().getIntValue();

            if (i < 0) i += holder.history.size();

            try {
                return newString(runtime, holder.history.get(i));
            } catch (IndexOutOfBoundsException ioobe) {
                throw runtime.newIndexError("invalid history index: " + i);
            }
        }

        @JRubyMethod(name = "[]=")
        public static IRubyObject s_hist_set(IRubyObject recv, IRubyObject index, IRubyObject val) {
            Ruby runtime = recv.getRuntime();
            ConsoleHolder holder = getHolder(runtime);
            int i = index.convertToInteger().getIntValue();

            if (i < 0) i += holder.history.size();

            try {
                holder.history.set(i, val.asJavaString());
            } catch (IndexOutOfBoundsException ioobe) {
                throw runtime.newIndexError("invalid history index: " + i);
            }

            return runtime.getNil();
        }

        @JRubyMethod(name = "shift")
        public static IRubyObject s_hist_shift(IRubyObject recv) {
            Ruby runtime = recv.getRuntime();
            ConsoleHolder holder = getHolder(runtime);

            if (holder.history.isEmpty()) return runtime.getNil();

            try {
                return newString(runtime, holder.history.removeFirst());
            } catch (IndexOutOfBoundsException ioobe) {
                throw runtime.newIndexError("history shift error");
            }
        }

        @JRubyMethod(name = {"length", "size"})
        public static IRubyObject s_hist_length(IRubyObject recv) {
            Ruby runtime = recv.getRuntime();
            ConsoleHolder holder = getHolder(runtime);

            return runtime.newFixnum(holder.history.size());
        }

        @JRubyMethod(name = "empty?")
        public static IRubyObject s_hist_empty_p(IRubyObject recv) {
            Ruby runtime = recv.getRuntime();
            ConsoleHolder holder = getHolder(runtime);

            return runtime.newBoolean(holder.history.isEmpty());
        }

        @JRubyMethod(name = "delete_at")
        public static IRubyObject s_hist_delete_at(IRubyObject recv, IRubyObject index) {
            Ruby runtime = recv.getRuntime();

            ConsoleHolder holder = getHolder(runtime);
            int i = RubyNumeric.num2int(index);

            if (i < 0) i += holder.history.size();
            
            try {
                return newString(runtime, holder.history.remove(i));
            } catch (IndexOutOfBoundsException ioobe) {
                throw runtime.newIndexError("invalid history index: " + i);
            }
        }

        @JRubyMethod(name = "each")
        public static IRubyObject each(ThreadContext context, IRubyObject recv, Block block) {
            ConsoleHolder holder = getHolder(context.runtime);

            for (Iterator<History.Entry> i = holder.history.iterator(); i.hasNext();) {
                block.yield(context, newString(context.runtime, i.next().value()));
            }
            return recv;
        }

        @JRubyMethod(name = "clear")
        public static IRubyObject clear(ThreadContext context, IRubyObject recv, Block block) {
            ConsoleHolder holder = getHolder(context.runtime);

            holder.history.clear();
            
            return context.nil;
        }

        @Deprecated
        public static IRubyObject s_hist_each(IRubyObject recv, Block block) {
            return each(recv.getRuntime().getCurrentContext(), recv, block);
        }

    }

    private static RubyString newString(final Ruby runtime, CharSequence str) {
        if (str instanceof RubyString) return (RubyString) str;

        RubyString s = RubyString.newString(runtime, str);
        s.setTaint(true);
        return s;
    }

    private static void warn(ThreadContext context, final IRubyObject recv, CharSequence str) {
        recv.callMethod(context, "warn", RubyString.newString(context.runtime, str));
    }

    // Complete using a Proc object
    public static class ProcCompleter implements Completer {

        final IRubyObject proc;
        //\t\n\"\\'`@$><=;|&{(
        static String[] delimiters = {" ", "\t", "\n", "\"", "\\", "'", "`", "@", "$", ">", "<", "=", ";", "|", "&", "{", "("};

        public ProcCompleter(IRubyObject proc) { this.proc = proc; }

        public static String getDelimiter() {
            StringBuilder str = new StringBuilder(delimiters.length);
            for (String delimiter : delimiters) {
                str.append(delimiter);
            }
            return str.toString();
        }

        public static void setDelimiter(String delimiter) {
            List<String> list = new ArrayList<String>();
            CharBuffer buf = CharBuffer.wrap(delimiter);
            while (buf.hasRemaining()) {
                list.add(String.valueOf(buf.get()));
            }
            delimiters = list.toArray(new String[list.size()]);
        }

        private int wordIndexOf(String buffer) {
            int index = 0;
            for (String c : delimiters) {
                index = buffer.lastIndexOf(c);
                if (index != -1) return index;
            }
            return index;
        }

        public int complete(String buffer, int cursor, List<CharSequence> candidates) {
            buffer = buffer.substring(0, cursor);
            int index = wordIndexOf(buffer);
            if (index != -1) buffer = buffer.substring(index + 1);

            Ruby runtime = proc.getRuntime();
            ThreadContext context = runtime.getCurrentContext();
            IRubyObject result = proc.callMethod(context, "call", runtime.newString(buffer));
            if (!(result instanceof RubyArray)) {
                result = result.callMethod(context, "to_a");
            }
            
            if (result instanceof List) {
                List<Object> list = (List) result;
                for (int i=0; i<list.size(); i++) {
                    Object obj = list.get(i);
                    if (obj != null) candidates.add(obj.toString());
                }
                Collections.sort((List) candidates);
            }
            return cursor - buffer.length();
        }
    }

    // Fix FileNameCompletor to work mid-line
    public static class RubyFileNameCompletor extends FileNameCompleter {
        @Override
        public int complete(String buffer, int cursor, List<CharSequence> candidates) {
            buffer = buffer.substring(0, cursor);
            int index = buffer.lastIndexOf(' ');
            if (index != -1) {
                buffer = buffer.substring(index + 1);
            }
            return index + 1 + super.complete(buffer, cursor, candidates);
        }
    }
}
