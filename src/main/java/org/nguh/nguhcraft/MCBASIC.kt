package org.nguh.nguhcraft

import net.minecraft.server.command.ServerCommandSource
import net.minecraft.text.ClickEvent
import net.minecraft.text.MutableText
import net.minecraft.text.Text
import net.minecraft.util.Formatting

/**
 * BASIC (sort of) implementation in which minecraft commands are
 * statements.
 *
 * In actuality, this programming language is more ‘basic’ than
 * ‘BASIC’, but the name still works imo.
 *
 * Yes, doing this nonsense is literally easier than trying to figure
 * out how to hook into '/execute if' and '/return', and also, those
 * are stupid because I can’t even use them to query whether a player
 * is dead or alive.
 */
object MCBASIC {
    private const val SERIALISED_STMT_SEPARATOR = "\u0001"
    private val WHITESPACE_REGEX = "\\s+".toRegex()

    /** The AST of the program. */
    private sealed class CachedAST {
        /** The program has not yet been compiled after it was last changed. */
        class NotCached() : CachedAST()

        /** The program has been compiled and is ready to execute. */
        class Cached(val Root: Block) : CachedAST()

        /** The program contains a syntax error. */
        class Error(val Err: SyntaxException) : CachedAST()
    }

    /**
     * A program that can be executed and modified.
     *
     * Programs are canonically stored as source text, which is compiled
     * and cached the first time it is executed after being modified.
     */
    class Program(
        /**
         * The source text of the program, which may not constitute
         * a syntactically valid program.
         */
        private val SourceLines: MutableList<String> = mutableListOf<String>()
    ) {

        /** The compiled representation. */
        private var AST: CachedAST = CachedAST.NotCached()

        /** Add a line to the program. */
        fun Add(Line: String) = ClearCache().also { SourceLines.add(Line.trim()) }

        /** Clear the program. */
        fun Clear() = ClearCache().also { SourceLines.clear() }

        /** Clear the cached program state. */
        fun ClearCache() { AST = CachedAST.NotCached() }

        /** Compile the program. */
        fun Compile() {
            AST = Compiler(SourceLines.joinToString("\n").trim()).CompileAST()
        }

        /** Load a program from a serialised representation. */
        fun DeserialiseFrom(S: String) = ClearCache().also {
            SourceLines.clear()
            if (!S.isEmpty()) SourceLines.addAll(S.split(SERIALISED_STMT_SEPARATOR))
        }

        /** Delete a line from the program. */
        fun Delete(Line: Int) = ClearCache().also { SourceLines.removeAt(Line) }

        /** The indicator that displays the programs state. */
        fun DisplayIndicator() = when (AST) {
            is CachedAST.Error -> "%"
            is CachedAST.Cached -> ""
            is CachedAST.NotCached -> "*"
        }

        /** Print the program. */
        fun DisplaySource(MT: MutableText, Indent: Int, ClickEventFactory: (Line: Int, Text: String) -> String): MutableText {
            val IndentStr = " ".repeat(Indent)
            if (SourceLines.isEmpty()) {
                MT.append(IndentStr).append(Text.literal("<empty>").formatted(Formatting.GRAY))
                return MT
            }

            SourceLines.forEachIndexed { I, S ->
                MT.append("\n$IndentStr[$I] ").append(Text
                    .literal(S)
                    .formatted(Formatting.AQUA)
                    .styled {
                        it.withClickEvent(
                            ClickEvent(
                                ClickEvent.Action.SUGGEST_COMMAND,
                                ClickEventFactory(I, S)
                            )
                        )
                    }
                )
            }
            return MT
        }

        /**
        * Execute the program.
        *
        * @throws Exception If there was a syntax error or an unexpected error executing the program.
        */
        @Throws(Exception::class)
        fun ExecuteAndThrow(S: ServerCommandSource) {
            when (AST) {
                is CachedAST.Cached -> Executor(S).ExecuteAST((AST as CachedAST.Cached).Root)
                is CachedAST.Error -> throw (AST as CachedAST.Error).Err
                is CachedAST.NotCached -> {
                    Compile()
                    ExecuteAndThrow(S)
                }
            }
        }

        /** Insert a line into the program. */
        fun Insert(Line: Int, Text: String) = ClearCache().also { SourceLines.add(Line, Text.trim()) }

        /** Whether the program is empty. */
        fun IsEmpty(): Boolean = SourceLines.isEmpty()

        /** The line count of the program. */
        fun LineCount(): Int = SourceLines.size

        /** Save the program as a string. */
        fun Save(): String = SourceLines.joinToString(SERIALISED_STMT_SEPARATOR)

        /** Set a line. */
        operator fun set(Line: Int, Text: String) = ClearCache().also { SourceLines[Line] = Text.trim() }
    }

    private class Executor(val Source: ServerCommandSource) {
        fun ExecuteAST(AST: Block) {
            try {
                AST.Execute(this)
            } catch (_: ReturnException) {
                // Ignored.
            } catch (T: Throwable) {
                throw T
            }
        }
    }

