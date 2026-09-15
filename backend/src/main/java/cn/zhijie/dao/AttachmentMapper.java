package cn.zhijie.dao;

import cn.zhijie.pojo.entity.*;
import cn.zhijie.pojo.query.*;
import java.util.*;

public interface AttachmentMapper {
    void insertAttachment(InsertAttachmentCommand p);
    AttachmentEntity attachment(UUID id);
    AttachmentEntity contentAttachment(ContentAttachmentCommand p);
    boolean attachmentPublished(UUID id);
}
