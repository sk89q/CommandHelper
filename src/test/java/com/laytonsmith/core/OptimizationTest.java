package com.laytonsmith.core;

import com.laytonsmith.PureUtilities.SimpleVersion;
import com.laytonsmith.core.compiler.CompilerEnvironment;
import com.laytonsmith.core.compiler.CompilerWarning;
import com.laytonsmith.core.compiler.OptimizationUtilities;
import com.laytonsmith.core.constructs.Target;
import com.laytonsmith.core.environments.Environment;
import com.laytonsmith.core.exceptions.ConfigCompileException;
import com.laytonsmith.core.exceptions.ConfigCompileGroupException;
import com.laytonsmith.testing.StaticTest;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.junit.Assert;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * This class tests optimizations by looking at the tree after optimization occurs to see if it matches expectation.
 *
 * This is also used to test the lexer/compiler at a low level
 */
public class OptimizationTest {

	static Set<Class<? extends Environment.EnvironmentImpl>> envs = Environment.getDefaultEnvClasses();
	static Environment env;

	@BeforeClass
	public static void setUpClass() {
		StaticTest.InstallFakeServerFrontend();
	}

	public static String optimize(String script, boolean pureMethodScript, Environment env) throws Exception {
		try {
			try {
				return OptimizationUtilities.optimize(script, env, envs, null, false, pureMethodScript);
			} catch(ConfigCompileException ex) {
				throw new ConfigCompileGroupException(new HashSet<>(Arrays.asList(ex)), ex);
			}
		} catch(ConfigCompileGroupException ex) {
			Target t = new ArrayList<>(ex.getList()).get(0).getTarget();
			String msg = ex.getList().toString() + " " + t;
			throw new ConfigCompileException(msg, t, ex);
		}
	}

	public String optimize(String script) throws Exception {
		return optimize(script, true);
	}

	public String optimize(String script, boolean pureMethodScript) throws Exception {
		env = Static.GenerateStandaloneEnvironment();
		return optimize(script, pureMethodScript, env);
	}

	@Test
	public void testTestFramework() throws Exception {
		//This just tests to see that the basic framework works. This shouldn't optimize.
		assertEquals("msg('this is a string','so is this')", optimize("msg(\n 'this is a string',\nso is this\n)"));
		assertEquals("msg('\\'quoted\\'')", optimize("msg( '\\'quoted\\'' )"));
	}

	@Test
	public void testIfBasic() throws Exception {
		assertEquals("msg('hi')", optimize("if(true){ msg('hi') } else { msg('fail') }"));
	}

	@Test
	public void testIfWithBraces() throws Exception {
		assertEquals("if(dyn(),msg('hi'),msg('hi'))", optimize("if(dyn()){ msg('hi') } else { msg('hi') }"));
	}

	@Test
	public void testIfElse() throws Exception {
		assertEquals("ifelse(dyn(1),msg(''),dyn(2),msg(''),msg(''))", optimize("if(dyn(1)){ msg('') } else if(dyn(2)){ msg('') } else { msg('') }"));
	}

	@Test
	public void testIfElseWithDie() throws Exception {
		assertEquals("ifelse(is_null($pl),die(''),not(dyn($pl)),die(concat($pl,'')))",
				optimize("if(is_null($pl)) {\ndie('') } else if(!dyn($pl)){ die($pl.'') }"));
	}

	// Need to add this back too
//	@Test public void testNestedIfsWithRemoval() throws Exception {
//		assertEquals("p()", optimize("ifelse(1, if(0, msg('')), msg(''))"));
//	}
	@Test
	public void testMultipleLinesInBraces() throws Exception {
		assertEquals("if(dyn(false),msg('nope'),sconcat(msg('hi'),msg('hi')))", optimize("if(dyn(false)){\n"
				+ "msg('nope')\n"
				+ "} else {\n"
				+ " msg('hi')\n"
				+ " msg('hi')\n"
				+ "}"));
	}

	//TODO: These tests are not intended to be corrected in master, so I'm removing them for now
//	@Test public void testProcOptimization1() throws Exception{
//		//The proc stays there, but the call to it should be consolidated
//		assertEquals("sconcat(proc('_add',@a,@b,return(add(@a,@b))),4)", optimize("proc(_add, @a, @b, return(@a + @b)) _add(2, 2)"));
//	}
	@Test
	public void testProcOptimizationRecursion() throws Exception {
		assertEquals("sconcat(__statements__(proc('_loop',@a,if(gt(@a,0),_loop(subtract(@a,1)),return(@a)))),_loop(2))",
				optimize("proc(_loop, @a, if(@a > 0, _loop(@a - 1), return(@a))) _loop(2)"));
	}

//	@Test(expected=ConfigCompileException.class)
//	public void testProcOptimization2() throws Exception{
//		optimize("proc(_divide, @a, return(@a / 0)) _divide(1)");
//	}
	@Test
	public void testProcOptimization3() throws Exception {
		//Rather, lack of optimization
		assertEquals("sconcat(__statements__(proc('_nope',msg('Hi'))),_nope())",
				optimize("proc(_nope, msg('Hi')) _nope()"));
	}

//	@Test
//	public void testProcOptimiztion4() throws Exception{
//		//Test embedded procs
//		assertEquals("sconcat(proc('_outer',sconcat(proc('_inner',@a,return(@a)),'blah')),_inner('huh'))",
//				optimize("proc(_outer, proc(_inner, @a, return(@a)) _inner('blah')) _inner('huh')"));
//	}
	@Test
	public void testProcReturn() throws Exception {
		assertEquals("sconcat(__statements__(proc('_proc',return(array(1)))),array_get(_proc(),0))",
				optimize("proc(_proc, return(array(1))) _proc()[0]"));
	}

