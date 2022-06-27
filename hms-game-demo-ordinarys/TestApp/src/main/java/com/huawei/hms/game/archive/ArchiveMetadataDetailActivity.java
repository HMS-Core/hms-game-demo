
/*
 * Copyright 2020. Huawei Technologies Co., Ltd. All rights reserved.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.huawei.hms.game.archive;

import java.io.IOException;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.huawei.hmf.tasks.OnFailureListener;
import com.huawei.hmf.tasks.OnSuccessListener;
import com.huawei.hmf.tasks.Task;
import com.huawei.hms.R;
import com.huawei.hms.common.ApiException;
import com.huawei.hms.game.common.BaseActivity;
import com.huawei.hms.game.common.TimeUtil;
import com.huawei.hms.jos.games.ArchivesClient;
import com.huawei.hms.jos.games.Games;
import com.huawei.hms.jos.games.archive.Archive;
import com.huawei.hms.jos.games.archive.ArchiveSummary;
import com.huawei.hms.jos.games.archive.OperationResult;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.support.annotation.Nullable;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class ArchiveMetadataDetailActivity extends BaseActivity {
    @BindView(R.id.archive_cover_image)
    public ImageView archiveCoverImage;

    @BindView(R.id.archive_id)
    public TextView archiveIdTv;

    @BindView(R.id.archive_name)
    public TextView archiveNameTv;

    @BindView(R.id.archive_des)
    public TextView descriptionTv;

    @BindView(R.id.modify_time)
    public TextView modifyTimeTv;

    @BindView(R.id.progress)
    public TextView progressTv;

    @BindView(R.id.played_time)
    public TextView playedTimeTV;

    private String archiveId;

    private String description;

    private long playedTime;

    private long progress;

    private ArchivesClient archivesClient;

    private ArchiveSummary archiveSummary;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState, @Nullable PersistableBundle persistentState) {
        super.onCreate(savedInstanceState, persistentState);
        archivesClient = Games.getArchiveClient(this);
    }

    private ArchivesClient getArchivesClient() {
        if (archivesClient == null) {
            archivesClient = Games.getArchiveClient(this);
        }
        return archivesClient;
    }

    /**
     * Open archive metadata
     * *
     * 打开存档元数据
     */
    @OnClick(R.id.btn_archive_open)
    public void openArchive() {
        int conflictPolicy = getConflictPolicy();
        Task<OperationResult> task;
        if (conflictPolicy == -1) {
            /*
             * Use archive ID to open archive metadata, does not support the specified conflict
             * strategy.
             * 使用存档ID打开存档元数据，不支持指定冲突策略。
             */
            task = getArchivesClient().loadArchiveDetails(archiveId);
        } else {
            /*
             * Use archive ID to open archive metadata and support specifying conflict strategies.
             * *
             * 使用存档ID打开存档元数据，支持指定冲突策略。
             */
            task = getArchivesClient().loadArchiveDetails(archiveId, conflictPolicy);
        }

        task.addOnSuccessListener(new OnSuccessListener<OperationResult>() {
            @Override
            public void onSuccess(OperationResult archiveDataOrConflict) {
                showLog(
                    "isDifference:" + ((archiveDataOrConflict == null) ? "" : archiveDataOrConflict.isDifference()));
                if (archiveDataOrConflict != null && !archiveDataOrConflict.isDifference()) {
                    Archive archive = archiveDataOrConflict.getArchive();
                    if (archive != null && archive.getSummary() != null) {
                        showLog("ArchiveId:" + archive.getSummary().getId());
                        try {
                            showLog("content:" + new String(archive.getDetails().get(), "UTF-8"));
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        showLog("Description:" + archive.getSummary().getDescInfo());
                        showLog("UniqueName:" + archive.getSummary().getFileName());
                        showLog("PlayedTime:" + archive.getSummary().getActiveTime());
                        showLog("ProgressValue:" + archive.getSummary().getCurrentProgress());
                        showLog("ModifiedTimestamp:" + TimeUtil.longToUTC(archive.getSummary().getRecentUpdateTime()));
                        showLog("CoverImageAspectRatio:" + archive.getSummary().getThumbnailRatio());
                        showLog("hasThumbnail:" + archive.getSummary().hasThumbnail());
                        final RequestOptions options =
                            new RequestOptions().diskCacheStrategy(DiskCacheStrategy.NONE).skipMemoryCache(true);
                        if (archive.getSummary().hasThumbnail()) {
                            Task<Bitmap> coverImageTask =
                                getArchivesClient().getThumbnail(archive.getSummary().getId());
                            coverImageTask.addOnSuccessListener(new OnSuccessListener<Bitmap>() {
                                @Override
                                public void onSuccess(Bitmap bitmap) {
                                    Glide.with(getApplicationContext())
                                        .load(bitmap)
                                        .apply(options)
                                        .into(archiveCoverImage);
                                }
                            }).addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(Exception e) {
                                    if (e instanceof ApiException) {
                                        Toast
                                            .makeText(getApplicationContext(),
                                                "load image " + "failed" + ((ApiException) e).getStatusCode(),
                                                Toast.LENGTH_SHORT)
                                            .show();
                                    }
                                }
                            });
                        }
                        showPlayerAndGame(archive.getSummary());
                    }
                } else {
                    handleConflict(archiveDataOrConflict);
                }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(Exception e) {
                ApiException apiException = (ApiException) e;
                showLog("loadArchiveDetails result:" + apiException.getStatusCode());
            }
        });
    }

    /**
     * Handle conflict.
     * *
     * 处理冲突
     *
     * @param archiveDataOrConflict
     */
    private void handleConflict(OperationResult archiveDataOrConflict) {
        if (archiveDataOrConflict != null) {
            OperationResult.Difference archiveConflict = archiveDataOrConflict.getDifference();
            Archive openedArchive = archiveConflict.getRecentArchive();
            Archive serverArchive = archiveConflict.getServerArchive();
            showConflictDialog(openedArchive, serverArchive, new ConflictResolveCallback() {
                @Override
                public void onResult(Archive archive) {
                    if (archive == null) {
                        return;
                    }
                    /*
                     * Use archived data to resolve data conflicts in an asynchronous
                     * process. This method will replace conflicted archived data with the
                     * specified Archive.
                     * *
                     * 使用存档数据以异步方式解决数据冲突，此方法将使用指定的Archive替换冲突的存档数据。
                     */
                    Task<OperationResult> task = getArchivesClient().updateArchive(archive);
                    task.addOnSuccessListener(new OnSuccessListener<OperationResult>() {
                        @Override
                        public void onSuccess(OperationResult archiveDataOrConflict) {
                            showLog("isDifference:"
                                + ((archiveDataOrConflict == null) ? "" : archiveDataOrConflict.isDifference()));
                            if (archiveDataOrConflict != null && !archiveDataOrConflict.isDifference()) {
                                Archive archive = archiveDataOrConflict.getArchive();
                                if (archive != null && archive.getSummary() != null) {
                                    showLog("ArchiveId:" + archive.getSummary().getId());
                                    try {
                                        showLog("content:" + new String(archive.getDetails().get(), "UTF-8"));
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                    showLog("Description:" + archive.getSummary().getDescInfo());
                                    showLog("UniqueName:" + archive.getSummary().getFileName());
                                    showLog("PlayedTime:" + archive.getSummary().getActiveTime());
                                    showLog("ProgressValue:" + archive.getSummary().getCurrentProgress());
                                    showLog("ModifiedTimestamp:" + archive.getSummary().getRecentUpdateTime());
                                    showLog("CoverImageAspectRatio:" + archive.getSummary().getThumbnailRatio());
                                    showLog("hasThumbnail:" + archive.getSummary().hasThumbnail());
                                    final RequestOptions options =
                                        new RequestOptions().diskCacheStrategy(DiskCacheStrategy.NONE)
                                            .skipMemoryCache(true);
                                    if (archive.getSummary().hasThumbnail()) {
                                        Task<Bitmap> coverImageTask =
                                            getArchivesClient().getThumbnail(archive.getSummary().getId());
                                        coverImageTask.addOnSuccessListener(new OnSuccessListener<Bitmap>() {
                                            @Override
                                            public void onSuccess(Bitmap bitmap) {
                                                Glide.with(getApplicationContext())
                                                    .load(bitmap)
                                                    .apply(options)
                                                    .into(archiveCoverImage);
                                            }
                                        }).addOnFailureListener(new OnFailureListener() {
                                            @Override
                                            public void onFailure(Exception e) {
                                                if (e instanceof ApiException) {
                                                    Toast
                                                        .makeText(getApplicationContext(),
                                                            "load" + " image failed"
                                                                + ((ApiException) e).getStatusCode(),
                                                            Toast.LENGTH_SHORT)
                                                        .show();
                                                }
                                            }
                                        });
                                    }
                                    showPlayerAndGame(archive.getSummary());
                                }
                            } else {

                                handleConflict(archiveDataOrConflict);
                            }
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(Exception e) {
                            ApiException apiException = (ApiException) e;
                            showLog("resolve result:" + apiException.getStatusCode());
                        }
                    });
                }
            });
        }
    }

    public interface ConflictResolveCallback {
        void onResult(Archive archive);
    }

    private String getArchiveMessage(Archive archive) {
        return "PlayedTime:" + archive.getSummary().getActiveTime() + "\n" + "Progress:"
            + archive.getSummary().getCurrentProgress() + "\n" + "Description:" + archive.getSummary().getDescInfo()
            + "\n" + "ModifyTime:" + archive.getSummary().getRecentUpdateTime();
    }

    /**
     * Show conflict dialog
     * *
     * 显示冲突提示弹窗
     *
     * @param openArchive The Archive object to be modified when a conflict occurs.
     * @param serverArchive Archive data that already exists in the game server.
     * @param callback Resolve conflict callback
     */
    private void showConflictDialog(final Archive openArchive, final Archive serverArchive,
        final ConflictResolveCallback callback) {

        if (openArchive == null || serverArchive == null) {
            return;
        }

        final ConflictDialog dialog = new ConflictDialog(this);

        String recentMessage = getArchiveMessage(openArchive);
        String serverMessage = getArchiveMessage(serverArchive);

        dialog.setRecentMessage(recentMessage)
            .setServerMessage(serverMessage)
            .setTitle("ResolveConflict")
            .setOnClickBottomListener(new ConflictDialog.OnClickBottomListener() {
                @Override
                public void onPositiveClick(int type) {
                    if (type == ConflictDialog.TYPE_ARCHIVE_RECENT) {
                        callback.onResult(openArchive);
                        Toast
                            .makeText(ArchiveMetadataDetailActivity.this, "chose opened " + "archive",
                                Toast.LENGTH_SHORT)
                            .show();
                    } else if (type == ConflictDialog.TYPE_ARCHIVE_SERVER) {
                        callback.onResult(serverArchive);
                        Toast
                            .makeText(ArchiveMetadataDetailActivity.this, "chose server " + "archive",
                                Toast.LENGTH_SHORT)
                            .show();
                    }
                    dialog.dismiss();
                }

                @Override
                public void onNegativeClick() {
                    callback.onResult(null);
                    dialog.dismiss();
                }
            })
            .show();
    }

    /**
     * Delete archive records, including Huawei game servers and locally cached archive records.
     * The Huawei game server side is deleted according to the ID of the archive record, and the
     * local cache is deleted according to the name of the archive record.
     * *
     * 删除存档记录，包括华为游戏服务器和本地缓存的存档记录。华为游戏服务器侧根据存档记录的ID删除，本地缓存根据
     * 存档记录的名称删除。
     */
    @OnClick(R.id.btn_delete)
    public void delete() {
        Task<String> task = getArchivesClient().removeArchive(archiveSummary);

        task.addOnSuccessListener(new OnSuccessListener<String>() {
            @Override
            public void onSuccess(String s) {
                Toast.makeText(ArchiveMetadataDetailActivity.this, "removeArchive success: " + s, Toast.LENGTH_SHORT)
                    .show();
                finish();
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(Exception e) {
                if (e instanceof ApiException) {
                    ApiException apiException = (ApiException) e;
                    Toast
                        .makeText(ArchiveMetadataDetailActivity.this,
                            "removeArchive failed, " + "rtnCode:" + apiException.getStatusCode(), Toast.LENGTH_SHORT)
                        .show();
                    finish();
                }
            }
        });
    }

    /**
     * Open commit archive activity to update
     * *
     * 跳转提交档案界面进行更新
     */
    @OnClick(R.id.btn_update)
    public void update() {
        Intent intent = new Intent(this, CommitArchiveActivity.class);
        Bundle data = new Bundle();
        data.putLong("progressValue", progress);
        data.putLong("playedTime", playedTime);
        data.putString("description", description);
        data.putString("archiveId", archiveId);
        data.putBoolean("hasThumbnail", archiveSummary.hasThumbnail());
        data.putString("id", archiveSummary.getId());
        intent.putExtras(data);
        startActivityForResult(intent, 1000);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_archive_detail);
        ButterKnife.bind(this);
        initData();
    }

    private void initData() {
        Intent intent = getIntent();
        Bundle data = intent.getExtras();
        if (data != null) {
            archiveSummary = data.getParcelable("archiveSummary");
            if (archiveSummary != null) {
                archiveId = archiveSummary.getId();
                description = archiveSummary.getDescInfo();
                progress = archiveSummary.getCurrentProgress();
                playedTime = archiveSummary.getActiveTime();

                archiveIdTv.setText(String.format("ArchiveId:%s", archiveId));
                archiveNameTv.setText(String.format("UniqueName:%s", archiveSummary.getFileName()));
                descriptionTv.setText(String.format("Description:%s", description));
                progressTv.setText(String.format("ProgressValue:%s", progress + ""));
                playedTimeTV.setText(String.format("PlayedTime:%s", playedTime + ""));
                modifyTimeTv.setText(
                    String.format("LastModifyTime:%s", TimeUtil.longToUTC(archiveSummary.getRecentUpdateTime())));
                modifyTimeTv.setText(String.format("PlayerId:%s",
                    archiveSummary.getGamePlayer() != null ? archiveSummary.getGamePlayer().getPlayerId() : ""));
                modifyTimeTv.setText(String.format("DisplayName:%s",
                    archiveSummary.getGamePlayer() != null ? archiveSummary.getGamePlayer().getDisplayName() : ""));
            }
        }
    }

    private int getConflictPolicy() {
        Spinner etAmount = findViewById(R.id.conflict_policy);
        if (etAmount != null) {
            String text = etAmount.getSelectedItem().toString();
            switch (text) {
                case "manual":
                    return -1;
                case "longPlayedTime":
                    return 3;
                case "recentlyModified":
                    return 2;
                case "heightProgress":
                    return 1;
                case "other":
                    return 0;
            }
        } else {
            return -1;
        }
        return -1;
    }
}
