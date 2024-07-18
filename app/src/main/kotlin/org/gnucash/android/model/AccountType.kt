package org.gnucash.android.model

/**
 * The type of account.
 */
enum class AccountType(
    /**
     * Indicates that this type of normal balance the account type has
     *
     * To increase the value of an account with normal balance of credit, one would credit the
     * account.
     * To increase the value of an account with normal balance of debit, one would likewise debit
     * the account.
     */
    private val normalBalanceType: TransactionType = TransactionType.CREDIT
) {
    CASH(TransactionType.DEBIT),
    BANK(TransactionType.DEBIT),
    CREDIT,
    ASSET(TransactionType.DEBIT),
    LIABILITY,
    INCOME,
    EXPENSE(TransactionType.DEBIT),
    PAYABLE,
    RECEIVABLE(TransactionType.DEBIT),
    EQUITY,
    CURRENCY,
    STOCK(TransactionType.DEBIT),
    MUTUAL(TransactionType.DEBIT),
    TRADING,
    ROOT;

    fun hasDebitNormalBalance(): Boolean {
        return normalBalanceType === TransactionType.DEBIT
    }
}