	@Test
	public void testClosure() throws Exception {
		assertEquals("__statements__(assign(@c,closure(@target,msg(concat('Hello ',@target,'!')))),execute('world',@c))",
				optimize("@c = closure(@target) {msg('Hello '.@target.'!')}; @c('world');"));
	}

	@Test
	public void testUnreachableCode() throws Exception {
		assertEquals("if(dyn(0),sconcat(die()),sconcat(msg('2'),msg('3')))",
				optimize("if(dyn(0)){ die() msg('1') } else { msg('2') msg('3') }"));
		assertEquals("sconcat(die())", optimize("if(true){ die() msg('1') } else { msg('2') msg('3') }"));
	}

	@Test
	public void testUnreachableCodeWithBranchTypeFunction() throws Exception {
		assertEquals("if(@var,die(),msg(''))", optimize("if(@var){ die() } else { msg('') }"));
		assertEquals("__statements__(while(lt(rand(),0.5),__statements__(die())),msg('survived'))",
				optimize("while(rand() < 0.5) { die(); } msg('survived');"));
	}

	@Test
	public void testUnreachableCodeComplex() throws Exception {
		assertEquals("__statements__(assign(@a,closure(__statements__(return(5)))),execute(@a))",
				optimize("@a = closure(){"
						+ "return(5);"
						+ "}"
						+ "execute(@a);"));
		assertEquals("__statements__(msg('a'),if(dyn(1),ifelse(dyn(1),__statements__(die()),dyn(2),"
				+ "__statements__(die()),__statements__(die())),__statements__(msg('b'))))",
				optimize("msg('a');"
						+ "if(dyn(1)){"
						+ "	if(dyn(1)){"
						+ "		die();"
						+ "		msg('bad');"
						+ "	} else if(dyn(2)){"
						+ "		die();"
						+ "		msg('bad');"
						+ "	} else {"
						+ "		die();"
						+ "		msg('bad');"
						+ "	}"
						+ "} else {"
						+ "	msg('b');"
						+ "}"));
		assertEquals("__statements__(msg('a'),die())",
				optimize("msg('a');"
						+ "die();"
						+ "if(dyn(1)){"
						+ "	if(dyn(1)){"
						+ "		die();"
						+ "		msg('bad');"
						+ "	} else if(dyn(2)){"
						+ "		die();"
						+ "		msg('bad');"
						+ "	} else {"
						+ "		die();"
						+ "		msg('bad');"
						+ "	}"
						+ "} else {"
						+ "	msg('bad');"
						+ "}"));
	}

	@Test
	public void testInnerDie() throws Exception {
		// Since p is not a branch function, we expect a die inside of that to bubble up
		assertEquals("__statements__(concat(die()))",
				optimize("p(concat(die(), msg('bad'))); msg('bad');"));
	}

	@Test
	public void testRegSplitOptimization1() throws Exception {
		assertEquals("split('pattern',dyn('subject'))", optimize("reg_split('pattern', dyn('subject'))"));
	}

	@Test
	public void testRegSplitOptimization2() throws Exception {
		assertEquals("split('.',dyn('subject'))", optimize("reg_split(reg_escape('.'), dyn('subject'))"));
	}

	@Test
	public void testRegReplaceOptimization1() throws Exception {
		assertEquals("replace('this is a thing','thing','hi')", optimize("reg_replace('thing', 'hi', 'this is a thing')"));
	}

	@Test
	public void testTrivialAssignmentWithEqualsSymbol() throws Exception {
		assertEquals("assign(@a,1)", optimize("@a = 1"));
	}

	@Test
	public void testAssignWithEqualsSymbol() throws Exception {
		assertEquals("sconcat(assign(@var,'ab'),'c')", optimize("@var = 'a'.'b' 'c'"));
	}

	@Test
	public void testAssignWithOperators() throws Exception {
		assertEquals("assign(@one,add(@one,1))", optimize("@one += 1"));
		assertEquals("assign(@one,subtract(@one,1))", optimize("@one -= 1"));
		assertEquals("assign(@one,multiply(@one,1))", optimize("@one *= 1"));
		assertEquals("assign(@one,divide(@one,1))", optimize("@one /= 1"));
		assertEquals("assign(@one,concat(@one,1))", optimize("@one .= 1"));
	}

