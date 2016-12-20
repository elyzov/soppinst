package com.github.earkhipenko.shoppinst;

import android.content.Intent;
import android.graphics.Paint;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import io.realm.Realm;
import io.realm.RealmConfiguration;
import io.realm.RealmResults;
import io.realm.Sort;

public class MainActivity extends AppCompatActivity {

    private RecyclerView shoppingItems;

    private Realm realm;

    private List<ShoppingItem> dataSet;
    private String activeList;

    private RecyclerView.Adapter shoppingItemsAdapter = new RecyclerView.Adapter() {
        private final int ACTIVE_VIEW=1;
        private final int INACTIVE_VIEW=2;
        private final int SUBHEADER_VIEW=3;

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            if (viewType == ACTIVE_VIEW) {
                View v = getLayoutInflater().inflate(R.layout.active_item, parent, false);
                return new ActiveItemViewHolder(v,
                        (LinearLayout)v.findViewById(R.id.shopping_item),
                        (CheckBox)v.findViewById(R.id.item_status),
                        (TextView)v.findViewById(R.id.item_name),
                        (TextView)v.findViewById(R.id.item_quantity),
                        (ImageView)v.findViewById(R.id.item_action)
                );
            } else if (viewType == INACTIVE_VIEW) {
                View v = getLayoutInflater().inflate(R.layout.inactive_item, parent, false);
                return new InactiveItemViewHolder(v,
                        (CheckBox)v.findViewById(R.id.item_status),
                        (TextView)v.findViewById(R.id.item_name),
                        (ImageView)v.findViewById(R.id.item_action)
                );
            } else {
                View v = getLayoutInflater().inflate(R.layout.subheader, parent, false);
                return new SubheaderViewHolder(v,
                        (ImageView)v.findViewById(R.id.delete_comleted)
                );
            }
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            final ShoppingItem currentItem = dataSet.get(position);
            if (currentItem.getTimestamp() == -1) {
                SubheaderViewHolder h = (SubheaderViewHolder)holder;
                h.deleteCompleted.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        realm.beginTransaction();
                        RealmResults<ShoppingItem> inactiveItemResults
                                = realm.where(ShoppingItem.class).equalTo("completed", true)
                                .findAll();
                        inactiveItemResults.deleteAllFromRealm();
                        realm.commitTransaction();
                        initializeDataSet();
                        shoppingItemsAdapter.notifyDataSetChanged();
                    }
                });

            } else if (currentItem.isCompleted()) {
                InactiveItemViewHolder h = (InactiveItemViewHolder)holder;
                h.itemName.setText(currentItem.getName());
                h.itemName.setPaintFlags(h.itemName.getPaintFlags()| Paint.STRIKE_THRU_TEXT_FLAG);
                h.itemAction.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        realm.beginTransaction();
                        currentItem.setCompleted(false);
                        currentItem.setTimestamp(System.currentTimeMillis());
                        realm.commitTransaction();
                        initializeDataSet();
                        shoppingItemsAdapter.notifyDataSetChanged();
                    }
                });
            } else {
                ActiveItemViewHolder h = (ActiveItemViewHolder)holder;
                h.itemName.setText(currentItem.getName());
                h.itemQuantity.setText(currentItem.getQuantity());
                h.itemStatus.setChecked(false);
                h.itemStatus.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        if (isChecked) {
                            realm.beginTransaction();
                            currentItem.setCompleted(true);
                            currentItem.setTimestamp(System.currentTimeMillis());
                            realm.commitTransaction();
                            initializeDataSet();
                            shoppingItemsAdapter.notifyDataSetChanged();
                        }
                    }
                });
                h.shoppingItem.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent i = new Intent(MainActivity.this, ItemActivity.class);
                        i.putExtra("TITLE", "Edit item");
                        i.putExtra("ITEM_NAME", currentItem.getName());
                        i.putExtra("ITEM_QUANTITY", currentItem.getQuantity());
                        i.putExtra("ITEM_ID", currentItem.getId());
                        startActivityForResult(i, 1);
                    }
                });
                h.itemAction.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        realm.beginTransaction();
                        currentItem.deleteFromRealm();
                        realm.commitTransaction();
                        initializeDataSet();
                        shoppingItemsAdapter.notifyDataSetChanged();
                    }
                });
            }
        }

        @Override
        public int getItemCount() {
            return dataSet.size();
        }

        @Override
        public int getItemViewType(int position) {
            ShoppingItem currentItem = dataSet.get(position);
            if (currentItem.getTimestamp() == -1) return SUBHEADER_VIEW;
            if (currentItem.isCompleted()) return INACTIVE_VIEW;
            return ACTIVE_VIEW;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        Realm.init(this);
        RealmConfiguration configuration =
                new RealmConfiguration
                        .Builder()
                        .deleteRealmIfMigrationNeeded()
                        .build();
        Realm.setDefaultConfiguration(configuration);
        realm = Realm.getDefaultInstance();

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(MainActivity.this, ItemActivity.class);
                i.putExtra("TITLE", "Add item");
                startActivityForResult(i, 1);
            }
        });

        shoppingItems = (RecyclerView)findViewById(R.id.shopping_items);
        shoppingItems.setLayoutManager(new LinearLayoutManager(this));
        initializeDataSet();
        shoppingItems.setAdapter(shoppingItemsAdapter);
    }

    private void initializeDataSet() {
        dataSet = new ArrayList<>();
        RealmResults<ShoppingItem> activeItemResults
                = realm.where(ShoppingItem.class).equalTo("completed", false)
                .findAllSorted("timestamp", Sort.DESCENDING);
        RealmResults<ShoppingItem> inactiveItemResults
                = realm.where(ShoppingItem.class).equalTo("completed", true)
                .findAllSorted("timestamp", Sort.DESCENDING);

        ShoppingItem subheader = new ShoppingItem();
        subheader.setTimestamp(-1);

        for (ShoppingItem item:activeItemResults) dataSet.add(item);
        if (inactiveItemResults.size() > 0 ) dataSet.add(subheader);
        for (ShoppingItem item:inactiveItemResults) dataSet.add(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            initializeDataSet();
            shoppingItemsAdapter.notifyDataSetChanged();
        }
    }
}
