package org.nguh.nguhcraft

import com.mojang.brigadier.exceptions.CommandSyntaxException
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
        /** The AST of the program. */
        private sealed class CachedAST {
            /** The program has not yet been compiled after it was last changed. */
            class NotCached() : CachedAST()

            /** The program has been compiled and is ready to execute. */
            class Cached(val Root: Block) : CachedAST()

            /** The program contains a syntax error. */
            class Error(val Err: SyntaxException) : CachedAST()
        }

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
            var L  = 0
            try {
                val List = mutableListOf<Stmt>()
                for (Line in SourceLines) {
                    CompileStmt(Line.trim())?.let { List.add(it) }
                    L++
                }
                AST = CachedAST.Cached(Block(List))
            } catch (E: SyntaxException) {
                AST = CachedAST.Error(SyntaxException("In line $L: ${E.message}"))
            }
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

    /** Compile a statement. */
    private fun CompileStmt(S: String): Stmt? {
        val Tokens = S.split(WHITESPACE_REGEX)
        if (Tokens.isEmpty()) return null
        val Cmd = Tokens.first()

        // This is not supported because it won’t work properly.
        if (Cmd == "/return") throw SyntaxException("Use 'return' instead of '/return'")

        // Compile a Minecraft command.
        if (Cmd.startsWith("/")) return CommandStmt(S.substring(1))

        // Compile a return statement.
        if (Cmd == "return") return CompileReturnStmt(Tokens.drop(1))
        throw SyntaxException("Unknown statement: '$S'")
    }

    /** Compile a 'return' statement. */
    private fun CompileReturnStmt(Tokens: List<String>): Stmt {
        if (Tokens.isNotEmpty()) throw SyntaxException("Unexpected tokens after 'return'")
        return ReturnStmt()
    }
}