	@Test
	public void testMultiAssign() throws Exception {
		assertEquals("assign(@one,assign(@two,''))", optimize("@one = @two = ''"));
		assertEquals("sconcat(assign(@one,assign(@two,'')),'blah')", optimize("@one = @two = '' 'blah'"));
	}

	@Test
	public void testAssignWithOr() throws Exception {
		assertEquals("assign(@one,or(@two,not(@three)))",
				optimize("@one = @two || !@three"));
	}

	@Test
	public void testAssignWithInc() throws Exception {
		assertEquals("assign(@one,or(inc(@two),inc(@three)))",
				optimize("@one = ++@two || ++@three"));
	}

	@Test
	public void testAssignWithEquals() throws Exception {
		assertEquals("assign(@one,and(@two,equals(postinc(@three),neg(@four))))",
				optimize("@one = @two && @three++ == -@four"));
	}

	@Test
	public void testAssignWithComplexSymbols() throws Exception {
		assertEquals("assign(@one,or(postinc(@one),add(inc(@two),@three)))",
				optimize("@one = @one++ || ++@two + @three"));
	}

	@Test
	public void testMultipleAdjacentAssignment() throws Exception {
		assertEquals("__statements__(assign(@one,inc(@two)),assign(ms.lang.int,@three,0),assign(@four,'test'))",
				optimize("@one = ++@two; int @three = 0; @four = 'test';"));
	}

	@Test
	public void testAdditiveAssignmentWithInc() throws Exception {
		assertEquals("assign(@one,add(@one,subtract(inc(@two),inc(@three))))",
				optimize("@one += ++@two - ++@three"));
	}

	@Test
	public void testAssignmentMixedWithAddition1() throws Exception {
		assertEquals("add(1,assign(@a,1))", optimize("1 + @a = 1"));
	}

	@Test
	public void testAssignmentMixedWithAddition2() throws Exception {
		assertEquals("add(1,assign(@a,add(@b,2)))", optimize("1 + @a = @b + 2"));
	}

	@Test
	public void testAssignmentMixedWithAddition3() throws Exception {
		assertEquals("add(1,assign(@a,add(@b,@c,2)))", optimize("1 + @a = @b + @c + 2"));
	}

	@Test
	public void testAssignmentMixedWithAddition4() throws Exception {
		assertEquals("add(1,assign(@a,add(@a,@b,@c,2)))", optimize("1 + @a += @b + @c + 2"));
	}

	@Test
	public void testAssignmentMixedWithAddition5() throws Exception {
		assertEquals("add(1,assign(@_,assign(@a,add(@a,@b,@c,2))))", optimize("1 + @_ = @a += @b + @c + 2"));
	}

	@Test
	public void testAssignmentMixedWithAddition6() throws Exception {
		assertEquals("sconcat(add(1,assign(@_,assign(@a,add(@a,@b,@c,2)))),'blah')", optimize("1 + @_ = @a += @b + @c + 2 'blah'"));
	}

	@Test
	public void testAssignmentMixedWithAddition7() throws Exception {
		assertEquals("add(@one,assign(@one,inc(@two)),@three)",
				optimize("@one + (@one = ++@two) + @three"));
	}

	@Test
	public void testInnerIfAnded() throws Exception {
		assertEquals("if(and(@a,@b),msg(''))", optimize("if(@a){ if(@b){ msg('') } }"));
	}

	@Test
	public void testInnerIfWithOtherStatements1() throws Exception {
		assertEquals("if(@a,if(@b,msg(''),msg('')))", optimize("if(@a){ if(@b){ msg('') } else { msg('') } }"));
	}

	@Test
	public void testInnerIfWithOtherStatements2() throws Exception {
		assertEquals("if(@a,sconcat(if(@b,msg('')),msg('')))", optimize("if(@a){ if(@b){ msg('') } msg('') }"));
	}

	@Test
	public void testInnerIfWithExistingAnd() throws Exception {
		assertEquals("if(and(@a,@b,@c),msg(''))", optimize("if(@a && @b){ if(@c){ msg('') } }"));
	}

	@Test
	public void testForWithPostfix() throws Exception {
		assertEquals("__statements__(for(assign(@i,0),lt(@i,5),inc(@i),msg('')))",
				optimize("for(@i = 0, @i < 5, @i++, msg(''))"));
		assertEquals("__statements__(for(assign(@i,0),lt(@i,5),dec(@i),msg('')))",
				optimize("for(@i = 0, @i < 5, @i--, msg(''))"));
	}

