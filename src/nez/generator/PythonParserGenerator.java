package nez.generator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Stack;

import nez.lang.Expression;
import nez.lang.Production;
import nez.lang.expr.Cany;
import nez.lang.expr.Cbyte;
import nez.lang.expr.Cmulti;
import nez.lang.expr.Cset;
import nez.lang.expr.NonTerminal;
import nez.lang.expr.Pand;
import nez.lang.expr.Pchoice;
import nez.lang.expr.Pnot;
import nez.lang.expr.Pone;
import nez.lang.expr.Poption;
import nez.lang.expr.Psequence;
import nez.lang.expr.Pzero;
import nez.lang.expr.Tcapture;
import nez.lang.expr.Tdetree;
import nez.lang.expr.Tlink;
import nez.lang.expr.Tnew;
import nez.lang.expr.Treplace;
import nez.lang.expr.Ttag;
import nez.lang.expr.Xblock;
import nez.lang.expr.Xdef;
import nez.lang.expr.Xdefindent;
import nez.lang.expr.Xexists;
import nez.lang.expr.Xif;
import nez.lang.expr.Xindent;
import nez.lang.expr.Xis;
import nez.lang.expr.Xlocal;
import nez.lang.expr.Xmatch;
import nez.lang.expr.Xon;
import nez.parser.GenerativeGrammar;
import nez.parser.ParserGenerator;
import nez.util.StringUtils;
import nez.util.UList;

public class PythonParserGenerator extends ParserGenerator {

	@Override
	protected String getFileExtension() {
		return "py";
	}

	@Override
	public void makeHeader(GenerativeGrammar g) {
		W("# This file is generated by the Nez Python parser generator");
		Import("sys");
		Import("time");
		L().Func("sys.path.append", "'libnez/py/'");
		FromImport("libnez", "*");
		L();
	}

	@Override
	public void makeFooter(GenerativeGrammar g) {
		Let("argvs", "sys.argv");
		Let("argc", "len(argvs)");
		If("argc != 2").Begin().Print("Usage: python [parser_file] [input_file]").Quit().End();
		Let("f", _func("open", "argvs[1]", "'r'"));
		Let("inputs", _func("''.join", _func("f.readlines", "")));
		Let("length", _func("len", "inputs") + " - 1");
		Let("inputs", "inputs + '\\0'");
		Let("compiler", _func("ASTMachineCompiler", ""));
		Let("memoTable", _func("ElasticTable", "32", String.valueOf(memoPoint)));
		Let("parser", _func("PyNez", "inputs", "compiler, memoTable"));
		Let("start", "time.clock()");
		L("r = ").Func("parser.pFile", "True");
		L().Func("compiler.encode", "Instruction.Iret", "0", "None");
		If("r == False").Begin().Print("parse error!!").End();
		// ElIf("parser.pos != length").Begin().Print("unconsume!!").End();
		Else().Begin().Let("end", "time.clock()").Print("time = {0}[sec]", "(end - start)").Print("match!!");
		Let("machine", _func("ASTMachine", "inputs"));
		Print("\\nAST Construction:\\n");
		Let("ast", _func("machine.commitLog", "compiler.func"));
		L().Func("ast.dump", "");
		End();
	}

	@Override
	public void generate(GenerativeGrammar gg) {
		this.openOutputFile(this.getFileExtension());
		makeHeader(gg);
		Class("PyNez").Begin();
		makeParserClass();
		for (Production p : gg) {
			visitProduction(gg, p);
		}
		makeByteMap();
		End();
		makeFooter(gg);
		file.writeNewLine();
		file.flush();
	}

	protected void makeParserClass() {
		FuncDef("__init__", "self", "inputs, compiler, memoTable").Begin();
		Let("self.pos", "0");
		Let("self.inputs", "inputs");
		Let("self.inputSize", _func("len", "inputs"));
		Let("self.compiler", "compiler");
		Let("self.memoTable", "memoTable");
		End().L();
		FuncDef("charInputAt", "self").Begin();
		If("self.inputSize == self.pos").Begin().Return("None").End();
		Return("self.inputs[self.pos]").End().L();
		FuncDef("matchCharMap", "self", "map").Begin();
		Let("ch", "self.charInputAt()");
		If("ch is not None").Begin();
		If("map[ord(ch)] == True").Begin().Consume().Return("True").End();
		End();
		Return("False").End().L();
	}

	protected void makeByteMap() {
		for (Cset map : this.byteMapList) {
			L("map").W(String.valueOf(map.getId())).W(" = [");
			for (int i = 0; i < map.byteMap.length; i++) {
				if (map.byteMap[i]) {
					W("True");
				} else {
					W("False");
				}
				if (i != map.byteMap.length - 1) {
					W(", ");
				}
			}
			W("]").L();
		}
	}

