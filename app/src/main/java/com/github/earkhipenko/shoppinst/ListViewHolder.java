package com.github.earkhipenko.shoppinst;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

/**
 * Created by Evgeny on 27.12.2016.
 */

public class ListViewHolder extends RecyclerView.ViewHolder {
    TextView listName;
    ImageView listAction;
    ImageView listDelete;
    RelativeLayout listItem;

    public ListViewHolder(View itemView, RelativeLayout listItem, TextView listName, ImageView listAction, ImageView listDelete) {
        super(itemView);
        this.listItem = listItem;
        this.listName = listName;
        this.listAction = listAction;
        this.listDelete = listDelete;
    }
}
