package com.github.earkhipenko.shoppinst;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * Created by Evgeny on 18.12.2016.
 */

public class ActiveItemViewHolder extends RecyclerView.ViewHolder {
    CheckBox itemStatus;
    TextView itemName;
    TextView itemQuantity;
    ImageView itemAction;
    LinearLayout shoppingItem;

    public ActiveItemViewHolder(View itemView, LinearLayout shoppingItem, CheckBox itemStatus, TextView itemName, TextView itemQuantity, ImageView itemAction) {
        super(itemView);
        this.shoppingItem = shoppingItem;
        this.itemStatus = itemStatus;
        this.itemName = itemName;
        this.itemQuantity = itemQuantity;
        this.itemAction = itemAction;
    }
}