	@Override
	protected PythonParserGenerator W(String word) {
		file.write(word);
		return this;
	}

	@Override
	protected PythonParserGenerator L() {
		file.writeIndent();
		return this;
	}

	@Override
	protected PythonParserGenerator L(String line) {
		file.writeIndent(line);
		return this;
	}

	@Override
	protected PythonParserGenerator Begin() {
		file.incIndent();
		return this;
	}

	@Override
	protected PythonParserGenerator End() {
		file.decIndent();
		return this;
	}

	protected PythonParserGenerator Import(String name) {
		L("import ").W(name);
		return this;
	}

	protected PythonParserGenerator FromImport(String packageName, String name) {
		L("from ").W(packageName).W(" import ").W(name);
		return this;
	}

	protected PythonParserGenerator Class(String name) {
		L("class ").W(name).W(":");
		return this;
	}

	protected String _func(String name, String... args) {
		StringBuilder sb = new StringBuilder();
		sb.append(name).append("(");
		for (int i = 0; i < args.length; i++) {
			sb.append(args[i]);
			if (i != args.length - 1) {
				sb.append(", ");
			}
		}
		sb.append(")");
		return sb.toString();
	}

	protected PythonParserGenerator FuncDef(Production p, String... args) {
		L("def p").W(p.getLocalName()).W("(");
		for (int i = 0; i < args.length; i++) {
			W(args[i]);
			if (i != args.length - 1) {
				W(", ");
			}
		}
		W("):");
		return this;
	}

	protected PythonParserGenerator FuncDef(String name, String... args) {
		L("def ").W(name).W("(");
		for (int i = 0; i < args.length; i++) {
			W(args[i]);
			if (i != args.length - 1) {
				W(", ");
			}
		}
		W("):");
		return this;
	}

	protected PythonParserGenerator Func(String name, String... args) {
		W(name).W("(");
		for (int i = 0; i < args.length; i++) {
			W(args[i]);
			if (i != args.length - 1) {
				W(", ");
			}
		}
		W(")");
		return this;
	}

	protected PythonParserGenerator Return(String ret) {
		L("return ").W(ret);
		return this;
	}

	protected PythonParserGenerator Break() {
		L("break");
		return this;
	}

	protected PythonParserGenerator Pass() {
		L("pass");
		return this;
	}

	protected PythonParserGenerator Let(String var, String e) {
		L(var).W(" = ").W(e);
		return this;
	}

	protected PythonParserGenerator While(String cond) {
		L("while ").W(cond).W(":");
		return this;
	}

	protected PythonParserGenerator If(String cond) {
		L("if ").W(cond).W(":");
		return this;
	}

	protected PythonParserGenerator ElIf(String cond) {
		L("elif ").W(cond).W(":");
		return this;
	}

	protected PythonParserGenerator Else() {
		L("else").W(":");
		return this;
	}

	protected PythonParserGenerator Print(String str) {
		L("print('").W(str).W("')");
		return this;
	}

	protected PythonParserGenerator Print(String str, String val) {
		L("print(\"").W(str).W("\".format(" + val + "))");
		return this;
	}

	protected PythonParserGenerator Quit() {
		L("quit()");
		return this;
	}

	protected PythonParserGenerator Inc(String val) {
		W(val).W(" += ").W("1");
		return this;
	}

	protected PythonParserGenerator Dec(String val) {
		W(val).W(" -= ").W("1");
		return this;
	}

	protected PythonParserGenerator Consume() {
		L().Inc("self.pos");
		return this;
	}

	protected PythonParserGenerator Fail() {
		Let("result", "False");
		return this;
	}

	protected PythonParserGenerator Succ() {
		Let("result", "True");
		return this;
	}

	protected String _match(String str) {
		return "self.inputs[self.pos]" + "== " + str;
	}

	protected PythonParserGenerator Inew() {
		L().Func("self.compiler.encode", "Instruction.Inew", "self.pos", "None");
		return this;
	}

	protected PythonParserGenerator Ileftnew() {
		L().Func("self.compiler.encode", "Instruction.Ileftnew", "self.pos", "0");
		return this;
	}

	protected PythonParserGenerator Icapture() {
		L().Func("self.compiler.encode", "Instruction.Icapture", "self.pos", "None");
		return this;
	}

	protected PythonParserGenerator Ileftcapture() {
		L().Func("self.compiler.encode", "Instruction.Ileftcapture", "self.pos", "None");
		return this;
	}

	protected PythonParserGenerator Ilink(int index) {
		L().Func("self.compiler.encode", "Instruction.Ilink", "0", String.valueOf(index));
		return this;
	}

	protected PythonParserGenerator Itag(String tag) {
		L().Func("self.compiler.encode", "Instruction.Itag", "0", "'" + tag + "'");
		return this;
	}

