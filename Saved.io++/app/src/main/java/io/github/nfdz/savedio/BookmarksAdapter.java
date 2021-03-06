/*
 * Copyright (C) 2017 Noe Fernandez
 */
package io.github.nfdz.savedio;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import io.github.nfdz.savedio.model.Bookmark;
import io.github.nfdz.savedio.utils.URLUtils;
import io.realm.RealmChangeListener;
import io.realm.RealmResults;

/**
 * This class is a realm recycler view adapter and manage the creation and binding of bookmark UI items.
 */
public class BookmarksAdapter extends RecyclerView.Adapter<BookmarksAdapter.BookmarksViewHolder> {

    public static final String NO_FILTER = null;

    private final Context mContext;
    private final BookmarkOnClickHandler mClickHandler;
    private final DataChangesListener mChangesListener;
    private final List<Bookmark> mSortedData;
    private final List<Bookmark> mFilteredData;

    private RealmResults<Bookmark> mData;
    private Comparator<Bookmark> mComparator;
    private String mFilter;

    /**
     * The interface to be implemented to receive on click events.
     */
    public interface BookmarkOnClickHandler {
        void onFavoriteClick(Bookmark bookmark);
        void onBookmarkClick(Bookmark bookmark);
        void onLongBookmarkClick(Bookmark bookmark);
    }

    /**
     * Constructor.
     * @param context
     * @param clickHandler
     */
    public BookmarksAdapter(@NonNull Context context,
                            @Nullable BookmarkOnClickHandler clickHandler) {
        mContext = context;
        mClickHandler = clickHandler;
        mSortedData = new ArrayList<>();
        mFilteredData = new ArrayList<>();
        mChangesListener = new DataChangesListener();
        mFilter = NO_FILTER;
    }

    public void swapData(RealmResults<Bookmark> data) {
        if (mData != null) mData.removeChangeListener(mChangesListener);
        mSortedData.clear();
        mData = data;
        if (mData != null) {
            mData.addChangeListener(mChangesListener);
            mSortedData.addAll(mData);
            sort();
            filter();
        }
        notifyDataSetChanged();
    }

    private void sort() {
        // TODO: perform this in a background thread
        if (mComparator != null) Collections.sort(mSortedData, mComparator);
    }

    private void filter() {
        mFilteredData.clear();
        if (mFilter != NO_FILTER) {
            for (Bookmark bm : mSortedData) {
                if (bm.getTitle().toLowerCase().contains(mFilter)) {
                    mFilteredData.add(bm);
                }
            }
        } else {
            mFilteredData.addAll(mSortedData);
        }
    }

    public void setComparator(Comparator<Bookmark> comparator) {
        mComparator = comparator;
        sort();
        filter();
        notifyDataSetChanged();
    }

    public void setFilter(String filter) {
        mFilter = TextUtils.isEmpty(filter) ? NO_FILTER : filter.toLowerCase();
        filter();
        notifyDataSetChanged();
    }

    public String getFilter() {
        return mFilter;
    }

    @Override
    public BookmarksViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        int layoutId = R.layout.bookmark_list_item;
        LayoutInflater inflater = LayoutInflater.from(mContext);
        boolean shouldAttachToParent = false;
        View view = inflater.inflate(layoutId, parent, shouldAttachToParent);
        return new BookmarksViewHolder(view);
    }

    @Override
    public void onBindViewHolder(BookmarksViewHolder holder, int position) {
        Bookmark bookmark = mFilteredData.get(position);
        holder.mBookmarkName.setText(bookmark.getTitle());
        Drawable favoriteDrawable = bookmark.isFavorite() ?
                ContextCompat.getDrawable(mContext, R.drawable.ic_favorite_on)
              : ContextCompat.getDrawable(mContext, R.drawable.ic_favorite_off);
        holder.mFavoriteButton.setImageDrawable(favoriteDrawable);
        String url = URLUtils.processURL(bookmark.getUrl());
        String faviconPath = URLUtils.getFaviconPath(url);
        // add no poster art meanwhile Picasso is loading the poster
        Drawable noFavicon = ContextCompat.getDrawable(mContext, R.drawable.art_no_favicon);
        if (faviconPath != null) {
            Picasso.with(mContext)
                    .load(faviconPath)
                    .placeholder(noFavicon)
                    .into(holder.mBookmarkFavicon);
        } else {
            holder.mBookmarkFavicon.setImageDrawable(noFavicon);
        }
        boolean isTheLastOne = position == getItemCount() - 1;
        holder.mSeparator.setVisibility(isTheLastOne ? View.INVISIBLE : View.VISIBLE);
        holder.itemView.setTag(bookmark.getId());
    }

    @Override
    public int getItemCount() {
        return mFilteredData.size();
    }

    private class DataChangesListener implements RealmChangeListener<RealmResults<Bookmark>> {
        @Override
        public void onChange(RealmResults<Bookmark> element) {
            mSortedData.clear();
            mSortedData.addAll(element);
            sort();
            filter();
            notifyDataSetChanged();
        }
    }

    /**
     * Cache of the children views for a bookmark list item.
     */
    public class BookmarksViewHolder extends RecyclerView.ViewHolder {

        public final TextView mBookmarkName;
        public final ImageView mBookmarkFavicon;
        public final View mSeparator;
        private final ImageView mFavoriteButton;

        public BookmarksViewHolder(View view) {
            super(view);
            mBookmarkName = (TextView) view.findViewById(R.id.tv_bookmark_item_name);
            mBookmarkFavicon = (ImageView) view.findViewById(R.id.iv_bookmark_item_favicon);
            mFavoriteButton = (ImageView) view.findViewById(R.id.iv_bookmark_item_favorite);
            mSeparator = view.findViewById(R.id.bookmark_item_separator);

            // register listeners
            mFavoriteButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int adapterPosition = getAdapterPosition();
                    Bookmark bookmark = mFilteredData.get(adapterPosition);
                    if (mClickHandler != null) mClickHandler.onFavoriteClick(bookmark);
                }
            });

            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int adapterPosition = getAdapterPosition();
                    Bookmark bookmark = mFilteredData.get(adapterPosition);
                    if (mClickHandler != null) mClickHandler.onBookmarkClick(bookmark);
                }
            });

            view.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    int adapterPosition = getAdapterPosition();
                    Bookmark bookmark = mFilteredData.get(adapterPosition);
                    if (mClickHandler != null) mClickHandler.onLongBookmarkClick(bookmark);
                    return true;
                }
            });
        }
    }
}
