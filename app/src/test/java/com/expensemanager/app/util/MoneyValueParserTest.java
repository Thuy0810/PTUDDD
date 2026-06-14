package com.expensemanager.app.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class MoneyValueParserTest {

    // ---------------------- toLong(Object) ----------------------

    @Test
    public void toLong_nullReturnsNull() {
        assertNull(MoneyValueParser.toLong(null));
    }

    @Test
    public void toLong_LongValue() {
        assertEquals(Long.valueOf(1000L), MoneyValueParser.toLong(Long.valueOf(1000L)));
    }

    @Test
    public void toLong_IntegerValue() {
        assertEquals(Long.valueOf(1000L), MoneyValueParser.toLong(Integer.valueOf(1000)));
    }

    @Test
    public void toLong_DoubleWholeNumber() {
        assertEquals(Long.valueOf(1000L), MoneyValueParser.toLong(Double.valueOf(1000.0)));
    }

    @Test
    public void toLong_DoubleFractionRounds() {
        assertEquals(Long.valueOf(1001L), MoneyValueParser.toLong(Double.valueOf(1000.5)));
        assertEquals(Long.valueOf(1000L), MoneyValueParser.toLong(Double.valueOf(1000.4)));
    }

    @Test
    public void toLong_FloatValue() {
        assertEquals(Long.valueOf(1000L), MoneyValueParser.toLong(Float.valueOf(1000.0f)));
    }

    @Test
    public void toLong_StringPlainNumber() {
        assertEquals(Long.valueOf(1000L), MoneyValueParser.toLong("1000"));
    }

    @Test
    public void toLong_StringWithDotGrouping() {
        assertEquals(Long.valueOf(1000000L), MoneyValueParser.toLong("1.000.000"));
    }

    @Test
    public void toLong_StringWithCommaGrouping() {
        assertEquals(Long.valueOf(1000000L), MoneyValueParser.toLong("1,000,000"));
    }

    @Test
    public void toLong_EmptyStringReturnsNull() {
        assertNull(MoneyValueParser.toLong(""));
    }

    @Test
    public void toLong_InvalidStringReturnsNull() {
        assertNull(MoneyValueParser.toLong("abc"));
    }

    @Test
    public void toLong_BooleanReturnsNull() {
        assertNull(MoneyValueParser.toLong(Boolean.TRUE));
    }

    @Test
    public void toLong_NaNReturnsNull() {
        assertNull(MoneyValueParser.toLong(Double.NaN));
    }

    // ---------------------- tryParse(String, long) ----------------------

    @Test
    public void tryParse_nullReturnsDefault() {
        assertEquals(0L, MoneyValueParser.tryParse(null, 0L));
        assertEquals(100L, MoneyValueParser.tryParse(null, 100L));
    }

    @Test
    public void tryParse_emptyReturnsDefault() {
        assertEquals(0L, MoneyValueParser.tryParse("", 0L));
    }

    @Test
    public void tryParse_whitespaceReturnsDefault() {
        assertEquals(0L, MoneyValueParser.tryParse("   ", 0L));
    }

    @Test
    public void tryParse_plainNumber() {
        assertEquals(123L, MoneyValueParser.tryParse("123", 0L));
    }

    @Test
    public void tryParse_dotGrouping() {
        assertEquals(1234567L, MoneyValueParser.tryParse("1.234.567", 0L));
    }

    @Test
    public void tryParse_commaGrouping() {
        assertEquals(1234567L, MoneyValueParser.tryParse("1,234,567", 0L));
    }

    @Test
    public void tryParse_negativeReturnsDefault() {
        assertEquals(0L, MoneyValueParser.tryParse("-100", 0L));
    }

    @Test
    public void tryParse_invalidStringReturnsDefault() {
        assertEquals(100L, MoneyValueParser.tryParse("abc", 100L));
    }

    @Test
    public void tryParse_trimsWhitespace() {
        assertEquals(500L, MoneyValueParser.tryParse("  500  ", 0L));
    }

    @Test
    public void tryParse_stripsCurrencySuffix() {
        assertEquals(500L, MoneyValueParser.tryParse("500đ", 0L));
        assertEquals(500L, MoneyValueParser.tryParse("500d", 0L));
    }

    // ---------------------- tryParseStrict(String) ----------------------

    @Test
    public void tryParseStrict_valid() {
        assertEquals(Long.valueOf(123L), MoneyValueParser.tryParseStrict("123"));
    }

    @Test
    public void tryParseStrict_emptyReturnsNull() {
        assertNull(MoneyValueParser.tryParseStrict(""));
    }

    @Test
    public void tryParseStrict_zeroReturnsNull() {
        assertNull(MoneyValueParser.tryParseStrict("0"));
    }

    @Test
    public void tryParseStrict_negativeReturnsNull() {
        assertNull(MoneyValueParser.tryParseStrict("-1"));
    }

    @Test
    public void tryParseStrict_dotAsGrouping() {
        // VND không có thập phân, "1.5" = 15
        assertEquals(Long.valueOf(15L), MoneyValueParser.tryParseStrict("1.5"));
    }

    // ---------------------- isValidAmount(long) ----------------------

    @Test
    public void isValidAmount_zeroFalse() {
        assertFalse(MoneyValueParser.isValidAmount(0L));
    }

    @Test
    public void isValidAmount_positiveTrue() {
        assertTrue(MoneyValueParser.isValidAmount(1L));
    }

    @Test
    public void isValidAmount_negativeFalse() {
        assertFalse(MoneyValueParser.isValidAmount(-1L));
    }

    @Test
    public void isValidAmount_maxValueTrue() {
        assertTrue(MoneyValueParser.isValidAmount(Long.MAX_VALUE));
    }

    // ---------------------- orZero, add ----------------------

    @Test
    public void orZero_nullBecomesZero() {
        assertEquals(0L, MoneyValueParser.orZero(null));
    }

    @Test
    public void orZero_valuePasses() {
        assertEquals(50L, MoneyValueParser.orZero(50L));
    }

    @Test
    public void add_bothNull() {
        assertEquals(0L, MoneyValueParser.add(null, null));
    }

    @Test
    public void add_leftNull() {
        assertEquals(7L, MoneyValueParser.add(null, 7L));
    }

    @Test
    public void add_rightNull() {
        assertEquals(7L, MoneyValueParser.add(7L, null));
    }

    @Test
    public void add_both() {
        assertEquals(12L, MoneyValueParser.add(5L, 7L));
    }
}
