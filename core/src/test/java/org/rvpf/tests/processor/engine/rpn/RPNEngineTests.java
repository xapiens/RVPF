/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: RPNEngineTests.java 4095 2019-06-24 17:44:43Z SFB $
 */

package org.rvpf.tests.processor.engine.rpn;

import java.io.Serializable;

import java.math.BigDecimal;
import java.math.BigInteger;

import java.util.Optional;
import java.util.TimeZone;

import org.rvpf.base.DateTime;
import org.rvpf.base.Params;
import org.rvpf.base.Point;
import org.rvpf.base.logger.Logger;
import org.rvpf.base.tool.Require;
import org.rvpf.base.value.BigRational;
import org.rvpf.base.value.Complex;
import org.rvpf.base.value.Dict;
import org.rvpf.base.value.PointValue;
import org.rvpf.base.value.Rational;
import org.rvpf.base.value.ResultValue;
import org.rvpf.base.value.Tuple;
import org.rvpf.metadata.entity.ContentEntity;
import org.rvpf.metadata.entity.PointEntity;
import org.rvpf.processor.engine.rpn.RPNEngine;
import org.rvpf.processor.engine.rpn.RPNExecutor;
import org.rvpf.processor.engine.rpn.operation.BigDecimalOperations;
import org.rvpf.processor.engine.rpn.operation.BigIntegerOperations;
import org.rvpf.processor.engine.rpn.operation.BigRationalOperations;
import org.rvpf.processor.engine.rpn.operation.BooleanOperations;
import org.rvpf.processor.engine.rpn.operation.ComplexOperations;
import org.rvpf.processor.engine.rpn.operation.CompoundOperations;
import org.rvpf.processor.engine.rpn.operation.ContainerOperations;
import org.rvpf.processor.engine.rpn.operation.DateTimeOperations;
import org.rvpf.processor.engine.rpn.operation.DoubleOperations;
import org.rvpf.processor.engine.rpn.operation.LongOperations;
import org.rvpf.processor.engine.rpn.operation.Operations;
import org.rvpf.processor.engine.rpn.operation.RationalOperations;
import org.rvpf.processor.engine.rpn.operation.StackOperations;
import org.rvpf.processor.engine.rpn.operation.StringOperations;
import org.rvpf.tests.processor.engine.EngineTests;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * RPN engine tests.
 */