	// Need to add this back too
//	@Test public void testIfelseWithInnerDynamic() throws Exception{
//		assertEquals("if(dyn(),msg('success'))", optimize("ifelse(1, if(dyn(), msg('success')),msg('fail'))"));
//	}
	@Test
	public void testAndOrPullsUp() throws Exception {
		assertEquals("or(dyn(),dyn(),dyn())", optimize("dyn() || dyn() || dyn()"));
		assertEquals("and(dyn(),dyn(),dyn())", optimize("dyn() && dyn() && dyn()"));
	}

	@Test
	public void testAndRemovesTrues() throws Exception {
		assertEquals("and(dyn(),dyn())", optimize("and(true, dyn(), true, dyn())"));
		assertEquals("true", optimize("and(true, true, true)"));
	}

	@Test
	public void testOrRemovesFalses() throws Exception {
		assertEquals("or(dyn(),dyn())", optimize("or(false, dyn(), false, dyn())"));
		assertEquals("false", optimize("or(false, false, false)"));
	}

	// This will be a nice optimization to add back soon
//	@Test public void testNoOperationIf() throws Exception {
//		assertEquals("g(dyn(1))", optimize("if(dyn(1)){ }"));
//	}
	//This won't work as easily, because the reg_count has already been evaluated
//	@Test public void testNoSideEffectsRemovesUnusedBranches1() throws Exception {
//		assertEquals("msg('hi')", optimize("if(reg_count('hi', 'hi')){ } msg('hi')"));
//	}
	// This is actually invalid, because the dyn has side effects (or rather, isn't registered to not
	// have them). ALL nodes in the condition tree must have no side effects for this to be valid. For
	// now, this isn't implemented anyways.
//	@Test public void testNoSideEffectsRemovesUnusedBranches2() throws Exception {
//		assertEquals("msg('hi')", optimize("if(reg_count('hi', dyn('hi'))){ } msg('hi')"));
//	}
	//tests the new switch syntax
	@Test
	public void testSwitch1() throws Exception {
		assertEquals("__statements__(switch(@a,array(1,2),__statements__(msg('1, 2')),"
				+ "3..4,__statements__(msg('3'),msg('4')),"
				+ "false,__statements__(msg('false')),"
				+ "0.07,__statements__(msg(0.07)),"
				+ "__statements__(msg('default'))))",
				optimize("switch(@a){"
						+ "	case 1:"
						+ "	case 2:"
						+ "		msg('1, 2');"
						+ "	case 3..4:"
						+ "		msg('3');"
						+ "		msg('4');"
						+ "	case false:"
						+ "		msg('false');"
						+ "	case 00.07:"
						+ "		msg(00.07);"
						+ "	case 'ignored':"
						+ "	default:"
						+ "		msg('default');"
						+ "}"));
	}

	@Test
	public void testSwitchInSwitch() throws Exception {
		assertEquals("__statements__(switch(dyn(1),2,switch(dyn(4),5,__statements__(msg('hi')),6,__statements__(msg('hi'))),3,__statements__(msg('hi'))))",
				optimize("switch(dyn(1)) {\n"
						+ "case 2:\n"
						+ "		switch(dyn(4)) {\n"
						+ "			case 5:\n"
						+ "				msg('hi');\n"
						+ "			case 6:\n"
						+ "				msg('hi');\n"
						+ "		}\n"
						+ "case 3:\n"
						+ "		msg('hi');\n"
						+ "}\n"));
	}

	@Test(expected = ConfigCompileException.class)
	public void testSwitch2() throws Exception {
		optimize("switch(@a){"
				+ "	case 1:"
				+ "	case 0..2:"
				+ "		msg('invalid');"
				+ "}");
	}

	@Test
	public void testEmptySwitch() throws Exception {
		assertEquals("__statements__(switch(dyn(1)))",
				optimize("switch(dyn(1)){ case 1: case 2: default: }"));
	}

	@Test
	public void testDuplicatedDefault() throws Exception {
		// TODO: When typechecking is enabled globally, this will break, because msg (will eventually) return void
		assertEquals("switch(dyn(1),msg('hello'))",
				optimize("switch(dyn(1)) { case 1: case 2: default: msg('hello') }"));
	}

