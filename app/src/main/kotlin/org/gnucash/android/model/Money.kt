/*
 * Copyright (c) 2012-2024 GnuCash Android developers
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gnucash.android.model

import android.os.Build
import android.os.Parcel
import android.os.Parcelable
import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.NumberFormat
import java.util.Locale
import timber.log.Timber

/** Rounding mode to be applied when performing operations. */
private val MONEY_ROUNDING_MODE = RoundingMode.HALF_EVEN

/**
 * Money represents a money amount and a corresponding currency.
 *
 * <p>Money internally uses [BigDecimal] to represent the amounts, which enables it
 * to represent the amount exactly. Money objects are immutable: all
 * operations return new Money objects.
 *
 * <p>Money String constructors should not be passed any locale-formatted numbers. Only
 * [Locale.US] is supported e.g. "2.45" will be parsed as 2.45 meanwhile
 * "2,45" will be parsed to 245 although that could be a decimal in [Locale.GERMANY]
 *
 * @author Ngewi Fet<ngewif@gmail.com>
 */
data class Money(private val _amount: BigDecimal, val commodity: Commodity) : Comparable<Money>, Parcelable {
    val amount: BigDecimal by lazy {
        _amount.setScale(commodity.smallestFractionDigits, MONEY_ROUNDING_MODE)
    }

    /**
     * Constructs a new money amount given the amount and currency code.
     *
     * @param amount Value of the amount
     * @param currencyCode Currency code as specified by ISO 4217
     */
    constructor(amount: BigDecimal, currencyCode: String?) : this(
        amount,
        Commodity.getInstance(currencyCode)!!
    )

    /**
     * Constructs a new money amount given the amount as string and currency code.
     *
     * @param amount Value of the amount
     * @param currencyCode Currency code as specified by ISO 4217
     */
    constructor(amount: String?, currencyCode: String?) : this(
        BigDecimal(amount),
        currencyCode
    )

    /**
     * Constructs a new money amount given the numerator and denominator of the amount.
     * The rounding mode used for the division is [BigDecimal.ROUND_HALF_EVEN]
     *
     * @param numerator    Numerator as integer
     * @param denominator  Denominator as integer
     * @param currencyCode 3-character currency code string
     */
    constructor(numerator: Long, denominator: Long, currencyCode: String) : this(
        bigDecimalOfFraction(numerator, denominator),
        currencyCode
    )

    /**
     * Returns a new `Money` object the currency specified by `currency`
     * and the same amount as this one. No value exchange between the currencies is performed.
     *
     * @param commodity [Commodity] to assign to new `Money` object
     * @return [Money] object with same value as current object, but with new `currency`
     */
    fun withCurrency(commodity: Commodity): Money {
        return Money(_amount, commodity)
    }

    /**
     * Returns the GnuCash format numerator for this amount.
     *
     * Example: Given an amount 32.50$, the numerator will be 3250
     *
     * @return GnuCash numerator for this amount
     */
    val numerator: Long
        get() = try {
            _amount.scaleByPowerOfTen(scale).longValueExact()
        } catch (e: ArithmeticException) {
            val msg = "Currency " + commodity.currencyCode +
                " with scale " + scale +
                " has amount " + _amount
            Timber.e(e, msg)
            throw ArithmeticException(msg)
        }

    /**
     * Returns the GnuCash amount format denominator for this amount
     *
     * The denominator is 10 raised to the power of number of fractional digits in the currency
     *
     * @return GnuCash format denominator
     */
    val denominator: Long
        get() = BigDecimal.ONE.scaleByPowerOfTen(scale).longValueExact()

    /**
     * Returns the scale (precision) used for the decimal places of this amount.
     *
     * The scale used depends on the commodity
     *
     * @return Scale of amount as integer
     */
    private val scale: Int
        get() {
            var scale = commodity.smallestFractionDigits
            if (scale < 0) {
                scale = _amount.scale()
            }
            if (scale < 0) {
                scale = 0
            }
            return scale
        }

    /**
     * Returns the amount represented by this Money object
     *
     * The scale and rounding mode of the returned value are set to that of this Money object
     *
     * @return [BigDecimal] valure of amount in object
     */
    fun asBigDecimal(): BigDecimal {
        return _amount.setScale(commodity.smallestFractionDigits, RoundingMode.HALF_EVEN)
    }