public final class RPNEngineTests
    extends EngineTests
{
    /**
     * Sets up this.
     */
    @BeforeMethod
    public void setUp()
    {
        _engine = new RPNEngine();
        setUpEngine(_engine, Optional.empty());

        _executor = new RPNExecutor(_engine, _GMT_TIME_ZONE);
    }

    /**
     * Tests big decimal operations.
     *
     * @throws Exception On failure.
     */
    @Test(dependsOnMethods = {"testDoubleOperations"})
    public void testBigDecimalOperations()
        throws Exception
    {
        _registerOperations(BigDecimalOperations.class);

        _resetResultValue(getBigDecimalContent(2));

        Require
            .equal(
                _apply("'1234567890123456789012345678901234567890.1' bigdec"),
                new BigDecimal("1234567890123456789012345678901234567890.10"));
        Require
            .equal(_apply("'1.10' bigdec 10 / neg"), new BigDecimal("-0.11"));
        Require.equal(_apply("'-0.67' bigdec abs"), new BigDecimal("0.67"));
        Require
            .equal(
                _apply("'2.00' bigdec -3 bigdec /"),
                new BigDecimal("-0.67"));
        Require.equal(_apply("'0.11' bigdec 10 *").toString(), "1.10");
        Require
            .equal(
                _apply("'1.23' bigdec '4.567' bigdec +"),
                new BigDecimal("5.80"));
        Require
            .equal(
                _apply("'1.23' bigdec '4.567' bigdec -"),
                new BigDecimal("-3.34"));
        Require
            .equal(
                _apply("'1.23' bigdec '4.567' bigdec max"),
                new BigDecimal("4.57"));
        Require
            .equal(
                _apply("'1.23' bigdec '4.567' bigdec min"),
                new BigDecimal("1.23"));
        Require
            .equal(_apply("'0.111' bigdec 1 scale="), new BigDecimal("0.10"));
        Require
            .equal(
                _apply("'0.111' bigdec scale bigdec"),
                new BigDecimal("3.00"));
        Require
            .equal(_apply("'0.111' bigdec 3 .right"), new BigDecimal("111.00"));
        Require.equal(_apply("111 bigdec 2 .left"), new BigDecimal("1.11"));
        Require
            .equal(
                _apply("'600.0' bigdec strip scale"),
                new BigDecimal("-2.00"));
        Require
            .equal(
                _apply("'600.0' bigdec strip unscaled"),
                new BigDecimal("6.00"));

        _resetResultValue(LOGICAL_CONTENT);

        Require.equal(_apply("'1.23' bigdec '4.567' bigdec gt"), Boolean.TRUE);
        Require.equal(_apply("'1.23' bigdec '4.567' bigdec lt"), Boolean.FALSE);
        Require.equal(_apply("'0.001' bigdec 0?"), Boolean.FALSE);
        Require.equal(_apply("0.0 bigdec 0?"), Boolean.TRUE);
        Require.equal(_apply("0.1 bigdec 0+?"), Boolean.TRUE);
        Require.equal(_apply("-0.1 bigdec 0-?"), Boolean.TRUE);
    }

    /**
     * Tests big integer operations.
     *
     * @throws Exception On failure.
     */
    @Test(dependsOnMethods = {"testLongOperations"})
    public void testBigIntegerOperations()
        throws Exception
    {
        _registerOperations(BigIntegerOperations.class);

        _resetResultValue(BIG_INTEGER_CONTENT);

        Require
            .equal(
                _apply("'1234567890123456789012345678901234567890' bigint"),
                new BigInteger("1234567890123456789012345678901234567890"));
        Require.equal(_apply("-2 bigint abs"), new BigInteger("2"));
        Require.equal(_apply("'3' bigint -2 bigint /"), new BigInteger("-1"));
        Require
            .equal(
                _apply("123 bigint '4567' bigint +"),
                new BigInteger("4690"));
        Require
            .equal(
                _apply("123 bigint '4567' bigint -"),
                new BigInteger("-4444"));
        Require
            .equal(
                _apply("'12345678901234567890' bigint"
                + " '12345678901234567890' bigint *"),
                new BigInteger("152415787532388367501905199875019052100"));
        Require
            .equal(
                _apply("'99999999999999999999' bigint ++"),
                new BigInteger("100000000000000000000"));
        Require
            .equal(
                _apply("'100000000000000000000' bigint --"),
                new BigInteger("99999999999999999999"));
    }

    /**
     * Tests big rational operations.
     *
     * @throws Exception On failure.
     */
    @Test(dependsOnMethods = {"testRationalOperations"})
    public void testBigRationalOperations()
        throws Exception
    {
        _registerOperations(RationalOperations.class);
        _registerOperations(BigRationalOperations.class);

        _resetResultValue(BIG_RATIONAL_CONTENT);

        Require.equal(_apply("1 2 bigrat"), BigRational.valueOf(1, 2));
        Require.equal(_apply("'1/2' bigrat"), BigRational.valueOf(1, 2));
        Require.equal(_apply("'1/2' rat bigrat"), BigRational.valueOf(1, 2));
        Require.equal(_apply("1 2 bigrat rat"), BigRational.valueOf(1, 2));
        Require
            .equal(
                _apply("1 2 bigrat split bigrat"),
                BigRational.valueOf(1, 2));
        Require.equal(_apply("-11 22 bigrat"), BigRational.valueOf(-1, 2));
        Require.equal(_apply("11 -22 bigrat"), BigRational.valueOf(-1, 2));
        Require.equal(_apply("-11 -22 bigrat"), BigRational.valueOf(1, 2));
        Require.equal(_apply("-11 22 bigrat abs"), BigRational.valueOf(1, 2));
        Require
            .equal(_apply("1 2 rat 3 4 bigrat +"), BigRational.valueOf(5, 4));
        Require
            .equal(_apply("1 2 bigrat 3 4 rat -"), BigRational.valueOf(-1, 4));
        Require
            .equal(_apply("1 2 rat 3 4 bigrat *"), BigRational.valueOf(3, 8));
        Require
            .equal(_apply("1 2 bigrat 3 4 rat /"), BigRational.valueOf(2, 3));
    }

    /**
     * Tests BooleanOperations.
     *
     * @throws Exception On failure.
     */
    @Test(dependsOnMethods = {"testStackOperations"})
    public void testBooleanOperations()
        throws Exception
    {
        _registerOperations(StackOperations.class);
        _registerOperations(BooleanOperations.class);

        _resetResultValue(LOGICAL_CONTENT);

        Require.equal(_apply("false not"), Boolean.TRUE);
        Require.equal(_apply("true false or"), Boolean.TRUE);
        Require.equal(_apply("true true and"), Boolean.TRUE);
        Require.equal(_apply("true true xor"), Boolean.FALSE);
        Require.equal(_apply("true assert false"), Boolean.FALSE);
        Require.equal(null, _apply("false assert false"));
        Require.equal(null, _apply("true null assert"));

        _resetResultValue(COUNT_CONTENT);

        Require.equal(_apply("0 1 true ?:"), Long.valueOf(1));
        Require.equal(_apply("1 2 false ?:"), Long.valueOf(1));
    }

    /**
     * Tests complex operations.
     *
     * @throws Exception On failure.
     */
    @Test(dependsOnMethods = {"testDoubleOperations"})
    public void testComplexOperations()
        throws Exception
    {
        _registerOperations(DoubleOperations.class);
        _registerOperations(ComplexOperations.class);

        _resetResultValue(COMPLEX_CONTENT);

        Require
            .equal(
                _apply("2.0 1.0 cplx"),
                Complex.cartesian(2.0, 1.0),
                "cartesian");
        Require
            .equal(
                _apply("2.0 pi 4 / polar"),
                Complex.polar(2.0, Math.PI / 4),
                "polar");
        Require
            .equal(
                _apply("2.0 45 rad polar"),
                Complex.polar(2.0, Math.PI / 4),
                "polar");
        Require
            .equal(
                _apply("'2.0cis1.0' polar"),
                Complex.polar(2.0, 1.0),
                "polar");
        Require
            .equal(
                _apply("'1+2j' cplx"),
                Complex.cartesian(+1.0, +2.0),
                "cartesian");
        Require
            .equal(
                _apply("'-1+2j' cplx"),
                Complex.cartesian(-1.0, +2.0),
                "cartesian");
        Require
            .equal(
                _apply("'-1-2i' cplx"),
                Complex.cartesian(-1.0, -2.0),
                "cartesian");
        Require
            .equal(
                _apply("'+1-2i' cplx"),
                Complex.cartesian(+1.0, -2.0),
                "cartesian");
        Require
            .equal(_apply("'1+0i' cplx"), Complex.cartesian(1, 0), "cartesian");
        Require
            .equal(
                _apply("'1cis0' cplx"),
                Complex.cartesian(1, 0),
                "cartesian");
        Require.equal(_apply("'1+0i' polar"), Complex.polar(1, 0), "polar");
        Require.equal(_apply("'1cis0' polar"), Complex.polar(1, 0), "polar");
        Require
            .equal(_apply("'1+0i' cplx polar"), Complex.polar(1, 0), "polar");
        Require
            .equal(
                _apply("'1cis0' polar cplx"),
                Complex.cartesian(1, 0),
                "cartesian");

        Require
            .equal(
                _apply("3 4 cplx split cplx"),
                Complex.cartesian(3, 4),
                "cartesian");
        Require
            .equal(
                _apply("3 pi polar split polar"),
                Complex.polar(3, Math.PI),
                "polar");

        Require
            .equal(
                _apply("'1+2i' cplx conj"),
                Complex.cartesian(+1.0, -2.0),
                "cartesian");
        Require
            .equal(
                _apply("'1-2i' cplx neg"),
                Complex.cartesian(-1.0, +2.0),
                "cartesian");
        Require.equal(_apply("i 2 +"), Complex.cartesian(2, 1), "cartesian");
        Require.equal(_apply("i 2 -"), Complex.cartesian(-2, 1), "cartesian");
        Require.equal(_apply("i 2 *"), Complex.cartesian(0, 2), "cartesian");
        Require.equal(_apply("i 2 /"), Complex.cartesian(0, 0.5), "cartesian");
        Require.equal(_apply("2 i +"), Complex.cartesian(2, 1), "cartesian");
        Require.equal(_apply("2 i -"), Complex.cartesian(2, -1), "cartesian");
        Require.equal(_apply("2 i *"), Complex.cartesian(0, 2), "cartesian");
        Require.equal(_apply("2 i /"), Complex.cartesian(0, -2), "cartesian");

        _resetResultValue(NUMERIC_CONTENT);

        Require.equal(_apply("3.0 4.0 cplx abs"), Double.valueOf(5.0));
        Require.equal(_apply("4 3 cplx abs"), Double.valueOf(5.0));
    }

    /**
     * Tests CompoundOperations.
     *
     * @throws Exception On failure.
     */
    @Test(dependsOnMethods =
    {
        "testBooleanOperations", "testLongOperations"
    })
    public void testCompoundOperations()
        throws Exception
    {
        _registerOperations(StackOperations.class);
        _registerOperations(BooleanOperations.class);
        _registerOperations(CompoundOperations.class);
        _registerOperations(LongOperations.class);

        _resetResultValue(LOGICAL_CONTENT);

        Require.equal(_apply("{ true false or }"), Boolean.TRUE);

        _resetResultValue(COUNT_CONTENT);

        Require.equal(_apply("true if 1"), Long.valueOf(1));
        Require.equal(_apply("true if 1 else 2"), Long.valueOf(1));
        Require.equal(_apply("false if 1 else 2"), Long.valueOf(2));
        Require.equal(_apply("true if else 2"), Long.valueOf(2));
        Require.equal(null, _apply("true unless 1"));
        Require.equal(_apply("false unless 2"), Long.valueOf(2));

        _resetResultValue(LOGICAL_CONTENT);

        Require
            .equal(
                _apply("true if { true false or } else false"),
                Boolean.TRUE);
        Require
            .equal(
                _apply("false if false else { true false or }"),
                Boolean.TRUE);

        _resetResultValue(COUNT_CONTENT);

        Require
            .equal(
                _apply("1 2 3 true while { drop depth 0 lt } 4"),
                Long.valueOf(4));
        Require
            .equal(_apply("1 2 3 do { drop depth 0 lt } 4"), Long.valueOf(4));

        Require.equal(_apply("2 try { 3 : ! } 7 +"), Long.valueOf(5));
        Require.equal(_apply("2 try { null ! } 7 +"), Long.valueOf(9));
        Require
            .equal(
                _apply("2 [ 10 try 20 nop depth #1= reduce + #1 / ] +"),
                Long.valueOf(17));

        Require
            .equal(
                _apply("9 8 7 6 5 reduce { 6 eq if break } depth :$0= clear"),
                Long.valueOf(3));
        Require
            .equal(
                _apply("9 8 7 6 5 reduce { depth 4 eq if { drop continue } + }"),
                Long.valueOf(24));
    }

    /**
     * Tests ContainerOperations.
     *
     * @throws Exception On failure.
     */
    @Test(dependsOnMethods = {"testCompoundOperations"})
    public void testContainerOperations()
        throws Exception
    {
        final Dict dict = new Dict();
        final Tuple tuple = new Tuple();

        dict.put("ONE", Integer.valueOf(1));
        dict.put("TWO", Integer.valueOf(2));
        dict.put("THREE", Integer.valueOf(3));
        tuple.add(Integer.valueOf(10));
        tuple.add(Integer.valueOf(20));
        tuple.add(Integer.valueOf(30));

        _registerOperations(StackOperations.class);
        _registerOperations(LongOperations.class);
        _registerOperations(BooleanOperations.class);
        _registerOperations(CompoundOperations.class);
        _registerOperations(ContainerOperations.class);

        _resetResultValue(COUNT_CONTENT);
        _addInputValue(getContent(COUNT_DICT_CONTENT), dict);
        _addInputValue(getContent(COUNT_TUPLE_CONTENT), tuple);

        Require
            .equal(
                _apply("$1! dict? true! $2! tuple? true! 1"),
                Long.valueOf(1));
        Require
            .equal(
                _apply("$2! dict? false! $1! tuple? false! 2"),
                Long.valueOf(2));
        Require
            .equal(_apply("$1 apply { size : 3 eq true! }"), Long.valueOf(3));
        Require
            .equal(
                _apply("$1 apply { values depth 3 eq true! reduce + }"),
                Long.valueOf(6));
        Require
            .equal(_apply("$2 apply { size : 3 eq true! }"), Long.valueOf(3));
        Require
            .equal(
                _apply("$2 apply { values depth 3 eq true! reduce + }"),
                Long.valueOf(60));
        Require
            .equal(
                _apply("$1 apply { keys depth reduce nip }"),
                Long.valueOf(3));
        Require
            .equal(
                _apply("tuple apply { [ 100 200 300 0 #reduce append ] 1 get }"),
                Long.valueOf(200));
        Require
            .equal(
                _apply("dict apply { [ 1 'A' 2 'B' 3 'C' reduce put ] 'B' get }"),
                Long.valueOf(2));

        _resetResultValue(COUNT_DICT_CONTENT);
        _addInputValue(getContent(COUNT_DICT_CONTENT), dict);

        Require
            .equal(
                dict,
                _apply("dict $0= $1 apply entries $0 apply reduce put $0"));

        _resetResultValue(COUNT_TUPLE_CONTENT);
        _addInputValue(getContent(COUNT_TUPLE_CONTENT), tuple);

        Require
            .equal(
                tuple,
                _apply("$1 apply values reverse tuple :#1= "
                + "apply { 0 #reduce append } #1"));
    }

    /**
     * Tests DateTimeOperations.
     *
     * @throws Exception On failure.
     */
    @Test(dependsOnMethods =
    {
        "testCompoundOperations", "testDoubleOperations", "testStringOperations"
    })
    public void testDateTimeOperations()
        throws Exception
    {
        _registerOperations(DateTimeOperations.class);
        _registerOperations(StringOperations.class);
        _registerOperations(CompoundOperations.class);
        _registerOperations(DoubleOperations.class);

        _resetResultValue(UNSPECIFIED_CONTENT);

        final DateTime.Context dateTimeContext = new DateTime.Context(
            _GMT_TIME_ZONE);

        _setResultValueStamp("2000-02-01 12:30:10.5", dateTimeContext);
        _addInputValue(getContent(UNSPECIFIED_CONTENT), null);

        Require.equal(_apply("$0@ year"), Long.valueOf(2000));
        Require.equal(_apply("$0@ month"), Long.valueOf(2));
        Require.equal(_apply("$0@ day"), Long.valueOf(1));
        Require.equal(_apply("$0@ hour"), Long.valueOf(12));
        Require.equal(_apply("$0@ minute"), Long.valueOf(30));
        Require.equal(_apply("$0@ second"), Double.valueOf(10.5));
        Require.equal(_apply("$0@ split reduce +"), Double.valueOf(2055.5));
        Require.equal(_apply("$0@ split join"), _resultValue.getStamp());
        Require.equal(_apply("$0@ dow"), Long.valueOf(2));
        Require.equal(_apply("$0@ dim"), Long.valueOf(29));
        Require.equal(_apply("$0@ --month dim"), Long.valueOf(31));
        Require.equal(_apply("$0@ raw"), Long.valueOf(44561250105000000L));
        Require.equal(_apply("$1@ raw"), Long.valueOf(44561250105000000L));

        _setResultValueStamp("0000-01-01", dateTimeContext);

        Require.equal(_apply("$0@ year"), Long.valueOf(0));
        Require.equal(_apply("$0@ month"), Long.valueOf(1));
        Require.equal(_apply("$0@ day"), Long.valueOf(1));
        Require.equal(_apply("$0@ hour"), Long.valueOf(0));
        Require.equal(_apply("$0@ minute"), Long.valueOf(0));
        Require.equal(_apply("$0@ second"), Double.valueOf(0.0));

        _setResultValueStamp("-2000-02-01 12:30:10.5", dateTimeContext);

        Require.equal(_apply("$0@ year"), Long.valueOf(-2000));
        Require.equal(_apply("$0@ month"), Long.valueOf(2));
        Require.equal(_apply("$0@ day"), Long.valueOf(1));
        Require.equal(_apply("$0@ hour"), Long.valueOf(12));
        Require.equal(_apply("$0@ minute"), Long.valueOf(30));
        Require.equal(_apply("$0@ second"), Double.valueOf(10.5));
        Require.equal(_apply("$0@ split reduce +"), Double.valueOf(-1944.5));
        Require.equal(_apply("$0@ split join"), _resultValue.getStamp());
        Require.equal(_apply("$0@ dow"), Long.valueOf(2));
        Require.equal(_apply("$0@ dim"), Long.valueOf(29));

        Require
            .equal(
                _apply("'GMT' tz '2000-01-01 12:00' mjd"),
                dateTimeContext.fromString("2000-01-01 12:00"));

        Require
            .equal(
                _apply("'GMT' tz '2000-02-01 12:30:10.5' mjd 2 hours - str"),
                "2000-02-01T10:30:10.5Z");
    }

    /**
     * Tests DoubleOperations.
     *
     * @throws Exception On failure.
     */
    @Test(dependsOnMethods = {"testBooleanOperations"})
    public void testDoubleOperations()
        throws Exception
    {
        _registerOperations(StackOperations.class);
        _registerOperations(DoubleOperations.class);
        _registerOperations(BooleanOperations.class);

        _resetResultValue(NUMERIC_CONTENT);

        Require.equal(_apply("-1.0 abs"), Double.valueOf(1.0));
        Require.equal(_apply("3 2 ** 4 2 ** + sqrt"), Double.valueOf(5.0));
        Require.equal(_apply("1 2 10.0 20.0 gt ?:"), Double.valueOf(2.0));
        Require.equal(_apply("1 2 10.0 20.0 lt ?:"), Double.valueOf(1.0));
        Require.equal(_apply("1 2 10.0 20.0 ne ?:"), Double.valueOf(2.0));
        Require.equal(_apply("1 2 10.0 20.0 eq ?:"), Double.valueOf(1.0));
        Require.equal(_apply("e log"), Double.valueOf(1.0));
        Require.equal(_apply("10 log10"), Double.valueOf(1.0));
        Require.equal(_apply("2.0 1.0 min"), Double.valueOf(1.0));
        Require.equal(_apply("1.0 2.0 min"), Double.valueOf(1.0));
        Require.equal(_apply("2.0 1.0 max"), Double.valueOf(2.0));
        Require.equal(_apply("1.0 2.0 max"), Double.valueOf(2.0));
        Require.equal(_apply("8.0 cbrt"), Double.valueOf(2.0));
        Require.equal(_apply("3.0 4.0 hypot"), Double.valueOf(5.0));

        _resetResultValue(LOGICAL_CONTENT);

        Require.equal(_apply("-0.3 0.5 0~?"), Boolean.TRUE);
        Require.equal(_apply("-0.7 -1.0 0.5 eq~"), Boolean.TRUE);

        _resetResultValue(NUMERIC_CONTENT);

        Require.equal(_apply("0 sin asin"), Double.valueOf(0.0));
        Require.equal(_apply("pi 2 / cos acos 2 *"), Double.valueOf(Math.PI));
        Require.equal(_apply("45 rad tan atan deg"), Double.valueOf(45.0));

        Require.equal(_apply("4.0 3.0 %"), Double.valueOf(1.0));
        Require.equal(_apply("-4.0 3.0 %"), Double.valueOf(-1.0));
        Require.equal(_apply("4.0 -3.0 %"), Double.valueOf(-1.0));
        Require.equal(_apply("-4.0 -3.0 %"), Double.valueOf(1.0));

        Require.equal(_apply("4.0 3.0 mod"), Double.valueOf(1.0));
        Require.equal(_apply("-4.0 3.0 mod"), Double.valueOf(2.0));
        Require.equal(_apply("4.0 -3.0 mod"), Double.valueOf(2.0));
        Require.equal(_apply("-4.0 -3.0 mod"), Double.valueOf(1.0));
    }

    /**
     * Tests long operations.
     *
     * @throws Exception On failure.
     */
    @Test
    public void testLongOperations()
        throws Exception
    {
        _registerOperations(LongOperations.class);

        _resetResultValue(COUNT_CONTENT);

        Require.equal(_apply("1 2 +"), Long.valueOf(3));
        Require.equal(_apply("-3 abs ++"), Long.valueOf(4));
        Require.equal(_apply("2 1 - --"), Long.valueOf(0));
        Require.equal(_apply("2 1 min"), Long.valueOf(1));
        Require.equal(_apply("1 2 min"), Long.valueOf(1));
        Require.equal(_apply("2 1 max"), Long.valueOf(2));
        Require.equal(_apply("1 2 max"), Long.valueOf(2));

        Require.equal(_apply("4 3 %"), Long.valueOf(1));
        Require.equal(_apply("-4 3 %"), Long.valueOf(-1));
        Require.equal(_apply("4 -3 %"), Long.valueOf(-1));
        Require.equal(_apply("-4 -3 %"), Long.valueOf(1));
        Require.equal(_apply("-4 -3 /"), Long.valueOf(1));

        Require.equal(_apply("4 3 mod"), Long.valueOf(1));
        Require.equal(_apply("-4 3 mod"), Long.valueOf(2));
        Require.equal(_apply("4 -3 mod"), Long.valueOf(2));
        Require.equal(_apply("-4 -3 mod"), Long.valueOf(1));

        _resetResultValue(MASK_CONTENT);

        Require
            .equal(_apply("0x6060 0x4040 xor 4 rshft"), Long.valueOf(0x0202));
    }

    /**
     * Tests macros.
     *
     * @throws Exception On failure.
     */
    @Test(dependsOnMethods =
    {
        "testCompoundOperations", "testDateTimeOperations"
    })
    public void testMacros()
        throws Exception
    {
        final String[] macros = {"add(...=0) { [ ... reduce + ] }",
                "sub(x!, ...) { x add(...) - }",
                "mul(...!) { [ ... reduce * ] }",
                "div(x!, ...!) { x mul(...) / }", "lt(x!, y!) { y x lt }",
                "if(cond, true, false) { cond if { true } else { false } }",
                "try(action, failed) try { action } { failed }",
                "now { now str debug }", };

        _registerOperations(StackOperations.class);
        _registerOperations(StringOperations.class);
        _registerOperations(CompoundOperations.class);
        _registerOperations(LongOperations.class);
        _registerOperations(DateTimeOperations.class);
        _registerOperations(BooleanOperations.class);

        _resetResultValue(COUNT_CONTENT);

        Require
            .equal(
                _apply("add(1, 2, 3)", macros, _NO_STRINGS),
                Long.valueOf(6));
        Require.equal(_apply("add()", macros, _NO_STRINGS), Long.valueOf(0));
        Require
            .equal(
                _apply("sub(5, 3, 1)", macros, _NO_STRINGS),
                Long.valueOf(1));
        Require
            .equal(_apply("mul(3, 5)", macros, _NO_STRINGS), Long.valueOf(15));
        Require
            .equal(_apply("div(15, 5)", macros, _NO_STRINGS), Long.valueOf(3));
        Require
            .equal(
                _apply("if(lt(3, 5), sub(5, 3), fail)", macros, _NO_STRINGS),
                Long.valueOf(2));
        Require
            .equal(
                _apply("try(1 fail, 2)", macros, _NO_STRINGS),
                Long.valueOf(2));
        Require.equal(null, _apply("now", macros, _NO_STRINGS));
        Require
            .equal(
                null,
                _apply("if('X' null?, fail, fail)", macros, _NO_STRINGS));
        Require
            .equal(
                _apply("if(true, add(sub(2, 1), 3))", macros, _NO_STRINGS),
                Long.valueOf(4));
    }

    /**
     * Tests normalization.
     *
     * @throws Exception On failure.
     */
    @Test
    public void testNormalization()
        throws Exception
    {
        _resetResultValue(getSIContent(MILLIMETER_UNIT));
        _addInputValue(getSIContent(CENTIMETER_UNIT), Float.valueOf(30.0f));

        Require.equal(_apply("$1"), Double.valueOf(300.0));

        _resetResultValue(CELCIUS_CONTENT);
        _addInputValue(getContent(FAHRENHEIT_CONTENT), Integer.valueOf(32));

        Require.equal(_apply("$1"), Double.valueOf(0.0));
    }

    /**
     * Tests rational operations.
     *
     * @throws Exception On failure.
     */
    @Test(dependsOnMethods = {"testLongOperations"})
    public void testRationalOperations()
        throws Exception
    {
        _registerOperations(RationalOperations.class);

        _resetResultValue(RATIONAL_CONTENT);

        Require.equal(_apply("1 2 rat"), Rational.valueOf(1, 2));
        Require.equal(_apply("'1/2' rat"), Rational.valueOf(1, 2));
        Require.equal(_apply("1 2 rat split rat"), Rational.valueOf(1, 2));
        Require.equal(_apply("-11 22 rat"), Rational.valueOf(-1, 2));
        Require.equal(_apply("11 -22 rat"), Rational.valueOf(-1, 2));
        Require.equal(_apply("-11 -22 rat"), Rational.valueOf(1, 2));
        Require.equal(_apply("-11 22 rat abs"), Rational.valueOf(1, 2));
        Require.equal(_apply("1 2 rat 3 4 rat +"), Rational.valueOf(5, 4));
        Require.equal(_apply("1 2 rat 3 4 rat -"), Rational.valueOf(-1, 4));
        Require.equal(_apply("1 2 rat 3 4 rat *"), Rational.valueOf(3, 8));
        Require.equal(_apply("1 2 rat 3 4 rat /"), Rational.valueOf(2, 3));
    }

    /**
     * Tests StackOperations.
     *
     * @throws Exception On failure.
     */
    @Test
    public void testStackOperations()
        throws Exception
    {
        _registerOperations(StackOperations.class);

        _resetResultValue(COUNT_CONTENT);

        Require.equal(_apply("1 2 3 3 1 roll 2 clear"), Long.valueOf(3));
        Require
            .equal(
                _apply("1 mark 2 mark 3 unmark depth clear"),
                Long.valueOf(1));
        Require
            .equal(null, _apply("'Before mark' [ 2 null 4 dump depth* clear"));
        Require
            .equal(null, _apply("'Before mark' [ 2 null 4 dump* depth* clear"));
    }

    /**
     * Tests StringOperations.
     *
     * @throws Exception On failure.
     */
    @Test(dependsOnMethods = {"testStackOperations"})
    public void testStringOperations()
        throws Exception
    {
        _registerOperations(StackOperations.class);
        _registerOperations(StringOperations.class);

        _resetResultValue(TEXT_CONTENT);

        Require.equal(_apply("'A' '\"B\"' '\\'C\\'' + +"), "A\"B\"'C'");
        Require
            .equal(
                _apply("0 1 2 3 '%d-%d-%d' format $0= depth clear"),
                "3-2-1");
        Require
            .equal(
                _apply("1 [ 2 3 '%d-%d-%d' format* $0= depth clear ] drop"),
                "3-2-1");
        Require.equal(null, _apply("'debug test' debug"));
        Require.equal(null, _apply("'info test' info"));
        Require.equal(_apply("'ABC' 1 2 substring"), "B");
    }

    /**
     * Tests variables.
     *
     * @throws Exception On failure.
     */
    @Test(dependsOnMethods = {"testStringOperations"})
    public void testVariables()
        throws Exception
    {
        final Params params = new Params();

        _registerOperations(StackOperations.class);
        _registerOperations(StringOperations.class);

        params.add(Point.PARAM_PARAM, "A");
        params.add(Point.PARAM_PARAM, "B");
        params.add(Point.PARAM_PARAM, "C");
        params.freeze();

        _resetResultValue(TEXT_CONTENT);
        _setResultParams(params);

        Require.equal(_apply("@1 : + @2! : + @3! : + + +"), "AABBCC");
        Require.equal(_apply("'X' $0= 'Y' $0 +"), "YX");
        Require
            .equal(
                _apply("$0. /* Returns the name of the result point. */"),
                TEST_RESULT_POINT_NAME);

        _addInputValue(getContent(TEXT_CONTENT), "D");
        Require.equal(_apply("$1! :#1= lower $1= #1! $1 +"), "Dd");
        _addInputValue(getContent(TEXT_CONTENT), null);
        Require.equal(null, _apply("$1 $2! + ( Fails on missing 2nd input. )"));
        Require
            .equal(
                _apply("$1. $2. +"),
                TEST_INPUT_POINT_NAME + "1" + TEST_INPUT_POINT_NAME + "2");
    }

    /**
     * Tests words.
     *
     * @throws Exception On failure.
     */
    @Test(dependsOnMethods =
    {
        "testLongOperations", "testStackOperations"
    })
    public void testWords()
        throws Exception
    {
        final String[] words = {":! ( x -- x ) ; : !", ":+ ( n -- n*2 ) ; : +",
                "dozen ( n -- n*12 ) : triple ( n -- 3*n ) 3 * ; triple :+ :+", };

        _registerOperations(LongOperations.class);
        _registerOperations(StackOperations.class);

        _resetResultValue(COUNT_CONTENT);

        Require.equal(_apply("0 :!", _NO_STRINGS, words), Long.valueOf(0));
        Require.equal(null, _apply("null :!", _NO_STRINGS, words));
        Require.equal(_apply("1 :+", _NO_STRINGS, words), Long.valueOf(2));
        Require
            .equal(
                _apply(": :* : * ; 1 :+ :*", _NO_STRINGS, words),
                Long.valueOf(4));
        Require.equal(_apply("3 dozen", _NO_STRINGS, words), Long.valueOf(36));
    }

    private void _addInputValue(
            final ContentEntity content,
            final Serializable value)
    {
        addInputValue(content, value, _resultValue);
    }

    private Serializable _apply(final String formula)
    {
        return _apply(formula, _NO_STRINGS, _NO_STRINGS);
    }

    private Serializable _apply(
            final String formula,
            final String[] macros,
            final String[] words)
    {
        _LOGGER.reset();

        final PointValue pointValue = _executor
            .execute(formula, macros, words, _resultValue, _LOGGER);

        Require
            .failure(
                _LOGGER.hasLogged(Logger.LogLevel.WARN),
                "Formula \"" + formula + "\" failed");

        return (pointValue != null)? pointValue.getValue(): null;
    }

    private void _registerOperations(
            final Class<? extends Operations> operationsClass)
        throws Exception
    {
        _engine.register(operationsClass.newInstance());
    }

    private void _resetResultValue(final ContentEntity content)
    {
        _resultValue = newResultValue(content);
    }

    private void _resetResultValue(final String contentName)
    {
        _resetResultValue(getContent(contentName));
    }

    private void _setResultParams(final Params params)
    {
        final PointEntity resultPoint = (PointEntity) _resultValue
            .getPoint()
            .get();

        resultPoint.setParams(Optional.of(params));
    }

    private void _setResultValueStamp(
            final String time,
            final DateTime.Context dateTimeContext)
    {
        _resultValue = (ResultValue) _resultValue
            .morph(
                Optional.empty(),
                Optional
                    .of(
                            dateTimeContext
                                    .fromString(
                                            time,
                                                    Optional
                                                            .of(DateTime.now()))));
    }

    /** Centimeter unit. */
    public static final String CENTIMETER_UNIT = "cm";

    /** Millimeter unit. */
    public static final String MILLIMETER_UNIT = "mm";

    /**  */

    private static final TimeZone _GMT_TIME_ZONE = TimeZone.getTimeZone("GMT");
    private static final Logger _LOGGER = Logger
        .getInstance(RPNEngineTests.class);
    private static final String[] _NO_STRINGS = new String[0];

    private RPNEngine _engine;
    private RPNExecutor _executor;
    private ResultValue _resultValue;
}

/* This is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License
 * version 2.1 as published by the Free Software Foundation.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 * 02110-1301, USA
 */