	// Tests "-" signs in front of values to negate them.
	@Test
	public void testMinusWithoutValueInFront() throws Exception {
		assertEquals("assign(@b,neg(@a))", optimize("@b = -@a"));
		assertEquals("assign(@b,neg(@a))", optimize("@b = - @a"));

		assertEquals("assign(@b,array(neg(@a)))", optimize("@b = array(-@a)"));
		assertEquals("assign(@b,array(neg(@a)))", optimize("@b = array(- @a)"));

		assertEquals("assign(@b,neg(array_get(@a,1)))", optimize("@b = -@a[1]"));
		assertEquals("assign(@b,neg(array_get(@a,1)))", optimize("@b = - @a[1]"));

		assertEquals("assign(@b,neg(dec(@a)))", optimize("@b = -dec(@a)"));
		assertEquals("assign(@b,neg(dec(@a)))", optimize("@b = - dec(@a)"));

		assertEquals("assign(@b,neg(array_get(array(1,2,3),1)))", optimize("@b = -array(1,2,3)[1]"));

		assertEquals("assign(@b,neg(array_get(array_get(array_get(array(array(array(2))),0),0),0)))", optimize("@b = -array(array(array(2)))[0][0][0]"));

		assertEquals("assign(@b,neg(array_get(array_get(array_get(array(array(array(2))),neg(array_get(array(1,0),1))),0),0)))",
				optimize("@b = -array(array(array(2)))[-array(1,0)[1]][0][0]"));

		// Test behaviour where the value should not be negated.
		assertEquals("assign(@c,subtract(@a,@b))", optimize("@c = @a - @b"));
		assertEquals("assign(@c,subtract(array_get(@a,0),@b))", optimize("@c = @a[0] - @b"));
		assertEquals("assign(@c,subtract(abs(@a),@b))", optimize("@c = abs(@a) - @b"));
		assertEquals("assign(@c,subtract(if(@bool,2,3),@b))", optimize("@c = if(@bool) {2} else {3} - @b"));

		assertEquals("assign(@b,subtract(dec(@a),2))", optimize("@b = dec(@a)-2"));
		assertEquals("assign(@b,subtract(dec(@a),2))", optimize("@b = dec(@a)- 2"));
	}

	//TODO: This is a bit ambitious for now, put this back at some point, and then make it pass.
//	@Test public void testAssign() throws Exception{
//		//In this test, there's no way it won't ever be 'hi', so do a replacement (we still need to keep
//		//the assign, because it does need to go into the variable table for reflective purposes)
//		assertEquals("sconcat(assign(@a,'hi'),msg('hi'))", optimize("assign(@a, 'hi') msg(@a)"));
//		//In this case, the first use may be hardcoded, but after the if, it may have changed, so we
//		//can no longer assume it's always going to be 'hi'
//		assertEquals("sconcat(assign(@a,'hi'),msg('hi'),if(dyn(),assign(@a,'bye')),msg(@a))",
//				optimize(""
//				+ "assign(@a, 'hi')"
//				+ "msg(@a)"
//				+ "if(dyn(), assign(@a, 'bye'))"
//				+ "msg(@a)"));
//		//In this case, we have a worthless assignment; We know @a is already 'hi' and it's always going
//		//to be that, and we're trying to assign 'hi' again, so we can completely remove this from
//		//the code, at which point the last msg can be optimized.
//		assertEquals("sconcat(assign(@a,'hi'),msg('hi'),if(dyn(),null),msg('hi'))",
//				optimize(""
//				+ "assign(@a, 'hi')"
//				+ "msg(@a)"
//				+ "if(dyn(), assign(@a, 'hi'))"
//				+ "msg(@a)"));
//	}
	@Test
	public void testNotinstanceofKeyword() throws Exception {
		assertEquals("__statements__(msg(not(instanceof(dyn(2),ms.lang.int))))", optimize("msg(dyn(2) notinstanceof int);"));
	}

	@Test
	public void testDor() throws Exception {
		assertEquals("dor(dyn(''),dyn('a'))", optimize("dyn('') ||| dyn('a')"));
		assertEquals("dor(dyn(''),dyn('a'),dyn('b'))", optimize("dyn('') ||| dyn('a') ||| dyn('b')"));
	}

	@Test
	public void testDand() throws Exception {
		assertEquals("dand(dyn(''),dyn('a'))", optimize("dyn('') &&& dyn('a')"));
		assertEquals("dand(dyn(''),dyn('a'),dyn('b'))", optimize("dyn('') &&& dyn('a') &&& dyn('b')"));
	}

//	@Test
//	public void testDorOptimization() throws Exception {
//		assertEquals("'a'", optimize("dor(false, false, 'a')"));
//	}

	@Test
	public void testDandOptimization() throws Exception {
		assertEquals("''", optimize("dand(true, true, '')"));
	}

	@Test
	public void testCommentBlock() throws Exception {
		assertEquals(2, MethodScriptCompiler.lex("/*/ still a comment -()*/", null, new File("OptimizationTest"), true, true).size());
	}

	@Test
	public void testSwitchIc() throws Exception {
		assertEquals("__statements__(switch_ic(to_lower(dyn('AsDf')),'asdf',__statements__(msg('hello')),'fdsa',__statements__(msg('nope'))))",
				optimize("switch_ic(dyn('AsDf')) { case 'aSdF': msg('hello'); case 'fdsa': msg('nope'); }"));
	}

	@Test
	public void testSwitchWithComments() throws Exception {
		assertEquals("__statements__(switch(dyn(1),1,__statements__(break()),2,__statements__(break()),3,__statements__(break())))",
				optimize("switch(dyn(1)) { /** comment */ case 1: break(); /* comment */ case 2: break();\n"
						+ "// comment\ncase /*comment*/ 3: /** comment */ break(); }"));
	}

