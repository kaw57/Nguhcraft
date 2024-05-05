package org.nguh.nguhcraft.client;

import com.mojang.logging.LogUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Component;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Convert Markdown text into a Component.
 * <p>
 * This only handles inline markdown because that’s all Minecraft can render. The dialect
 * of Markdown we use is close to whatever Discord uses.
 *
 * @implNote This code was prototyped as and adapted from
 * <a href="https://github.com/Sirraide/inline-markdown-parser">this C++ implementation</a>.
 */
@Environment(EnvType.CLIENT)
public final class MarkdownParser {
    static private sealed class Node permits Emph, Span { }
    static private final class Emph extends Node {
        LinkedList<Node> Children = new LinkedList<>();
        ChatFormatting S;

        Emph(ChatFormatting S) { this.S = S; }
    }

    static private final class Span extends Node {
        /** Position of the first character this span represents. */
        int Start;

        /** Position of the first character after this span. */
        int End;

        /** Whether this is a code span. */
        boolean IsCode;

        /** Index of this span in the Nodes array, if it is present there. */
        int Index = 0;

        Span(int Start, int End) {
            this.Start = Start;
            this.End = End;
        }

        Span(int Start, int End, boolean Code) {
            this(Start, End);
            this.IsCode = Code;
        }

        /** @return The number of characters in this span. */
        int Size() { return End - Start; }
    }

    private record Delimiter(Span S, boolean CanOpen, boolean CanClose) {
        /** @return Whether this delimiter is both closing and opening. */
        boolean Clopen() { return CanOpen && CanClose; }

        /** @return How long this delimiter is; e.g. 2 for `**`. */
        int Count() { return S.Size(); }

        /** @return The delimiter kind, e.g. `*` for `**`. */
        char Kind(MarkdownParser P) { return P.MD.charAt(S.Start); }

        /** Remove `Count` characters from the start of this delimiter. */
        void Remove(int Count) { S.Start += Count; }
    }

    static private final Logger LOGGER = LogUtils.getLogger();
    static private final String ESCAPABLE = "!\"#$%&'()*+,-./:;<=>?@[\\]^_`{|}~";

    private final String MD;
    private final ArrayList<Delimiter> DelimiterStack = new ArrayList<>();
    private final ArrayList<Node> Nodes = new ArrayList<>();

    /** Render a Markdown string into a Component. */
    static public MutableComponent Render(String MD) {
        try {
            final var P = new MarkdownParser(MD);
            return P.RenderNodes(P.Nodes);
        } catch (final Exception E) {
            LOGGER.error("Error rendering markdown: {}", MD);
            E.printStackTrace();
            return Component.literal(MD);
        }
    }

    private MarkdownParser(final String MD) {
        this.MD = MD;
        DelimiterStack.add(null); // Bottom of stack.
        Parse();
        ProcessEmphasis();
    }

    /** Classify a delimiter’s properties such as openness etc. */
    private boolean ClassifyDelimiter(final int StartOfText, final Span Text) {
        // 6.2 Emphasis and strong emphasis.
        //
        // Rules rearranged slightly.
        //
        // 1. `*`/`**`
        //   1a. can open (strong) emphasis iff it is part of a left-flanking delimiter run, and
        //   1b. can close (strong) emphasis iff it is part of a right-flanking delimiter run.
        //
        // EXTENSION: `~~`/`||` behaves like `**`. Discord also seems to apply these rules for
        // '__', which CommonMark does not. We follow Discord’s behaviour here, as this also
        // simplifies the implementation.
        final char Next = Text.End < MD.length() ? MD.charAt(Text.End) : 0;
        final char Prev = Text.Start > 0 ? MD.charAt(Text.Start - 1) : 0;
        final boolean LeftFlanking = IsFlankingDelimiter(Prev, Next);
        final boolean RightFlanking = IsFlankingDelimiter(Next, Prev);

        // If this can neither open nor close, it is not a delimiter.
        if (!LeftFlanking && !RightFlanking) return false;

        // We have a delimiter; append the text we’ve read so far.
        if (StartOfText != Text.Start) Nodes.add(new Span(StartOfText, Text.Start));

        // Then, create the delimiter.
        Nodes.add(Text);
        DelimiterStack.add(new Delimiter(Text, LeftFlanking, RightFlanking));
        return true;
    }

