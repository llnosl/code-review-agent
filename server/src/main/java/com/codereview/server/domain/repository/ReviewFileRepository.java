package com.codereview.server.domain.repository;

import com.codereview.server.domain.model.ReviewFile;
import java.util.List;

/**
 * 审查文件存储接口。
 */
public interface ReviewFileRepository {

    /**
     * 批量保存审查文件。
     */
    void saveAll(List<ReviewFile> reviewFiles);
}
