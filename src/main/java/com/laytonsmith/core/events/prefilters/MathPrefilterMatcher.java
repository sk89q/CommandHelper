package com.laytonsmith.core.events.prefilters;

import com.laytonsmith.core.ArgumentValidation;
import com.laytonsmith.core.ParseTree;
import com.laytonsmith.core.compiler.CompilerEnvironment;
import com.laytonsmith.core.compiler.CompilerWarning;
import com.laytonsmith.core.constructs.CNumber;
import com.laytonsmith.core.constructs.Target;
import com.laytonsmith.core.environments.Environment;
import com.laytonsmith.core.events.BindableEvent;
import com.laytonsmith.core.exceptions.ConfigCompileException;
import com.laytonsmith.core.exceptions.ConfigCompileGroupException;
import com.laytonsmith.core.exceptions.ConfigRuntimeException;
import com.laytonsmith.core.natives.interfaces.Mixed;

/**
 *
 * @param <T>
 */
public abstract class MathPrefilterMatcher<T extends BindableEvent> implements PrefilterMatcher<T> {

	@Override
	public String filterType() {
		return "math match";
	}

	@Override
	public void validate(ParseTree node, Environment env) throws ConfigCompileException, ConfigCompileGroupException, ConfigRuntimeException {
		if(!node.getType(env).doesExtend(CNumber.TYPE)) {
			env.getEnv(CompilerEnvironment.class).addCompilerWarning(node.getFileOptions(),
				new CompilerWarning("Expecting a number here, this may not perform as expected.",
						node.getTarget(), null));
		}
	}

	@Override
	public boolean matches(Mixed value, T event, Target t) {
		double val = ArgumentValidation.getDouble(value, t);
		return Math.abs(val - getProperty(event)) <= getTolerance();
	}

	protected abstract double getProperty(T event);

	protected abstract double getTolerance();



}