    /** @return Whether `Closer` can be used to close `Opener` */
    private boolean DelimitersMatch(final Delimiter Opener, final Delimiter Closer) {
        // 6.2 Emphasis and strong emphasis Rule 9/10
        //
        // If one of the delimiters can both open and close emphasis, then the sum of the
        // lengths of the delimiter runs containing the opening and closing delimiters must
        // not be a multiple of 3 unless both lengths are multiples of 3.
        if (Opener.Kind(this) != Closer.Kind(this)) return false;
        if (!Opener.Clopen() && !Closer.Clopen()) return true;
        final var L1 = Opener.Count();
        final var L2 = Closer.Count();
        if (L1 % 3 == 0 && L2 % 3 == 0) return true;
        return (L1 + L2) % 3 != 0;
    }

    /**
     * Get the style for a delimiter.
     *
     * @param Kind The delimiter kind, e.g. `*` for `**`.
     * @param Strong Whether the delimiter run contains 2 or more delimiters.
     * @return The style that corresponds to a delimiter
     */
    private ChatFormatting DelimiterStyle(final char Kind, final boolean Strong) {
        return switch (Kind) {
            case '*' -> Strong ? ChatFormatting.BOLD : ChatFormatting.ITALIC;
            case '_' -> Strong ? ChatFormatting.UNDERLINE : ChatFormatting.ITALIC;
            case '~' -> ChatFormatting.STRIKETHROUGH; // Always strong.
            case '|' -> ChatFormatting.OBFUSCATED; // Always strong.
            default -> throw new IllegalArgumentException("Invalid delimiter kind: " + Kind);
        };
    }

    /**
     * Check whether a delimiter is left-flanking or right-flanking.
     *
     * @param Prev The character before the delimiter if this is a left-flanking delimiter,
     *             or after if right-flanking.
     * @param Next The character after the delimiter if this is a left-flanking delimiter,
     *             or before if right-flanking.
     * @return Whether the delimiter is left/right-flanking, depending on the parameters.
     */
    private boolean IsFlankingDelimiter(final char Prev, final char Next) {
        // 6.2 Emphasis and strong emphasis
        //
        // A left-flanking delimiter run is a delimiter run that is
        //
        //   (1) not followed by Unicode whitespace,
        //
        //   (2) and either
        //
        //       (2a) not followed by a Unicode punctuation character, or
        //
        //       (2b) followed by a Unicode punctuation character and preceded
        //            by Unicode whitespace or a Unicode punctuation character.
        //
        //   (*) For purposes of this definition, the beginning and the end of the
        //       line count as Unicode whitespace.
        //
        // -----
        //
        // In more intelligible terms, this means that:
        //
        //   1. If the delimiter is at end of text, return FALSE.
        //   2. If the next character is whitespace, return FALSE.
        //   3. If the next character is NOT punctuation, return TRUE.
        //   4. If the delimiter is at start of text, return TRUE.
        //   5. If the previous character is whitespace, return TRUE.
        //   6. If the previous character is punctuation, return TRUE.
        //   7. Otherwise, return FALSE.
        //
        // The same applies to right-flanking, but replace every occurrence of ‘next’
        // with ‘previous’ and vice versa. The algorithm implemented here is the one
        // for left-flanking delimiters. It can be used to compute the right-flanking
        // property by swapping the ‘Next’ and ‘Prev’ parameters.
        if (Next == 0) return false; // 1.
        if (IsWhitespace(Next)) return false; // 2.
        if (!IsPunctuation(Next)) return true; // 3.
        if (Prev == 0) return true; // 4.
        if (IsWhitespace(Prev)) return true; // 5.
        if (IsPunctuation(Prev)) return true; // 6.
        return false; // 7.
    }

