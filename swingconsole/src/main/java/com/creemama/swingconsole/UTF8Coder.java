/***** BEGIN LICENSE BLOCK *****
 * Version: EPL 2.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Eclipse Public
 * License Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/epl-v20.html
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
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

package com.creemama.swingconsole;

import static com.headius.backport9.buffer.Buffers.clearBuffer;
import static com.headius.backport9.buffer.Buffers.flipBuffer;

import java.lang.ref.SoftReference;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;

/**
 * A UTF-8 encoder and decoder.
 * <p>
 * The original code and license for this class come from <a href=
 * "https://github.com/jruby/jruby/blob/9.2.10.0/core/src/main/java/org/jruby/RubyEncoding.java">JRuby's
 * RubyEncoding class</a>.
 * </p>
 */
public class UTF8Coder {
	public static final Charset UTF8 = StandardCharsets.UTF_8;

	public static byte[] encodeUTF8(String str) {
		return encodeUTF8((CharSequence) str);
	}

	public static byte[] encodeUTF8(CharSequence str) {
		if (str.length() > CHAR_THRESHOLD) {
			return getBytes(UTF8.encode(toCharBuffer(str)));
		}
		return getBytes(getUTF8Coder().encode(str));
	}

	private static CharBuffer toCharBuffer(CharSequence str) {
		return str instanceof CharBuffer ? (CharBuffer) str : CharBuffer.wrap(str);
	}

	private static byte[] getBytes(final ByteBuffer buffer) {
		byte[] bytes = new byte[buffer.limit()];
		buffer.get(bytes);
		return bytes;
	}

	public static String decodeUTF8(byte[] bytes, int start, int length) {
		if (length > CHAR_THRESHOLD) {
			return UTF8.decode(ByteBuffer.wrap(bytes, start, length)).toString();
		}
		return getUTF8Coder().decode(bytes, start, length).toString();
	}

	public static String decodeUTF8(byte[] bytes) {
		return decodeUTF8(bytes, 0, bytes.length);
	}

	/**
	 * The maximum number of characters we can encode/decode in our cached buffers
	 */
	private static final int CHAR_THRESHOLD = 1024;

	/**
	 * UTF8Coder wrapped in a SoftReference to avoid possible ClassLoader leak. See
	 * JRUBY-6522
	 */
	private static final ThreadLocal<SoftReference<UTF8Coder>> UTF8_CODER = new ThreadLocal<>();

	private static UTF8Coder getUTF8Coder() {
		UTF8Coder coder;
		SoftReference<UTF8Coder> ref = UTF8_CODER.get();
		if (ref == null || (coder = ref.get()) == null) {
			coder = new UTF8Coder();
			UTF8_CODER.set(new SoftReference<>(coder));
		}

		return coder;
	}

	private final CharsetEncoder encoder = UTF8.newEncoder();
	private final CharsetDecoder decoder = UTF8.newDecoder();
	/**
	 * The resulting encode/decode buffer sized by the max number of characters
	 * (using 4 bytes per char possible for utf-8)
	 */
	private static final int BUF_SIZE = CHAR_THRESHOLD * 4;
	private final ByteBuffer byteBuffer = ByteBuffer.allocate(BUF_SIZE);
	private final CharBuffer charBuffer = CharBuffer.allocate(BUF_SIZE);

	UTF8Coder() {
		decoder.onMalformedInput(CodingErrorAction.REPLACE);
		decoder.onUnmappableCharacter(CodingErrorAction.REPLACE);
	}

	public final ByteBuffer encode(String str) {
		ByteBuffer buf = byteBuffer;
		CharBuffer cbuf = charBuffer;
		clearBuffer(buf);
		clearBuffer(cbuf);
		cbuf.put(str);
		flipBuffer(cbuf);
		encoder.encode(cbuf, buf, true);
		flipBuffer(buf);

		return buf;
	}

	public final ByteBuffer encode(CharSequence str) {
		ByteBuffer buf = byteBuffer;
		CharBuffer cbuf = charBuffer;
		clearBuffer(buf);
		clearBuffer(cbuf);
		// NOTE: doesn't matter is we toString here in terms of speed
		// ... so we "safe" some space at least by not copy-ing char[]
		for (int i = 0; i < str.length(); i++)
			cbuf.put(str.charAt(i));
		flipBuffer(cbuf);
		encoder.encode(cbuf, buf, true);
		flipBuffer(buf);

		return buf;
	}

	public final CharBuffer decode(byte[] bytes, int start, int length) {
		CharBuffer cbuf = charBuffer;
		ByteBuffer buf = byteBuffer;
		clearBuffer(cbuf);
		clearBuffer(buf);
		buf.put(bytes, start, length);
		flipBuffer(buf);
		decoder.decode(buf, cbuf, true);
		flipBuffer(cbuf);

		return cbuf;
	}

}
