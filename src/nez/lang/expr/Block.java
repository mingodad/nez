package nez.lang.expr;

import nez.ast.SourcePosition;
import nez.lang.Expression;
import nez.lang.GrammarReshaper;
import nez.lang.Visa;
import nez.vm.Instruction;
import nez.vm.NezEncoder;

public class Block extends Unary {
	Block(SourcePosition s, Expression e) {
		super(s, e);
	}

	@Override
	public final boolean equalsExpression(Expression o) {
		if (o instanceof Block) {
			return this.get(0).equalsExpression(o.get(0));
		}
		return false;
	}

	@Override
	public String getPredicate() {
		return "block";
	}

	@Override
	public Expression reshape(GrammarReshaper m) {
		return m.reshapeBlock(this);
	}

	@Override
	public boolean isConsumed() {
		return this.inner.isConsumed();
	}

	@Override
	public int inferTypestate(Visa v) {
		return this.inner.inferTypestate(v);
	}

	@Override
	public short acceptByte(int ch) {
		return this.inner.acceptByte(ch);
	}

	@Override
	public Instruction encode(NezEncoder bc, Instruction next, Instruction failjump) {
		return bc.encodeBlock(this, next, failjump);
	}

}