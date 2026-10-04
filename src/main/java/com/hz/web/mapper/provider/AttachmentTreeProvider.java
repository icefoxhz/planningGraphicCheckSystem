package com.hz.web.mapper.provider;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.jdbc.SQL;

/**
 * @author saber
 */
public class AttachmentTreeProvider {
    public String deleteNode(@Param("nodeId") int nodeId, @Param("layerId") int layerId, @Param("entityId") int entityId){
        // 删除该节点 和 它的所有子节点
        SQL sql = new SQL().DELETE_FROM("attachmentTree")
                .WHERE(String.format("node_id = %d and layer_id = %d and entity_id = %d", nodeId, layerId, entityId));
        return  sql.toString();
    }

    public String deleteNodeById(@Param("nodeId") int nodeId){
        // 删除该节点 和 它的所有子节点
        SQL sql = new SQL().DELETE_FROM("attachmentTree")
                .WHERE(String.format("node_id = %d", nodeId));
        return  sql.toString();
    }

    public String getSelfAndAllChildren(@Param("nodeId") int nodeId){
        return "WITH RECURSIVE sub_nodes (node_id) AS (\n" +
                "    -- 初始选择节点X\n" +
                "    SELECT node_id\n" +
                "    FROM ATTACHMENTTREE\n" +
                "    WHERE node_id = #{nodeId}\n" +
                "\n" +
                "    UNION ALL\n" +
                "\n" +
                "    -- 递归查询子节点\n" +
                "    SELECT a.node_id\n" +
                "    FROM ATTACHMENTTREE a\n" +
                "             INNER JOIN sub_nodes b ON a.parent_id = b.node_id\n" +
                ")\n" +
                "SELECT * FROM sub_nodes";
    }
}
