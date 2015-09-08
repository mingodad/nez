package nez.lang.expr;

import nez.ast.SourcePosition;
import nez.lang.Expression;
import nez.lang.GrammarReshaper;
import nez.lang.Typestate;
import nez.lang.Visa;
import nez.vm.Instruction;
import nez.vm.NezEncoder;

public class Match extends Unary {
	Match(SourcePosition s, Expression inner) {
		super(s, inner);
	}

	@Override
	public final boolean equalsExpression(Expression o) {
		if (o instanceof Match) {
			return this.get(0).equalsExpression(o.get(0));
		}
		return false;
	}

	@Override
	public String getPredicate() {
		return "~";
	}

	@Override
	public String key() {
		return "~";
	}

	@Override
	public
	final void format(StringBuilder sb) {
		this.formatUnary(sb, "~", inner);
	}

	@Override
	public Expression reshape(GrammarReshaper m) {
		return m.reshapeMatch(this);
	}

	@Override
	public boolean isConsumed() {
		return this.inner.isConsumed();
	}

	@Override
	public int inferTypestate(Visa v) {
		return Typestate.BooleanType;
	}

	@Override
	public short acceptByte(int ch) {
		return this.inner.acceptByte(ch);
	}

	@Override
	public Instruction encode(NezEncoder bc, Instruction next, Instruction failjump) {
		return this.inner.encode(bc, next, failjump);
	}

}