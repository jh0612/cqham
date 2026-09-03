package com.reiwaxr.cq.cqham.utils.converter;

import java.nio.file.Path;

/**
 * Markdown转换器接口
 */
public interface DocumentToMarkdownConverter {
    String convert(Path filePath) throws Exception;
}
