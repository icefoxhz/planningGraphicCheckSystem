package com.hz.web.entity;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * @author saber
 */

@Data
@AllArgsConstructor
public class ExportBatchInsertErrData {
    private String dataId;
    private String errorMsg;
}
