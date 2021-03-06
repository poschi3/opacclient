package de.geeksfactory.opacclient.frontend;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import org.acra.ACRA;
import org.holoeverywhere.app.AlertDialog;
import org.holoeverywhere.widget.ExpandableListView;
import org.holoeverywhere.widget.ExpandableListView.OnChildClickListener;
import org.json.JSONException;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ListView;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

import de.geeksfactory.opacclient.OpacClient;
import de.geeksfactory.opacclient.R;
import de.geeksfactory.opacclient.objects.Account;
import de.geeksfactory.opacclient.objects.Library;
import de.geeksfactory.opacclient.storage.AccountDataSource;

public class AccountListActivity extends SherlockActivity {

	private List<Account> accounts;
	private List<Library> libraries;
	private AlertDialog dialog;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_account_list);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
	}

	public void add() {
		AlertDialog.Builder builder = new AlertDialog.Builder(
				AccountListActivity.this);
		// Get the layout inflater
		LayoutInflater inflater = getLayoutInflater();

		View view = inflater.inflate(R.layout.expandable_list_dialog, null);

		ExpandableListView lv = (ExpandableListView) view
				.findViewById(R.id.lvBibs);
		try {
			libraries = ((OpacClient) getApplication()).getLibraries();
		} catch (IOException e) {
			ACRA.getErrorReporter().handleException(e);
		} catch (JSONException e) {
			ACRA.getErrorReporter().handleException(e);
		}
		final LibraryListAdapter la = new LibraryListAdapter(this);
		Collections.sort(libraries);
		for (Library lib : libraries) {
			la.addItem(lib);
		}
		lv.setAdapter(la);
		lv.setOnChildClickListener(new OnChildClickListener() {

			@Override
			public boolean onChildClick(ExpandableListView arg0, View arg1,
					int groupPosition, int childPosition, long arg4) {
				AccountDataSource data = new AccountDataSource(
						AccountListActivity.this);
				data.open();
				Account acc = new Account();
				acc.setLibrary(la.getChild(groupPosition, childPosition)
						.getIdent());
				acc.setLabel(getString(R.string.default_account_name));
				long insertedid = data.addAccount(acc);
				data.close();
				dialog.dismiss();

				Intent i = new Intent(AccountListActivity.this,
						AccountEditActivity.class);
				i.putExtra("id", insertedid);
				i.putExtra("adding", true);
				startActivity(i);
				return false;
			}
		});

		builder.setView(view).setNegativeButton(R.string.cancel,
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int id) {
						dialog.cancel();
					}
				});
		dialog = builder.create();
		dialog.show();
	}

	private void refreshLv() {
		ListView lvAccounts = (ListView) findViewById(R.id.lvAccounts);
		AccountDataSource data = new AccountDataSource(this);
		data.open();
		accounts = data.getAllAccounts();
		data.close();
		AccountListAdapter adapter = new AccountListAdapter(this, accounts);
		lvAccounts.setAdapter(adapter);
	}

	@Override
	public void onStart() {
		super.onStart();
		ListView lvAccounts = (ListView) findViewById(R.id.lvAccounts);
		refreshLv();

		lvAccounts.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				Intent i = new Intent(AccountListActivity.this,
						AccountEditActivity.class);
				i.putExtra("id", accounts.get(position).getId());
				startActivity(i);
			}
		});
		lvAccounts.setOnItemLongClickListener(new OnItemLongClickListener() {

			@Override
			public boolean onItemLongClick(AdapterView<?> parent, View view,
					int position, long id) {
				((OpacClient) getApplication()).setAccount(accounts.get(
						position).getId());
				refreshLv();
				return true;
			}

		});
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getSupportMenuInflater().inflate(R.menu.activity_account_list, menu);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == R.id.action_add) {
			add();
			return true;
		} else if (item.getItemId() == android.R.id.home) {
			finish();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

}