    /**
     * Returns a string representation of the Money object formatted according to
     * the `locale` and includes the currency symbol.
     * The output precision is limited to the number of fractional digits supported by the currency
     *
     * @param locale Locale to use when formatting the object. Defaults to Locale.getDefault().
     * @return String containing formatted Money representation
     */
    @JvmOverloads
    fun formattedString(locale: Locale = Locale.getDefault()): String {
        val currencyFormat = NumberFormat.getCurrencyInstance(locale)
        //if we want to show US Dollars for locales which also use Dollars, for example, Canada
        val symbol = if (commodity == Commodity.USD && locale != Locale.US) {
            "US$"
        } else {
            commodity.symbol
        }
        val decimalFormatSymbols = (currencyFormat as DecimalFormat).decimalFormatSymbols
        decimalFormatSymbols.currencySymbol = symbol
        currencyFormat.decimalFormatSymbols = decimalFormatSymbols
        currencyFormat.setMinimumFractionDigits(commodity.smallestFractionDigits)
        currencyFormat.setMaximumFractionDigits(commodity.smallestFractionDigits)
        return currencyFormat.format(_amount)
    }

    /**
     * Returns a string representation of the Money object formatted according to
     * the `locale` without the currency symbol.
     * The output precision is limited to the number of fractional digits supported by the currency
     *
     * @param locale Locale to use when formatting the object. Defaults to Locale.getDefault().
     * @return String containing formatted Money representation
     */
    @JvmOverloads
    fun formattedStringWithoutSymbol(locale: Locale = Locale.getDefault()): String {
        val format = NumberFormat.getNumberInstance(locale)
        format.setMinimumFractionDigits(commodity.smallestFractionDigits)
        format.setMaximumFractionDigits(commodity.smallestFractionDigits)
        return format.format(_amount)
    }

    /**
     * Returns a new Money object whose amount is the negated value of this object amount.
     * The original `Money` object remains unchanged.
     *
     * @return Negated `Money` object
     */
    operator fun unaryMinus(): Money {
        return Money(_amount.negate(), commodity)
    }

    /**
     * Returns a new `Money` object whose value is the sum of the values of
     * this object and `addend`.
     *
     * @param addend Second operand in the addition.
     * @return Money object whose value is the sum of this object and `money`
     * @throws CurrencyMismatchException if the `Money` objects to be added have different Currencies
     */
    @Throws(CurrencyMismatchException::class)
    operator fun plus(addend: Money): Money {
        if (commodity != addend.commodity) throw CurrencyMismatchException()
        val bigD = _amount.add(addend._amount)
        return Money(bigD, commodity)
    }

    /**
     * Returns a new `Money` object whose value is the difference of the values of
     * this object and `subtrahend`.
     * This object is the minuend and the parameter is the subtrahend
     *
     * @param subtrahend Second operand in the subtraction.
     * @return Money object whose value is the difference of this object and `subtrahend`
     * @throws CurrencyMismatchException if the `Money` objects to be added have different Currencies
     */
    @Throws(CurrencyMismatchException::class)
    operator fun minus(subtrahend: Money): Money {
        if (commodity != subtrahend.commodity) throw CurrencyMismatchException()
        val bigD = _amount.subtract(subtrahend._amount)
        return Money(bigD, commodity)
    }

    /**
     * Returns the ratio of two `Money` objects with the same commodity as `BigDecimal`.
     *
     * This method uses the rounding mode [BigDecimal.ROUND_HALF_EVEN]
     *
     * @param divisor Second operand in the division.
     * @return BigDecimal object whose value is the quotient of this object and `divisor`
     * @throws CurrencyMismatchException if the `Money` objects to be added have different Currencies
     */
    @Throws(CurrencyMismatchException::class)
    operator fun div(divisor: Money): BigDecimal {
        if (commodity != divisor.commodity) throw CurrencyMismatchException()
        return _amount.divide(divisor._amount, commodity.smallestFractionDigits, MONEY_ROUNDING_MODE)
    }

    /**
     * Returns a new `Money` object whose value is the quotient of the division of this objects
     * value by the factor `divisor`
     *
     * @param divisor Second operand in the division.
     * @return Money object whose value is the quotient of this object and `divisor`
     */
    operator fun div(divisor: Long): Money = Money(_amount / BigDecimal.valueOf(divisor), commodity)

    /**
     * Returns a new `Money` object whose value is the product of this object
     * and the factor `multiplier`
     *
     * The currency of the returned object is the same as the current object
     *
     * @param multiplier Factor to multiply the amount by.
     * @return Money object whose value is the product of this objects values and `multiplier`
     */
    operator fun times(multiplier: Long) = times(BigDecimal.valueOf(multiplier))