	@Test
	public void testNotNot() throws Exception {
		assertEquals("@value", optimize("!!@value"));
		assertEquals("not(@value)", optimize("!@value"));
		// !!!!@value (or more than 2 !!) is broken in the compiler -.-
	}

	@Test
	public void testReturnAsKeyword() throws Exception {
		assertEquals("__statements__(proc('_name',__statements__(return('value'))))", optimize("proc _name() { return 'value'; }"));
		assertEquals("__statements__(proc('_name',__statements__(return(rand(1,10)))))", optimize("proc _name() { return rand(1, 10); }"));

		assertEquals("__statements__(proc('_name',__statements__(return('value'))))", optimize("<! strict > proc _name() { return 'value'; }"));
		assertEquals("__statements__(proc('_name',__statements__(return(rand(1,10)))))", optimize("<! strict > proc _name() { return rand(1, 10); }"));

		assertEquals("__statements__(proc('_name',__statements__(return(add(dyn(1),dyn(2))))))", optimize("proc _name() { return dyn(1) + dyn(2); }"));
		assertEquals("__statements__(proc('_name',__statements__(return(add(dyn(1),dyn(2))))))", optimize("<! strict > proc _name() { return dyn(1) + dyn(2); }"));

	}

	@Test
	public void testReturnVoidKeyword() throws Exception {
		assertEquals("__statements__(proc('_name',__statements__(return())))", optimize("<! strict > proc _name() { return; msg('Dead code'); }"));
		assertEquals("__statements__(proc('_name',sconcat(__statements__(return()))))", optimize("proc _name() { return; msg('Dead code') msg('Other dead code')}"));
		assertEquals("__statements__(proc('_name',__statements__(return(msg('Dead code')))))", optimize("<! strict > proc _name() { return msg('Dead code'); msg('Other dead code');}"));

		assertEquals("__statements__(proc('_name',__statements__(return())))", optimize("proc _name() { return; }"));
		assertEquals("__statements__(proc('_name',__statements__(return())))", optimize("<! strict > proc _name() { return; }"));
	}

	@Test(expected = ConfigCompileException.class)
	public void testIfWithStatementFailsInStrictMode() throws Exception {
		try {
			optimize("if(dyn(true);) { }");
		} catch(ConfigCompileException ex) {
			Assert.fail();
		}
		optimize("<! strict > if(dyn(true);) { }");
	}

	@Test(expected = ConfigCompileException.class)
	public void testIfStatementWithMultipleInvalidParameters() throws Exception {
		optimize("if(dyn(true); dyn(false);) { }");
	}

	@Test
	public void testSconcatWithNonStatement() throws Exception {
		assertEquals("__statements__(assign(ms.lang.int,@i,0),assign(ms.lang.int,@j,0))",
				optimize("int @i = 0; int @j = 0;"));
		assertEquals("sconcat(__statements__(assign(ms.lang.int,@i,0)),assign(ms.lang.int,@j,0))",
				optimize("int @i = 0; int @j = 0"));
	}

//	@Test(expected = ConfigCompileException.class)
	public void testPartialStatementsInStrictMSA() throws Exception {
		assertEquals("__statements__(assign(ms.lang.int,@i,0),assign(ms.lang.string,@s,'asdf'))",
				optimize("<! strict >\n"
						+ "/test = >>>\n"
						+ "	int @i = 0;\n"
						+ "	string @s = 'asdf'\n"
						+ "<<<\n", false));
	}

//	@Test(expected = ConfigCompileException.class)
	public void testPartialStatementsInStrictMSA2() throws Exception {
		assertEquals("__statements__(if(dyn(1),__statements__(assign(ms.lang.int,@i,1),msg(@i))))",
				optimize("<! strict >\n"
						+ "/test = >>>\n"
						+ "	if(dyn(1)) {\n"
						+ "		int @i = 1;\n"
						+ "		msg(@i)\n"
						+ "	}\n"
						+ "<<<\n", false));
	}

//	@Test(expected = ConfigCompileException.class)
	public void testPartialStatementsInStrict() throws Exception {
		assertEquals("__statements__(assign(ms.lang.int,@i,1),msg(@i),msg(@i))", optimize("<! strict >\n"
				+ "int @i = 1\n"
				+ "msg(@i);\n"
				+ "msg(@i);\n"));
	}

	@Test
	public void testStatementInArrayInNonStrict() throws Exception {
		// Not the erroneous semicolon after 'c';. We are in non-strict mode though, so this should just
		// work anyways.
		assertEquals("array('a','b','c')", optimize("array('a', 'b', p('c');)"));
	}

	@Test
	public void testSwitchWithSmartStrings() throws Exception {
		assertEquals("__statements__(switch(@level,'1',__statements__(msg('1')),'@2',__statements__(msg('2'))))",
				optimize("switch(@level) {\n"
				+ "		case \"1\":\n"
				+ "			msg('1');\n"
				+ "		case \"\\@2\":\n"
				+ "			msg('2');\n"
				+ "}\n"));
	}