    /** @return Whether `C` is a unicode punctuation character as per CommonMark. */
    private boolean IsPunctuation(final char C) {
        return switch (Character.getType(C)) {
            case Character.CONNECTOR_PUNCTUATION,
                 Character.DASH_PUNCTUATION,
                 Character.START_PUNCTUATION,
                 Character.END_PUNCTUATION,
                 Character.INITIAL_QUOTE_PUNCTUATION,
                 Character.FINAL_QUOTE_PUNCTUATION,
                 Character.OTHER_PUNCTUATION,
                 Character.MATH_SYMBOL,
                 Character.CURRENCY_SYMBOL,
                 Character.MODIFIER_SYMBOL,
                 Character.OTHER_SYMBOL -> true;

            default -> false;
        };
    }

    /** @return Whether `C` is a unicode whitespace character as per CommonMark. */
    private boolean IsWhitespace(final char C) {
        return switch (C) {
            case ' ', '\t', '\n', '\f', '\r' -> true;
            default -> Character.getType(C) == Character.SPACE_SEPARATOR;
        };
    }

    /** Split Markdown text into nodes and build delimiter stack. */
    private void Parse() {
        int Pos = 0;
        int StartOfText = Pos;
        final var Length = MD.length();
        parse: while (Pos < Length) {
            // 6.1 Code spans
            //
            // A backtick string is a string of one or more backtick characters
            // (`) that is neither preceded nor followed by a backtick.
            //
            // 6.2 Emphasis and strong emphasis
            //
            // A delimiter run is either
            //
            //    (1) a sequence of one or more `*` characters that is not preceded or
            //        followed by a non-backslash-escaped `*` character, or
            //
            //    (2) a sequence of one or more `_` characters that is not preceded or
            //        followed by a non-backslash-escaped `_` character.
            //
            // EXTENSION: `~~`/`||` are also a delimiters.
            final var Start = StringUtils.indexOfAny(MD.substring(Pos), "*_~|`");
            if (Start == -1) {
                Nodes.add(new Span(StartOfText, Length));
                return;
            }

            // Convert relative to absolute position.
            final var Offs = Pos + Start;

            // Check if this is escaped; to do that, read backslashes before the character;
            // note that backslashes can escape each other, so only treat this as escaped
            // if we find an odd number of backslashes.
            int Backslashes = 0;
            while (Start - Backslashes > 0 && MD.charAt(Offs - Backslashes - 1) == '\\') Backslashes++;
            if ((Backslashes & 1) != 0) {
                Pos = Offs + 1;
                continue;
            }

            // Read the rest of the delimiter.
            final char Kind = MD.charAt(Offs);
            int Count = 1;
            while (Offs + Count < Length && MD.charAt(Offs + Count) == Kind) Count++;

            // Handle backticks first; unlike emphasis, they are very straight-forward;
            // simply read ahead until we find a corresponding number of backticks (note
            // that backslash-escapes are not allowed in code spans, so we don’t even
            // have to worry about that).
            if (Kind == '`') {
                var SearchStart = Offs + Count;
                final var Run = MD.substring(Offs, Offs + Count);
                for (;;) {
                    final var End = MD.substring(SearchStart).indexOf(Run);

                    // If we don’t find a matching backtick string, then these backticks are
                    // literal; stop searching.
                    if (End == -1) {
                        // Only skip past the initial backticks.
                        Pos = Offs + Count;
                        continue parse;
                    }

                    // On the other hand, if there are extra backticks here, then this is a
                    // longer backtick string, which doesn’t match the one we’re looking for.
                    //
                    // This handles the case of e.g.: ‘` `` `’, which is ‘<code>``</code>’.
                    final var EndPos = SearchStart + End;
                    if (EndPos + Count < Length && MD.charAt(EndPos + Count) == '`') {
                        // As an optimisation, just skip past all backticks here since we
                        // know that this backtick string isn’t the end anyway.
                        int C = Count;
                        do C++;
                        while (EndPos + C < Length && MD.charAt(EndPos + C) == '`');
                        SearchStart = EndPos + C;
                        continue;
                    }

                    // Otherwise, we’ve found the end of a code span.
                    Nodes.add(new Span(StartOfText, Offs));
                    Nodes.add(new Span(Offs + Count, EndPos, true));
                    Pos = StartOfText = EndPos + Count;
                    continue parse;
                }
            }

            // EXTENSION: A single `~`/`/` is not a delimiter.
            if ((Kind == '~' || Kind == '|') && Count == 1) {
                Pos = Offs + 1;
                continue;
            }

            // Create the delimiter.
            if (ClassifyDelimiter(StartOfText, new Span(Offs, Offs + Count)))
                StartOfText = Offs + Count;

            // Move past it.
            Pos = Offs + Count;
        }

        // Append the last text node.
        if (StartOfText < Length) Nodes.add(new Span(StartOfText, Length));
    }

