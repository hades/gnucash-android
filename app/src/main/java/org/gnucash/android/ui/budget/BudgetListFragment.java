/*
 * Copyright (c) 2015 Ngewi Fet <ngewif@gmail.com>
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

package org.gnucash.android.ui.budget;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.Cursor;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.Loader;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.gnucash.android.R;
import org.gnucash.android.databinding.CardviewBudgetBinding;
import org.gnucash.android.databinding.FragmentBudgetListBinding;
import org.gnucash.android.db.DatabaseCursorLoader;
import org.gnucash.android.db.DatabaseSchema;
import org.gnucash.android.db.adapter.AccountsDbAdapter;
import org.gnucash.android.db.adapter.BudgetsDbAdapter;
import org.gnucash.android.model.Budget;
import org.gnucash.android.model.BudgetAmount;
import org.gnucash.android.model.Commodity;
import org.gnucash.android.model.Money;
import org.gnucash.android.ui.common.FormActivity;
import org.gnucash.android.ui.common.Refreshable;
import org.gnucash.android.ui.common.UxArgument;
import org.gnucash.android.ui.util.CursorRecyclerAdapter;

import java.math.BigDecimal;
import java.math.RoundingMode;

import timber.log.Timber;

/**
 * Budget list fragment
 */
public class BudgetListFragment extends Fragment implements Refreshable,
        LoaderManager.LoaderCallbacks<Cursor> {

    private static final int REQUEST_EDIT_BUDGET = 0xB;
    private static final int REQUEST_OPEN_ACCOUNT = 0xC;

    private BudgetRecyclerAdapter mBudgetRecyclerAdapter;

    private BudgetsDbAdapter mBudgetsDbAdapter;

    private FragmentBudgetListBinding mBinding;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mBinding = FragmentBudgetListBinding.inflate(inflater, container, false);
        View view = mBinding.getRoot();

        mBinding.budgetRecyclerView.setHasFixedSize(true);
        mBinding.budgetRecyclerView.setEmptyView(mBinding.emptyView);

        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            GridLayoutManager gridLayoutManager = new GridLayoutManager(getActivity(), 2);
            mBinding.budgetRecyclerView.setLayoutManager(gridLayoutManager);
        } else {
            LinearLayoutManager mLayoutManager = new LinearLayoutManager(getActivity());
            mBinding.budgetRecyclerView.setLayoutManager(mLayoutManager);
        }
        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mBudgetsDbAdapter = BudgetsDbAdapter.getInstance();
        mBudgetRecyclerAdapter = new BudgetRecyclerAdapter(null);

        mBinding.budgetRecyclerView.setAdapter(mBudgetRecyclerAdapter);

        getLoaderManager().initLoader(0, null, this);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        Timber.d("Creating the accounts loader");
        return new BudgetsCursorLoader(getActivity());
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loaderCursor, Cursor cursor) {
        Timber.d("Budget loader finished. Swapping in cursor");
        mBudgetRecyclerAdapter.swapCursor(cursor);
        mBudgetRecyclerAdapter.notifyDataSetChanged();
    }

    @Override
    public void onLoaderReset(Loader<Cursor> arg0) {
        Timber.d("Resetting the accounts loader");
        mBudgetRecyclerAdapter.swapCursor(null);
    }

    @Override
    public void onResume() {
        super.onResume();
        refresh();
        getActivity().findViewById(R.id.fab_create_budget).setVisibility(View.VISIBLE);
        ((AppCompatActivity) getActivity()).getSupportActionBar().setTitle("Budgets");
    }

    @Override
    public void refresh() {
        getLoaderManager().restartLoader(0, null, this);
    }

    /**
     * This method does nothing with the GUID.
     * Is equivalent to calling {@link #refresh()}
     *
     * @param uid GUID of relevant item to be refreshed
     */
    @Override
    public void refresh(String uid) {
        refresh();
    }

    /**
     * Opens the budget detail fragment
     *
     * @param budgetUID GUID of budget
     */
    public void onClickBudget(String budgetUID) {
        FragmentManager fragmentManager = getParentFragmentManager();
        fragmentManager.beginTransaction()
            .replace(R.id.fragment_container, BudgetDetailFragment.newInstance(budgetUID))
            .addToBackStack(null)
            .commit();
    }

    /**
     * Launches the FormActivity for editing the budget
     *
     * @param budgetId Db record Id of the budget
     */
    private void editBudget(long budgetId) {
        Intent addAccountIntent = new Intent(getActivity(), FormActivity.class);
        addAccountIntent.setAction(Intent.ACTION_INSERT_OR_EDIT);
        addAccountIntent.putExtra(UxArgument.FORM_TYPE, FormActivity.FormType.BUDGET.name());
        addAccountIntent.putExtra(UxArgument.BUDGET_UID, mBudgetsDbAdapter.getUID(budgetId));
        startActivityForResult(addAccountIntent, REQUEST_EDIT_BUDGET);
    }

    /**
     * Delete the budget from the database
     *
     * @param budgetId Database record ID
     */
    private void deleteBudget(long budgetId) {
        BudgetsDbAdapter.getInstance().deleteRecord(budgetId);
        refresh();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            refresh();
        }
    }

    class BudgetRecyclerAdapter extends CursorRecyclerAdapter<BudgetRecyclerAdapter.BudgetViewHolder> {

        public BudgetRecyclerAdapter(Cursor cursor) {
            super(cursor);
        }

        @Override
        public void onBindViewHolderCursor(BudgetViewHolder holder, Cursor cursor) {
            final Budget budget = mBudgetsDbAdapter.buildModelInstance(cursor);
            holder.budgetId = mBudgetsDbAdapter.getID(budget.getUID());

            holder.binding.listItem2Lines.primaryText.setText(budget.getName());

            AccountsDbAdapter accountsDbAdapter = AccountsDbAdapter.getInstance();
            String accountString;
            int numberOfAccounts = budget.getNumberOfAccounts();
            if (numberOfAccounts == 1) {
                accountString = accountsDbAdapter.getAccountFullName(budget.getBudgetAmounts().get(0).getAccountUID());
            } else {
                accountString = numberOfAccounts + " budgeted accounts";
            }
            holder.binding.listItem2Lines.secondaryText.setText(accountString);

            holder.binding.budgetRecurrence.setText(budget.getRecurrence().getRepeatString() + " - "
                    + budget.getRecurrence().getDaysLeftInCurrentPeriod() + " days left");

            BigDecimal spentAmountValue = BigDecimal.ZERO;
            for (BudgetAmount budgetAmount : budget.getCompactedBudgetAmounts()) {
                Money balance = accountsDbAdapter.getAccountBalance(budgetAmount.getAccountUID(),
                        budget.getStartofCurrentPeriod(), budget.getEndOfCurrentPeriod());
                spentAmountValue = spentAmountValue.add(balance.asBigDecimal());
            }

            Money budgetTotal = budget.getAmountSum();
            Commodity commodity = budgetTotal.getCommodity();
            String usedAmount = commodity.getSymbol() + spentAmountValue + " of "
                    + budgetTotal.formattedString();
            holder.binding.budgetAmount.setText(usedAmount);

            double budgetProgress = spentAmountValue.divide(budgetTotal.asBigDecimal(),
                            commodity.getSmallestFractionDigits(), RoundingMode.HALF_EVEN)
                    .doubleValue();
            holder.binding.budgetIndicator.setProgress((int) (budgetProgress * 100));

            holder.binding.budgetAmount.setTextColor(BudgetsActivity.getBudgetProgressColor(1 - budgetProgress));

            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onClickBudget(budget.getUID());
                }
            });
        }

        @Override
        public BudgetViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            CardviewBudgetBinding binding = CardviewBudgetBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
            return new BudgetViewHolder(binding);
        }

        class BudgetViewHolder extends RecyclerView.ViewHolder implements PopupMenu.OnMenuItemClickListener {
            long budgetId;
            CardviewBudgetBinding binding;

            public BudgetViewHolder(CardviewBudgetBinding binding) {
                super(binding.getRoot());
                this.binding = binding;
                binding.optionsMenu.setOnClickListener(v -> {
                    PopupMenu popup = new PopupMenu(getActivity(), v);
                    popup.setOnMenuItemClickListener(BudgetViewHolder.this);
                    MenuInflater inflater = popup.getMenuInflater();
                    inflater.inflate(R.menu.budget_context_menu, popup.getMenu());
                    popup.show();
                });

            }

            @Override
            public boolean onMenuItemClick(@NonNull MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.context_menu_edit_budget:
                        editBudget(budgetId);
                        return true;

                    case R.id.context_menu_delete:
                        deleteBudget(budgetId);
                        return true;

                    default:
                        return false;
                }
            }
        }
    }

    /**
     * Loads Budgets asynchronously from the database
     */
    private static class BudgetsCursorLoader extends DatabaseCursorLoader {

        /**
         * Constructor
         * Initializes the content observer
         *
         * @param context Application context
         */
        public BudgetsCursorLoader(Context context) {
            super(context);
        }

        @Override
        public Cursor loadInBackground() {
            mDatabaseAdapter = BudgetsDbAdapter.getInstance();
            return mDatabaseAdapter.fetchAllRecords(null, null, DatabaseSchema.BudgetEntry.COLUMN_NAME + " ASC");
        }
    }
}
