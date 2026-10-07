package com.codereview.server.application;

import com.codereview.server.domain.model.ReviewSourceType;
import java.util.List;
import java.util.Objects;

/**
 * 创建审查任务命令。
 *
 * @param sourceType 代码来源类型
 * @param files 待审查文件
 */
public record CreateReviewTaskCommand(
        ReviewSourceType sourceType,
        List<CreateReviewFileCommand> files) {

    /**
     * 校验并固化任务输入。
     */
    public CreateReviewTaskCommand {
        sourceType = Objects.requireNonNull(sourceType, "代码来源类型不能为空");
        if (files == null || files.isEmpty()) {
            throw new IllegalArgumentException("待审查文件不能为空");
        }
        if (files.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("待审查文件不能包含空值");
        }
        if (sourceType == ReviewSourceType.CODE_TEXT && files.size() != 1) {
            throw new IllegalArgumentException("粘贴代码只能创建一个审查文件");
        }
        files = List.copyOf(files);
    }
}