	protected PythonParserGenerator Ireplace(String value) {
		L().Func("self.compiler.encode", "Instruction.Ireplace", "0", "'" + value + "'");
		return this;
	}

	protected PythonParserGenerator Icall(String inst) {
		L().Let(inst, _func("self.compiler.encode", "Instruction.Icall", "0", "None"));
		return this;
	}

	protected PythonParserGenerator Iret() {
		L().Func("self.compiler.encode", "Instruction.Iret", "0", "None");
		return this;
	}

	protected PythonParserGenerator Abort() {
		L().Func("self.compiler.abort", "");
		return this;
	}

	protected PythonParserGenerator Abort(String arg) {
		L().Func("self.compiler.abortFunc", arg);
		return this;
	}

	protected PythonParserGenerator Lookup(int memoPoint) {
		Let("m", _func("self.memoTable.getMemo", "self.pos", String.valueOf(memoPoint)));
		If("m is not None").Begin();
		If("m.failed").Begin().Let("result", "False").End();
		Else().Begin();
		Let("self.pos", "self.pos + m.consumed");
		L("self.memoTable.stat.Used += 1");
		Print("memoHit");
		End();
		End();
		return this;
	}

	protected PythonParserGenerator LookupNode(int memoPoint, int index) {
		Let("m", _func("self.memoTable.getMemo", "self.pos", String.valueOf(memoPoint)));
		If("m is not None").Begin();
		If("m.failed").Begin().Let("result", "False").End();
		Else().Begin();
		L().Func("self.compiler.func.list.append", "m.inst");
		Ilink(index);
		Let("self.pos", "self.pos + m.consumed");
		L("self.memoTable.stat.Used += 1");
		End();
		End();
		return this;
	}

	protected PythonParserGenerator Memoize(int memoPoint, String pos) {
		L().Func("self.memoTable.setMemo", pos, String.valueOf(memoPoint), "not result", "None", "self.pos -" + pos);
		return this;
	}

	protected PythonParserGenerator MemoizeNode(int memoPoint, String pos, String inst) {
		L().Func("self.memoTable.setMemo", pos, String.valueOf(memoPoint), "not result", inst, "self.pos -" + pos);
		return this;
	}

	@Override
	public void visitProduction(GenerativeGrammar gg, Production r) {
		FuncDef(r, "self", "result").Begin();
		visitExpression(r.getExpression());
		Return("result").End().L();
	}

	@Override
	public void visitPempty(Expression p) {
		Pass();
	}

	@Override
	public void visitPfail(Expression p) {
		Fail();
	}

	@Override
	public void visitCany(Cany p) {
		If("self.inputs[self.pos] != '\\0'").Begin().Consume().End().Else().Begin().Fail().End();
	}

	@Override
	public void visitCbyte(Cbyte p) {
		If(_match(StringUtils.stringfyCharacter(p.byteChar))).Begin().Consume().End().Else().Begin().Fail().End();
	}

	ArrayList<Cset> byteMapList = new ArrayList<Cset>();

	@Override
	public void visitCset(Cset p) {
		if (!byteMapList.contains(p)) {
			byteMapList.add(p);
		}
		If("self.map" + p.getId() + "[ord(self.inputs[self.pos])]").Begin().Consume().End();
		Else().Begin().Fail().End();
	}

	@Override
	public void visitPoption(Poption p) {
		String pos = "pos_op" + p.getId();
		Let(pos, "self.pos");
		visitExpression(p.get(0));
		If("not result").Begin().Let("self.pos", pos).Succ().End();
	}

	@Override
	public void visitPzero(Pzero p) {
		String pos = "pos_op" + p.getId();
		While("result").Begin();
		Let(pos, "self.pos");
		visitExpression(p.get(0));
		If("not result").Begin().Break().End();
		End();
		Let("self.pos", pos).Succ();
	}

	@Override
	public void visitPone(Pone p) {
		visitExpression(p.get(0));
		If("result").Begin();
		String pos = "pos_op" + p.getId();
		While("result").Begin();
		Let(pos, "self.pos");
		visitExpression(p.get(0));
		If("not result").Begin().Break().End();
		End();
		Let("self.pos", pos).Succ();
		End();
	}

	@Override
	public void visitPand(Pand p) {
		String pos = "pos_and" + p.getId();
		Let(pos, "self.pos");
		visitExpression(p.get(0));
		Let("self.pos", pos);
	}

	@Override
	public void visitPnot(Pnot p) {
		String pos = "pos_not" + p.getId();
		Let(pos, "self.pos");
		visitExpression(p.get(0));
		Let("self.pos", pos);
		If("result").Begin().Fail().End();
		Else().Begin().Succ().End();
	}

