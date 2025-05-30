package nez.tool.peg;

import java.util.HashMap;

import nez.lang.Expression;
import nez.lang.Grammar;
import nez.lang.Nez;
import nez.lang.NonTerminal;
import nez.lang.Production;
import nez.parser.Parser;
import nez.parser.ParserStrategy;
import nez.util.FileBuilder;
import nez.util.StringUtils;
import nez.util.Verbose;

public abstract class GrammarTranslatorVisitor extends Expression.Visitor {
	protected Parser parser;
	protected ParserStrategy strategy;
	protected FileBuilder file;

	public GrammarTranslatorVisitor(FileBuilder file, Parser parser) {
		this.file = file;
		this.parser = parser;
		this.strategy = parser.getParserStrategy();
	}

	public void generate() {
		generate(this.parser.getGrammar());
	}

	public void generate(Grammar g) {
		makeHeader(g);
		makeBody(g);
		makeFooter(g);
		file.writeNewLine();
		file.flush();
	}

	public void makeHeader(Grammar g) {
	}

	public void makeBody(Grammar g) {
		for (Production p : g) {
			visitProduction(g, p);
		}
	}

	public abstract void visitProduction(Grammar g, Production p);

	public void makeFooter(Grammar g) {
	}

	/* Name */

	HashMap<String, String> m = new HashMap<String, String>();

	protected String name(String s, boolean cvtDQ) {
		String name = m.get(s);
		if (name != null) {
			return name;
		}
                if(s.charAt(0) == '"') {
                    if(s.substring(1, s.length()-1).matches("[A-Za-z_][A-Za-z0-9_]*")){
                        s = "TK_" + s.substring(1, s.length()-1);
                    }
                    else {
                        //s = "TK_" + s.substring(1, s.length()-1).;
                        String s2 = s.substring(1, s.length()-1);
                        s = "TK_";
                        for (int i = 0; i < s2.length(); i++) {
                            s += String.format("%02x", s2.codePointAt(i));
                        }
                    }
                }
		int loc = s.lastIndexOf(':');
                //need a better solution here
		if (loc > 0 && !(s.equals("\":\"") || s.equals("\"::\""))) {
			name = s.substring(loc + 1).replace("!", "_").replace("-", "PEG");
		} else {
			name = s.replace("!", "_").replace("-", "PEG");
		}
		m.put(s, name);
		return name;
	}

	protected String name(String s) {
		return name(s, false);
	}

	protected String name(Production p) {
		return name(p.getLocalName());
	}

	protected String unique(Expression e) {
		String key = e.toString() + " ";
		String unique = m.get(key);
		if (unique == null) {
			unique = "e" + m.size();
			m.put(key, unique);
		}
		return unique;
	}

	// Generator Macro

	protected String LineComment = "// ";
	protected String OpenClosure = "("; // C()
	protected String CloseClosure = ")"; // C()
	protected String ClosureDelim = ", "; // C()
	protected String BeginIndent = "{"; // Begin()
	protected String EndIndent = "}"; // End()

	@Deprecated
	protected GrammarTranslatorVisitor inc() {
		file.incIndent();
		return this;
	}

	@Deprecated
	protected GrammarTranslatorVisitor dec() {
		file.decIndent();
		return this;
	}

	public void pCommentLine(String line) {
		file.writeIndent(LineComment + line);
	}

	protected GrammarTranslatorVisitor L(String line) {
		file.writeIndent(line);
		return this;
	}

	protected GrammarTranslatorVisitor L() {
		file.writeIndent();
		return this;
	}

	protected GrammarTranslatorVisitor W(String word) {
		file.write(word);
		return this;
	}

	protected GrammarTranslatorVisitor W(Object word) {
		file.write(word.toString());
		return this;
	}

	protected GrammarTranslatorVisitor Begin(String t) {
		W(t);
		file.incIndent();
		return this;
	}

	protected GrammarTranslatorVisitor End(String t) {
		file.decIndent();
		if (t != null) {
			L(t);
		}
		return this;
	}

	protected GrammarTranslatorVisitor C(String name, Expression e) {
		int c = 0;
		W(name).W(OpenClosure);
		for (Expression sub : e) {
			if (c > 0) {
				W(ClosureDelim);
			}
			visitExpression(sub);
			c++;
		}
		W(CloseClosure);
		return this;
	}