    /** Process emphasis and construct final AST. */
    private void ProcessEmphasis() {
        // Note: we always have an empty delimiter at the bottom of the stack.
        if (DelimiterStack.size() < 2) return;

        // Let current_position point to the element on the delimiter stack
        // just above stack_bottom (or the first element if stack_bottom is NULL).
        var CurrentPosition = 1;

        // Initialise indices for each node.
        UpdateNodeIndices(0);

        // Then we repeat the following until we run out of potential closers:
        for (;;) {
            // Move current_position forward in the delimiter stack (if needed) until
            // we find the first potential closer with delimiter * or _. (This will
            // be the potential closer closest to the beginning of the input – the
            // first one in parse order.)
            //
            // Note: can_close_strong implies can_close, so we only need to check
            // for the latter.
            while (
                    CurrentPosition != DelimiterStack.size() &&
                            !DelimiterStack.get(CurrentPosition).CanClose
            ) CurrentPosition++;

            // Out of closers.
            if (CurrentPosition == DelimiterStack.size()) return;

            // Now, look back in the stack (staying above stack_bottom and the openers_bottom
            // for this delimiter type) for the first matching potential opener (see below
            // for the definition of ‘matching’).
            final var Closer = DelimiterStack.get(CurrentPosition);
            var Opener = CurrentPosition - 1;
            var Found = false;
            while (Opener != 0) {
                Found = DelimitersMatch(DelimiterStack.get(Opener), Closer);
                if (Found) break;
                Opener--;
            }

            // If one is found:
            if (Found) {
                // Figure out whether we have emphasis or strong emphasis: if both closer and
                // opener spans have length >= 2, we have strong, otherwise regular.
                //
                // EXTENSION: Two __ are underlining instead of strong emphasis.
                final var O = DelimiterStack.get(Opener);
                final var Strong = O.Count() >= 2 && Closer.Count() >= 2;
                final var Style = DelimiterStyle(O.Kind(this), Strong);

                // Move all nodes between opener and closer into a new node.
                final var E = new Emph(Style);
                final var TextNodes = Nodes.subList(O.S.Index + 1, Closer.S.Index);
                E.Children.addAll(TextNodes);
                TextNodes.clear();
                TextNodes.add(E);

                // Remove any delimiters between the opener and closer from the delimiter stack.
                final var Delims = DelimiterStack.subList(Opener + 1, CurrentPosition);
                final var DelimsRemoved = Delims.size();
                Delims.clear();

                // Remove 1 (for regular emph) or 2 (for strong emph) delimiters from the
                // opening and closing text nodes.
                final var Count = Strong ? 2 : 1;
                O.Remove(Count);
                Closer.Remove(Count);

                // If they become empty as a result, remove them and remove the corresponding
                // element of the delimiter stack.
                if (O.Count() == 0) {
                    // Removing nodes invalidated the opener and closer, and removing the opener
                    // invalidates the closer again.
                    Opener -= DelimsRemoved;
                    CurrentPosition -= DelimsRemoved + 1;

                    // Everything after the opener has moved, but the opener
                    // index is still correct, so we can just delete it here.
                    Nodes.remove(O.S.Index);
                    DelimiterStack.remove(Opener);
                }

                // If the closing node is removed, reset current_position to the next element
                // in the stack. In any case, we need to update the node indices first.
                UpdateNodeIndices(O.S.Index);
                if (Closer.Count() == 0) {
                    Nodes.remove(Closer.S.Index);
                    DelimiterStack.remove(CurrentPosition);
                    UpdateNodeIndices(Closer.S.Index);
                }
            }

            // If none is found:
            else {
                // If the closer at current_position is not a potential opener, remove it from the
                // delimiter stack (since we know it can’t be a closer either). Advance current_position
                // to the next element in the stack.
                if (!Closer.CanOpen) DelimiterStack.remove(CurrentPosition);
                else CurrentPosition++;
            }
        }
    }

