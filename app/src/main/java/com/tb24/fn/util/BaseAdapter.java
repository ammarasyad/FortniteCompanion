package com.tb24.fn.util;

import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by etiennelawlor on 12/3/16.
 */

public abstract class BaseAdapter<T> extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

	// region Constants
	protected static final int HEADER = 0;
	protected static final int ITEM = 1;
	protected static final int FOOTER = 2;
	// endregion

	// region Member Variables
	protected List<T> items;
	protected OnItemClickListener onItemClickListener;
	protected OnReloadClickListener onReloadClickListener;
	protected boolean isFooterAdded = false;
	// endregion

	// region Interfaces
	public interface OnItemClickListener {
		void onItemClick(int position, View view);
	}

	public interface OnReloadClickListener {
		void onReloadClick();
	}
	// endregion

	// region Constructors
	public BaseAdapter() {
		items = new ArrayList<>();
	}
	// endregion

	@NonNull
	@Override
	public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
		RecyclerView.ViewHolder viewHolder;

		switch (viewType) {
			case HEADER:
				viewHolder = createHeaderViewHolder(parent);
				break;
			default:
			case ITEM:
				viewHolder = createItemViewHolder(parent);
				break;
			case FOOTER:
				viewHolder = createFooterViewHolder(parent);
				break;
		}

		return viewHolder;
	}

	@Override
	public void onBindViewHolder(@NonNull RecyclerView.ViewHolder viewHolder, int position) {
		switch (getItemViewType(position)) {
			case HEADER:
				bindHeaderViewHolder(viewHolder);
				break;
			default:
			case ITEM:
				bindItemViewHolder(viewHolder, position);
				break;
			case FOOTER:
				bindFooterViewHolder(viewHolder);
				break;
		}
	}

	@Override
	public int getItemCount() {
		return items.size();
	}

	// region Abstract Methods
	protected abstract RecyclerView.ViewHolder createHeaderViewHolder(ViewGroup parent);

	protected abstract RecyclerView.ViewHolder createItemViewHolder(ViewGroup parent);

	protected abstract RecyclerView.ViewHolder createFooterViewHolder(ViewGroup parent);

	protected abstract void bindHeaderViewHolder(RecyclerView.ViewHolder viewHolder);

	protected abstract void bindItemViewHolder(RecyclerView.ViewHolder viewHolder, int position);

	protected abstract void bindFooterViewHolder(RecyclerView.ViewHolder viewHolder);

	protected abstract void displayLoadMoreFooter();

	protected abstract void displayErrorFooter();

	public abstract void addFooter();
	// endregion

	// region Helper Methods
	public T getItem(int position) {
		return items.get(position);
	}

	public void add(T item) {
		items.add(item);
		notifyItemInserted(items.size() - 1);
	}

	public void addAll(List<T> items) {
		int oldSize = this.items.size();
		this.items.addAll(items);
		notifyItemRangeInserted(oldSize, items.size());
	}

	private void remove(T item) {
		int position = items.indexOf(item);
		if (position > -1) {
			items.remove(position);
			notifyItemRemoved(position);
		}
	}

	public void clear() {
		isFooterAdded = false;
		while (getItemCount() > 0) {
			remove(getItem(0));
		}
	}

	public boolean isEmpty() {
		return items.isEmpty();
	}

	public boolean isLastPosition(int position) {
		return position == items.size() - 1;
	}

	public void removeFooter() {
		isFooterAdded = false;

		int position = items.size() - 1;
		T item = getItem(position);

		if (item == null) {
			items.remove(position);
			notifyItemRemoved(position);
		}
	}

	public void updateFooter(FooterType footerType) {
		switch (footerType) {
			case LOAD_MORE:
				displayLoadMoreFooter();
				break;
			case ERROR:
				displayErrorFooter();
				break;
			default:
				break;
		}
	}

	public void setOnItemClickListener(OnItemClickListener onItemClickListener) {
		this.onItemClickListener = onItemClickListener;
	}

	public void setOnReloadClickListener(OnReloadClickListener onReloadClickListener) {
		this.onReloadClickListener = onReloadClickListener;
	}

	public List<T> getItems() {
		return items;
	}

	// endregion

	// region Inner Classes
	public enum FooterType {
		LOAD_MORE, ERROR
	}
	// endregion
}