	protected GrammarTranslatorVisitor C(String name, String first, Expression e) {
		W(name);
		W(OpenClosure);
		W(first);
		W(ClosureDelim);
		for (Expression sub : e) {
			visitExpression(sub);
		}
		W(CloseClosure);
		return this;
	}

	protected GrammarTranslatorVisitor C(String name) {
		W(name);
		W(OpenClosure);
		W(CloseClosure);
		return this;
	}

	protected GrammarTranslatorVisitor C(String name, String arg) {
		if (arg.length() > 1 && arg.startsWith("\"") && arg.endsWith("\"")) {
		} else {
			arg = StringUtils.quoteString('"', arg, '"');
		}
		W(name);
		W(OpenClosure);
		W(arg);
		W(CloseClosure);
		return this;
	}

	protected GrammarTranslatorVisitor C(String name, int arg) {
		W(name);
		W(OpenClosure);
		W(String.valueOf(arg));
		W(CloseClosure);
		return this;
	}

	protected GrammarTranslatorVisitor C(String name, boolean[] arg) {
		int cnt = 0;
		W(name);
		W(OpenClosure);
		for (int c = 0; c < arg.length; c++) {
			if (arg[c]) {
				if (cnt > 0) {
					W(ClosureDelim);
				}
				W(String.valueOf(c));
				cnt++;
			}
		}
		W(CloseClosure);
		return this;
	}

	protected final void visitExpression(Expression e) {
		e.visit(this, null);
	}

	public abstract void visitNonTerminal(NonTerminal p);

	public abstract void visitEmpty(Expression p);

	public abstract void visitFail(Expression p);

	public abstract void visitAny(Nez.Any p);

	public abstract void visitByte(Nez.Byte p);

	public abstract void visitByteSet(Nez.ByteSet p);

	public abstract void visitString(Nez.MultiByte p);

	public abstract void visitOption(Nez.Option p);

	public abstract void visitZeroMore(Nez.ZeroMore p);

	public abstract void visitOneMore(Nez.OneMore p);

	public abstract void visitAnd(Nez.And p);

	public abstract void visitNot(Nez.Not p);

	public abstract void visitPair(Nez.Pair p);

	public abstract void visitChoice(Nez.Choice p);

	// AST Construction
	public abstract void visitLink(Nez.LinkTree p);

	public abstract void visitPreNew(Nez.BeginTree p);

	public abstract void visitLeftFold(Nez.FoldTree p);

	public abstract void visitNew(Nez.EndTree p);

	public abstract void visitTag(Nez.Tag p);

	public abstract void visitReplace(Nez.Replace p);

	public abstract void visitDetree(Nez.Detree p);

	// Symbol Tables
	public abstract void visitBlockScope(Nez.BlockScope p);

	public abstract void visitLocalScope(Nez.LocalScope p);

	public abstract void visitSymbolAction(Nez.SymbolAction p);

	public abstract void visitSymbolExists(Nez.SymbolExists p);

	public abstract void visitSymbolMatch(Nez.SymbolMatch p);

	public abstract void visitSymbolPredicate(Nez.SymbolPredicate p);

	public abstract void visitIf(Nez.IfCondition p);

	public abstract void visitOn(Nez.OnCondition p);

	public final Object visit(Expression e) {
		return e.visit(this, null);
	}

	public void visitUndefined(Expression e) {
		// TODO Auto-generated method stub

	}

	@Override
	public final Object visitAny(Nez.Any p, Object a) {
		this.visitAny(p);
		return null;
	}

	@Override
	public final Object visitByte(Nez.Byte p, Object a) {
		this.visitByte(p);
		return null;
	}

	@Override
	public final Object visitByteSet(Nez.ByteSet p, Object a) {
		this.visitByteSet(p);
		return null;
	}

	@Override
	public final Object visitMultiByte(Nez.MultiByte p, Object a) {
		this.visitString(p);
		return null;
	}

	@Override
	public final Object visitFail(Nez.Fail p, Object a) {
		this.visitFail(p);
		return null;
	}

	@Override
	public final Object visitOption(Nez.Option p, Object next) {
		this.visitOption(p);
		return null;
	}