	public void flattenSequence(Psequence seq, UList<Expression> l) {
		Expression first = seq.getFirst();
		Expression last = seq.getNext();
		if (first instanceof Psequence) {
			flattenSequence((Psequence) first, l);
			if (last instanceof Psequence) {
				flattenSequence((Psequence) last, l);
				return;
			}
			l.add(last);
			return;
		}
		l.add(first);
		if (last instanceof Psequence) {
			flattenSequence((Psequence) last, l);
			return;
		}
		l.add(last);
	}

	@Override
	public void visitPsequence(Psequence p) {
		Let("index" + p.getId(), _func("len", "self.compiler.func.list"));
		boolean isLeftNew = false;
		boolean isLink = false;
		UList<Expression> list = new UList<>(new Expression[p.size()]);
		flattenSequence(p, list);
		System.out.println(list.toString());
		for (int i = 0; i < list.size(); i++) {
			If("result").Begin();
			if (list.get(i) instanceof Tnew) {
				if (((Tnew) list.get(i)).leftFold) {
					isLeftNew = true;
				}
			}
			if (list.get(i) instanceof Tlink) {
				isLink = true;
			}
			visitExpression(list.get(i));
		}
		for (int i = list.size(); i > 0; i--) {
			End();
		}
		if (isLeftNew) {
			If("not result").Begin().Abort().End();
		} else if (isLink) {
			If("not result").Begin().Abort("index" + p.getId()).End();
		}
		isLeftNew = false;
	}

	@Override
	public void visitPchoice(Pchoice p) {
		String pos = "pos_c" + p.getId();
		Let(pos, "self.pos");
		for (int i = 0; i < p.size(); i++) {
			visitExpression(p.get(i));
			if (i < p.size() - 1) {
				If("not result").Begin().Let("self.pos", pos).Succ();
			}
		}
		for (int i = 0; i < p.size() - 1; i++) {
			End();
		}
	}

	HashMap<Integer, Integer> memoMap = new HashMap<Integer, Integer>();
	int memoPoint = 0;

	@Override
	public void visitNonTerminal(NonTerminal p) {
		Production rule = p.getProduction();
		if (rule.isNoNTreeConstruction()) {
			int memoPoint = 0;
			if (!memoMap.containsKey(p.getId())) {
				memoPoint = this.memoPoint++;
				this.memoMap.put(p.getId(), memoPoint);
			} else {
				memoPoint = memoMap.get(p.getId());
			}
			Lookup(memoPoint);
			Else().Begin();
			Let("pos" + p.getId(), "self.pos");
			Let("result", _func("self.p" + p.getLocalName(), "result"));
			Memoize(memoPoint, "pos" + p.getId());
			End();
		} else {
			Let("result", _func("self.p" + p.getLocalName(), "result"));
		}
	}

	@Override
	public void visitTlink(Tlink p) {
		int memoPoint = 0;
		if (!memoMap.containsKey(p.getId())) {
			memoPoint = this.memoPoint++;
			this.memoMap.put(p.getId(), memoPoint);
		} else {
			memoPoint = memoMap.get(p.getId());
		}
		LookupNode(memoPoint, p.index);
		Else().Begin();
		String inst = "inst" + p.getId();
		String pos = "pos" + p.getId();
		Let(pos, "self.pos");
		Icall(inst);
		visitExpression(p.get(0));
		If("result").Begin().Iret().Ilink(p.index).MemoizeNode(memoPoint, pos, inst).End();
		Else().Begin().Abort().MemoizeNode(memoPoint, pos, "None").End();
		// If("result").Begin().Iret().Ilink(p.index).End();
		// Else().Begin().Abort().End();
		End();
	}

	Stack<Boolean> markStack = new Stack<Boolean>();

	@Override
	public void visitTnew(Tnew p) {
		if (p.leftFold) {
			Ileftnew();
			markStack.push(true);
		} else {
			Inew();
			markStack.push(false);
		}
	}

	@Override
	public void visitTcapture(Tcapture p) {
		if (markStack.pop()) {
			If("result").Begin().Ileftcapture().End();
			Else().Begin().Abort().End();
		} else {
			Icapture();
		}
	}

	@Override
	public void visitTtag(Ttag p) {
		Itag(p.getTagName());
	}

	@Override
	public void visitTreplace(Treplace p) {
		Ireplace(p.value);
	}

	@Override
	public void visitXblock(Xblock p) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visitXdef(Xdef p) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visitXis(Xis p) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visitXmatch(Xmatch p) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visitXdefindent(Xdefindent p) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visitXindent(Xindent p) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visitXexists(Xexists p) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visitXlocal(Xlocal p) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visitCmulti(Cmulti p) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visitTdetree(Tdetree p) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visitXif(Xif p) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visitXon(Xon p) {
		// TODO Auto-generated method stub

	}

}
