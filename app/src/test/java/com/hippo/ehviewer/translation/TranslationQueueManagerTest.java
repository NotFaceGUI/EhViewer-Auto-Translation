package com.hippo.ehviewer.translation;

import com.hippo.ehviewer.dao.DownloadInfo;
import org.junit.Assert;
import org.junit.Test;

public class TranslationQueueManagerTest {
    @Test
    public void testEnqueueAndRemove() {
        TranslationQueueManager manager = TranslationQueueManager.getInstance();
        DownloadInfo info = new DownloadInfo();
        info.gid = 1;
        info.title = "t";
        manager.enqueue(info);
        Assert.assertEquals(1, manager.getList().size());
        manager.remove(1);
        Assert.assertEquals(0, manager.getList().size());
    }
}