    /** Render Markdown AST to Component. */
    private MutableComponent RenderNodes(final List<Node> Nodes) {
        final var C = Component.empty();
        for (final var N : Nodes) {
            if (N instanceof final Span S) C.append(Render(S));
            else if (N instanceof final Emph E) C.append(Render(E));
        }
        return C;
    }

    /** Render an Emph to a Component. */
    private Component Render(final Emph E) {
        return RenderNodes(E.Children).withStyle(E.S);
    }

    /** Render a Span to a Component. */
    private Component Render(final Span S) {
        // Apply normalisation to code spans.
        if (S.IsCode) {
            // First, line endings are converted to spaces.
            var Text = MD.substring(S.Start, S.End).replace('\n', ' ');

            // If the resulting string both begins and ends with a space character,
            // but does not consist entirely of space characters, a single space
            // character is removed from the front and back. This allows you to include
            // code that begins or ends with backtick characters, which must be separated
            // by whitespace from the opening or closing backtick strings.
            if (
                    Text.length() > 2 &&
                            Text.charAt(0) == ' ' &&
                            Text.charAt(Text.length() - 1) == ' ' &&
                            !Text.chars().allMatch(C -> C == ' ')
            ) Text = Text.substring(1, Text.length() - 1);

            // That’s all for code spans.
            return Component.literal(Text);
        }

        // Regular text; process escapes.
        final var SB = new StringBuilder();
        final var Text = MD.substring(S.Start, S.End);
        int Pos = 0;
        int StartOfText = 0;
        for (;;) {
            var Backslash = Text.indexOf('\\', Pos);
            if (Backslash == -1 || Backslash == Text.length() - 1) {
                SB.append(Text, StartOfText, Text.length());
                break;
            }

            // 2.4 Backslash escapes
            //
            // Any ASCII punctuation character may be backslash-escaped. Backslashes
            // before other characters are treated as literal backslashes.
            final char Escaped = Text.charAt(Backslash + 1);
            if (ESCAPABLE.indexOf(Escaped) != -1) {
                SB.append(Text, StartOfText, Backslash);
                SB.append(Escaped);
                StartOfText = Backslash + 2;
            }

            // Move past the backslash.
            Pos = Backslash + 2;
        }

        // Return the processed text.
        return Component.literal(SB.toString());
    }

    /** Set the stored index in a Span to that Span’s index in the Nodes array. */
    private void UpdateNodeIndices(final int StartingAt) {
        for (int I = StartingAt; I < Nodes.size(); I++)
            if (Nodes.get(I) instanceof Span S)
                S.Index = I;
    }
}
