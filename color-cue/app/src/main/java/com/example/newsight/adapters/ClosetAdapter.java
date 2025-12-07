package com.example.newsight.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.example.newsight.R;
import com.example.newsight.api.models.ClothingItem;

import java.util.List;

public class ClosetAdapter extends BaseAdapter {

    private Context context;
    private List<ClothingItem> items;

    public ClosetAdapter(Context context, List<ClothingItem> items) {
        this.context = context;
        this.items = items;
    }

    @Override
    public int getCount() { return items.size(); }

    @Override
    public Object getItem(int i) { return items.get(i); }

    @Override
    public long getItemId(int i) { return i; }

    @Override
    public View getView(int i, View view, ViewGroup parent) {
        if (view == null) {
            view = LayoutInflater.from(context).inflate(R.layout.item_closet, parent, false);
        }

        ClothingItem item = items.get(i);

        ImageView img = view.findViewById(R.id.imgItem);
        TextView txt = view.findViewById(R.id.txtItemInfo);

        txt.setText(item.color + " " + item.category);

        Glide.with(context).load(item.image_url).into(img);

        return view;
    }
}