	@Test(expected = ConfigCompileException.class)
	public void testSwitchWithSmartStrings2() throws Exception {
		optimize("switch(@level) {\n"
			+ "		case \"@level\":\n"
			+ "			msg('1');\n"
			+ "		case \"2\":\n"
			+ "			msg('2');\n"
			+ "}\n");
	}

	@Test
	public void testFallthroughCasesAndDoubleQuotes() throws Exception {
		assertEquals("__statements__(switch(@o,array('one','two'),__statements__(msg('hi')),__statements__(msg('hello'))))",
				optimize("switch(@o){\n"
				+ "		case \"one\":\n"
				+ "		case \"two\":\n"
				+ "			msg('hi');\n"
				+ "		default:\n"
				+ "			msg('hello');\n"
				+ "}"));
	}

	@Test(expected = ConfigCompileException.class)
	public void testSmartStringInArrayFails() throws Exception {
		optimize("@a = 'asdf'; array(\"@a\": 'test')");
	}

	@Test
	public void testParenthesisRewritesCorrectly1() throws Exception {
		assertEquals("__statements__(assign(ms.lang.closure,@c,closure(__statements__(noop()))),execute(@c))",
				optimize("closure @c = closure() {};\n@c();"));
	}

	@Test
	public void testParenthesisRewritesCorrectly2() throws Exception {
		assertEquals("__statements__(assign(ms.lang.closure,@c,closure(assign(ms.lang.int,@a,null),__statements__(msg(@a)))),execute(10,@c))",
				optimize("closure @c = closure(int @a) {msg(@a);};\n@c(10);"));
	}

	@Test
	public void testParenthesisRewritesCorrectly3() throws Exception {
		assertEquals("__statements__(assign(ms.lang.closure,@c,closure(assign(ms.lang.int,@a,null),assign(ms.lang.int,@b,null),__statements__(msg(@a)))),execute(10,11,@c))",
				optimize("closure @c = closure(int @a, int @b) {msg(@a);};\n@c(10,11);"));
	}

	@Test
	public void testFreeParenthesisWork() throws Exception {
		assertEquals("__statements__(msg(multiply(dyn(2),add(dyn(3),dyn(4)))))",
				optimize("msg(dyn(2) * (dyn(3) + dyn(4)));"));
	}

	@Test
	public void testParenthesisWarnsButRewritesCorrectly() throws Exception {
		optimize("closure @c = closure() {};\n@c\n();");
		assertEquals(1, env.getEnv(CompilerEnvironment.class).getCompilerWarnings().size());
	}

	@Test
	public void testNestedExecute() throws Exception {
		assertEquals("__statements__(proc('_t',__statements__(return(closure(__statements__(return(closure(__statements__(msg('hi'))))))))),"
				+ "execute(execute(_t())))",
				optimize("<!strict> proc _t() { return closure() { return closure() { msg('hi'); }; }; }; _t()()();"));
		assertEquals("__statements__(proc('_t',__statements__(return(closure(__statements__(return(closure(__statements__(msg('hi'))))))))),"
				+ "execute(execute(_t())))",
				optimize("proc _t() { return closure() { return closure() { msg('hi'); }; }; } _t()()();"));
		assertEquals("__statements__(proc('_t',@a,__statements__(return(closure(@b,__statements__(return(closure(@c,__statements__(msg('hi'))))))))),"
				+ "execute(3,4,execute(2,_t(1))))",
				optimize("<!strict> proc _t(@a) { return closure(@b) { return closure(@c) { msg('hi'); }; }; }; _t(1)(2)(3,4);"));
	}

	@Test
	public void testNestedExecuteWithStatement() throws Exception {
		assertEquals("execute(closure())", optimize("closure()()"));
		assertEquals("__statements__(execute(closure()))", optimize("closure()();"));
	}

	@Test
	public void testParenthesisInArrayDefinition() throws Exception {
		assertEquals("array(centry(a:,neg(@a)))", optimize("array(a: -(@a))"));
	}

	@Test
	public void testNoErrorWithParenthesisAfterSymbol() throws Exception {
		assertEquals("__statements__(if(and(@a,or(@b,@c)),__statements__(noop())))",
				optimize("if(@a &&\n(@b || @c)) {}"));
		assertEquals(0, env.getEnv(CompilerEnvironment.class).getCompilerWarnings().size());
	}

	@Test
	public void testProcReference() throws Exception {
		assertEquals("__statements__(proc(ms.lang.int,'_asdf',assign(ms.lang.int,@i,null),__statements__(return(@i))),"
				+ "assign(ms.lang.Callable,@c,get_proc('_asdf')))",
				optimize("int proc _asdf(int @i) { return @i; } Callable @c = proc _asdf;"));
	}

