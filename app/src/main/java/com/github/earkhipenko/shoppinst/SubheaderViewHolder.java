package com.github.earkhipenko.shoppinst;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;

/**
 * Created by Evgeny on 18.12.2016.
 */

public class SubheaderViewHolder extends RecyclerView.ViewHolder {

    ImageView deleteCompleted;

    public SubheaderViewHolder(View itemView,ImageView deleteCompleted) {
        super(itemView);
        this.deleteCompleted = deleteCompleted;
    }
}
