package nez.tool.parser;

import nez.lang.Expression;
import nez.lang.Grammar;
import nez.lang.Production;
import nez.util.ConsoleUtils;
import nez.util.FileBuilder;
import nez.util.StringUtils;

public class CParserGenerator extends AbstractParserGenerator {

	@Override
	protected void initTypeMap() {
		this.addType("$parse", "int");
		this.addType("$tag", "int");
		this.addType("$label", "int");
		this.addType("$table", "int");
		this.addType("$arity", "int");
		this.addType("$text", "const char");
		this.addType("$index", "const unsigned short");
		this.addType("$set", "const char");
		this.addType("$string", "const char *");

		this.addType("memo", "int");
		this.addType(_set(), "const char *");/* boolean */
		this.addType(_index(), "const unsigned char *");
		this.addType(_unchoiced(), "int");/* boolean */
		this.addType(_pos(), "const char *");
		this.addType(_tree(), "size_t");
		this.addType(_log(), "size_t");
		this.addType(_table(), "size_t");
		this.addType(_state(), "ParserContext *");
	}

	@Override
	protected String _True() {
		return "1";
	}

	@Override
	protected String _False() {
		return "0";
	}

	@Override
	protected String _Null() {
		return "NULL";
	}

	/* Expression */

	@Override
	protected String _Field(String o, String name) {
		return o + "->" + name;
	}

	@Override
	protected String _Func(String name, String... args) {
		StringBuilder sb = new StringBuilder();
		sb.append("ParserContext_");
		sb.append(name);
		sb.append("(");
		sb.append(_state());
		for (int i = 0; i < args.length; i++) {
			sb.append(",");
			sb.append(args[i]);
		}
		sb.append(")");
		return sb.toString();
	}

	@Override
	protected String _text(byte[] text) {
		return super._text(text) + ", " + _int(text.length);
	}

	@Override
	protected String _text(String key) {
		if (key == null) {
			return _Null() + ", 0";
		}
		return nameMap.get(key) + ", " + _int(StringUtils.toUtf8(key).length);
	}

	@Override
	protected String _function(String type) {
		return type;
	}

	/* Statement */

	@Override
	protected void DeclConst(String type, String name, String expr) {
		Statement("static " + type + " " + name + " = " + expr);
	}

	// Grammar Generator

	@Override
	protected String getFileExtension() {
		return "c";
	}

	@Override
	protected void generateHeader(Grammar g) {
		ImportFile("/nez/tool/parser/ext/c-parser-runtime.txt");
		DeclPrototype(g);
	}

	@Override
	protected void generateFooter(Grammar g) {
		ImportFile("/nez/tool/parser/ext/c-tree-utils.txt");
		//
		BeginDecl("void* " + _ns() + "parse(const char *text, size_t len, void *thunk, void* (*fnew)(symbol_t, const char *, size_t, size_t, void *), void  (*fset)(void *, size_t, symbol_t, void *, void *), void  (*fgc)(void *, int, void *))");
		{
			VarDecl("void*", "result", _Null());
			VarDecl(_state(), "ParserContext_new(text, len)");
			Statement(_Func("initTreeFunc", "thunk", "fnew", "fset", "fgc"));
			this.InitMemoPoint();
			If(_funccall(_funcname(g.getStartProduction())));
			{
				VarAssign("result", _Field(_state(), _tree()));
				If("result == NULL");
				{
					Statement("result = c->fnew(0, text, (c->pos - text), 0, c->thunk)");
				}
				EndIf();
			}
			EndIf();
			Statement(_Func("free"));
			Return("result");
		}
		EndDecl();
		BeginDecl("long " + _ns() + "match(const char *text, size_t len)");
		{
			VarDecl("long", "result", "-1");
			VarDecl(_state(), "ParserContext_new(text, len)");
			Statement(_Func("initNoTreeFunc"));
			this.InitMemoPoint();
			If(_funccall(_funcname(g.getStartProduction())));
			{
				VarAssign("result", _cpos() + "-" + _Field(_state(), "inputs"));
			}
			EndIf();
			Statement(_Func("free"));
			Return("result");
		}
		EndDecl();
		BeginDecl("const char* " + _ns() + "tag(symbol_t n)");
		{
			Return("_tags[n]");
		}
		EndDecl();
		BeginDecl("const char* " + _ns() + "label(symbol_t n)");
		{
			Return("_labels[n]");
		}
		EndDecl();
		Line("//#ifdef USE_MAIN");
		BeginDecl("int main(int ac, const char **argv)");
		{
			Statement("void *t = " + _ns() + "parse(argv[1], strlen(argv[1]), NULL, NULL, NULL, NULL)");
			Statement("cnez_dump(t, stdout)");
			Statement("fprintf(stdout, \"\\n\")");
			Return("0");
		}
		EndDecl();
		Line("//#endif/*USE_MAIN*/");
		file.writeIndent("// End of File");
		generateHeaderFile();

	}

	private void DeclPrototype(Grammar g) {
		int c = 0;
		for (Production p : g) {
			String n = _funcname(p.getUniqueName());
			DeclPrototype(n);
			c = checkInner(p.getExpression(), c);
		}
		for (int i = 0; i < c; i++) {
			DeclPrototype("e" + i);
		}
	}

	private int checkInner(Expression e, int c) {
		if (e.size() == 1) {
			return checkInner(e.get(0), c) + 1;
		}
		for (Expression sub : e) {
			c = checkInner(sub, c);
		}
		return c;
	}

	private void DeclPrototype(String name) {
		Statement(_function("int " + name) + "(ParserContext *c)");
	}

	private void generateHeaderFile() {
		String filename = FileBuilder.changeFileExtension(this.path, "h");
		this.file = new FileBuilder(filename);
		ConsoleUtils.println("generating " + filename + " ... ");
		Statement("typedef unsigned long int symbol_t");
		int c = 1;
		for (String s : this.tagList) {
			if (s.equals("")) {
				continue;
			}
			Line("#define _" + s + " ((symbol_t)" + c + ")");
			c++;
		}
		Line("#define MAXTAG " + c);
		c = 1;
		for (String s : this.labelList) {
			if (s.equals("")) {
				continue;
			}
			Line("#define _" + s + " ((symbol_t)" + c + ")");
			c++;
		}
		Line("#define MAXLABEL " + c);
		Statement("void* " + _ns() + "parse(const char *text, size_t len, void *, void* (*fnew)(symbol_t, const char *, size_t, size_t, void *), void  (*fset)(void *, size_t, symbol_t, void *, void *), void  (*fgc)(void *, int, void *))");
		Statement("long " + _ns() + "match(const char *text, size_t len)");
		Statement("const char* " + _ns() + "tag(symbol_t n)");
		Statement("const char* " + _ns() + "label(symbol_t n)");
		this.file.close();
	}

}
