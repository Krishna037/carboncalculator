package com.example.carbonfootprintcalculator;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class ActionItemsAdapter extends RecyclerView.Adapter<ActionItemsAdapter.ViewHolder> {
    public ArrayList<ActionItem> actionItems;
    public OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(int position);
    }

    public ActionItemsAdapter(ArrayList<ActionItem> actionItems, OnItemClickListener listener) {
        this.actionItems = actionItems;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_action, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ActionItem item = actionItems.get(position);
        holder.nameText.setText(item.getName());
        holder.dateText.setText(item.getDate());
        holder.footprintText.setText(String.format("%.1f kg CO₂", item.getCarbonFootprint()));
        holder.categoryText.setText(item.getCategory());
        holder.completedCheckbox.setChecked(item.isCompleted());

        holder.completedCheckbox.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return actionItems.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView nameText, dateText, footprintText, categoryText;
        CheckBox completedCheckbox;

        ViewHolder(View itemView) {
            super(itemView);
            nameText = itemView.findViewById(R.id.action_name);
            dateText = itemView.findViewById(R.id.action_date);
            footprintText = itemView.findViewById(R.id.action_footprint);
            categoryText = itemView.findViewById(R.id.action_category);
            completedCheckbox = itemView.findViewById(R.id.action_completed);
        }
    }
}