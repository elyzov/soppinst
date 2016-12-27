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
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import io.realm.Realm;
import io.realm.RealmConfiguration;
import io.realm.RealmResults;
import io.realm.Sort;

public class MainActivity extends AppCompatActivity {

    private RecyclerView shoppingItems;
    private RecyclerView shoppingLists;

    private Realm realm;

    private List<ShoppingItem> dataSet;
    private List<ShoppingList> listSet;
    private String activeList;
    private ImageView actionListSave;
    private ImageView actionListAdd;


    private EditText inputListName;

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

    private RecyclerView.Adapter shoppingListsAdapter = new RecyclerView.Adapter() {
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = getLayoutInflater().inflate(R.layout.list_item, parent, false);
            return new ListViewHolder(v,
                    (TextView)v.findViewById(R.id.list_name),
                    (ImageView)v.findViewById(R.id.list_action),
                    (ImageView)v.findViewById(R.id.list_delete)
            );
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            final ShoppingList currentList = listSet.get(position);
            ListViewHolder h = (ListViewHolder)holder;
            h.listName.setText(currentList.getName());
            h.listAction.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    actionListSave.setTag(currentList);
                    inputListName.setText(currentList.getName());
                    actionListSave.setVisibility(View.VISIBLE);
                    actionListAdd.setVisibility(View.GONE);
                }
            });
            h.listDelete.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    realm.beginTransaction();
                    RealmResults<ShoppingItem> inactiveItemResults
                            = realm.where(ShoppingItem.class).equalTo("list", currentList.getName())
                            .findAll();
                    inactiveItemResults.deleteAllFromRealm();
                    currentList.deleteFromRealm();
                    realm.commitTransaction();
                    initializeDataSet();
                    shoppingItemsAdapter.notifyDataSetChanged();
                    shoppingListsAdapter.notifyDataSetChanged();
                }
            });


        }

        @Override
        public int getItemCount() {
            return listSet.size();
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

        inputListName = (EditText) findViewById(R.id.input_list_name);
        actionListAdd = (ImageView) findViewById(R.id.list_add);
        actionListSave = (ImageView) findViewById(R.id.list_save);
        actionListAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addNewList();
            }
        });
        inputListName.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                addNewList();
                return true;
            }
        });
        actionListSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ShoppingList currentList = (ShoppingList) actionListSave.getTag();
                realm.beginTransaction();
                currentList.setName(inputListName.getText().toString());
                realm.commitTransaction();
                inputListName.setText("");
                actionListSave.setVisibility(View.GONE);
                actionListAdd.setVisibility(View.VISIBLE);
                actionListSave.setTag(null);
                shoppingListsAdapter.notifyDataSetChanged();
            }
        });

        shoppingItems = (RecyclerView)findViewById(R.id.shopping_items);
        shoppingItems.setLayoutManager(new LinearLayoutManager(this));

        shoppingLists = (RecyclerView)findViewById(R.id.shopping_lists);
        shoppingLists.setLayoutManager(new LinearLayoutManager(this));

        initializeDataSet();
        shoppingItems.setAdapter(shoppingItemsAdapter);
        shoppingLists.setAdapter(shoppingListsAdapter);
    }

    private void addNewList() {
        realm.beginTransaction();
        ShoppingList shoppingItem = realm.createObject(
                ShoppingList.class,
                UUID.randomUUID().toString()
        );
        shoppingItem.setName(inputListName.getText().toString());
        realm.commitTransaction();
        inputListName.setText("");
        initializeDataSet();
        shoppingListsAdapter.notifyDataSetChanged();
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

        listSet = new ArrayList<>();
        RealmResults<ShoppingList> listResults
                = realm.where(ShoppingList.class).findAll();
        for (ShoppingList list:listResults) listSet.add(list);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            initializeDataSet();
            shoppingItemsAdapter.notifyDataSetChanged();
        }
    }
}
