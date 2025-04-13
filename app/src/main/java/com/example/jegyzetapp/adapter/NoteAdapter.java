package com.example.jegyzetapp.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.jegyzetapp.R;
import com.example.jegyzetapp.model.Note;

import java.text.SimpleDateFormat;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class NoteAdapter extends RecyclerView.Adapter<NoteAdapter.NoteViewHolder> {

    public interface OnNoteClickListener {
        void onNoteClick(Note note);
    }

    public interface DeletionListener {
        void onNoteLongClick(Note note);
    }

    public interface SelectionToggleCallback {
        void onSelectionToggled(String noteId);
    }

    private List<Note> noteList;
    private Set<String> selectedIds = new HashSet<>();
    private OnNoteClickListener clickListener;
    private DeletionListener deletionListener;
    private SelectionToggleCallback selectionToggleCallback;

    public NoteAdapter(List<Note> noteList,
                       OnNoteClickListener clickListener,
                       DeletionListener deletionListener,
                       SelectionToggleCallback selectionToggleCallback) {
        this.noteList = noteList;
        this.clickListener = clickListener;
        this.deletionListener = deletionListener;
        this.selectionToggleCallback = selectionToggleCallback;
    }

    @NonNull
    @Override
    public NoteAdapter.NoteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_card_note, parent, false);
        return new NoteViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull NoteAdapter.NoteViewHolder holder, int position) {
        Note note = noteList.get(position);
        holder.bind(note, this);
    }

    @Override
    public int getItemCount() {
        return noteList.size();
    }

    public Set<String> getSelectedIds() {
        return selectedIds;
    }

    public void clearSelection() {
        selectedIds.clear();
        notifyDataSetChanged();
    }

    public void toggleSelection(Note note) {
        if (note == null || note.getId() == null) return;

        String noteId = note.getId();
        if (selectedIds.contains(noteId)) {
            selectedIds.remove(noteId);
        } else {
            selectedIds.add(noteId);
        }
        if (selectionToggleCallback != null) {
            selectionToggleCallback.onSelectionToggled(noteId);
        }
        notifyDataSetChanged();
    }

    public class NoteViewHolder extends RecyclerView.ViewHolder {
        private TextView tvTitle;
        private TextView tvContent;
        private TextView tvDate;

        public NoteViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvNoteTitle);
            tvContent = itemView.findViewById(R.id.tvNoteContent);
            tvDate = itemView.findViewById(R.id.tvNoteDate);
        }

        public void bind(final Note note, final NoteAdapter adapter) {
            tvTitle.setText(note.getTitle());
            tvContent.setText(note.getContent());

            if (note.getCreationDate() != null) {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
                tvDate.setText(sdf.format(note.getCreationDate()));
            } else {
                tvDate.setText("");
            }

            if (adapter.selectedIds.contains(note.getId())) {
                itemView.setBackgroundResource(R.drawable.note_item_selected_background);
            } else {
                itemView.setBackgroundResource(R.drawable.note_item_normal_background);
            }

            itemView.setOnClickListener(v -> {
                // Ha éppen van kijelölt elem, akkor többkijelölés lehetséges
                if (!adapter.selectedIds.isEmpty()) {
                    adapter.toggleSelection(note);
                } else {
                    adapter.clickListener.onNoteClick(note);
                }
            });

            itemView.setOnLongClickListener(v -> {
                adapter.toggleSelection(note);
                return true;
            });
        }
    }
}