    /**
     * Returns a new `Money` object whose value is the product of this object
     * and the factor `multiplier`
     *
     * @param multiplier Factor to multiply the amount by.
     * @return Money object whose value is the product of this objects values and `multiplier`
     */
    operator fun times(multiplier: BigDecimal): Money {
        return Money(_amount.multiply(multiplier), commodity)
    }

    /**
     * Returns true if the amount held by this Money object is negative
     *
     * @return `true` if the amount is negative, `false` otherwise.
     */
    val isNegative: Boolean
        get() = _amount.compareTo(BigDecimal.ZERO) == -1

    /**
     * Returns the string representation of the amount (without currency) of the Money object.
     *
     * This string is not locale-formatted. The decimal operator is a period (.)
     * For a locale-formatted version, see the method `formattedStringWithoutSymbol()`.
     *
     * @return String representation of the amount (without currency) of the Money object
     */
    fun toPlainString(): String {
        return _amount.setScale(commodity.smallestFractionDigits, MONEY_ROUNDING_MODE).toPlainString()
    }

    /**
     * Returns the string representation of the Money object (value + currency) formatted according
     * to the default locale
     *
     * @return String representation of the amount formatted with default locale
     */
    override fun toString(): String {
        return formattedString()
    }

    @Throws(CurrencyMismatchException::class)
    override fun compareTo(other: Money): Int {
        if (commodity != other.commodity) throw CurrencyMismatchException()
        return _amount.compareTo(other._amount)
    }

    /**
     * Returns a new instance of [Money] object with the absolute value of the current object
     *
     * @return Money object with absolute value of this instance
     */
    fun abs(): Money {
        return Money(_amount.abs(), commodity)
    }

    /**
     * Checks if the value of this amount is exactly equal to zero.
     *
     * @return `true` if this money amount is zero, `false` otherwise
     */
    val isAmountZero: Boolean
        get() = _amount.compareTo(BigDecimal.ZERO) == 0

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeSerializable(_amount)
        parcel.writeString(commodity.currencyCode)
    }

    override fun describeContents(): Int {
        return 0
    }

    inner class CurrencyMismatchException : IllegalArgumentException() {
        override val message: String
            get() = "Cannot perform operation on Money instances with different currencies"
    }

    companion object {
        /**
         * Default currency code (according ISO 4217)
         * This is typically initialized to the currency of the device default locale,
         * otherwise US dollars are used
         */
        @JvmField
        var DEFAULT_CURRENCY_CODE = Commodity.USD.currencyCode

        /**
         * Returns a Money instance initialized to the local currency and value 0
         *
         * @return Money instance of value 0 in locale currency
         */
        @JvmStatic
        val zeroInstance: Money by lazy {
            Money(BigDecimal.ZERO, Commodity.DEFAULT_COMMODITY)
        }

        /**
         * Creates a new Money instance with 0 amount and the `currencyCode`
         *
         * @param currencyCode Currency to use for this money instance
         * @return Money object with value 0 and currency `currencyCode`
         */
        @JvmStatic
        fun createZeroInstance(currencyCode: String): Money {
            val commodity = Commodity.getInstance(currencyCode) ?: Commodity.DEFAULT_COMMODITY
            return Money(BigDecimal.ZERO, commodity)
        }

        @JvmField
        val CREATOR = object : Parcelable.Creator<Money> {
            override fun createFromParcel(parcel: Parcel): Money {
                val amount: BigDecimal? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    parcel.readSerializable(javaClass.classLoader, BigDecimal::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    parcel.readSerializable() as? BigDecimal
                }
                val commodity = parcel.readString()!!
                return Money(amount!!, commodity)
            }

            override fun newArray(size: Int): Array<Money?> {
                return arrayOfNulls(size)
            }
        }
    }
}

fun Parcel.writeMoney(value: Money?, flags: Int) {
    writeParcelable(value, flags)
}

fun Parcel.readMoney(): Money? {
    val clazz = Money::class.java
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        readParcelable(clazz.classLoader, clazz)
    } else {
        readParcelable(clazz.classLoader)
    }
}

/**
 * Returns the [BigDecimal] from the `numerator` and `denominator`
 *
 * @param numerator   Number of the fraction
 * @param denominator Denominator of the fraction
 * @return BigDecimal representation of the number
 */
fun bigDecimalOfFraction(numerator: Long, denominator: Long): BigDecimal =
    if (numerator == 0L && denominator == 0L) {
        BigDecimal.ONE
    } else {
        BigDecimal(BigInteger.valueOf(numerator), 0).divide(BigDecimal(denominator), MONEY_ROUNDING_MODE)
    }