	@Test
	public void testInvalidStatements() throws Exception {
		assertEquals("__statements__(msg('test'))", optimize("msg(p('test'););"));
		try {
			optimize("<!strict> msg('test';);");
			fail();
		} catch (ConfigCompileException | ConfigCompileGroupException c) {
			// pass
		}
	}

	@Test
	public void testSmartStringToDumbStringRewriteWithEscapes() throws Exception {
		assertEquals("'@'", optimize("\"\\@\"")); // Escaped '@'.
		assertEquals("'\\\\'", optimize("\"\\\\\"")); // Escaped '\'.
		assertEquals("'\\\\@'", optimize("\"\\\\\\@\"")); // Escaped '@' and '\'.
		assertEquals("'@\\\\'", optimize("\"\\@\\\\\"")); // Escaped '\' and '@'.
		assertEquals("'@@'", optimize("\"\\@\\@\"")); // Double escaped '@'.
		assertEquals("'\\\\\\\\'", optimize("\"\\\\\\\\\"")); // Double escaped '\'.
		assertEquals("'\\\\@\\\\\\\\@@\\\\\\\\\\\\@@@'",
				optimize("\"\\\\\\@\\\\\\\\\\@\\@\\\\\\\\\\\\\\@\\@\\@\"")); // Combination of both.
		assertEquals("'\\t'", optimize("\"\\t\"")); // Regular '\t'.
		assertEquals("'\\\\\\t'", optimize("\"\\\\\\t\"")); // Escaped '\' followed by '\t'.
		assertEquals("'a@b@ c @d @ e\\\\f\\\\@g'", optimize("\"a\\@b\\@ c \\@d \\@ e\\\\f\\\\\\@g\"")); // Mix of above.
	}

	@Test
	public void testForIsSelfStatement() throws Exception {
		assertEquals("__statements__(for(assign(ms.lang.int,@i,0),lt(@i,10),inc(@i),__statements__(msg(@i))))",
				optimize("for(int @i = 0, @i < 10, @i++) { msg(@i); }"));
		assertEquals("__statements__(while(true,__statements__(msg(''),for(assign(ms.lang.int,@i,0),lt(@i,10),inc(@i),__statements__(msg(@i))))))",
				optimize("while(true) { msg('') for(int @i = 0, @i < 10, @i++) { msg(@i); }}"));
	}

	@Test
	public void testEmptyStatementsAreRemoved() throws Exception {
		assertEquals("__statements__(msg('hi'))", optimize("msg('hi');;;;;;;;;;;;;"));
	}

	@Test
	public void testCallableOrderOfOperations() throws Exception {
		assertEquals("__statements__(assign(@c,closure(__statements__(return('test')))),msg(concat('test ',execute(@c))))",
				optimize("@c = closure() { return 'test'; }; msg('test ' . @c());"));
	}

	@Test
	public void testArrayValueInParenthesis() throws Exception {
		assertEquals("array(centry(key:,'value'))", optimize("array('key': ('value'))"));
	}

	private void testSemicolonUsage(String script, boolean passExpected) throws Exception {
		try {
			optimize(script);
			if(!passExpected) {
				if(MSVersion.LATEST.lte(new SimpleVersion(3, 3, 6))) {
					assertEquals(1, env.getEnv(CompilerEnvironment.class).getCompilerWarnings().size());
				} else {
					fail();
				}
			}
		} catch(ConfigCompileException ex) {
			// pass
			if(passExpected) {
				fail();
			}
		}
	}

	@Test
	public void testMissingSemicolonWarnsInStrictMode() throws Exception {
		testSemicolonUsage("<! strict > if(dyn(true)) { if(dyn(true)) {} if(dyn(true)) {} }", true);
		testSemicolonUsage("<! strict > if(dyn(true)) { } else if(dyn(1) == 1) { }", true);
		testSemicolonUsage("<! strict > for(int @i = 0, @i < 10, @i++) { msg(''); }", true);
		testSemicolonUsage("<! strict > array() if(dyn(true)) {}", false);
		testSemicolonUsage("<! strict > msg('hi');", true);
		testSemicolonUsage("<! strict > msg('hi')", false);
		testSemicolonUsage("<! strict > if(dyn(true)) {} array()", false);
	}

	@Test
	public void testConstantIsntStatement() throws Exception {
		try {
			optimize("'string';");
			fail();
		} catch(ConfigCompileException ex) {
			// pass
		}

		try {
			optimize("closure('string')");
			fail();
		} catch(ConfigCompileException ex) {
			// pass
		}
	}

//	@Test
	public void testIfWithMissingInnerStatement() throws Exception {
		String script = "if(dyn(true)){\n"
				+ "noop()\n"
				+ "} else {\n"
				+ "noop();\n"
				+ "}";
		optimize(script);
		CompilerWarning warning = env.getEnv(CompilerEnvironment.class).getCompilerWarnings().get(0);
		assertEquals(2, warning.getTarget().line());
	}
}
