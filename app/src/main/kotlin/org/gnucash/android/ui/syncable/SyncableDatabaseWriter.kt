package org.gnucash.android.ui.syncable

import android.util.Log
import org.gnucash.android.app.GnuCashApplication
import org.gnucash.android.db.DatabaseHelper
import org.gnucash.android.db.adapter.AccountsDbAdapter
import org.gnucash.android.db.adapter.BooksDbAdapter
import org.gnucash.android.db.adapter.DatabaseAdapter
import org.gnucash.android.db.adapter.SplitsDbAdapter
import org.gnucash.android.db.adapter.TransactionsDbAdapter
import org.gnucash.android.model.Account
import org.gnucash.android.model.AccountType
import org.gnucash.android.model.Book

class SyncableDatabaseWriter {
    fun writeAccountTree(accountTree: SyncableAccountTree) {
        val book = Book()
        book.rootAccountUID = accountTree.root
        book.isActive = true
        book.displayName = "Syncable Book"
        val dbHelper = DatabaseHelper(GnuCashApplication.getAppContext(), book.uID)
        val mainDb = dbHelper.writableDatabase
        val transactionsAdapter = TransactionsDbAdapter(mainDb, SplitsDbAdapter(mainDb))
        val accountsAdapter = AccountsDbAdapter(mainDb, transactionsAdapter)
        val booksAdapter = BooksDbAdapter.getInstance()
        accountsAdapter.beginTransaction()
        try {
            accountsAdapter.deleteAllRecords()
            val rootAccount = Account("ROOT")
            rootAccount.uID = accountTree.root
            rootAccount.accountType = AccountType.ROOT
            accountsAdapter.addRecord(rootAccount, DatabaseAdapter.UpdateMethod.insert)
            var remainingAccounts = accountTree.accounts
            val insertedAccounts = mutableSetOf(rootAccount.uID)
            while (remainingAccounts.isNotEmpty()) {
                val nextLayer = mutableListOf<SyncableAccount>()
                remainingAccounts.forEach { account ->
                    if (insertedAccounts.contains(account.parent)) {
                        val model = Account(account.name)
                        model.uID = account.guid
                        model.parentUID = account.parent
                        Log.i(TAG, "Inserting account ${account.guid}")
                        accountsAdapter.addRecord(model, DatabaseAdapter.UpdateMethod.insert)
                        insertedAccounts.add(account.guid)
                    } else {
                        nextLayer.add(account)
                    }
                }
                remainingAccounts = nextLayer
            }
            accountsAdapter.setTransactionSuccessful()
            booksAdapter.addRecord(book, DatabaseAdapter.UpdateMethod.insert)
        } finally {
            accountsAdapter.endTransaction()
        }
    }

    companion object {
        const val TAG = "SyncableDatabaseWriter"
    }
}