	@Override
	public final Object visitZeroMore(Nez.ZeroMore p, Object next) {
		this.visitZeroMore(p);
		return null;
	}

	@Override
	public final Object visitOneMore(Nez.OneMore p, Object a) {
		this.visitOneMore(p);
		return null;
	}

	@Override
	public final Object visitAnd(Nez.And p, Object a) {
		this.visitAnd(p);
		return null;
	}

	@Override
	public final Object visitNot(Nez.Not p, Object a) {
		this.visitNot(p);
		return null;
	}

	@Override
	public final Object visitPair(Nez.Pair p, Object a) {
		this.visitPair(p);
		return null;
	}

	@Override
	public final Object visitSequence(Nez.Sequence p, Object a) {
		Verbose.TODO("supporting Sequence");
		return null;
	}

	@Override
	public final Object visitChoice(Nez.Choice p, Object a) {
		this.visitChoice(p);
		return null;
	}

	@Override
	public final Object visitDispatch(Nez.Dispatch p, Object a) {
		return null;
	}

	@Override
	public final Object visitNonTerminal(NonTerminal p, Object a) {
		this.visitNonTerminal(p);
		return null;
	}

	// AST Construction

	@Override
	public final Object visitDetree(Nez.Detree p, Object a) {
		if (strategy.TreeConstruction) {
			this.visitDetree(p);
		} else {
			this.visitExpression(p.get(0));
		}
		return null;
	}

	@Override
	public final Object visitLinkTree(Nez.LinkTree p, Object a) {
		if (strategy.TreeConstruction) {
			this.visitLink(p);
		} else {
			this.visitExpression(p.get(0));
		}
		return null;
	}

	@Override
	public final Object visitBeginTree(Nez.BeginTree p, Object next) {
		if (strategy.TreeConstruction) {
			this.visitPreNew(p);
		}
		return null;
	}

	@Override
	public final Object visitFoldTree(Nez.FoldTree p, Object next) {
		if (strategy.TreeConstruction) {
			this.visitLeftFold(p);
		}
		return null;
	}

	@Override
	public final Object visitEndTree(Nez.EndTree p, Object next) {
		if (strategy.TreeConstruction) {
			this.visitNew(p);
		}
		return null;
	}

	@Override
	public final Object visitTag(Nez.Tag p, Object next) {
		if (strategy.TreeConstruction) {
			this.visitTag(p);
		}
		return null;
	}

	@Override
	public final Object visitReplace(Nez.Replace p, Object next) {
		if (strategy.TreeConstruction) {
			this.visitReplace(p);
		}
		return null;
	}

	@Override
	public final Object visitBlockScope(Nez.BlockScope p, Object a) {
		this.visitBlockScope(p);
		return null;
	}

	@Override
	public final Object visitLocalScope(Nez.LocalScope p, Object a) {
		this.visitLocalScope(p);
		return null;
	}

	@Override
	public final Object visitSymbolAction(Nez.SymbolAction p, Object a) {
		this.visitSymbolAction(p);
		return null;
	}

	@Override
	public final Object visitSymbolExists(Nez.SymbolExists p, Object a) {
		this.visitSymbolExists(p);
		return null;
	}

	@Override
	public final Object visitSymbolMatch(Nez.SymbolMatch p, Object a) {
		this.visitSymbolMatch(p);
		return null;
	}

	@Override
	public final Object visitSymbolPredicate(Nez.SymbolPredicate p, Object a) {
		this.visitSymbolPredicate(p);
		return null;
	}

	@Override
	public final Object visitScan(Nez.Scan p, Object a) {
		// this.visitSymbolMatch(p);
		return null;
	}

	@Override
	public final Object visitRepeat(Nez.Repeat p, Object a) {
		// this.visitSymbolPredicate(p);
		return null;
	}

	@Override
	public final Object visitLabel(Nez.Label p, Object a) {
		// this.visitSymbolPredicate(p);
		return null;
	}

	@Override
	public final Object visitEmpty(Nez.Empty p, Object a) {
		this.visitEmpty(p);
		return null;
	}

	@Override
	public final Object visitOn(Nez.OnCondition p, Object a) {
		this.visitOn(p);
		return null;
	}

	@Override
	public final Object visitIf(Nez.IfCondition p, Object a) {
		this.visitIf(p);
		return null;
	}

}
