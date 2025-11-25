package com.hippo.ehviewer.ui.scene.translation.part;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.hippo.ehviewer.R;
import com.hippo.ehviewer.translation.TranslationApi;
import com.hippo.ehviewer.translation.TranslationQueueManager;
import com.hippo.ehviewer.translation.TranslationTaskInfo;
import com.hippo.ehviewer.ui.scene.translation.TranslationQueueScene;
import com.hippo.ehviewer.client.EhCacheKeyFactory;
import com.hippo.widget.LoadImageView;
import android.widget.ProgressBar;

import java.util.List;

public class TranslationAdapter extends RecyclerView.Adapter<TranslationAdapter.Holder> {

    private final LayoutInflater inflater;
    private final TranslationQueueManager manager;

    public TranslationAdapter(TranslationQueueScene scene, TranslationQueueManager manager) {
        this.inflater = LayoutInflater.from(scene.getContext());
        this.manager = manager;
        setHasStableIds(true);
    }

    @Override
    public long getItemId(int position) {
        List<TranslationTaskInfo> list = manager.getList();
        if (list == null || position < 0 || position >= list.size()) return 0;
        return list.get(position).gid;
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(inflater.inflate(R.layout.item_translation, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        List<TranslationTaskInfo> list = manager.getList();
        if (list == null || position < 0 || position >= list.size()) return;
        TranslationTaskInfo info = list.get(position);
        holder.title.setText("[" + info.jobId + "] " + info.title);
        String stateText = info.stateText();
        String cur = manager.getCurrentJobId();
        if (cur != null && cur.equals(info.jobId)) {
            stateText = stateText + "（当前）";
        }
        holder.state.setText(stateText);
        if (info.downloading) {
            holder.progressBar.setVisibility(View.VISIBLE);
            holder.percent.setVisibility(View.VISIBLE);
            holder.progressBar.setIndeterminate(false);
            holder.progressBar.setMax(100);
            holder.progressBar.setProgress(info.downloadProgress);
            holder.percent.setText(info.downloadProgress + "%");
        } else if (info.downloaded && info.state == TranslationTaskInfo.State.Completed) {
            holder.progressBar.setVisibility(View.GONE);
            holder.percent.setVisibility(View.GONE);
        } else {
            holder.progressBar.setVisibility(View.VISIBLE);
            holder.percent.setVisibility(View.VISIBLE);
            holder.progressBar.setIndeterminate(false);
            holder.progressBar.setMax(100);
            holder.progressBar.setProgress(info.progress);
            holder.percent.setText(info.progress + "%");
        }
        if (info.thumb != null) {
            holder.thumb.load(EhCacheKeyFactory.getThumbKey(info.gid), info.thumb, null, true, false);
        }
        boolean canceled = info.state == TranslationTaskInfo.State.Canceled;
        holder.itemView.setAlpha(canceled ? 0.5f : 1f);
        holder.itemView.setEnabled(!canceled);
        boolean canCancel = info.state != TranslationTaskInfo.State.Completed;
        holder.cancel.setEnabled(canCancel);
        holder.cancel.setOnClickListener(v -> {
            TranslationApi.cancelCurrentAsync((ok, e) -> {
                // 无论成功失败，状态将通过轮询更新；这里仅禁用按钮以避免重复操作
                holder.cancel.setEnabled(false);
            });
        });

        holder.itemView.setOnClickListener(v -> {
            android.content.Context ctx = v.getContext();
            com.hippo.ehviewer.download.DownloadManager dm = com.hippo.ehviewer.EhApplication.getDownloadManager(ctx);
            com.hippo.ehviewer.dao.DownloadInfo di = dm.getDownloadInfo(info.gid);
            if (di == null) di = new com.hippo.ehviewer.dao.DownloadInfo(info.gid);
            android.content.Intent intent = new android.content.Intent(ctx, com.hippo.ehviewer.ui.GalleryActivity.class);
            intent.setAction(com.hippo.ehviewer.ui.GalleryActivity.ACTION_EH);
            intent.putExtra(com.hippo.ehviewer.ui.GalleryActivity.KEY_GALLERY_INFO, di);
            if (info.singlePage && info.pageIndex >= 0) {
                intent.putExtra(com.hippo.ehviewer.ui.GalleryActivity.KEY_PAGE, info.pageIndex);
            }
            ctx.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        List<TranslationTaskInfo> list = manager.getList();
        return list == null ? 0 : list.size();
    }

    public static class Holder extends RecyclerView.ViewHolder {
        public final LoadImageView thumb;
        public final TextView title;
        public final TextView state;
        public final TextView percent;
        public final ProgressBar progressBar;
        public final View cancel;

        public Holder(@NonNull View itemView) {
            super(itemView);
            thumb = itemView.findViewById(R.id.thumb);
            title = itemView.findViewById(R.id.title);
            state = itemView.findViewById(R.id.state);
            percent = itemView.findViewById(R.id.percent);
            progressBar = itemView.findViewById(R.id.progress_bar);
            cancel = itemView.findViewById(R.id.cancel);
        }
    }
}