    /** Root of the AST class hierarchy. */
    private abstract class Stmt {
        abstract fun Execute(E: Executor)
    }

    /** A list of statements. */
    private class Block(val Statements: List<Stmt>) : Stmt() {
        override fun Execute(E: Executor) {
            for (S in Statements) S.Execute(E)
        }
    }

    /** A statement that is actually a Minecraft command. */
    private class CommandStmt(val Command: String) : Stmt() {
        override fun Execute(E: Executor) {
            val CM = E.Source.server.commandManager
            CM.execute(CM.dispatcher.parse(Command, E.Source), Command)
        }
    }

    /** A statement that returns from the current procedure or program. */
    private class ReturnStmt() : Stmt() {
        override fun Execute(E: Executor) {
            throw ReturnException.INSTANCE
        }
    }

    /**
     * Why bother implementing returning from out of several levels
     * deep in an AST that we walk recursively to evaluate it if we
     * can just cheat instead?
     *
     * In other words, this exception is thrown to return from a function.
     */
    private class ReturnException private constructor() : Exception() {
        companion object { val INSTANCE = ReturnException() }
    }

    /** A syntax or semantic error. */
    private class SyntaxException(Msg: String) : Exception(Msg)

    /** Class that compiles a program. */
    private class Compiler(val SourceCode: String) {
        private enum class TokenKind {
            // A series of characters that starts with '/'.
            Command,

            // A series of characters that starts with a backquote.
            QuotedCommand,

            // The 'return' keyword.
            Return,

            // End of file.
            Eof,
        }

        data class Token(
            val Kind: TokenKind,
            val Value: String,
        )

        var Code = SourceCode
        var Tok = Next()
        fun CompileAST(): CachedAST {
            try {
                val List = mutableListOf<Stmt>()
                while (Tok.Kind != TokenKind.Eof) CompileStmt()?.let { List.add(it) }
                return CachedAST.Cached(Block(List))
            } catch (E: SyntaxException) {
                // Find the line we’re on.
                var Line = SourceCode.take(SourceCode.length - Code.length).count { it == '\n' }
                return CachedAST.Error(SyntaxException("Near line $Line: ${E.message}"))
            }
        }

        /** Get the next token. */
        private fun Next(): Token {
            Tok = NextImpl()
            return Tok
        }

        /** Call Next() instead of this. */
        private fun NextImpl(): Token {
            Code = Code.trimStart()
            if (Code.isEmpty()) return Token(TokenKind.Eof, "")
            val C = Code.first()
            when (C) {
                '/' -> {
                    // Yeet '/'. It must be followed by a non-whitespace character.
                    Code = Code.drop(1)
                    if (Code.isEmpty() || Code.first().isWhitespace())
                        throw SyntaxException("Expected command name after '/'")

                    // The rest of the line is the command.
                    val Command = Code.takeWhile { it != '\n' }.trimEnd()
                    Code = Code.drop(Command.length + 1)
                    return Token(TokenKind.Command, Command)
                }
                '`' -> {
                    // Yeet '`' and take everything up to the next '`'.
                    Code = Code.drop(1)
                    val Command = Code.takeWhile { it != '`' }
                    Code = Code.drop(Command.length)
                    if (!Code.startsWith('`')) throw SyntaxException("Expected closing backquote")
                    Code = Code.drop(1)
                    return Token(TokenKind.QuotedCommand, Command)
                }
                else -> {
                    val Kw = Code.takeWhile { !it.isWhitespace() }
                    when (Kw) {
                        "return" -> {
                            Code = Code.drop(Kw.length)
                            return Token(TokenKind.Return, "")
                        }
                        else -> throw SyntaxException("Unexpected token: '$Kw'")
                    }
                }
            }
        }

        /**
         * Compile a statement.
         *
         * <stmt> ::= <command> | <stmt-return> | <stmt-if>
         */
        private fun CompileStmt(): Stmt? {
            when (Tok.Kind) {
                TokenKind.Eof -> return null

                /** <stmt-cmd> ::= COMMAND | QUOTED-COMMAND */
                TokenKind.Command, TokenKind.QuotedCommand -> {
                    // '/return' is not allowed because it doesn’t work the
                    // way you’d expect it to.
                    if (Tok.Value.startsWith("return"))
                        throw SyntaxException("'/return' cannot be used as a command. Use a 'return' statement instead.")
                    val Cmd = CommandStmt(Tok.Value)
                    Next()
                    return Cmd
                }

                /** <stmt-return> ::= RETURN */
                TokenKind.Return -> {
                    Next()
                    return ReturnStmt()
                }
            }
        }

        /**
         * Compile an 'if' statement.
         *
         * <stmt-if> ::= IF <expr> THEN <stmt>
         */
        /*private fun CompileIfStmt(Tokens: List<String>): Stmt {

        }*/
    }

}